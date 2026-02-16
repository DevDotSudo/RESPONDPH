package com.ionres.respondph.util;

import com.ionres.respondph.sendsms.CustomEvacMessageDAO;
import com.ionres.respondph.sendsms.CustomEvacMessageDAOImpl;

import java.util.logging.Logger;


public class CustomEvacMessageManager {

    private static final Logger LOGGER = Logger.getLogger(CustomEvacMessageManager.class.getName());

    private static CustomEvacMessageManager instance;
    private final CustomEvacMessageDAO messageDAO;

    private CustomEvacMessageManager() {
        this.messageDAO = new CustomEvacMessageDAOImpl();
        LOGGER.info("CustomEvacMessageManager initialized with database support");
    }

    public static synchronized CustomEvacMessageManager getInstance() {
        if (instance == null) {
            instance = new CustomEvacMessageManager();
        }
        return instance;
    }


    public void setCustomEvacuationMessage(String messageTemplate) {
        if (messageTemplate == null || messageTemplate.trim().isEmpty()) {
            LOGGER.warning("Cannot set empty evacuation message");
            return;
        }

        boolean saved = messageDAO.saveCustomMessage(messageTemplate.trim());

        if (saved) {
            LOGGER.info("Custom evacuation message saved to database");
        } else {
            LOGGER.warning("Failed to save custom evacuation message to database");
        }
    }


    public String getCustomEvacuationMessage() {
        String message = messageDAO.getActiveCustomMessage();
        LOGGER.info("Retrieved custom message from database");
        return message;
    }


    public boolean hasCustomMessage() {
        return messageDAO.hasActiveCustomMessage();
    }


    public String buildMessage(String beneficiaryName, String evacSiteName) {
        String template = getCustomEvacuationMessage();

        String name = (beneficiaryName != null && !beneficiaryName.trim().isEmpty())
                ? beneficiaryName.trim()
                : "Beneficiary";

        String site = (evacSiteName != null && !evacSiteName.trim().isEmpty())
                ? evacSiteName.trim()
                : "the assigned evacuation center";

        String message = template
                .replace("{name}", name)
                .replace("{evacSite}", site)
                .replace("{site}", site); // Support both placeholders

        LOGGER.fine("Built message: " + message);
        return message;
    }


    public String builtInMessage(String beneficiaryName, String evacSiteName) {
        return buildMessage(beneficiaryName, evacSiteName);
    }


    public String getDefaultTemplate() {
        return messageDAO.getDefaultMessage();
    }


    public void resetToDefault() {
        String defaultMsg = messageDAO.getDefaultMessage();
        messageDAO.saveCustomMessage(defaultMsg);
        LOGGER.info("Reset to default evacuation message");
    }
}