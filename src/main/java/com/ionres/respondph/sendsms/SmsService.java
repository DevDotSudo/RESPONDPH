package com.ionres.respondph.sendsms;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.common.interfaces.BulkProgressListener;
import com.ionres.respondph.util.SMSSender;

import java.util.List;

public interface SmsService {

    boolean sendSingleSMS(String phoneNumber, String fullname, String message, String method);
    int sendBulkSMS(List<BeneficiaryModel> beneficiaries, String message, String method);
    boolean resendSMS(SmsModel sms, String method);
    void cancelBulkSend();
    List<SmsModel> getAllSMSLogs();
    List<SmsModel> getSMSLogsByStatus(String status);
    void notifyProgress(int done, int total, int success, String method);
    void notifyFinished(int total, int success, String method);
    void startGsmBulkSession(int total);
    void endGsmBulkSessionIfDone();
    void handleSendResult(SMSSender.SendResult result);
    void updateSMSStatus(int messageID, String status);
}