package com.ionres.respondph.util;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SMSSender {

    private static SMSSender instance;
    private GSMDongle dongle;

    // ── Cancel flag — volatile so background thread sees writes immediately ───
    private volatile boolean cancelRequested = false;

    private final CopyOnWriteArrayList<Consumer<SendResult>> sendResultListeners
            = new CopyOnWriteArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    // SendResult
    // ─────────────────────────────────────────────────────────────────────────

    public static class SendResult {
        private final String  phoneNumber;
        private final String  fullname;
        private final String  message;
        private final boolean success;
        private final int     beneficiaryId;
        private final long    timestamp;
        private final String  errorMessage;

        public SendResult(String phoneNumber, String fullname, String message,
                          boolean success, int beneficiaryId, String errorMessage) {
            this.phoneNumber   = phoneNumber;
            this.fullname      = fullname;
            this.message       = message;
            this.success       = success;
            this.beneficiaryId = beneficiaryId;
            this.timestamp     = System.currentTimeMillis();
            this.errorMessage  = errorMessage;
        }

        public String  getPhoneNumber()  { return phoneNumber; }
        public String  getFullname()     { return fullname; }
        public String  getMessage()      { return message; }
        public boolean isSuccess()       { return success; }
        public int     getBeneficiaryId(){ return beneficiaryId; }
        public long    getTimestamp()    { return timestamp; }
        public String  getErrorMessage() { return errorMessage; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Singleton
    // ─────────────────────────────────────────────────────────────────────────

    private SMSSender() {}

    public static synchronized SMSSender getInstance() {
        if (instance == null) instance = new SMSSender();
        return instance;
    }

    public void requestCancel() {
        cancelRequested = true;
        System.out.println("[SMSSender] Cancel requested.");
    }

    /** Reset the cancel flag — must be called before every new bulk send. */
    public void resetCancel() {
        cancelRequested = false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Listeners
    // ─────────────────────────────────────────────────────────────────────────

    public void addSendResultListener(Consumer<SendResult> listener) {
        if (listener != null) sendResultListeners.addIfAbsent(listener);
    }

    public void removeSendResultListener(Consumer<SendResult> listener) {
        if (listener != null) sendResultListeners.remove(listener);
    }

    private void notifyListeners(SendResult result) {
        // Fire on the JavaFX thread so listeners can safely touch UI / DAO
        Platform.runLater(() -> {
            for (Consumer<SendResult> listener : sendResultListeners) {
                try {
                    listener.accept(result);
                } catch (Throwable t) {
                    System.err.println("SendResultListener threw: " + t.getMessage());
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GSM Dongle (inner class)
    // ─────────────────────────────────────────────────────────────────────────

    private static class GSMDongle {
        private final SerialPort   serialPort;
        private final InputStream  in;
        private final OutputStream out;
        private int consecutiveFails = 0;

        public GSMDongle(SerialPort sp) {
            this.serialPort = sp;
            this.in  = sp.getInputStream();
            this.out = sp.getOutputStream();
        }

        public SerialPort getSerialPort() { return serialPort; }

        public void connect() throws Exception {
            serialPort.setComPortParameters(9600, 8,
                    SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);

            // Important: shorter reads for probing; your readResponse handles timeout anyway
            serialPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 700, 700);

            if (!serialPort.openPort())
                throw new IllegalStateException(
                        "Failed to open port: " + serialPort.getSystemPortName());

            try {
                verifyGsmModem();

                // After verified, apply your normal init
                sendAT("ATE0",              500);   // Echo off
                sendAT("AT+CMGF=1",         800);   // Text mode
                sendAT("AT+CNMI=2,2,0,0,0", 800);   // New message indication

            } catch (Exception e) {
                // ❗ If probe fails, close port so app doesn't think it's connected
                try { serialPort.closePort(); } catch (Exception ignored) {}
                throw e;
            }
        }

        public void disconnect() {
            if (serialPort != null && serialPort.isOpen())
                serialPort.closePort();
        }

        public boolean sendMessage(String phoneNumber, String message) throws Exception {
            try {
                String resp = sendAT("AT+CMGS=\"" + phoneNumber + "\"", 2000);
                if (resp == null || !resp.contains(">")) {
                    consecutiveFails++;
                    return false;
                }

                out.write((message + "\u001A").getBytes());
                out.flush();

                String finalResponse = readResponse(15000);
                boolean success = finalResponse != null &&
                        (finalResponse.contains("OK") || finalResponse.contains("+CMGS"));

                if (success) consecutiveFails = 0;
                else         consecutiveFails++;

                return success;
            } catch (Exception e) {
                consecutiveFails++;
                throw e;
            }
        }

        public int getConsecutiveFails() { return consecutiveFails; }

        private String sendAT(String cmd, int timeoutMs) throws Exception {
            out.write((cmd + "\r").getBytes());
            out.flush();
            return readResponse(timeoutMs);
        }

        private String readResponse(int timeoutMs) throws IOException, InterruptedException {
            StringBuilder response  = new StringBuilder();
            long          startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                while (in.available() > 0) response.append((char) in.read());

                String respText = response.toString();
                if (respText.contains("OK") || respText.contains("ERROR")
                        || respText.contains(">"))
                    return respText;

                Thread.sleep(50);
            }
            return response.toString();
        }

        private static String norm(String s) {
            if (s == null) return "";
            // normalize CR/LF, remove echoed command noise
            return s.replace("\r", "\n");
        }

        private boolean hasOK(String resp) {
            String r = norm(resp).toUpperCase();
            return r.contains("\nOK") || r.trim().endsWith("OK") || r.contains("OK\n");
        }

        private boolean hasError(String resp) {
            String r = norm(resp).toUpperCase();
            return r.contains("ERROR") || r.contains("+CME ERROR") || r.contains("+CMS ERROR");
        }

        /** strict probe: must behave like a modem */
        private void verifyGsmModem() throws Exception {
            // flush possible garbage
            try { while (in.available() > 0) in.read(); } catch (Exception ignored) {}

            String r1 = sendAT("AT", 800);
            if (!hasOK(r1) || hasError(r1)) {
                throw new IllegalStateException("Port is not responding to AT (not GSM modem). Resp=" + r1);
            }

            // Identify
            String r2 = sendAT("ATI", 1200);
            if (!hasOK(r2) || r2.trim().length() < 3) {
                throw new IllegalStateException("Device didn't return valid ATI response (not GSM modem). Resp=" + r2);
            }

            // Manufacturer / Model check
            String r3 = sendAT("AT+CGMI", 1200);
            String r4 = sendAT("AT+CGMM", 1200);

            // If both fail to contain OK, reject.
            if (!(hasOK(r3) || hasOK(r4))) {
                throw new IllegalStateException("Device is not a GSM modem (CGMI/CGMM failed).");
            }

            // Optional SIM readiness check (don’t fail hard for modems without SIM inserted)
            String sim = sendAT("AT+CPIN?", 1200);
            if (hasError(sim)) {
                // Some modules need SIM ready later; not fatal
                System.out.println("[GSMDongle] CPIN check error (ignored): " + sim);
            }
        }
    }

    public synchronized List<String> getGsmPortsOnly() {
        SerialPort[] ports = SerialPort.getCommPorts();
        List<String> gsm = new ArrayList<>();

        for (SerialPort p : ports) {
            try {
                SerialPort sp = SerialPort.getCommPort(p.getSystemPortName());
                GSMDongle test = new GSMDongle(sp);
                test.connect();          // will probe
                test.disconnect();
                gsm.add(p.getSystemPortName());
            } catch (Exception ignored) {
                // not GSM
            }
        }
        return gsm;
    }

//    // ─────────────────────────────────────────────────────────────────────────
//    // Port management
//    // ─────────────────────────────────────────────────────────────────────────
//
//    public synchronized List<String> getAvailablePorts() {
//        SerialPort[] ports = SerialPort.getCommPorts();
//        List<String> names = new ArrayList<>();
//        for (SerialPort p : ports) names.add(p.getSystemPortName());
//        return names;
//    }

    public synchronized boolean connectToPort(String portName, int timeoutMs) {
        if (portName == null || portName.trim().isEmpty()) {
            System.err.println("connectToPort: invalid port name");
            return false;
        }
        try {
            if (dongle != null) { dongle.disconnect(); dongle = null; }

            SerialPort sp = SerialPort.getCommPort(portName);
            if (sp == null) {
                System.err.println("connectToPort: port not found: " + portName);
                return false;
            }
            if (sp.isOpen()) sp.closePort();

            this.dongle = new GSMDongle(sp);
            this.dongle.connect();
            System.out.println("Successfully connected to " + portName);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to connect to " + portName + ": " + e.getMessage());
            e.printStackTrace();
            this.dongle = null;
            return false;
        }
    }

    public synchronized void disconnect() {
        if (dongle != null) { dongle.disconnect(); dongle = null; }
    }

    public synchronized boolean isConnected() {
        return dongle != null && dongle.getSerialPort().isOpen();
    }

    public synchronized String getConnectedPort() {
        return (dongle != null && dongle.getSerialPort() != null)
                ? dongle.getSerialPort().getSystemPortName() : null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Single send
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized boolean sendSingleMessage(String phoneNumber,
                                                  String message) throws Exception {
        if (!isConnected())
            throw new IllegalStateException("No connected GSM modem");
        if (phoneNumber == null || phoneNumber.trim().isEmpty())
            throw new IllegalArgumentException("Phone number cannot be empty");
        if (message == null || message.trim().isEmpty())
            throw new IllegalArgumentException("Message cannot be empty");

        return dongle.sendMessage(phoneNumber, message);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bulk send
    // ─────────────────────────────────────────────────────────────────────────

    public int sendBulkSMS(List<String> recipients, List<String> fullnames,
                           List<Integer> beneficiaryIds, String message,
                           int delayMsBetween, int maxRetriesPerRecipient) {

        if (recipients == null || recipients.isEmpty()) {
            System.err.println("sendBulkSMS: No recipients provided");
            return 0;
        }
        if (!isConnected())
            throw new IllegalStateException("No connected GSM modem");

        // Always start clean — SmsServiceImpl.sendBulkSMS already called resetCancel(),
        // but a direct caller might not, so we reset here too.
        cancelRequested = false;

        int successCount = 0;

        for (int i = 0; i < recipients.size(); i++) {

            if (cancelRequested) {
                System.out.println("[SMSSender] Bulk send cancelled after "
                        + successCount + " of " + recipients.size() + " messages.");
                break;
            }

            String phone = recipients.get(i);
            String fullname = (fullnames != null && fullnames.size() > i)
                    ? fullnames.get(i) : "";
            int beneficiaryId = (beneficiaryIds != null && beneficiaryIds.size() > i
                    && beneficiaryIds.get(i) != null)
                    ? beneficiaryIds.get(i) : 0;

            if (phone == null || phone.trim().isEmpty()) continue;

            boolean sent         = false;
            String  errorMessage = null;
            int     attempts     = 0;

            while (attempts < maxRetriesPerRecipient && !sent) {
                // ── Also check cancel inside the retry loop ───────────────────
                if (cancelRequested) {
                    errorMessage = "Cancelled";
                    break;
                }

                attempts++;
                try {
                    sent = sendSingleMessage(phone.trim(), message);
                    if (!sent) errorMessage = "Send failed (attempt " + attempts + ")";
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    System.err.println("[SMSSender] Error sending to " + phone
                            + " (attempt " + attempts + "): " + e.getMessage());
                }

                if (!sent && attempts < maxRetriesPerRecipient && !cancelRequested) {
                    try { Thread.sleep(500); }
                    catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            if (sent) successCount++;

            notifyListeners(new SendResult(
                    phone, fullname, message, sent, beneficiaryId, errorMessage));

            if (!cancelRequested && i < recipients.size() - 1) {
                try { Thread.sleep(Math.max(0, delayMsBetween)); }
                catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        cancelRequested = false;
        return successCount;
    }

    public synchronized int getConsecutiveFails() {
        return dongle != null ? dongle.getConsecutiveFails() : 0;
    }
}