package com.ionres.respondph.sendsms;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.common.interfaces.BulkProgressListener;
import com.ionres.respondph.util.SMSApi;
import com.ionres.respondph.util.SMSSender;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SmsServiceImpl implements SmsService {
    private volatile BulkProgressListener bulkProgressListener;
    private final SmsDAO smsDAO;
    private final SMSApi smsApi;
    private final SMSSender smsSender;

    private final Object bulkLock = new Object();
    private boolean gsmBulkActive = false;
    private int gsmTotal = 0;
    private int gsmDone = 0;
    private int gsmSuccess = 0;

    public SmsServiceImpl() {
        this.smsDAO = new SmsDAOImpl();
        this.smsApi = new SMSApi();
        this.smsSender = SMSSender.getInstance();

        this.smsSender.addSendResultListener(this::handleSendResult);
    }

    public void setBulkProgressListener(BulkProgressListener listener) {
        this.bulkProgressListener = listener;
    }

    public SmsServiceImpl(SmsDAO smsDAO) {
        this.smsDAO = smsDAO;
        this.smsApi = new SMSApi();
        this.smsSender = SMSSender.getInstance();

        this.smsSender.addSendResultListener(this::handleSendResult);
    }

    @Override
    public void handleSendResult(SMSSender.SendResult result) {
        try {
            SmsModel smsModel = new SmsModel();
            smsModel.setBeneficiaryID(result.getBeneficiaryId());
            smsModel.setDateSent(new Timestamp(result.getTimestamp()));
            smsModel.setFullname(result.getFullname());
            smsModel.setPhonenumber(result.getPhoneNumber());
            smsModel.setPhoneString(result.getPhoneNumber());
            smsModel.setMessage(result.getMessage());
            smsModel.setStatus(result.isSuccess() ? "SENT" : "FAILED");
            smsModel.setSendMethod("GSM");

            System.out.println("DEBUG: GSM send result - Success: " + result.isSuccess() +
                    ", Phone: " + result.getPhoneNumber());

            smsDAO.saveSMS(smsModel);
        } catch (Exception e) {
            System.err.println("Error saving SMS result to database: " + e.getMessage());
            e.printStackTrace();
        }

        boolean active;
        int done, total, success;

        synchronized (bulkLock) {
            active = gsmBulkActive;
            if (!active) return;

            gsmDone++;
            if (result.isSuccess()) gsmSuccess++;

            done = gsmDone;
            total = gsmTotal;
            success = gsmSuccess;
        }

        notifyProgress(done, total, success, "GSM");
        endGsmBulkSessionIfDone();
    }

    @Override
    public boolean sendSingleSMS(String phoneNumber, String fullname, String message, String method) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            System.err.println("Cannot send SMS: phone number is empty");
            return false;
        }

        if (message == null || message.trim().isEmpty()) {
            System.err.println("Cannot send SMS: message is empty");
            return false;
        }

        System.out.println("DEBUG: Sending SMS via " + method + " to " + phoneNumber);
        System.out.println("DEBUG: Message: " + message.substring(0, Math.min(50, message.length())) + "...");

        SmsModel smsModel = new SmsModel(phoneNumber, fullname, message);
        smsModel.setSendMethod(method);
        smsModel.setStatus("SENDING");

        boolean success = false;

        try {
            if ("API".equalsIgnoreCase(method)) {
                System.out.println("DEBUG: Using API method");

                success = smsApi.sendSMS(phoneNumber.trim(), message);

                System.out.println("DEBUG: API response - Success: " + success);

                // For API, we need to manually save to DB since there's no listener
                smsModel.setStatus(success ? "SENT" : "FAILED");
                smsModel.setDateSent(Timestamp.valueOf(LocalDateTime.now()));
                smsDAO.saveSMS(smsModel);

            } else if ("GSM".equalsIgnoreCase(method)) {
                System.out.println("DEBUG: Using GSM method");

                if (!smsSender.isConnected()) {
                    System.err.println("GSM modem not connected");
                    return false;
                }

                // For GSM, the listener will handle saving to DB
                List<String> numbers = List.of(phoneNumber.trim());
                List<String> names = List.of(fullname != null ? fullname : "");
                List<Integer> ids = List.of(0);

                int sent = smsSender.sendBulkSMS(numbers, names, ids, message, 0, 3);
                success = sent > 0;

                System.out.println("DEBUG: GSM send result - Sent: " + sent + " messages");

            } else {
                System.err.println("Unknown send method: " + method);
                return false;
            }

        } catch (Exception e) {
            System.err.println("Error sending SMS: " + e.getMessage());
            e.printStackTrace();

            // Save failed attempt for API method
            if ("API".equalsIgnoreCase(method)) {
                smsModel.setStatus("FAILED");
                smsModel.setDateSent(Timestamp.valueOf(LocalDateTime.now()));
                smsDAO.saveSMS(smsModel);
            }
        }

        return success;
    }

    @Override
    public int sendBulkSMS(List<BeneficiaryModel> beneficiaries, String message, String method) {
        if (beneficiaries == null || beneficiaries.isEmpty()) {
            System.err.println("No beneficiaries to send SMS to");
            notifyFinished(0, 0, method);
            return 0;
        }

        System.out.println("DEBUG: Starting bulk SMS send to " + beneficiaries.size() +
                " beneficiaries via " + method);

        int successCount = 0;

        if ("API".equalsIgnoreCase(method)) {

            int total = 0;
            // count valid recipients
            for (BeneficiaryModel b : beneficiaries) {
                String phone = b.getMobileNumber();
                if (phone != null && !phone.trim().isEmpty()) total++;
            }

            int done = 0;
            notifyProgress(0, total, 0, "API");

            for (BeneficiaryModel beneficiary : beneficiaries) {
                String phone = beneficiary.getMobileNumber();
                if (phone == null || phone.trim().isEmpty()) continue;

                String fullname = buildFullName(beneficiary);

                boolean sent = sendSingleSMS(phone.trim(), fullname, message, "API");
                done++;
                if (sent) successCount++;

                notifyProgress(done, total, successCount, "API");

                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            notifyFinished(total, successCount, "API");
            return successCount;

        } else if ("GSM".equalsIgnoreCase(method)) {

            if (!smsSender.isConnected()) {
                System.err.println("GSM modem not connected");
                notifyFinished(0, 0, "GSM");
                return 0;
            }

            List<String> numbers = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();

            for (BeneficiaryModel beneficiary : beneficiaries) {
                String phone = beneficiary.getMobileNumber();
                if (phone == null || phone.trim().isEmpty()) continue;

                numbers.add(phone.trim());
                names.add(buildFullName(beneficiary));
                ids.add(beneficiary.getId());
            }

            int total = numbers.size();
            if (total == 0) {
                notifyFinished(0, 0, "GSM");
                return 0;
            }

            startGsmBulkSession(total);
            System.out.println("DEBUG: Sending bulk GSM to " + total + " recipients");
            smsSender.sendBulkSMS(numbers, names, ids, message, 1000, 3);

            return 0;
        }

        System.err.println("Unknown method: " + method);
        notifyFinished(0, 0, method);
        return 0;
    }

    @Override
    public boolean resendSMS(SmsModel sms, String method) {
        if (sms == null) {
            System.err.println("Cannot resend: SMS model is null");
            return false;
        }

        String phone = sms.getPhonenumber();
        if (phone == null || phone.trim().isEmpty()) {
            System.err.println("Cannot resend: phone number is empty");
            return false;
        }

        System.out.println("DEBUG: Resending SMS to " + phone + " via " + method);

        boolean success = sendSingleSMS(
                phone.trim(),
                sms.getFullname(),
                sms.getMessage(),
                method
        );

        // Update original record status
        if (sms.getMessageID() > 0) {
            smsDAO.updateSMSStatus(sms.getMessageID(), success ? "SENT" : "FAILED");
        }

        return success;
    }

    @Override
    public List<SmsModel> getAllSMSLogs() {
        return smsDAO.getAllSMS();
    }

    @Override
    public List<SmsModel> getSMSLogsByStatus(String status) {
        return smsDAO.getSMSByStatus(status);
    }

    @Override
    public void notifyProgress(int done, int total, int success, String method) {
        BulkProgressListener l = bulkProgressListener;
        if (l != null) l.onProgress(done, total, success, method);
    }

    @Override
    public void notifyFinished(int total, int success, String method) {
        BulkProgressListener l = bulkProgressListener;
        if (l != null) l.onFinished(total, success, method);
    }

    @Override
    public void startGsmBulkSession(int total) {
        synchronized (bulkLock) {
            gsmBulkActive = true;
            gsmTotal = total;
            gsmDone = 0;
            gsmSuccess = 0;
        }
        notifyProgress(0, total, 0, "GSM");
    }

    @Override
    public void endGsmBulkSessionIfDone() {
        boolean finished;
        int total, success;

        synchronized (bulkLock) {
            finished = gsmBulkActive && gsmDone >= gsmTotal;
            total = gsmTotal;
            success = gsmSuccess;

            if (finished) {
                gsmBulkActive = false;
                gsmTotal = 0;
                gsmDone = 0;
                gsmSuccess = 0;
            }
        }

        if (finished) notifyFinished(total, success, "GSM");
    }

    @Override
    public void updateSMSStatus(int messageID, String status) {
        smsDAO.updateSMSStatus(messageID, status);
    }

    private String buildFullName(BeneficiaryModel beneficiary) {
        if (beneficiary == null) return "";

        String fn = beneficiary.getFirstname() == null ? "" : beneficiary.getFirstname();
        String mn = beneficiary.getMiddlename() == null ? "" : beneficiary.getMiddlename();
        String ln = beneficiary.getLastname() == null ? "" : beneficiary.getLastname();

        return (fn + " " + (mn.isEmpty() ? "" : mn + " ") + ln).trim();
    }
}