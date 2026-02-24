package com.ionres.respondph.beneficiary;

import com.ionres.respondph.aid_type.AidTypeModelComboBox;
import com.ionres.respondph.household_score.HouseholdScoreModel;
import com.ionres.respondph.util.AppContext;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ionres.respondph.database.DBConnection;

public class ViewInfoAndScoreController {

    private static final Logger LOGGER = Logger.getLogger(ViewInfoAndScoreController.class.getName());

    @FXML private VBox root;
    @FXML private ScrollPane mainScroll;
    // ── Header ──
    @FXML private Label fullNameLabel;
    @FXML private Label beneficiaryIdLabel;
    @FXML private Label regDateLabel;
    @FXML private Label addedByLabel;

    // ── Personal Info ──
    @FXML private Label firstnameLabel;
    @FXML private Label middlenameLabel;
    @FXML private Label lastnameLabel;
    @FXML private Label birthdateLabel;
    @FXML private Label genderLabel;
    @FXML private Label maritalStatusLabel;
    @FXML private Label soloParentLabel;
    @FXML private Label mobileLabel;
    @FXML private Label barangayLabel;

    // ── Socioeconomic ──
    @FXML private Label employmentLabel;
    @FXML private Label incomeLabel;
    @FXML private Label educationLabel;
    @FXML private Label houseTypeLabel;
    @FXML private Label ownershipLabel;
    @FXML private Label digitalAccessLabel;
    @FXML private Label cleanWaterLabel;
    @FXML private Label sanitationLabel;

    // ── Health & Disability ──
    @FXML private Label healthConditionLabel;
    @FXML private Label disabilityTypeLabel;

    // ── Location ──
    @FXML private Label latitudeLabel;
    @FXML private Label longitudeLabel;

    // ── Score Section Header ──
    @FXML private Label scoreSectionTitleLabel;
    @FXML private ComboBox<AidTypeModelComboBox> aidTypeComboBox;

    // ── Score Cards ──
    @FXML private VBox noScorePane;
    @FXML private GridPane scoreGrid;

    @FXML private Label ageScoreLabel;
    @FXML private ProgressBar ageScoreBar;
    @FXML private Label genderScoreLabel;
    @FXML private ProgressBar genderScoreBar;
    @FXML private Label maritalScoreLabel;
    @FXML private ProgressBar maritalScoreBar;
    @FXML private Label soloParentScoreLabel;
    @FXML private ProgressBar soloParentScoreBar;
    @FXML private Label disabilityScoreLabel;
    @FXML private ProgressBar disabilityScoreBar;
    @FXML private Label healthScoreLabel;
    @FXML private ProgressBar healthScoreBar;
    @FXML private Label cleanWaterScoreLabel;
    @FXML private ProgressBar cleanWaterScoreBar;
    @FXML private Label sanitationScoreLabel;
    @FXML private ProgressBar sanitationScoreBar;
    @FXML private Label houseScoreLabel;
    @FXML private ProgressBar houseScoreBar;
    @FXML private Label ownershipScoreLabel;
    @FXML private ProgressBar ownershipScoreBar;
    @FXML private Label damageScoreLabel;
    @FXML private ProgressBar damageScoreBar;
    @FXML private Label employmentScoreLabel;
    @FXML private ProgressBar employmentScoreBar;
    @FXML private Label incomeScoreLabel;
    @FXML private ProgressBar incomeScoreBar;
    @FXML private Label educationScoreLabel;
    @FXML private ProgressBar educationScoreBar;
    @FXML private Label digitalScoreLabel;
    @FXML private ProgressBar digitalScoreBar;
    @FXML private Label dependencyScoreLabel;
    @FXML private ProgressBar dependencyScoreBar;

    // ── Total Score ──
    @FXML private Label totalScoreTitleLabel;
    @FXML private Label totalScoreLabel;
    @FXML private Label totalScoreCategory;

    // ── Close Button ──
    @FXML private Button closeButton;

