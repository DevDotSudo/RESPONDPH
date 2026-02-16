package com.ionres.respondph.sendsms;

/**
 * Data Access Object interface for managing custom evacuation messages
 */
public interface CustomEvacMessageDAO {

    /**
     * Save or update the active custom evacuation message
     * @param messageTemplate The message template with placeholders {name} and {evacSite}
     * @return true if successful, false otherwise
     */
    boolean saveCustomMessage(String messageTemplate);

    /**
     * Get the currently active custom evacuation message
     * @return The active message template, or null if none exists
     */
    String getActiveCustomMessage();

    /**
     * Check if a custom message exists and is active
     * @return true if an active custom message exists
     */
    boolean hasActiveCustomMessage();

    /**
     * Deactivate all previous messages and set a new active message
     * @param messageTemplate The new message template
     * @param createdBy Username or identifier of who created the message
     * @return true if successful
     */
    boolean setActiveMessage(String messageTemplate, String createdBy);

    /**
     * Get the default built-in evacuation message
     * @return The default message template
     */
    String getDefaultMessage();
}