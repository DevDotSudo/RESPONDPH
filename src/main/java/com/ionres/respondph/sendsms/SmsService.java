package com.ionres.respondph.sendsms;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import java.util.List;

public interface SmsService {
    boolean sendSingleSMS(String phoneNumber, String fullname, String message, String method);
    int sendBulkSMS(List<BeneficiaryModel> beneficiaries, String message, String method);
    boolean resendSMS(SmsModel sms, String method);
    List<SmsModel> getAllSMSLogs();
    List<SmsModel> getSMSLogsByStatus(String status);
    void updateSMSStatus(int messageID, String status);
}