    // ── State ──
    private int currentBeneficiaryId;
    private double xOffset, yOffset;
    private Stage dialogStage;

    @FXML
    private void initialize() {
        makeDraggable();
    }

    public void setBeneficiary(BeneficiaryModel bm) {
        this.currentBeneficiaryId = bm.getId();
        populatePersonalInfo(bm);
        loadAidTypeComboBox();
        javafx.application.Platform.runLater(() -> mainScroll.setVvalue(0.0));
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }


    private void loadAidTypeComboBox() {
        try {
            List<AidTypeModelComboBox> aidTypes = AppContext.aidTypeService.findAll();

            AidTypeModelComboBox noneOption = new AidTypeModelComboBox();
            noneOption.setAidTypeId(-1);
            noneOption.setAidName("None");

            List<AidTypeModelComboBox> options = new ArrayList<>(aidTypes);
            options.add(0, noneOption);

            aidTypeComboBox.setItems(FXCollections.observableArrayList(options));

            aidTypeComboBox.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(AidTypeModelComboBox item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getAidName());
                }
            });
            aidTypeComboBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(AidTypeModelComboBox item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getAidName());
                }
            });

            aidTypeComboBox.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldVal, newVal) -> {
                        if (newVal == null || newVal.getAidTypeId() == -1) {
                            scoreSectionTitleLabel.setText("HOUSEHOLD VULNERABILITY SCORES");
                            loadAndPopulateHouseholdScore(currentBeneficiaryId);
                        } else {
                            scoreSectionTitleLabel.setText("AID SCORES — " + newVal.getAidName().toUpperCase());
                            loadAndPopulateAidScore(currentBeneficiaryId, newVal.getAidTypeId());
                        }
                    }
            );

            aidTypeComboBox.getSelectionModel().selectFirst();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load aid types for combobox", e);
        }
    }

    // ─────────────────────────────────────────────
    //  Personal Information
    // ─────────────────────────────────────────────

    private void populatePersonalInfo(BeneficiaryModel bm) {
        String fullName = safe(bm.getFirstname()) + " "
                + safe(bm.getMiddlename()) + " "
                + safe(bm.getLastname());
        fullNameLabel.setText(fullName.trim());
        beneficiaryIdLabel.setText("ID #" + bm.getId());
        regDateLabel.setText(safe(bm.getRegDate()));
        addedByLabel.setText("by " + safe(bm.getAddedBy()));

        firstnameLabel.setText(safe(bm.getFirstname()));
        middlenameLabel.setText(safe(bm.getMiddlename()));
        lastnameLabel.setText(safe(bm.getLastname()));
        birthdateLabel.setText(safe(bm.getBirthDate()));
        genderLabel.setText(safe(bm.getGender()));
        maritalStatusLabel.setText(safe(bm.getMaritalStatus()));
        soloParentLabel.setText(safe(bm.getSoloParentStatus()));
        mobileLabel.setText(safe(bm.getMobileNumber()));
        barangayLabel.setText(safe(bm.getBarangay()));

        employmentLabel.setText(safe(bm.getEmploymentStatus()));
        incomeLabel.setText(safe(bm.getMonthlyIncome()));
        educationLabel.setText(safe(bm.getEducationalLevel()));
        houseTypeLabel.setText(safe(bm.getHouseType()));
        ownershipLabel.setText(safe(bm.getOwnerShipStatus()));
        digitalAccessLabel.setText(safe(bm.getDigitalAccess()));
        cleanWaterLabel.setText(safe(bm.getCleanWaterAccess()));
        sanitationLabel.setText(safe(bm.getSanitationFacility()));

        healthConditionLabel.setText(safe(bm.getHealthCondition()));
        disabilityTypeLabel.setText(safe(bm.getDisabilityType()));

        latitudeLabel.setText(safe(bm.getLatitude()));
        longitudeLabel.setText(safe(bm.getLongitude()));
    }

    // ─────────────────────────────────────────────
    //  Mode A: Raw Household Scores (no aid type)
    // ─────────────────────────────────────────────

    private void loadAndPopulateHouseholdScore(int beneficiaryId) {
        HouseholdScoreModel score = fetchLatestHouseholdScore(beneficiaryId);

        totalScoreTitleLabel.setVisible(false);
        totalScoreTitleLabel.setManaged(false);
        totalScoreLabel.setVisible(false);
        totalScoreLabel.setManaged(false);
        totalScoreCategory.setVisible(false);
        totalScoreCategory.setManaged(false);

        if (score == null) {
            showNoScore();
            return;
        }

        showScoreGrid();

        setScore(ageScoreLabel,          ageScoreBar,          score.getAgeScore());
        setScore(genderScoreLabel,       genderScoreBar,       score.getGenderScore());
        setScore(maritalScoreLabel,      maritalScoreBar,      score.getMaritalStatusScore());
        setScore(soloParentScoreLabel,   soloParentScoreBar,   score.getSoloParentScore());
        setScore(disabilityScoreLabel,   disabilityScoreBar,   score.getDisabilityScore());
        setScore(healthScoreLabel,       healthScoreBar,       score.getHealthConditionScore());
        setScore(cleanWaterScoreLabel,   cleanWaterScoreBar,   score.getAccessToCleanWaterScore());
        setScore(sanitationScoreLabel,   sanitationScoreBar,   score.getSanitationFacilitiesScore());
        setScore(houseScoreLabel,        houseScoreBar,        score.getHouseConstructionTypeScore());
        setScore(ownershipScoreLabel,    ownershipScoreBar,    score.getOwnershipScore());
        setScore(damageScoreLabel,       damageScoreBar,       score.getDamageSeverityScore());
        setScore(employmentScoreLabel,   employmentScoreBar,   score.getEmploymentStatusScore());
        setScore(incomeScoreLabel,       incomeScoreBar,       score.getMonthlyIncomeScore());
        setScore(educationScoreLabel,    educationScoreBar,    score.getEducationLevelScore());
        setScore(digitalScoreLabel,      digitalScoreBar,      score.getDigitalAccessScore());
        setScore(dependencyScoreLabel,   dependencyScoreBar,   score.getDependencyRatioScore());

        double total = score.getAgeScore() + score.getGenderScore()
                + score.getMaritalStatusScore() + score.getSoloParentScore()
                + score.getDisabilityScore() + score.getHealthConditionScore()
                + score.getAccessToCleanWaterScore() + score.getSanitationFacilitiesScore()
                + score.getHouseConstructionTypeScore() + score.getOwnershipScore()
                + score.getDamageSeverityScore() + score.getEmploymentStatusScore()
                + score.getMonthlyIncomeScore() + score.getEducationLevelScore()
                + score.getDigitalAccessScore() + score.getDependencyRatioScore();

        totalScoreLabel.setText(String.format("%.4f", total));
        totalScoreCategory.setText(resolveVulnerabilityCategory(total));
    }

    // ─────────────────────────────────────────────
    //  Mode B: Aid-Weighted Scores (aid type selected)
    // ─────────────────────────────────────────────

    private void loadAndPopulateAidScore(int beneficiaryId, int aidTypeId) {
        AidAndHouseholdScoreRow row = fetchAidHouseholdScore(beneficiaryId, aidTypeId);

        totalScoreTitleLabel.setText("FINAL SCORE");
        totalScoreTitleLabel.setVisible(true);
        totalScoreTitleLabel.setManaged(true);
        totalScoreLabel.setVisible(true);
        totalScoreLabel.setManaged(true);
        totalScoreCategory.setVisible(false);
        totalScoreCategory.setManaged(false);

        if (row == null) {
            showNoScore();
            return;
        }

        showScoreGrid();

        setScore(ageScoreLabel,          ageScoreBar,          row.ageScore);
        setScore(genderScoreLabel,       genderScoreBar,       row.genderScore);
        setScore(maritalScoreLabel,      maritalScoreBar,      row.maritalStatusScore);
        setScore(soloParentScoreLabel,   soloParentScoreBar,   row.soloParentScore);
        setScore(disabilityScoreLabel,   disabilityScoreBar,   row.disabilityScore);
        setScore(healthScoreLabel,       healthScoreBar,       row.healthConditionScore);
        setScore(cleanWaterScoreLabel,   cleanWaterScoreBar,   row.cleanWaterScore);
        setScore(sanitationScoreLabel,   sanitationScoreBar,   row.sanitationScore);
        setScore(houseScoreLabel,        houseScoreBar,        row.houseConstructionScore);
        setScore(ownershipScoreLabel,    ownershipScoreBar,    row.ownershipScore);
        setScore(damageScoreLabel,       damageScoreBar,       row.damageSeverityScore);
        setScore(employmentScoreLabel,   employmentScoreBar,   row.employmentStatusScore);
        setScore(incomeScoreLabel,       incomeScoreBar,       row.monthlyIncomeScore);
        setScore(educationScoreLabel,    educationScoreBar,    row.educationLevelScore);
        setScore(digitalScoreLabel,      digitalScoreBar,      row.digitalAccessScore);
        setScore(dependencyScoreLabel,   dependencyScoreBar,   row.dependencyRatioScore != null ? row.dependencyRatioScore : 0.0);

        totalScoreLabel.setText(String.format("%.4f", row.finalScore));
    }

    // ─────────────────────────────────────────────
    //  DB Fetchers
    // ─────────────────────────────────────────────

    private HouseholdScoreModel fetchLatestHouseholdScore(int beneficiaryId) {
        String sql = "SELECT * FROM household_score WHERE beneficiary_id = ? " +
                "ORDER BY COALESCE(updating_date, creation_date) DESC LIMIT 1";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, beneficiaryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    HouseholdScoreModel model = new HouseholdScoreModel();
                    model.setHouseholdScoreId(rs.getInt("household_score_id"));
                    model.setBeneficiaryId(rs.getInt("beneficiary_id"));
                    model.setAgeScore(rs.getDouble("age_score"));
                    model.setGenderScore(rs.getDouble("gender_score"));
                    model.setMaritalStatusScore(rs.getDouble("marital_status_score"));
                    model.setSoloParentScore(rs.getDouble("solo_parent_score"));
                    model.setDisabilityScore(rs.getDouble("disability_score"));
                    model.setHealthConditionScore(rs.getDouble("health_condition_score"));
                    model.setAccessToCleanWaterScore(rs.getDouble("access_to_clean_water_score"));
                    model.setSanitationFacilitiesScore(rs.getDouble("sanitation_facilities_score"));
                    model.setHouseConstructionTypeScore(rs.getDouble("house_construction_type_score"));
                    model.setOwnershipScore(rs.getDouble("ownership_score"));
                    model.setDamageSeverityScore(rs.getDouble("damage_severity_score"));
                    model.setEmploymentStatusScore(rs.getDouble("employment_status_score"));
                    model.setMonthlyIncomeScore(rs.getDouble("monthly_income_score"));
                    model.setEducationLevelScore(rs.getDouble("education_level_score"));
                    model.setDigitalAccessScore(rs.getDouble("digital_access_score"));
                    model.setDependencyRatioScore(rs.getDouble("dependency_ratio_score"));
                    return model;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching household score for beneficiary " + beneficiaryId, e);
        }
        return null;
    }

    private AidAndHouseholdScoreRow fetchAidHouseholdScore(int beneficiaryId, int aidTypeId) {
        String sql = "SELECT * FROM aid_and_household_score " +
                "WHERE beneficiary_id = ? AND aid_type_id = ? " +
                "ORDER BY assessment_date DESC LIMIT 1";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, beneficiaryId);
            ps.setInt(2, aidTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AidAndHouseholdScoreRow row = new AidAndHouseholdScoreRow();
                    row.ageScore              = rs.getDouble("age_score");
                    row.genderScore           = rs.getDouble("gender_score");
                    row.maritalStatusScore    = rs.getDouble("marital_status_score");
                    row.soloParentScore       = rs.getDouble("solo_parent_score");
                    row.disabilityScore       = rs.getDouble("disability_score");
                    row.healthConditionScore  = rs.getDouble("health_condition_score");
                    row.cleanWaterScore       = rs.getDouble("access_to_clean_water_score");
                    row.sanitationScore       = rs.getDouble("sanitation_facilities_score");
                    row.houseConstructionScore = rs.getDouble("house_construction_type_score");
                    row.ownershipScore        = rs.getDouble("ownership_score");
                    row.damageSeverityScore   = rs.getDouble("damage_severity_score");
                    row.employmentStatusScore  = rs.getDouble("employment_status_score");
                    row.monthlyIncomeScore    = rs.getDouble("monthly_income_score");
                    row.educationLevelScore   = rs.getDouble("education_level_score");
                    row.digitalAccessScore    = rs.getDouble("digital_access_score");
                    row.dependencyRatioScore  = rs.getObject("dependency_ratio_score") != null
                            ? rs.getDouble("dependency_ratio_score") : null;
                    row.finalScore            = rs.getDouble("final_score");
                    row.scoreCategory         = rs.getString("score_category");
                    return row;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Error fetching aid_and_household_score for beneficiary " + beneficiaryId
                            + " aidType " + aidTypeId, e);
        }
        return null;
    }

    // ─────────────────────────────────────────────
    //  UI helpers
    // ─────────────────────────────────────────────

    private void showNoScore() {
        noScorePane.setVisible(true);
        noScorePane.setManaged(true);
        scoreGrid.setVisible(false);
        scoreGrid.setManaged(false);
        totalScoreLabel.setText("N/A");
        totalScoreCategory.setText("No score available");
    }

    private void showScoreGrid() {
        noScorePane.setVisible(false);
        noScorePane.setManaged(false);
        scoreGrid.setVisible(true);
        scoreGrid.setManaged(true);
    }

    private void setScore(Label label, ProgressBar bar, double value) {
        label.setText(String.format("%.4f", value));
        bar.setProgress(Math.min(1.0, Math.max(0.0, value)));
        bar.getStyleClass().removeAll("score-bar-low", "score-bar-medium", "score-bar-high");
        if (value >= 0.75) {
            bar.getStyleClass().add("score-bar-high");
        } else if (value >= 0.40) {
            bar.getStyleClass().add("score-bar-medium");
        } else {
            bar.getStyleClass().add("score-bar-low");
        }
    }

    private String resolveVulnerabilityCategory(double total) {
        if (total >= 12.0) return "⚠ Critical Vulnerability";
        if (total >= 8.0)  return "High Vulnerability";
        if (total >= 4.0)  return "Moderate Vulnerability";
        return "Low Vulnerability";
    }

    private String safe(String val) {
        return (val == null || val.isBlank()) ? "—" : val;
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    // ─────────────────────────────────────────────
    //  Draggable
    // ─────────────────────────────────────────────

    private void makeDraggable() {
        root.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });

        root.setOnMouseDragged(e -> {
            if (dialogStage != null) {
                dialogStage.setX(e.getScreenX() - xOffset);
                dialogStage.setY(e.getScreenY() - yOffset);
            }
        });
    }

    // ─────────────────────────────────────────────
    //  Inner DTO for aid_and_household_score row
    // ─────────────────────────────────────────────

    private static class AidAndHouseholdScoreRow {
        double ageScore, genderScore, maritalStatusScore, soloParentScore;
        double disabilityScore, healthConditionScore, cleanWaterScore, sanitationScore;
        double houseConstructionScore, ownershipScore, damageSeverityScore, employmentStatusScore;
        double monthlyIncomeScore, educationLevelScore, digitalAccessScore;
        Double dependencyRatioScore;
        double finalScore;
        String scoreCategory;
    }
}