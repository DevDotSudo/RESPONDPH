package com.ionres.respondph.sendsms;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.common.interfaces.BulkProgressListener;
import com.ionres.respondph.util.SMSApi;
import com.ionres.respondph.util.SMSSender;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SmsServiceImpl implements SmsService {

    private volatile BulkProgressListener bulkProgressListener;
    private final SmsDAO    smsDAO;
    private final SMSApi    smsApi;
    private final SMSSender smsSender;

    // ── Cancel flag ───────────────────────────────────────────────────────────
    private final AtomicBoolean bulkCancelled = new AtomicBoolean(false);

    // ── GSM bulk session state (guarded by bulkLock) ──────────────────────────
    private final Object bulkLock      = new Object();
    private boolean      gsmBulkActive = false;
    private int          gsmTotal      = 0;
    private int          gsmDone       = 0;
    private int          gsmSuccess    = 0;

    // ─────────────────────────────────────────────────────────────────────────
    // Constructors
    // ─────────────────────────────────────────────────────────────────────────

    public SmsServiceImpl() {
        this.smsDAO    = new SmsDAOImpl();
        this.smsApi    = new SMSApi();
        this.smsSender = SMSSender.getInstance();
        this.smsSender.addSendResultListener(this::handleSendResult);
    }

    public SmsServiceImpl(SmsDAO smsDAO) {
        this.smsDAO    = smsDAO;
        this.smsApi    = new SMSApi();
        this.smsSender = SMSSender.getInstance();
        this.smsSender.addSendResultListener(this::handleSendResult);
    }

    public void setBulkProgressListener(BulkProgressListener listener) {
        this.bulkProgressListener = listener;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void cancelBulkSend() {
        bulkCancelled.set(true);
        smsSender.requestCancel();          // signals the GSM modem loop
        System.out.println("[SmsService] Cancel requested.");

        // If a GSM bulk session is active, force-finish it so the UI unblocks.
        // We read current counters under lock and then call notifyFinished outside.
        int total, done, success;
        boolean wasActive;
        synchronized (bulkLock) {
            wasActive = gsmBulkActive;
            total     = gsmTotal;
            done      = gsmDone;
            success   = gsmSuccess;
            if (wasActive) {
                gsmBulkActive = false;
                gsmTotal      = 0;
                gsmDone       = 0;
                gsmSuccess    = 0;
            }
        }
        if (wasActive) {
            // Notify finished with a "cancelled" summary so the UI toast closes
            String summary = "Cancelled after " + done + " of " + total;
            notifyFinished(done, success, summary);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GSM SEND-RESULT LISTENER
    // Called by SMSSender on the background thread for each GSM send attempt.
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void handleSendResult(SMSSender.SendResult result) {
        // Persist to DB
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

            System.out.println("DEBUG: GSM send result - Success: " + result.isSuccess()
                    + ", Phone: " + result.getPhoneNumber());

            smsDAO.saveSMS(smsModel);
        } catch (Exception e) {
            System.err.println("Error saving SMS result to database: " + e.getMessage());
            e.printStackTrace();
        }

        // Update GSM session counters
        boolean active;
        int done, total, success;
        synchronized (bulkLock) {
            active = gsmBulkActive;
            if (!active) return;    // session already closed (e.g. cancelled)

            gsmDone++;
            if (result.isSuccess()) gsmSuccess++;

            done    = gsmDone;
            total   = gsmTotal;
            success = gsmSuccess;
        }

        notifyProgress(done, total, success, "GSM");
        endGsmBulkSessionIfDone();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SINGLE SEND
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean sendSingleSMS(String phoneNumber, String fullname,
                                 String message, String method) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            System.err.println("Cannot send SMS: phone number is empty");
            return false;
        }
        if (message == null || message.trim().isEmpty()) {
            System.err.println("Cannot send SMS: message is empty");
            return false;
        }

        System.out.println("DEBUG: Sending SMS via " + method + " to " + phoneNumber);

        SmsModel smsModel = new SmsModel(phoneNumber, fullname, message);
        smsModel.setSendMethod(method);
        smsModel.setStatus("SENDING");

        boolean success = false;

        try {
            if ("API".equalsIgnoreCase(method)) {
                success = smsApi.sendSMS(phoneNumber.trim(), message);
                smsModel.setStatus(success ? "SENT" : "FAILED");
                smsModel.setDateSent(Timestamp.valueOf(LocalDateTime.now()));
                smsDAO.saveSMS(smsModel);

            } else if ("GSM".equalsIgnoreCase(method)) {
                if (!smsSender.isConnected()) {
                    System.err.println("GSM modem not connected");
                    return false;
                }
                // Single send via GSM — listener handles DB persistence
                List<String>  numbers = List.of(phoneNumber.trim());
                List<String>  names   = List.of(fullname != null ? fullname : "");
                List<Integer> ids     = List.of(0);
                int sent = smsSender.sendBulkSMS(numbers, names, ids, message, 0, 3);
                success = sent > 0;

            } else {
                System.err.println("Unknown send method: " + method);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error sending SMS: " + e.getMessage());
            e.printStackTrace();
            if ("API".equalsIgnoreCase(method)) {
                smsModel.setStatus("FAILED");
                smsModel.setDateSent(Timestamp.valueOf(LocalDateTime.now()));
                smsDAO.saveSMS(smsModel);
            }
        }

        return success;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BULK SEND
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public int sendBulkSMS(List<BeneficiaryModel> beneficiaries,
                           String message, String method) {
        if (beneficiaries == null || beneficiaries.isEmpty()) {
            System.err.println("No beneficiaries to send SMS to");
            notifyFinished(0, 0, method);
            return 0;
        }

        // Always reset the cancel flag at the start of a fresh bulk send
        bulkCancelled.set(false);
        smsSender.resetCancel();

        System.out.println("DEBUG: Starting bulk SMS send to " + beneficiaries.size()
                + " beneficiaries via " + method);

        // ── API path ──────────────────────────────────────────────────────────
        if ("API".equalsIgnoreCase(method)) {

            // Count valid recipients upfront
            int total = 0;
            for (BeneficiaryModel b : beneficiaries) {
                String phone = b.getMobileNumber();
                if (phone != null && !phone.trim().isEmpty()) total++;
            }

            int done         = 0;
            int successCount = 0;
            notifyProgress(0, total, 0, "API");

            for (BeneficiaryModel beneficiary : beneficiaries) {

                // ── Cancellation check ────────────────────────────────────────
                if (bulkCancelled.get()) {
                    System.out.println("[SmsService] API bulk cancelled at "
                            + done + "/" + total);
                    break;
                }

                String phone = beneficiary.getMobileNumber();
                if (phone == null || phone.trim().isEmpty()) continue;

                String fullname = buildFullName(beneficiary);
                boolean sent = sendSingleSMS(phone.trim(), fullname, message, "API");
                done++;
                if (sent) successCount++;

                notifyProgress(done, total, successCount, "API");

                // Delay between messages — also interruptible on cancel
                if (!bulkCancelled.get()) {
                    try { Thread.sleep(300); }
                    catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            final boolean wasCancelled = bulkCancelled.get();
            bulkCancelled.set(false);

            String summary = wasCancelled
                    ? "Cancelled after " + done + " of " + total
                    : successCount + " sent, " + (done - successCount) + " failed";
            notifyFinished(done, successCount, summary);
            return successCount;
        }

        // ── GSM path ──────────────────────────────────────────────────────────
        if ("GSM".equalsIgnoreCase(method)) {

            if (!smsSender.isConnected()) {
                System.err.println("GSM modem not connected");
                notifyFinished(0, 0, "GSM");
                return 0;
            }

            List<String>  numbers = new ArrayList<>();
            List<String>  names   = new ArrayList<>();
            List<Integer> ids     = new ArrayList<>();

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

            // SMSSender.sendBulkSMS() is blocking — it checks cancelRequested
            // between each recipient and breaks early if set.
            // handleSendResult() updates the session counters as each result arrives.
            smsSender.sendBulkSMS(numbers, names, ids, message, 1000, 3);

            // After sendBulkSMS() returns, check if the session was cancelled
            // mid-loop (cancelBulkSend() may have already closed the session,
            // so endGsmBulkSessionIfDone() is safe to call again — it's a no-op
            // if the session is already closed).
            endGsmBulkSessionIfDone();

            return 0; // GSM result is reported async via handleSendResult / notifyFinished
        }

        System.err.println("Unknown method: " + method);
        notifyFinished(0, 0, method);
        return 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESEND
    // ─────────────────────────────────────────────────────────────────────────

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
                phone.trim(), sms.getFullname(), sms.getMessage(), method);

        if (sms.getMessageID() > 0)
            smsDAO.updateSMSStatus(sms.getMessageID(), success ? "SENT" : "FAILED");

        return success;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<SmsModel> getAllSMSLogs() {
        return smsDAO.getAllSMS();
    }

    @Override
    public List<SmsModel> getSMSLogsByStatus(String status) {
        return smsDAO.getSMSByStatus(status);
    }

    @Override
    public void updateSMSStatus(int messageID, String status) {
        smsDAO.updateSMSStatus(messageID, status);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROGRESS HELPERS
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // GSM SESSION LIFECYCLE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void startGsmBulkSession(int total) {
        synchronized (bulkLock) {
            gsmBulkActive = true;
            gsmTotal      = total;
            gsmDone       = 0;
            gsmSuccess    = 0;
        }
        notifyProgress(0, total, 0, "GSM");
    }

    @Override
    public void endGsmBulkSessionIfDone() {
        boolean finished;
        int total, done, success;

        synchronized (bulkLock) {
            // Session is "done" when all expected results have arrived,
            // OR when it was already closed by cancelBulkSend().
            finished = gsmBulkActive && gsmDone >= gsmTotal;
            total    = gsmTotal;
            done     = gsmDone;
            success  = gsmSuccess;

            if (finished) {
                gsmBulkActive = false;
                gsmTotal      = 0;
                gsmDone       = 0;
                gsmSuccess    = 0;
            }
        }

        if (finished) {
            final boolean wasCancelled = bulkCancelled.get();
            bulkCancelled.set(false);
            String summary = wasCancelled
                    ? "Cancelled after " + done + " of " + total
                    : success + " sent, " + (done - success) + " failed";
            notifyFinished(done, success, summary);
        }
    }

    private String buildFullName(BeneficiaryModel beneficiary) {
        if (beneficiary == null) return "";
        String fn = beneficiary.getFirstname()  == null ? "" : beneficiary.getFirstname();
        String mn = beneficiary.getMiddlename() == null ? "" : beneficiary.getMiddlename();
        String ln = beneficiary.getLastname()   == null ? "" : beneficiary.getLastname();
        return (fn + " " + (mn.isEmpty() ? "" : mn + " ") + ln).trim();
    }
}