package com.ionres.respondph.aid_type;

import java.time.LocalDateTime;


public class AidTypeModelComboBox {

    private int aidTypeId;
    private String aidName;
    private String notes;
    private int createdByAdminId;
    private LocalDateTime createdAt;

    public AidTypeModelComboBox() {
    }

    public AidTypeModelComboBox(int aidTypeId, String aidName) {
        this.aidTypeId = aidTypeId;
        this.aidName = aidName;
    }

    public int getAidTypeId() {
        return aidTypeId;
    }

    public void setAidTypeId(int aidTypeId) {
        this.aidTypeId = aidTypeId;
    }

    public String getAidName() {
        return aidName;
    }

    public void setAidName(String aidName) {
        this.aidName = aidName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getCreatedByAdminId() {
        return createdByAdminId;
    }

    public void setCreatedByAdminId(int createdByAdminId) {
        this.createdByAdminId = createdByAdminId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * This is what displays in the ComboBox
     */
    @Override
    public String toString() {
        return aidName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AidTypeModelComboBox that = (AidTypeModelComboBox) obj;
        return aidTypeId == that.aidTypeId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(aidTypeId);
    }
}
