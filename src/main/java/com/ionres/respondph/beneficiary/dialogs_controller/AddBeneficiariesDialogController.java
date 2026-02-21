package com.ionres.respondph.beneficiary.dialogs_controller;

import com.ionres.respondph.beneficiary.AgeScoreCalculate;
import com.ionres.respondph.beneficiary.BeneficiaryController;
import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.beneficiary.BeneficiaryService;
import com.ionres.respondph.common.controller.MappingDialogController;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * IMPORT FLOW (updated):
 * - Import ALL beneficiaries in excel (e.g., 300+)
 * - Balanced-random barangay distribution across 18 barangays
 * - Randomize phone every row
 * - DatePicker supports Excel date cells + strings
 * - Continue UI is NON-MODAL (you can still click the form)
 * - Continue button ONLY enabled when lat & long are VALID numbers
 * - During import, Get Location button is disabled (manual lat/long entry)
 */
public class AddBeneficiariesDialogController {

    // ✅ allow 300+ rows (no cap)
    private static final int IMPORT_LIMIT = Integer.MAX_VALUE;

    @FXML private VBox root;

    // Step 1
    @FXML private TextField firstNameFld;
    @FXML private TextField middleNameFld;
    @FXML private TextField lastNameFld;
    @FXML private DatePicker birthDatePicker;
    @FXML private ComboBox<String> barangaySelection;
    @FXML private ComboBox<String> genderSelection;
    @FXML private TextField mobileNumberFld;
    @FXML private ComboBox<String> maritalStatusSelection;
    @FXML private ComboBox<String> soloParentStatusSelection;

    // Step 2
    @FXML public TextField latitudeFld;
    @FXML public TextField longitudeFld;
    @FXML private Button getLocationBtn;

    // Step 3
    @FXML private ComboBox<String> disabilityTypeSelection;
    @FXML private ComboBox<String> healthConditionSelection;
    @FXML private ComboBox<String> cleanWaterAccessSelection;
    @FXML private ComboBox<String> sanitationFacilitiesSelection;

    // Step 4
    @FXML private ComboBox<String> houseConstructionTypeSelection;
    @FXML private ComboBox<String> ownershipStatusSelection;

    // Step 5
    @FXML private ComboBox<String> employmentStatusSelection;
    @FXML private ComboBox<String> educationLevelSelection;
    @FXML private ComboBox<String> monthlyIncomeSelection;
    @FXML private ComboBox<String> digitalAccessSelection;

    @FXML private Button exitBtn;
    @FXML private Button addBeneficiaryBtn;

    // Add this in FXML with onAction="#onImportExcel"
    @FXML private Button importExcelBtn;

    private double yOffset = 0;
    private double xOffset = 0;

    private BeneficiaryService beneficiaryService;
    private BeneficiaryController beneficiaryController;
    private Stage dialogStage;

    // ===== Import state =====
    private List<ExcelRow> importRows = List.of();
    private int importIndex = 0;
    private boolean importRunning = false;

    // Non-modal continue dialog (so you can still click fields)
    private Stage continueStage;
    private Button continueBtn;
    private Label continueHeader;

    // ✅ Balanced barangay assignment (randomized but evenly distributed)
    private final Deque<String> barangayQueue = new ArrayDeque<>();
    private final Random rng = new Random();

    // ===== Excel headers =====
    private static final String H_FIRSTNAME = "FirstName";
    private static final String H_LASTNAME = "LastName";
    private static final String H_MIDDLENAME = "MiddleName";
    private static final String H_BIRTHDATE = "Birthdate";
    private static final String H_GENDER = "Gender";
    private static final String H_MARITAL = "MaritalStatus";
    private static final String H_SOLOPARENT = "SoloParent";
    private static final String H_DISABILITY = "DisabilityType";
    private static final String H_HEALTH = "HealthCondition";
    private static final String H_CLEANWATER = "AccessToCleanWater";
    private static final String H_SANITATION = "SanitationFacilities";
    private static final String H_HOUSE = "HouseConstructionType";
    private static final String H_OWNERSHIP = "OwnershipStatus";
    private static final String H_EMPLOYMENT = "EmploymentStatus";
    private static final String H_MONTHLY = "MonthlyIncome";
    private static final String H_EDU = "EducationLevel";
    private static final String H_DIGITAL = "DigitalAccess";

