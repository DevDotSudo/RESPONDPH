package com.ionres.respondph.sendsms;

import java.util.List;

public interface smsDAO {

    void saveSMS(smsModel sms);
    List<smsModel> getAllSMS();
    void resendSMS(smsModel sms);
}
