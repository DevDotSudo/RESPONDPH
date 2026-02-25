package com.ionres.respondph.familymembers.dialogs_controller;

import com.ionres.respondph.beneficiary.AgeScoreCalculate;
import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.familymembers.FamilyMemberService;
import com.ionres.respondph.familymembers.FamilyMembersController;
import com.ionres.respondph.familymembers.FamilyMembersModel;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.UpdateTrigger;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dropdown strategy — StackPane overlay pattern.
 *
 *   StackPane (height locked to 44px)
 *     ├── HBox beneficiaryInputRow  — always visible, sizes the StackPane
 *     └── ListView beneficiarySearchList — translateY=44, viewOrder=-1,
 *                                          floats over content below
 *
 * The ListView is always managed=true. Show/hide via setVisible() only.
 * The parent form-section has viewOrder=-1 so it paints above sections below.
 *
 * Chevron (fx:id="beneficiaryCaretIcon") is a clickable toggle:
 *   - Empty field  → show ALL beneficiaries
 *   - Typed field  → show current filtered results
 *   - Open list    → close list
 * Chevron rotates 180° when open, resets to 0° when closed.
 */
public class AddFamilyController {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private VBox root;
    @FXML private TextField            firstNameFld;
    @FXML private TextField            middleNameFld;
    @FXML private TextField            lastNameFld;
    @FXML private DatePicker           birthDatePicker;
    @FXML private ComboBox<String>     genderSelection;
    @FXML private ComboBox<String>     maritalStatusSelection;
    @FXML private ComboBox<String>     disabilityTypeSelection;
    @FXML private ComboBox<String>     healthConditionSelection;
    @FXML private ComboBox<String>     employmentStatusSelection;
    @FXML private ComboBox<String>     educationLevelSelection;
    @FXML private ComboBox<String>     relationshipSelection;
    @FXML private TextArea             notesFld;
    @FXML private Button               saveBtn;
    @FXML private Button               exitBtn;

    // ── Beneficiary search ────────────────────────────────────────────────────
    @FXML private HBox                 beneficiaryInputRow;
    @FXML private TextField            beneficiaryNameFld;
    @FXML private ListView<String>     beneficiarySearchList;
    @FXML private FontAwesomeIconView  beneficiaryCaretIcon;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ObservableList<String> searchItems = FXCollections.observableArrayList();
    private List<BeneficiaryModel> allBeneficiaries;
    private BeneficiaryModel       selectedBeneficiary;
    private boolean suppressListener    = false;
    private boolean isSelectingFromList = false;

    // ── Drag ──────────────────────────────────────────────────────────────────
    private double xOffset = 0;
    private double yOffset = 0;

    // ── Services ──────────────────────────────────────────────────────────────
    private FamilyMemberService     familyMemberService;
    private FamilyMembersController familyMembersController;

    // ═════════════════════════════════════════════════════════════════════════
    // Setters
    // ═════════════════════════════════════════════════════════════════════════

    public void setFamilyMemberService(FamilyMemberService svc) {
        this.familyMemberService = svc;
        loadBeneficiaries();
    }

    public void setFamilyMemberController(FamilyMembersController ctrl) {
        this.familyMembersController = ctrl;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // initialize
    // ═════════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        makeDraggable();
        setupButtons();
        setupKeyHandlers();
        setupDropdown();
        wireSearchTextField();
        wireChevronToggle();
    }

    // ── Key shortcuts ─────────────────────────────────────────────────────────

