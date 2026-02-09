package com.ionres.respondph.sendsms;

import java.util.List;

/**
 * Data Access Object interface for SMS operations
 */
public interface SmsDAO {

    /**
     * Save an SMS record to the database
     * @param sms The SMS model to save
     */
    void saveSMS(SmsModel sms);

    /**
     * Get all SMS records
     * @return List of all SMS records
     */
    List<SmsModel> getAllSMS();

    /**
     * Get SMS records by status
     * @param status The status to filter by (SENT, FAILED, SENDING)
     * @return List of SMS records with the specified status
     */
    List<SmsModel> getSMSByStatus(String status);

    /**
     * Get SMS records by phone number
     * @param phoneNumber The phone number to search for
     * @return List of SMS records for that phone number
     */
    List<SmsModel> getSMSByPhoneNumber(String phoneNumber);

    void updateSMSStatus(int messageID, String status);

    /**
     * Delete an SMS record
     * @param messageID The message ID to delete
     */
    void deleteSMS(int messageID);

    /**
     * Get a specific SMS record by ID
     * @param messageID The message ID
     * @return The SMS model, or null if not found
     */
    SmsModel getSMSById(int messageID);
}