package com.ionres.respondph.sendsms;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.util.SMSSender;

import java.util.List;

public interface SmsService {

    boolean sendSingleSMS(String phoneNumber, String fullname, String message, String method);

    int sendBulkSMS(List<BeneficiaryModel> beneficiaries, String message, String method);

    boolean resendSMS(SmsModel sms, String method);

    List<SmsModel> getAllSMSLogs();

    List<SmsModel> getSMSLogsByStatus(String status);
    public void notifyProgress(int done, int total, int success, String method);
    public void notifyFinished(int total, int success, String method);
    public void startGsmBulkSession(int total);
    public void endGsmBulkSessionIfDone();
    public void handleSendResult(SMSSender.SendResult result);
    void updateSMSStatus(int messageID, String status);
}