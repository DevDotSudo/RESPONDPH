package com.ionres.respondph.sendsms;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class SmsModel {
    private int messageID;
    private int beneficiaryID;
    private Timestamp dateSent;
    private String phonenumber;
    private String fullname;
    private String message;
    private String status;
    private String phoneString;
    private String sendMethod; // "GSM" or "API"

    // Constructors
    public SmsModel() {
    }

    public SmsModel(String phonenumber, String fullname, String message) {
        this.phonenumber = phonenumber;
        this.fullname = fullname;
        this.message = message;
        this.dateSent = Timestamp.valueOf(LocalDateTime.now());
        this.status = "PENDING";
        this.sendMethod = "GSM";
    }

    // Getters and Setters
    public int getMessageID() {
        return messageID;
    }

    public void setMessageID(int messageID) {
        this.messageID = messageID;
    }

    public int getBeneficiaryID() {
        return beneficiaryID;
    }

    public void setBeneficiaryID(int beneficiaryID) {
        this.beneficiaryID = beneficiaryID;
    }

    public Timestamp getDateSent() {
        return dateSent;
    }

    public void setDateSent(Timestamp dateSent) {
        this.dateSent = dateSent;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhoneString() {
        return phoneString;
    }

    public void setPhoneString(String phoneString) {
        this.phoneString = phoneString;
    }

    public String getSendMethod() {
        return sendMethod;
    }

    public void setSendMethod(String sendMethod) {
        this.sendMethod = sendMethod;
    }
}