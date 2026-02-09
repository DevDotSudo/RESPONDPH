package com.ionres.respondph.sendsms;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import java.util.List;

/**
 * Service interface for SMS operations
 */
public interface SmsService {

    /**
     * Send a single SMS
     * @param phoneNumber The recipient's phone number
     * @param fullname The recipient's full name
     * @param message The message content
     * @param method The sending method (API or GSM)
     * @return true if successful, false otherwise
     */
    boolean sendSingleSMS(String phoneNumber, String fullname, String message, String method);

    /**
     * Send SMS to multiple beneficiaries
     * @param beneficiaries List of beneficiaries to send to
     * @param message The message content
     * @param method The sending method (API or GSM)
     * @return Number of successfully sent messages
     */
    int sendBulkSMS(List<BeneficiaryModel> beneficiaries, String message, String method);

    /**
     * Resend a failed SMS
     * @param sms The SMS to resend
     * @param method The sending method (API or GSM)
     * @return true if successful, false otherwise
     */
    boolean resendSMS(SmsModel sms, String method);

    /**
     * Get all SMS logs
     * @return List of all SMS records
     */
    List<SmsModel> getAllSMSLogs();

    /**
     * Get SMS logs by status
     * @param status The status to filter by
     * @return List of SMS records with the specified status
     */
    List<SmsModel> getSMSLogsByStatus(String status);

    /**
     * Update SMS status
     * @param messageID The message ID
     * @param status The new status
     */
    void updateSMSStatus(int messageID, String status);
}