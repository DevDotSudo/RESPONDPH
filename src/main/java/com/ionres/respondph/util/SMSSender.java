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

    private final CopyOnWriteArrayList<Consumer<SendResult>> sendResultListeners = new CopyOnWriteArrayList<>();

    public static class SendResult {
        private final String phoneNumber;
        private final String fullname;
        private final String message;
        private final boolean success;
        private final int beneficiaryId;
        private final long timestamp;
        private final String errorMessage;

        public SendResult(String phoneNumber, String fullname, String message,
                          boolean success, int beneficiaryId, String errorMessage) {
            this.phoneNumber = phoneNumber;
            this.fullname = fullname;
            this.message = message;
            this.success = success;
            this.beneficiaryId = beneficiaryId;
            this.timestamp = System.currentTimeMillis();
            this.errorMessage = errorMessage;
        }

        // Getters
        public String getPhoneNumber() { return phoneNumber; }
        public String getFullname() { return fullname; }
        public String getMessage() { return message; }
        public boolean isSuccess() { return success; }
        public int getBeneficiaryId() { return beneficiaryId; }
        public long getTimestamp() { return timestamp; }
        public String getErrorMessage() { return errorMessage; }
    }

    private SMSSender() {}

    public static synchronized SMSSender getInstance() {
        if (instance == null) instance = new SMSSender();
        return instance;
    }

    /**
     * Add a listener to be notified when SMS send operations complete
     */
    public void addSendResultListener(Consumer<SendResult> listener) {
        if (listener != null) sendResultListeners.addIfAbsent(listener);
    }

    public void removeSendResultListener(Consumer<SendResult> listener) {
        if (listener != null) sendResultListeners.remove(listener);
    }

    private void notifyListeners(SendResult result) {
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

    /**
     * GSM Dongle wrapper for serial communication
     */
    private static class GSMDongle {
        private final SerialPort serialPort;
        private final InputStream in;
        private final OutputStream out;
        private int consecutiveFails = 0;

        public GSMDongle(SerialPort sp) {
            this.serialPort = sp;
            this.in = sp.getInputStream();
            this.out = sp.getOutputStream();
        }

        public SerialPort getSerialPort() {
            return serialPort;
        }

        public void connect() throws Exception {
            serialPort.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 5000, 5000);

            if (!serialPort.openPort()) {
                throw new IllegalStateException("Failed to open port: " + serialPort.getSystemPortName());
            }

            // Initialize modem
            sendAT("ATE0", 500);           // Echo off
            sendAT("AT+CMGF=1", 500);      // Text mode
            sendAT("AT+CNMI=2,2,0,0,0", 500); // New message indication
        }

        public void disconnect() {
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
            }
        }

        public boolean sendMessage(String phoneNumber, String message) throws Exception {
            try {
                // Request to send SMS
                String resp = sendAT("AT+CMGS=\"" + phoneNumber + "\"", 2000);
                if (resp == null || !resp.contains(">")) {
                    consecutiveFails++;
                    return false;
                }

                // Send message content + Ctrl+Z
                out.write((message + "\u001A").getBytes());
                out.flush();

                // Wait for confirmation
                String finalResponse = readResponse(15000);
                boolean success = finalResponse != null &&
                        (finalResponse.contains("OK") || finalResponse.contains("+CMGS"));

                if (success) {
                    consecutiveFails = 0;
                } else {
                    consecutiveFails++;
                }

                return success;
            } catch (Exception e) {
                consecutiveFails++;
                throw e;
            }
        }

        public int getConsecutiveFails() {
            return consecutiveFails;
        }

        private String sendAT(String cmd, int timeoutMs) throws Exception {
            out.write((cmd + "\r").getBytes());
            out.flush();
            return readResponse(timeoutMs);
        }

        private String readResponse(int timeoutMs) throws IOException, InterruptedException {
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

    /**
     * Get list of available serial ports
     */
    public synchronized List<String> getAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        List<String> names = new ArrayList<>();
        for (SerialPort p : ports) {
            names.add(p.getSystemPortName());
        }
        return names;
    }

    /**
     * Connect to a specific serial port
     */
    public synchronized boolean connectToPort(String portName, int timeoutMs) {
        if (portName == null || portName.trim().isEmpty()) {
            System.err.println("connectToPort: invalid port name");
            return false;
        }

        try {
            // Disconnect existing connection
            if (dongle != null) {
                dongle.disconnect();
                dongle = null;
            }

            SerialPort sp = SerialPort.getCommPort(portName);
            if (sp == null) {
                System.err.println("connectToPort: port not found: " + portName);
                return false;
            }

            if (sp.isOpen()) {
                sp.closePort();
            }

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

    /**
     * Disconnect from current port
     */
    public synchronized void disconnect() {
        if (dongle != null) {
            dongle.disconnect();
            dongle = null;
        }
    }

    /**
     * Check if modem is connected
     */
    public synchronized boolean isConnected() {
        return dongle != null && dongle.getSerialPort().isOpen();
    }

    /**
     * Get the currently connected port name
     */
    public synchronized String getConnectedPort() {
        if (dongle != null && dongle.getSerialPort() != null) {
            return dongle.getSerialPort().getSystemPortName();
        }
        return null;
    }

    /**
     * Send a single SMS message
     * @return true if sent successfully
     */
    public synchronized boolean sendSingleMessage(String phoneNumber, String message) throws Exception {
        if (!isConnected()) {
            throw new IllegalStateException("No connected GSM modem");
        }

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }

        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }

        return dongle.sendMessage(phoneNumber, message);
    }

    public int sendBulkSMS(List<String> recipients, List<String> fullnames,
                           List<Integer> beneficiaryIds, String message,
                           int delayMsBetween, int maxRetriesPerRecipient) {

        if (recipients == null || recipients.isEmpty()) {
            System.err.println("sendBulkSMS: No recipients provided");
            return 0;
        }

        if (!isConnected()) {
            throw new IllegalStateException("No connected GSM modem");
        }

        int successCount = 0;

        for (int i = 0; i < recipients.size(); i++) {
            String phone = recipients.get(i);
            String fullname = (fullnames != null && fullnames.size() > i) ? fullnames.get(i) : "";
            int beneficiaryId = (beneficiaryIds != null && beneficiaryIds.size() > i && beneficiaryIds.get(i) != null)
                    ? beneficiaryIds.get(i) : 0;

            if (phone == null || phone.trim().isEmpty()) {
                continue;
            }

            boolean sent = false;
            String errorMessage = null;
            int attempts = 0;

            while (attempts < maxRetriesPerRecipient && !sent) {
                attempts++;
                try {
                    sent = sendSingleMessage(phone.trim(), message);
                    if (!sent) {
                        errorMessage = "Send failed (attempt " + attempts + ")";
                    }
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    System.err.println("sendBulkSMS: error sending to " + phone +
                            " (attempt " + attempts + "): " + e.getMessage());
                }

                if (!sent && attempts < maxRetriesPerRecipient) {
                    try {
                        Thread.sleep(500); // Wait before retry
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            if (sent) {
                successCount++;
            }

            SendResult result = new SendResult(phone, fullname, message, sent, beneficiaryId, errorMessage);
            notifyListeners(result);

            if (i < recipients.size() - 1) {
                try {
                    Thread.sleep(Math.max(0, delayMsBetween));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return successCount;
    }

    public synchronized int getConsecutiveFails() {
        return dongle != null ? dongle.getConsecutiveFails() : 0;
    }
}