    private void setupKeyHandlers() {
        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                if (isDropdownVisible()) hideDropdown();
                else exitBtn.fire();
            }
        });
        root.requestFocus();
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    private void setupButtons() {
        saveBtn.setOnAction(this::handleSave);
        exitBtn.setOnAction(this::handleExit);
    }

    private void handleSave(ActionEvent e) { addFamilyMembers(); }
    private void handleExit(ActionEvent e) { closeDialog(); }

    private void closeDialog() {
        hideDropdown();
        Stage stage = (Stage) exitBtn.getScene().getWindow();
        stage.hide();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Dropdown setup
    // ═════════════════════════════════════════════════════════════════════════

    private void setupDropdown() {
        beneficiarySearchList.setItems(searchItems);
        beneficiarySearchList.setFixedCellSize(40);

        beneficiarySearchList.setCellFactory(lv -> new ListCell<>() {
            {
                setMaxWidth(Double.MAX_VALUE);
                setTextOverrun(OverrunStyle.ELLIPSIS);
                setWrapText(false);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((item == null || empty) ? null : item);
            }
        });

        // MOUSE_PRESSED fires before TextField loses focus — capture selection here
        beneficiarySearchList.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                isSelectingFromList = true;
                int index = (int) (e.getY() / beneficiarySearchList.getFixedCellSize());
                if (index >= 0 && index < searchItems.size()) {
                    String item = searchItems.get(index);
                    if (item != null && !item.isBlank()) handleBeneficiarySelected(item);
                }
                e.consume();
            }
        });

        // Keyboard navigation inside the list
        beneficiarySearchList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String sel = beneficiarySearchList.getSelectionModel().getSelectedItem();
                if (sel != null) handleBeneficiarySelected(sel);
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideDropdown();
                beneficiaryNameFld.requestFocus();
                e.consume();
            }
        });
    }

    // ── Wire the TextField ────────────────────────────────────────────────────

    private void wireSearchTextField() {

        // Text change → filter and show/hide dropdown
        beneficiaryNameFld.textProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressListener) return;

            if (selectedBeneficiary != null) selectedBeneficiary = null;

            String filter = (newVal == null)
                    ? ""
                    : newVal.trim().replaceAll("\\s+", " ").toLowerCase();

            if (filter.isEmpty()) {
                searchItems.clear();
                hideDropdown();
                return;
            }

            List<String> filtered = allBeneficiaries == null
                    ? List.of()
                    : allBeneficiaries.stream()
                    .filter(b -> matchesBeneficiary(b, filter))
                    .sorted(Comparator.comparing(b -> b.getFirstName().toLowerCase()))
                    .map(this::displayText)
                    .collect(Collectors.toList());

            searchItems.setAll(filtered);

            if (!filtered.isEmpty()) showDropdown();
            else                     hideDropdown();
        });

        // DOWN arrow moves focus into the list
        beneficiaryNameFld.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case DOWN -> {
                    if (!isDropdownVisible()) {
                        populateAllBeneficiaries();
                        if (!searchItems.isEmpty()) showDropdown();
                    }
                    if (isDropdownVisible()) {
                        beneficiarySearchList.requestFocus();
                        beneficiarySearchList.getSelectionModel().select(0);
                    }
                    e.consume();
                }
                case ESCAPE -> { hideDropdown(); e.consume(); }
                case ENTER  -> {
                    if (!isDropdownVisible()) saveBtn.fire();
                    e.consume();
                }
            }
        });

        // Focus lost: hide only when the user is NOT pressing a list row
        beneficiaryNameFld.focusedProperty().addListener((obs, was, isFocused) -> {
            if (!isFocused && !isSelectingFromList) Platform.runLater(this::hideDropdown);
        });
    }

    // ── Chevron / input row click toggle ──────────────────────────────────────

    private void wireChevronToggle() {
        beneficiaryInputRow.setOnMouseClicked(e -> {
            // Let the TextField handle its own clicks normally
            if (e.getTarget() == beneficiaryNameFld) return;

            if (isDropdownVisible()) {
                hideDropdown();
            } else {
                String current = beneficiaryNameFld.getText();
                if (current == null || current.isBlank()) {
                    populateAllBeneficiaries();
                }
                // else text listener already populated filtered results
                if (!searchItems.isEmpty()) showDropdown();
                beneficiaryNameFld.requestFocus();
            }
            e.consume();
        });
    }

    /** Fills searchItems with the full unfiltered beneficiary list. */
    private void populateAllBeneficiaries() {
        if (allBeneficiaries == null) return;
        List<String> all = allBeneficiaries.stream()
                .map(this::displayText)
                .collect(Collectors.toList());
        searchItems.setAll(all);
    }

    // ── Show / hide ───────────────────────────────────────────────────────────

    private void showDropdown() {
        int    rows = Math.min(8, Math.max(1, searchItems.size()));
        double h    = rows * beneficiarySearchList.getFixedCellSize() + 2;

        beneficiarySearchList.setPrefHeight(h);
        beneficiarySearchList.setMinHeight(h);
        beneficiarySearchList.setMaxHeight(h);

        // managed stays true always — StackPane height is locked to 44px
        beneficiarySearchList.setVisible(true);

        if (!beneficiaryInputRow.getStyleClass().contains("beneficiary-search-open"))
            beneficiaryInputRow.getStyleClass().add("beneficiary-search-open");

        if (beneficiaryCaretIcon != null) beneficiaryCaretIcon.setRotate(180);
    }

    private void hideDropdown() {
        beneficiarySearchList.setVisible(false);
        beneficiaryInputRow.getStyleClass().remove("beneficiary-search-open");
        if (beneficiaryCaretIcon != null) beneficiaryCaretIcon.setRotate(0);
    }

    private boolean isDropdownVisible() {
        return beneficiarySearchList.isVisible();
    }

    // ── Confirm pick ──────────────────────────────────────────────────────────

    private void handleBeneficiarySelected(String chosenDisplayText) {
        isSelectingFromList = false;

        if (chosenDisplayText == null || chosenDisplayText.isBlank()) return;

        BeneficiaryModel found = allBeneficiaries == null ? null
                : allBeneficiaries.stream()
                .filter(b -> chosenDisplayText.equals(displayText(b)))
                .findFirst().orElse(null);

        if (found == null) return;

        selectedBeneficiary = found;

        suppressListener = true;
        beneficiaryNameFld.setText(displayText(found));
        suppressListener = false;

        hideDropdown();
        searchItems.clear();
        Platform.runLater(() -> firstNameFld.requestFocus());
    }

    // ── Search predicate ──────────────────────────────────────────────────────

    private boolean matchesBeneficiary(BeneficiaryModel b, String filter) {
        String id        = String.valueOf(b.getBeneficiaryId());
        String firstName = b.getFirstName()  != null ? b.getFirstName().toLowerCase()  : "";
        String midName   = b.getMiddlename() != null ? b.getMiddlename().toLowerCase() : "";
        String lastName  = b.getLastname()   != null ? b.getLastname().toLowerCase()   : "";
        String fullName  = (firstName + " " + midName + " " + lastName)
                .replaceAll("\\s+", " ").trim();
        String display   = displayText(b).toLowerCase();
        return id.contains(filter)
                || firstName.contains(filter)
                || midName.contains(filter)
                || lastName.contains(filter)
                || fullName.contains(filter)
                || display.contains(filter);
    }

    // ── Display string ────────────────────────────────────────────────────────

    private String displayText(BeneficiaryModel b) {
        if (b == null) return "";
        return b.getBeneficiaryId() + " - "
                + b.getFirstName() + " "
                + (b.getMiddlename() != null ? b.getMiddlename() : "") + " "
                + (b.getLastname()   != null ? b.getLastname()   : "");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Load beneficiaries
    // ═════════════════════════════════════════════════════════════════════════

    private void loadBeneficiaries() {
        try {
            allBeneficiaries = familyMemberService.getAllBeneficiaries();
            allBeneficiaries.sort(Comparator.comparing(b -> b.getFirstName().toLowerCase()));
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Error loading beneficiaries: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Save
    // ═════════════════════════════════════════════════════════════════════════

    private void addFamilyMembers() {
        try {
            if (!validateInput()) return;

            String firstName        = firstNameFld.getText().trim();
            String middleName       = middleNameFld.getText().trim();
            String lastName         = lastNameFld.getText().trim();
            String relationship     = relationshipSelection.getValue();
            String birthDate        = birthDatePicker.getValue() != null
                    ? birthDatePicker.getValue().toString() : "";
            double ageScore         = AgeScoreCalculate.calculateAgeScoreFromBirthdate(birthDate);
            String gender           = genderSelection.getValue();
            String maritalStatus    = maritalStatusSelection.getValue();
            String disabilityType   = disabilityTypeSelection.getValue();
            String healthCondition  = healthConditionSelection.getValue();
            String employmentStatus = employmentStatusSelection.getValue();
            String educationalLevel = educationLevelSelection.getValue();
            String notes            = notesFld.getText().trim();
            String regDate          = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));
            int    beneficiaryId    = selectedBeneficiary.getBeneficiaryId();

            FamilyMembersModel fm = new FamilyMembersModel(
                    firstName, middleName, lastName, relationship,
                    birthDate, ageScore, gender, maritalStatus,
                    disabilityType, healthCondition, employmentStatus, educationalLevel,
                    beneficiaryId, notes, regDate);

            boolean success = familyMemberService.createfamilyMember(fm);
            if (success) {
                new UpdateTrigger().triggerCascadeUpdate(beneficiaryId);
                AlertDialogManager.showSuccess("Success",
                        "Family member has been successfully added.\n"
                                + "Add more family members or click 'Calculate Household Scores' when done.");
                familyMembersController.loadTable();
                clearFields();
            } else {
                AlertDialogManager.showError("Error",
                        "Failed to add family member. Please try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Error", "An error occurred: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Validation
    // ═════════════════════════════════════════════════════════════════════════

    private boolean validateInput() {
        if (selectedBeneficiary == null) {
            AlertDialogManager.showWarning("Validation Error",
                    "Please search and select a beneficiary.");
            beneficiaryNameFld.requestFocus(); return false;
        }
        if (firstNameFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "First name is required.");
            firstNameFld.requestFocus(); return false;
        }
        if (middleNameFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Middle name is required.");
            middleNameFld.requestFocus(); return false;
        }
        if (lastNameFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Last name is required.");
            lastNameFld.requestFocus(); return false;
        }
        if (relationshipSelection.getValue() == null
                || relationshipSelection.getValue().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error",
                    "Relationship to beneficiary is required.");
            relationshipSelection.requestFocus(); return false;
        }
        if (birthDatePicker.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Birth date is required.");
            birthDatePicker.requestFocus(); return false;
        }
        if (genderSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Gender is required.");
            genderSelection.requestFocus(); return false;
        }
        if (maritalStatusSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Marital status is required.");
            maritalStatusSelection.requestFocus(); return false;
        }
        if (disabilityTypeSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Disability type is required.");
            disabilityTypeSelection.requestFocus(); return false;
        }
        if (healthConditionSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Health condition is required.");
            healthConditionSelection.requestFocus(); return false;
        }
        if (employmentStatusSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Employment status is required.");
            employmentStatusSelection.requestFocus(); return false;
        }
        if (educationLevelSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Educational level is required.");
            educationLevelSelection.requestFocus(); return false;
        }
        return true;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Clear fields
    // ═════════════════════════════════════════════════════════════════════════

    private void clearFields() {
        suppressListener = true;
        beneficiaryNameFld.clear();
        suppressListener = false;

        selectedBeneficiary = null;
        hideDropdown();
        searchItems.clear();

        firstNameFld.clear();
        middleNameFld.clear();
        lastNameFld.clear();
        birthDatePicker.setValue(null);

        genderSelection.getSelectionModel().clearSelection();
        maritalStatusSelection.getSelectionModel().clearSelection();
        relationshipSelection.getSelectionModel().clearSelection();
        disabilityTypeSelection.getSelectionModel().clearSelection();
        healthConditionSelection.getSelectionModel().clearSelection();
        employmentStatusSelection.getSelectionModel().clearSelection();
        educationLevelSelection.getSelectionModel().clearSelection();
        notesFld.clear();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Drag
    // ═════════════════════════════════════════════════════════════════════════

    private void makeDraggable() {
        root.setOnMousePressed(event -> {
            if (isSelectingFromList) return;
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            if (isSelectingFromList) return;
            Stage s = (Stage) root.getScene().getWindow();
            s.setX(event.getScreenX() - xOffset);
            s.setY(event.getScreenY() - yOffset);
        });
    }
}