    // Optional headers
    private static final String H_BARANGAY = "Barangay";
    private static final String H_MOBILE = "MobileNumber";

    private static final List<String> BARANGAYS = List.of(
            "Alacaygan","Bariga","Belen","Bobon","Bularan","Carmelo","De La Paz","Dugwakan","Fuentes",
            "Juanico","Libertad","Magdalo","Managopaya","Merced","Poblacion","San Salvador","Talokgangan","Zona Sur"
    );

    public void setBeneficiaryService(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    public void setBeneficiaryController(BeneficiaryController beneficiaryController) {
        this.beneficiaryController = beneficiaryController;
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
        onShow();
    }

    public Stage getDialogStage() { return dialogStage; }

    @FXML
    public void initialize() {
        makeDraggable();

        EventHandler<ActionEvent> handlers = this::handleActions;
        exitBtn.setOnAction(handlers);
        addBeneficiaryBtn.setOnAction(handlers);
        getLocationBtn.setOnAction(handlers);
        if (importExcelBtn != null) importExcelBtn.setOnAction(handlers);

        PhoneNumberValidator.setupInputFilter(mobileNumberFld);

        latitudeFld.textProperty().addListener((obs, o, n) -> refreshContinueBtn());
        longitudeFld.textProperty().addListener((obs, o, n) -> refreshContinueBtn());
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == addBeneficiaryBtn) {
            addBeneficiary();
            if (beneficiaryController != null) beneficiaryController.loadTable();

        } else if (src == exitBtn) {
            close();

        } else if (src == getLocationBtn) {
            handleGetLocationBtn();

        } else if (src == importExcelBtn) {
            onImportExcel();
        }
    }

    private void handleGetLocationBtn() {
        MappingDialogController controller = DialogManager.getController("mapping", MappingDialogController.class);
        controller.setListener(latLng -> {
            latitudeFld.setText(String.valueOf(latLng.lat));
            longitudeFld.setText(String.valueOf(latLng.lon));
        });
        DialogManager.show("mapping");
    }

