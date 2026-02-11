package com.ionres.respondph.util;


public class CustomEvacMessageManager {
    private static CustomEvacMessageManager instance;
    private String customEvacuationMessage;

    private CustomEvacMessageManager() {
        this.customEvacuationMessage = null;
    }

    public static synchronized CustomEvacMessageManager getInstance() {
        if (instance == null) {
            instance = new CustomEvacMessageManager();
        }
        return instance;
    }


    public void setCustomEvacuationMessage(String message) {
        this.customEvacuationMessage = message;
    }


    public String getCustomEvacuationMessage() {
        return this.customEvacuationMessage;
    }


    public boolean hasCustomMessage() {
        return this.customEvacuationMessage != null && !this.customEvacuationMessage.trim().isEmpty();
    }


    public void clearCustomMessage() {
        this.customEvacuationMessage = null;
    }


    public String builtInMessage(String beneficiaryName, String evacuationSite) {
        if (!hasCustomMessage()) {
            return beneficiaryName + ", Go to " + evacuationSite + " to Evacuate.";
        }

        return customEvacuationMessage
                .replace("{name}", beneficiaryName != null ? beneficiaryName : "")
                .replace("{evacSite}", evacuationSite != null ? evacuationSite : "")
                .replace("{site}", evacuationSite != null ? evacuationSite : "");
    }
}