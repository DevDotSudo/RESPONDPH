package com.ionres.respondph.sendsms;

import java.util.List;

public interface SmsDAO {
    void saveSMS(SmsModel sms);
    List<SmsModel> getAllSMS();
    void resendSMS(SmsModel sms);
}