    // ===========================
    // ✅ IMPORT EXCEL (ALL ROWS)
    // ===========================
    @FXML
    private void onImportExcel() {
        if (importRunning) {
            AlertDialogManager.showWarning("Import", "Import is already running.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Select Excel File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        File file = fc.showOpenDialog(addBeneficiaryBtn.getScene().getWindow());
        if (file == null) return;

        try (FileInputStream in = new FileInputStream(file)) {
            List<ExcelRow> all = readExcelRows(in, 0);

            if (all.isEmpty()) {
                AlertDialogManager.showWarning("Import", "No rows found.");
                return;
            }

            importRows = (all.size() > IMPORT_LIMIT) ? all.subList(0, IMPORT_LIMIT) : all;

            importIndex = 0;
            importRunning = true;

            // ✅ prepare balanced-random barangay distribution for ALL imported rows
            resetBarangayQueue(importRows.size());

            // ✅ manual location during import
            if (getLocationBtn != null) getLocationBtn.setDisable(true);

            showOrUpdateContinueWindow();
            importNextRow();

        } catch (Exception e) {
            importRunning = false;
            hideContinueWindow();
            if (getLocationBtn != null) getLocationBtn.setDisable(false);
            AlertDialogManager.showError("Import Error", e.getMessage());
            e.printStackTrace();
        }
    }

    private void importNextRow() {
        if (!importRunning) return;

        if (importIndex >= importRows.size()) {
            importRunning = false;
            hideContinueWindow();
            if (getLocationBtn != null) getLocationBtn.setDisable(false);
            AlertDialogManager.showSuccess("Import",
                    "Finished importing " + importRows.size() + " rows.");
            return;
        }

        ExcelRow row = importRows.get(importIndex);
        fillFromExcelRow(row);

        showOrUpdateContinueWindow();
        refreshContinueHeader();
        refreshContinueBtn();
    }

    private void showOrUpdateContinueWindow() {
        if (continueStage != null && continueStage.isShowing()) {
            refreshContinueHeader();
            refreshContinueBtn();
            return;
        }

        continueHeader = new Label();
        continueHeader.setWrapText(true);

        continueBtn = new Button("Continue (Save)");
        Button cancelBtn = new Button("Cancel Import");

        continueBtn.setDefaultButton(true);

        continueBtn.setOnAction(e -> {
            if (!importRunning) return;

            // ✅ Require VALID lat/long numbers
            if (!isValidLat(latitudeFld.getText()) || !isValidLon(longitudeFld.getText())) {
                AlertDialogManager.showWarning("Import", "Enter valid Latitude (-90..90) and Longitude (-180..180).");
                return;
            }

            addBeneficiary();
            if (beneficiaryController != null) beneficiaryController.loadTable();

            importIndex++;
            Platform.runLater(this::importNextRow);
        });

        cancelBtn.setOnAction(e -> {
            importRunning = false;
            hideContinueWindow();
            if (getLocationBtn != null) getLocationBtn.setDisable(false);
            AlertDialogManager.showWarning("Import", "Import cancelled at row " + (importIndex + 1) + ".");
        });

        VBox box = new VBox(
                10,
                continueHeader,
                new Label("Enter Latitude & Longitude manually, then click Continue."),
                new Separator(),
                new javafx.scene.layout.HBox(10, continueBtn, cancelBtn)
        );

        box.setStyle("-fx-padding: 14; -fx-background-color: #1f2733; -fx-border-color: #3a4455; -fx-border-radius: 8; -fx-background-radius: 8;");
        continueHeader.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

        for (Node n : box.getChildren()) {
            if (n instanceof Label && n != continueHeader) ((Label) n).setStyle("-fx-text-fill: #cfd8e3;");
        }

        Scene scene = new Scene(box);
        continueStage = new Stage();
        continueStage.setTitle("Import Controller");
        continueStage.setScene(scene);

        // ✅ NON-MODAL so you can still click your form fields
        continueStage.initModality(Modality.NONE);

        // optional
        continueStage.setAlwaysOnTop(true);

        continueStage.setOnCloseRequest(e -> {
            importRunning = false;
            if (getLocationBtn != null) getLocationBtn.setDisable(false);
            AlertDialogManager.showWarning("Import", "Import cancelled at row " + (importIndex + 1) + ".");
        });

        if (dialogStage != null) {
            continueStage.setX(dialogStage.getX() + dialogStage.getWidth() + 10);
            continueStage.setY(dialogStage.getY() + 60);
        }

        refreshContinueHeader();
        refreshContinueBtn();
        continueStage.show();
    }

    private void hideContinueWindow() {
        if (continueStage != null) {
            continueStage.hide();
        }
        continueStage = null;
        continueBtn = null;
        continueHeader = null;
    }

    private void refreshContinueHeader() {
        if (continueHeader == null) return;
        continueHeader.setText("Row " + (importIndex + 1) + " of " + importRows.size());
    }

    private void refreshContinueBtn() {
        if (continueBtn == null) return;
        boolean ok = isValidLat(latitudeFld.getText()) && isValidLon(longitudeFld.getText());
        continueBtn.setDisable(!ok);
    }

    // ===========================
    // ✅ Balanced barangay distribution
    // ===========================
    private void resetBarangayQueue(int totalRows) {
        barangayQueue.clear();

        int nBrgys = BARANGAYS.size();
        int base = totalRows / nBrgys;
        int extra = totalRows % nBrgys;

        List<String> expanded = new ArrayList<>(totalRows);

        for (String b : BARANGAYS) {
            for (int i = 0; i < base; i++) expanded.add(b);
        }

        List<String> shuffled = new ArrayList<>(BARANGAYS);
        Collections.shuffle(shuffled, rng);
        for (int i = 0; i < extra; i++) expanded.add(shuffled.get(i));

        Collections.shuffle(expanded, rng);
        barangayQueue.addAll(expanded);
    }

    private String nextBarangayBalanced() {
        if (barangayQueue.isEmpty()) return randomBarangay();
        return barangayQueue.removeFirst();
    }

    private static String randomBarangay() {
        return BARANGAYS.get(new Random().nextInt(BARANGAYS.size()));
    }

    private static boolean isValidLat(String s) {
        Double d = tryParseDouble(s);
        return d != null && d >= -90 && d <= 90;
    }

    private static boolean isValidLon(String s) {
        Double d = tryParseDouble(s);
        return d != null && d >= -180 && d <= 180;
    }

    private static Double tryParseDouble(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try { return Double.parseDouble(t); }
        catch (Exception e) { return null; }
    }

    // ===========================
    // ✅ Row -> UI mapping
    // ===========================
    private void fillFromExcelRow(ExcelRow row) {
        Map<String, String> r = row.values;

        firstNameFld.setText(r.getOrDefault(H_FIRSTNAME, "").trim());
        middleNameFld.setText(r.getOrDefault(H_MIDDLENAME, "").trim());
        lastNameFld.setText(r.getOrDefault(H_LASTNAME, "").trim());

        birthDatePicker.setValue(row.birthdate != null ? row.birthdate : parseBirthdate(r.get(H_BIRTHDATE)));

        // ✅ Balanced-random barangay assignment
        selectComboIgnoreCase(barangaySelection, nextBarangayBalanced());

        selectComboIgnoreCase(genderSelection, mapGender(r.get(H_GENDER)));
        selectComboIgnoreCase(maritalStatusSelection, mapMarital(r.get(H_MARITAL)));
        selectComboIgnoreCase(soloParentStatusSelection, mapSoloParent(r.get(H_SOLOPARENT)));

        selectComboIgnoreCase(disabilityTypeSelection, mapDisability(r.get(H_DISABILITY)));
        selectComboIgnoreCase(healthConditionSelection, mapHealth(r.get(H_HEALTH)));
        selectComboIgnoreCase(cleanWaterAccessSelection, mapCleanWater(r.get(H_CLEANWATER)));
        selectComboIgnoreCase(sanitationFacilitiesSelection, mapSanitation(r.get(H_SANITATION)));

        selectComboIgnoreCase(houseConstructionTypeSelection, mapHouseConstructionType(r.get(H_HOUSE)));
        selectComboIgnoreCase(ownershipStatusSelection, mapOwnership(r.get(H_OWNERSHIP)));

        selectComboIgnoreCase(employmentStatusSelection, mapEmployment(r.get(H_EMPLOYMENT)));
        selectComboIgnoreCase(monthlyIncomeSelection, mapMonthlyIncome(r.get(H_MONTHLY)));
        selectComboIgnoreCase(educationLevelSelection, mapEducationLevel(r.get(H_EDU)));
        selectComboIgnoreCase(digitalAccessSelection, mapDigitalAccess(r.get(H_DIGITAL)));

        // ✅ Randomize phone ALWAYS
        mobileNumberFld.setText(generatePHMobile());

        // ✅ user must type manually for each row
        latitudeFld.clear();
        longitudeFld.clear();
    }

    // ===========================
    // ✅ Your addBeneficiary (unchanged)
    // ===========================
    private void addBeneficiary() {
        try {
            String firstname = firstNameFld.getText().trim();
            String middlename = middleNameFld.getText().trim();
            String lastname = lastNameFld.getText().trim();
            String birthDate = birthDatePicker.getValue() != null ? birthDatePicker.getValue().toString() : "";

            String barangay = barangaySelection.getValue();
            double ageScore = AgeScoreCalculate.calculateAgeScoreFromBirthdate(birthDate);

            String gender = genderSelection.getValue();
            String mobileNumber = mobileNumberFld.getText().trim();
            String maritalStatus = maritalStatusSelection.getValue();
            String soloParentStatus = soloParentStatusSelection.getValue();
            String latitude = latitudeFld.getText().trim();
            String longitude = longitudeFld.getText().trim();
            String disabilityType = disabilityTypeSelection.getValue();
            String healthCondition = healthConditionSelection.getValue();
            String cleanWaterAccess = cleanWaterAccessSelection.getValue();
            String sanitationFacility = sanitationFacilitiesSelection.getValue();
            String houseType = houseConstructionTypeSelection.getValue();
            String ownershipStatus = ownershipStatusSelection.getValue();
            String employmentStatus = employmentStatusSelection.getValue();
            String monthlyIncome = monthlyIncomeSelection.getValue();
            String educationalLevel = educationLevelSelection.getValue();
            String digitalAccess = digitalAccessSelection.getValue();

            if (firstname.isEmpty()) { AlertDialogManager.showWarning("Warning", "First name is required"); return; }
            if (middlename.isEmpty()) { AlertDialogManager.showWarning("Warning", "Middle name is required"); return; }
            if (lastname.isEmpty()) { AlertDialogManager.showWarning("Warning", "Last name is required"); return; }
            if (birthDate.isEmpty()) { AlertDialogManager.showWarning("Warning", "Birth date is required"); return; }

            if (barangay == null || barangay.isEmpty()) { AlertDialogManager.showWarning("Warning", "Barangay is required"); return; }
            if (gender == null) { AlertDialogManager.showWarning("Warning", "Gender is required"); return; }

            if (!PhoneNumberValidator.isValid(mobileNumber)) {
                AlertDialogManager.showWarning("Invalid Mobile Number", PhoneNumberValidator.getErrorMessage(mobileNumber));
                return;
            }

            if (maritalStatus == null) { AlertDialogManager.showWarning("Warning", "Marital status is required"); return; }
            if (soloParentStatus == null) { AlertDialogManager.showWarning("Warning", "Solo parent status is required"); return; }

            // ✅ strong validation for manual location
            if (!isValidLat(latitude)) { AlertDialogManager.showWarning("Warning", "Latitude must be a valid number (-90..90)"); return; }
            if (!isValidLon(longitude)) { AlertDialogManager.showWarning("Warning", "Longitude must be a valid number (-180..180)"); return; }

            if (disabilityType == null) { AlertDialogManager.showWarning("Warning", "Disability type is required"); return; }
            if (healthCondition == null) { AlertDialogManager.showWarning("Warning", "Health condition is required"); return; }
            if (cleanWaterAccess == null) { AlertDialogManager.showWarning("Warning", "Clean water access is required"); return; }
            if (sanitationFacility == null) { AlertDialogManager.showWarning("Warning", "Sanitation facility is required"); return; }
            if (houseType == null) { AlertDialogManager.showWarning("Warning", "House type is required"); return; }
            if (ownershipStatus == null) { AlertDialogManager.showWarning("Warning", "Ownership status is required"); return; }
            if (employmentStatus == null) { AlertDialogManager.showWarning("Warning", "Employment status is required"); return; }
            if (monthlyIncome == null) { AlertDialogManager.showWarning("Warning", "Monthly income is required"); return; }
            if (educationalLevel == null) { AlertDialogManager.showWarning("Warning", "Educational level is required"); return; }
            if (digitalAccess == null) { AlertDialogManager.showWarning("Warning", "Digital access is required"); return; }

            String regDate = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            String addedBy = SessionManager.getInstance().getCurrentAdminFirstName();

            BeneficiaryModel bm = new BeneficiaryModel(
                    firstname, middlename, lastname, birthDate, barangay, ageScore, gender,
                    maritalStatus, soloParentStatus, latitude, longitude,
                    mobileNumber, disabilityType, healthCondition, cleanWaterAccess,
                    sanitationFacility, houseType, ownershipStatus, employmentStatus,
                    monthlyIncome, educationalLevel, digitalAccess, addedBy, regDate
            );

            boolean success = beneficiaryService.createBeneficiary(bm);

            if (success) {
                int newBeneficiaryId = getLatestBeneficiaryId();

                if (newBeneficiaryId > 0) {
                    int adminId = SessionManager.getInstance().getCurrentAdminId();
                    UpdateTrigger trigger = new UpdateTrigger();
                    boolean cascadeSuccess = trigger.triggerCascadeUpdate(newBeneficiaryId);
                    trigger.triggerCascadeUpdateForNewBeneficiary(newBeneficiaryId, adminId);

                    if (cascadeSuccess) {
                        AlertDialogManager.showSuccess("Success", "Beneficiary and household scores successfully saved.");
                    } else {
                        AlertDialogManager.showError("Warning", "Beneficiary saved, but household score calculation failed.");
                    }
                }

                clearFields();
                DashboardRefresher.refresh();
                DashboardRefresher.refreshBeneInSend();
                DashboardRefresher.refreshMapInDisasterMapping();
            } else {
                javax.swing.JOptionPane.showMessageDialog(null, "Failed to add beneficiary.", "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public void clearFields() {
        firstNameFld.setText("");
        middleNameFld.setText("");
        lastNameFld.setText("");
        mobileNumberFld.setText("");
        latitudeFld.setText("");
        longitudeFld.setText("");
        birthDatePicker.setValue(null);

        barangaySelection.getSelectionModel().clearSelection();
        genderSelection.getSelectionModel().clearSelection();
        maritalStatusSelection.getSelectionModel().clearSelection();
        soloParentStatusSelection.getSelectionModel().clearSelection();
        disabilityTypeSelection.getSelectionModel().clearSelection();
        healthConditionSelection.getSelectionModel().clearSelection();
        cleanWaterAccessSelection.getSelectionModel().clearSelection();
        sanitationFacilitiesSelection.getSelectionModel().clearSelection();
        houseConstructionTypeSelection.getSelectionModel().clearSelection();
        ownershipStatusSelection.getSelectionModel().clearSelection();
        employmentStatusSelection.getSelectionModel().clearSelection();
        monthlyIncomeSelection.getSelectionModel().clearSelection();
        educationLevelSelection.getSelectionModel().clearSelection();
        digitalAccessSelection.getSelectionModel().clearSelection();
    }

    private void makeDraggable() {
        root.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
        root.setOnMouseDragged(e -> {
            if (dialogStage != null) {
                dialogStage.setX(e.getScreenX() - xOffset);
                dialogStage.setY(e.getScreenY() - yOffset);
            }
        });
    }

    private void close() {
        importRunning = false;
        hideContinueWindow();
        if (getLocationBtn != null) getLocationBtn.setDisable(false);
        if (dialogStage != null) dialogStage.hide();
    }

    public void onShow() { clearFields(); }

    private int getLatestBeneficiaryId() {
        try {
            String sql = "SELECT beneficiary_id FROM beneficiary ORDER BY beneficiary_id DESC LIMIT 1";
            java.sql.Connection conn = DBConnection.getInstance().getConnection();
            java.sql.PreparedStatement ps = conn.prepareStatement(sql);
            java.sql.ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("beneficiary_id");
                conn.close();
                return id;
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    // ===========================
    // ✅ Mapping helpers (unchanged)
    // ===========================
    private static String norm(String s) {
        if (s == null) return "";
        return s.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9, ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean has(String hay, String needle) { return hay.contains(needle); }
    private static String upper(String s) { return (s == null) ? "" : s.trim().toUpperCase(Locale.ROOT); }

    private void selectComboIgnoreCase(ComboBox<String> cb, String desired) {
        if (cb == null) return;
        if (desired == null || desired.trim().isBlank()) {
            cb.getSelectionModel().clearSelection();
            return;
        }
        String d = desired.trim();
        for (String item : cb.getItems()) {
            if (item != null && item.equalsIgnoreCase(d)) {
                cb.getSelectionModel().select(item);
                return;
            }
        }
        cb.getSelectionModel().clearSelection();
    }

    private static String mapGender(String v) {
        v = upper(v);
        return switch (v) {
            case "MALE" -> "Male";
            case "FEMALE" -> "Female";
            default -> "";
        };
    }

    private static String mapMarital(String v) {
        v = upper(v);
        return switch (v) {
            case "SINGLE" -> "Single";
            case "MARRIED" -> "Married";
            case "WIDOWED" -> "Widowed";
            case "SEPARATED" -> "Separated";
            default -> "";
        };
    }

    private static String mapSoloParent(String v) {
        v = upper(v);
        return switch (v) {
            case "NO" -> "Not a Solo Parent";
            case "YES" -> "Solo Parent (with support network)";
            default -> "";
        };
    }

    private static String mapDisability(String v) {
        String s = norm(v);
        if (s.isBlank() || has(s, "NONE")) return "None";
        if (has(s, "PHYSICAL")) return "Physical";
        if (has(s, "VISUAL")) return "Visual";
        if (has(s, "HEARING")) return "Hearing";
        if (has(s, "SPEECH")) return "Speech";
        if (has(s, "INTELLECTUAL")) return "Intellectual";
        if (has(s, "PSYCHOSOCIAL") || has(s, "MENTAL")) return "Mental/Psychosocial";
        if (has(s, "CHRONIC")) return "Due to Chronic Illness";
        if (has(s, "MULTIPLE")) return "Multiple Disabilities";
        return "";
    }

    private static String mapHealth(String v) {
        String s = norm(v);
        if (has(s, "HEALTHY")) return "Healthy";
        if (has(s, "TEMPORAR")) return "Temporarily ill";
        if (has(s, "CHRONIC")) return "Chronically ill";
        if (has(s, "IMMUNO")) return "Immunocompromised";
        if (has(s, "TERMINAL")) return "Terminally ill";
        return "";
    }

    private static String mapCleanWater(String v) {
        String s = norm(v);
        if (has(s, "YES") || has(s, "DAILY")) return "Daily access to clean and safe water";
        if (has(s, "IRREGULAR") || has(s, "LIMITED") || has(s, "OCCASION")) return "Irregular or limited access to clean water";
        if (has(s, "NO")) return "No access to clean water";
        return "";
    }

    private static String mapSanitation(String v) {
        String s = norm(v);
        if (has(s, "SAFELY") && has(s, "PRIVATE")) return "Safely managed private toilet";
        if (has(s, "SHARED")) return "Shared sanitation facility";
        if (has(s, "UNIMPROVED")) return "Unimproved sanitation facility";
        if (has(s, "NO SANITATION") || has(s, "NO")) return "No sanitation facility available";
        return "";
    }

    private static String mapHouseConstructionType(String v) {
        String s = norm(v);

        if (has(s, "SEMI") && (has(s, "ROOF") || has(s, "GI") || has(s, "ASBESTOS") || has(s, "LIGHT ROOF"))) {
            return "Semi-concrete with light roofing (GI, asbestos)";
        }
        if (has(s, "REINFORCED") || (has(s, "CONCRETE") && !has(s, "SEMI")) || has(s, "MASONRY")) {
            return "Reinforced concrete or masonry";
        }
        if (has(s, "LIGHT MATERIAL") || has(s, "BAMBOO") || has(s, "NIPA") || has(s, "COGON") || has(s, "THATCH")) {
            return "Light materials (bamboo, nipa, thatch, cogon)";
        }
        if (has(s, "MAKESHIFT") || has(s, "TARP") || has(s, "TARPAULIN") || has(s, "WOOD") || has(s, "TARPALIN")) {
            return "Makeshift shelter (wood, tarpaulin)";
        }
        return "";
    }

    private static String mapOwnership(String v) {
        String s = norm(v);
        if (has(s, "OWNED") && has(s, "FORMAL")) return "Owned with formal title";
        if (has(s, "OWNED") && has(s, "WITHOUT")) return "Owned without formal title";
        if (has(s, "RENT")) return "Rented";
        if (has(s, "INFORMAL")) return "Informal settler";
        if (has(s, "EVICT") || has(s, "DISPLAC")) return "Evicted or displaced";
        return "";
    }

    private static String mapEmployment(String v) {
        String s = norm(v);
        if (has(s, "REGULAR") && has(s, "FULL")) return "Regular full-time employment";
        if (has(s, "SELF") && has(s, "STABLE")) return "Self-employed with stable income";
        if (has(s, "SELF") && has(s, "UNSTABLE")) return "Self-employed with unstable income";
        if (has(s, "INFORMAL") || has(s, "IRREGULAR")) return "Informal or irregular employment";
        if (has(s, "UNEMPLOY")) return "Unemployed";
        return "";
    }

    private static String mapMonthlyIncome(String v) {
        String s = norm(v);
        if (has(s, "POOR") || has(s, "LESS THAN") || has(s, "12,030") || has(s, "12 030")) return "Less than 12,030 PHP (Poor)";
        if (has(s, "LOW") || (has(s, "12,030") && has(s, "24,060")) || (has(s, "12 030") && has(s, "24 060"))) return "12,030 to 24,060 PHP (Low Income)";
        if (has(s, "MIDDLE") || (has(s, "24,061") && has(s, "84,210")) || (has(s, "24 061") && has(s, "84 210"))) return "24,061 to 84,210 PHP (Middle Class)";
        if ((has(s, "UPPER") && has(s, "MIDDLE")) || (has(s, "84,211") && has(s, "144,360")) || (has(s, "84 211") && has(s, "144 360"))) return "84,211 to 144,360 PHP (Upper Middle Income)";
        if (has(s, "UPPER INCOME") || (has(s, "144,361") && has(s, "240,600")) || (has(s, "144 361") && has(s, "240 600"))) return "144,361 to 240,600 PHP (Upper Income)";
        if (has(s, "RICH") || has(s, "244,350") || has(s, "244 350") || has(s, "AT LEAST")) return "At least 244,350 (Rich)";
        return "";
    }

    private static String mapEducationLevel(String v) {
        String s = norm(v);
        if (has(s, "NO FORMAL") || s.equals("NONE")) return "No formal education";
        if (has(s, "ELEMENTARY")) return "Elementary level completed";
        if (has(s, "HIGH SCHOOL") || has(s, "SECONDARY")) return "High school level completed";
        if (has(s, "VOCATIONAL") || has(s, "TECHNICAL")) return "Vocational or technical training";
        if (has(s, "COLLEGE") || has(s, "UNIVERSITY")) return "College or university level";
        if (has(s, "GRADUATE") || has(s, "POSTGRAD")) return "Graduate education";
        return "";
    }

    private static String mapDigitalAccess(String v) {
        String s = norm(v);
        if (has(s, "RELIABLE")) return "Reliable Internet and Device Access";
        if (has(s, "INTERMITTENT")) return "Intermittent internet or device access";
        if (has(s, "LIMITED") || has(s, "SHARED")) return "Limited or shared access only";
        if (has(s, "NO DIGITAL") || s.equals("NONE")) return "No digital access";
        return "";
    }

    private static LocalDate parseBirthdate(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isBlank()) return null;

        DateTimeFormatter[] fmts = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ISO_LOCAL_DATE
        };

        for (DateTimeFormatter f : fmts) {
            try { return LocalDate.parse(s, f); } catch (Exception ignored) {}
        }
        return null;
    }

    private static String generatePHMobile() {
        Random r = new Random();
        StringBuilder sb = new StringBuilder("09");
        for (int i = 0; i < 9; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }

    private static class ExcelRow {
        final Map<String, String> values;
        final LocalDate birthdate;
        ExcelRow(Map<String, String> values, LocalDate birthdate) {
            this.values = values;
            this.birthdate = birthdate;
        }
    }

    private static List<ExcelRow> readExcelRows(InputStream in, int sheetIndex) throws Exception {
        try (Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(sheetIndex);
            Iterator<Row> it = sheet.rowIterator();
            if (!it.hasNext()) return List.of();

            DataFormatter fmt = new DataFormatter();

            Row headerRow = it.next();
            int lastCol = headerRow.getLastCellNum();

            List<String> headers = new ArrayList<>();
            for (int c = 0; c < lastCol; c++) {
                Cell cell = headerRow.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                headers.add(cell == null ? "" : fmt.formatCellValue(cell).trim());
            }

            List<ExcelRow> rows = new ArrayList<>();
            while (it.hasNext()) {
                Row r = it.next();
                if (isBlankRow(r, fmt)) continue;

                Map<String, String> map = new HashMap<>();
                LocalDate birth = null;

                for (int c = 0; c < headers.size(); c++) {
                    String key = headers.get(c);
                    if (key == null || key.isBlank()) continue;

                    Cell cell = r.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String val = cell == null ? "" : fmt.formatCellValue(cell).trim();
                    map.put(key, val);

                    if (H_BIRTHDATE.equalsIgnoreCase(key) && cell != null) {
                        try {
                            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                                Date d = cell.getDateCellValue();
                                birth = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            }
                        } catch (Exception ignored) {}
                    }
                }

                rows.add(new ExcelRow(map, birth));
            }
            return rows;
        }
    }

    private static boolean isBlankRow(Row r, DataFormatter fmt) {
        if (r == null) return true;
        int last = r.getLastCellNum();
        if (last <= 0) return true;
        for (int c = 0; c < last; c++) {
            Cell cell = r.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && !fmt.formatCellValue(cell).trim().isBlank()) return false;
        }
        return true;
    }
}