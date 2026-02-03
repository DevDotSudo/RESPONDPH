package com.ionres.respondph.sendsms;

import java.util.List;

public interface SmsDAO {
    void saveSMS(SmsModel sms);
    List<SmsModel> getAllSMS();
    List<SmsModel> getSMSByStatus(String status);
    List<SmsModel> getSMSByPhoneNumber(String phoneNumber);
    void updateSMSStatus(int messageID, String status);
    void deleteSMS(int messageID);
    SmsModel getSMSById(int messageID);
}