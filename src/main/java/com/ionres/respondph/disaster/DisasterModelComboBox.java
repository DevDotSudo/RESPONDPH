package com.ionres.respondph.disaster;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model for Disaster (for ComboBox display)
 */
public class DisasterModelComboBox {

    private int disasterId;
    private int disasterTypeId;
    private String disasterTypeName; // From join
    private String disasterName;
    private LocalDate disasterDate;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal radiusKm;
    private String notes;
    private LocalDateTime createdAt;

    // Constructors
    public DisasterModelComboBox() {
    }

    public DisasterModelComboBox(int disasterId, String disasterName, String disasterTypeName) {
        this.disasterId = disasterId;
        this.disasterName = disasterName;
        this.disasterTypeName = disasterTypeName;
    }

    // Getters and Setters
    public int getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(int disasterId) {
        this.disasterId = disasterId;
    }

    public int getDisasterTypeId() {
        return disasterTypeId;
    }

    public void setDisasterTypeId(int disasterTypeId) {
        this.disasterTypeId = disasterTypeId;
    }

    public String getDisasterTypeName() {
        return disasterTypeName;
    }

    public void setDisasterTypeName(String disasterTypeName) {
        this.disasterTypeName = disasterTypeName;
    }

    public String getDisasterName() {
        return disasterName;
    }

    public void setDisasterName(String disasterName) {
        this.disasterName = disasterName;
    }

    public LocalDate getDisasterDate() {
        return disasterDate;
    }

    public void setDisasterDate(LocalDate disasterDate) {
        this.disasterDate = disasterDate;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getRadiusKm() {
        return radiusKm;
    }

    public void setRadiusKm(BigDecimal radiusKm) {
        this.radiusKm = radiusKm;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    @Override
    public String toString() {
        if (disasterTypeName != null && disasterDate != null) {
            return String.format("%s - %s (%s)",
                    disasterName,
                    disasterTypeName,
                    disasterDate.toString()
            );
        } else if (disasterTypeName != null) {
            return String.format("%s - %s", disasterName, disasterTypeName);
        } else {
            return disasterName;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DisasterModelComboBox that = (DisasterModelComboBox) obj;
        return disasterId == that.disasterId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(disasterId);
    }
}