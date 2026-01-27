package com.ionres.respondph.util;
import com.fazecast.jSerialComm.SerialPort;
import com.ionres.respondph.sendsms.smsDAO;
import com.ionres.respondph.sendsms.smsDAOImpl;
import com.ionres.respondph.sendsms.smsModel;
import javafx.application.Platform;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SMSSender {

    private static SMSSender instance;
    private GSMdongle dongle;

    private final ExecutorService logExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SMSSender-SMSLog");
        t.setDaemon(true);
        return t;
    });

    private final smsDAO smsDao = new smsDAOImpl();
    private final CopyOnWriteArrayList<SmsLogListener> listeners = new CopyOnWriteArrayList<>();

    public interface SmsLogListener {
        void onSmsLogSaved(smsModel log);
    }

    public void addSmsLogListener(SmsLogListener l) {
        if (l != null) listeners.addIfAbsent(l);
    }

    public void removeSmsLogListener(SmsLogListener l) {
        if (l != null) listeners.remove(l);
    }

  
    private SMSSender() {}


    public static synchronized SMSSender getInstance() {
        if (instance == null) instance = new SMSSender();
        return instance;
    }


    private static class GSMdongle {
        private final SerialPort serialPort;
        private final InputStream in;
        private final OutputStream out;

        public GSMdongle (SerialPort sp){
            this.serialPort = sp;
            this.in = sp.getInputStream();
            this.out = sp.getOutputStream();
        }

        public SerialPort getSerialPort(){
            return serialPort;
        }

        public void connect() throws Exception {
            serialPort.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 5000, 5000);
            if(!serialPort.openPort()){
                throw new IllegalStateException("Failed to open port:" + serialPort.getSystemPortName());
            }
            sendAT("ATE0", 500);
            sendAT("AT+CMGF=1",500);
            sendAT("AT+CNMI=2,2,0,0,0",500);
        }

        public void disconnect(){
            if(serialPort.isOpen())
                serialPort.closePort();
        }
        private int consecutiveFails = 1;
        public boolean sendMessage(String phoneNumber, String message) throws Exception{
            try{
                String resp = sendAT("AT+CMGS=\"" + phoneNumber + "\"", 2000);
                if(resp == null || !resp.contains(">")){
                    consecutiveFails++;
                    return false;
                }

                out.write((message + "\u001A").getBytes());
                out.flush();

                String finalResponse = readResponse(15000);
                boolean success = finalResponse != null && (finalResponse.contains("OK") || finalResponse.contains("+CMGS"));

                if(success) consecutiveFails = 1;
                else consecutiveFails++;

                return success;
            }catch(Exception e){
                consecutiveFails++;
                throw e;
            }
        }

        private String sendAT(String cmd, int timeoutMs) throws Exception{
            out.write((cmd + "\r").getBytes());
            out.flush();
            return readResponse(timeoutMs);
        }

        private String readResponse(int timeoutMs) throws IOException, InterruptedException{
            StringBuilder response = new StringBuilder();
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                while (in.available() > 0) {
                    response.append((char) in.read());
                }
                String respText = response.toString();
                if (respText.contains("OK") || respText.contains("ERROR") || respText.contains(">")) {
                    return respText;
                }
                Thread.sleep(50);
            }
            return response.toString();
        }
    }

    public synchronized List<String> getAvailablePorts(){
        SerialPort[] ports = SerialPort.getCommPorts();
        List<String> names = new ArrayList<>();
        for(SerialPort p : ports) names.add(p.getSystemPortName());
        return names;
    }


    public synchronized boolean connectToPort(String portName, int timeoutMs){
        if(portName == null) return false;
        try{
            SerialPort sp = SerialPort.getCommPort(portName);
            if(sp == null) return false;
            if(sp.isOpen()) sp.closePort();
            this.dongle = new GSMdongle(sp);
            this.dongle.connect();
            return true;
        }catch(Exception e){
            System.out.println("Failed to connect to " + portName + ": " + e.getMessage());
            this.dongle = null;
            return false;
        }
    }

    public boolean sendSingleMessage(String phoneNumber, String message) throws Exception{
        if(!isConnected()) throw new IllegalStateException("No connected GSM modem");
        return dongle.sendMessage(phoneNumber, message);
    }

    public boolean isConnected() {
        return dongle != null && dongle.getSerialPort().isOpen();
    }


    public int sendSMS(List<String> recipients, List<String> fullnames, List<Integer> beneficiaryIds, String message, int delayMsBetween, int maxRetriesPerRecipient) {
        if (recipients == null || recipients.isEmpty()) return 0;
        if (!isConnected()) throw new IllegalStateException("No connected GSM modem");
        int successCount = 0;
        for (int i = 0; i < recipients.size(); i++) {
            String phone = recipients.get(i);
            String fullname = (fullnames != null && fullnames.size() > i) ? fullnames.get(i) : null;
            int beneficiaryId = (beneficiaryIds != null && beneficiaryIds.size() > i && beneficiaryIds.get(i) != null) ? beneficiaryIds.get(i) : 0;
            if (phone == null || phone.trim().isEmpty()) continue;
            int attempts = 0;
            boolean sent = false;
            while (attempts <= maxRetriesPerRecipient && !sent) {
                attempts++;
                try {
                    sent = sendSingleMessage(phone, message);
                } catch (Exception e) {
                    System.err.println("sendSMS: error sending to " + phone + ": " + e.getMessage());
                }
                if (!sent) {
                    try { Thread.sleep(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                }
            }
            if (sent) successCount++;
            submitSmsLog(phone, fullname, message, sent, beneficiaryId);

            try { Thread.sleep(Math.max(0, delayMsBetween)); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        return successCount;
    }


    private void submitSmsLog(String phone, String fullname, String message, boolean success, int beneficiaryId) {
        try {
            smsModel log = new smsModel();
            log.setDateSent(new Timestamp(System.currentTimeMillis()));
            log.setPhonenumber(phone == null ? "" : phone);
            log.setPhoneString(phone == null ? "" : phone);
            if (beneficiaryId > 0) log.setBeneficiaryID(beneficiaryId);
            log.setFullname(fullname == null ? "" : fullname);
            log.setMessage(message == null ? "" : message);
            log.setStatus(success ? "SENT" : "FAILED");

            logExecutor.submit(() -> {
                try {
                    smsDao.saveSMS(log);
                    Platform.runLater(() -> {
                        for (SmsLogListener l : listeners) {
                            try {
                                l.onSmsLogSaved(log);
                            } catch (Throwable t) {
                                System.err.println("SmsLogListener threw: " + t.getMessage());
                            }
                        }
                    });
                } catch (Throwable t) {
                    System.err.println("Failed to save sms log: " + t.getMessage());
                    t.printStackTrace();
                }
            });
        } catch (Throwable t) {
            System.err.println("submitSmsLog failed: " + t.getMessage());
            t.printStackTrace();
        }
    }

    public boolean resendSMS(smsModel log, String portName, int timeoutMs) {
        if (log == null) return false;
        if (!isConnected()) {
            if (portName == null) {
                System.err.println("resendSMS: no port available to connect");
                return false;
            }
            boolean ok = connectToPort(portName, timeoutMs);
            if (!ok) {
                System.err.println("resendSMS: failed to connect to port " + portName);
                return false;
            }
        }

        String phone = log.getPhoneString() != null && !log.getPhoneString().isEmpty() ? log.getPhoneString() : log.getPhonenumber();
        String message = log.getMessage() == null ? "" : log.getMessage();
        boolean sent = false;
        try {
            sent = sendSingleMessage(phone, message);
        } catch (Throwable t) {
            System.err.println("resendSMS: error while sending to " + phone + " -> " + t.getMessage());
            sent = false;
        }

        if (sent) {
            try {
                log.setStatus("SENT");
                smsDao.resendSMS(log);
            } catch (Throwable t) {
                System.err.println("resendSMS: failed to update DB status: " + t.getMessage());
            }

            final smsModel updated = log;
             Platform.runLater(() -> {
                 for (SmsLogListener l : listeners) {
                     try {
                         l.onSmsLogSaved(updated);
                     } catch (Throwable t) {
                         System.err.println("SmsLogListener threw: " + t.getMessage());
                     }
                 }
             });
         }

         return sent;
     }

}
