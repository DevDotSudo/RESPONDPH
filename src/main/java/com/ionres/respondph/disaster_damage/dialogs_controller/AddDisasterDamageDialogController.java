package com.ionres.respondph.disaster_damage.dialogs_controller;

import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.common.model.DisasterModel;
import com.ionres.respondph.disaster_damage.DisasterDamageController;
import com.ionres.respondph.disaster_damage.DisasterDamageModel;
import com.ionres.respondph.disaster_damage.DisasterDamageService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.SessionManager;
import com.ionres.respondph.util.UpdateTrigger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dropdown strategy — identical to DashboardController and AddFamilyController.
 *
 * Both beneficiary and disaster search fields use the VBox-wrapper pattern:
 *
 *   wrapVBox (spacing=0)
 *     ├── HBox inputRow   — always managed/visible
 *     └── ListView list   — managed=false/visible=false when closed
 *
 * showDropdown / hideDropdown toggle setManaged()+setVisible() only.
 * No editable ComboBox, no Popup, no coordinate math.
 */
public class AddDisasterDamageDialogController {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private VBox root;

    // Beneficiary search
    @FXML private HBox             beneficiaryInputRow;
    @FXML private TextField        beneficiarySearchFld;
    @FXML private ListView<String> beneficiarySearchList;

    // Disaster search
    @FXML private HBox             disasterInputRow;
    @FXML private TextField        disasterSearchFld;
    @FXML private ListView<String> disasterSearchList;

    // Other fields
    @FXML private ComboBox<String> damageSeverityFld;
    @FXML private DatePicker       assessmentDatePicker;
    @FXML private TextField        verifiedByFld;
    @FXML private TextArea         notesFld;
    @FXML private Button           saveBtn;
    @FXML private Button           exitBtn;

    // Photo upload
    @FXML private Button      uploadPhotoBtn;
    @FXML private Button      removePhotoBtn;
    @FXML private ImageView   damagePhotoView;
    @FXML private VBox        imagePlaceholder;
    @FXML private StackPane   previewBadge;
    @FXML private StackPane   imagePreviewContainer;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ObservableList<String> beneficiaryItems = FXCollections.observableArrayList();
    private final ObservableList<String> disasterItems    = FXCollections.observableArrayList();

    private List<BeneficiaryModel> allBeneficiaries;
    private List<DisasterModel>    allDisasters;

    private BeneficiaryModel selectedBeneficiary;
    private DisasterModel    selectedDisaster;

    private boolean suppressBeneficiaryListener = false;
    private boolean suppressDisasterListener    = false;
    private boolean isBeneficiarySelecting      = false;
    private boolean isDisasterSelecting         = false;

    private byte[] selectedImageBytes = null;

    // ── Drag ──────────────────────────────────────────────────────────────────
    private double xOffset = 0;
    private double yOffset = 0;

    // ── Services ──────────────────────────────────────────────────────────────
    private DisasterDamageService    disasterDamageService;
    private DisasterDamageController disasterDamageController;

    // ═════════════════════════════════════════════════════════════════════════
    // Setters
    // ═════════════════════════════════════════════════════════════════════════

    public void setDisasterDamageService(DisasterDamageService svc) {
        this.disasterDamageService = svc;
        loadBeneficiaries();
        loadDisasters();
    }

    public void setDisasterDamageController(DisasterDamageController ctrl) {
        this.disasterDamageController = ctrl;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // initialize
    // ═════════════════════════════════════════════════════════════════════════

    @FXML
    private void initialize() {
        makeDraggable();
        setupKeyHandlers();
        setupButtons();
        setupBeneficiaryDropdown();
        setupDisasterDropdown();

        damagePhotoView.fitWidthProperty().bind(
                imagePreviewContainer.widthProperty().subtract(20));
        damagePhotoView.fitHeightProperty().bind(
                imagePreviewContainer.heightProperty().subtract(20));
    }

    // ── Key shortcuts ─────────────────────────────────────────────────────────

    private void setupKeyHandlers() {
        root.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                if (isBeneficiaryDropdownVisible()) hideBeneficiaryDropdown();
                else if (isDisasterDropdownVisible()) hideDisasterDropdown();
                else exitBtn.fire();
            }
        });
        root.requestFocus();
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    private void setupButtons() {
        saveBtn.setOnAction(this::handleActions);
        exitBtn.setOnAction(this::handleActions);
        uploadPhotoBtn.setOnAction(this::handleActions);
        removePhotoBtn.setOnAction(this::handleActions);
    }

    private void handleActions(ActionEvent e) {
        Object src = e.getSource();
        if      (src == saveBtn)        addDisasterDamage();
        else if (src == exitBtn)        closeDialog();
        else if (src == uploadPhotoBtn) handleImageUpload();
        else if (src == removePhotoBtn) handleRemoveImage();
    }

    private void closeDialog() {
        hideBeneficiaryDropdown();
        hideDisasterDropdown();
        Stage stage = (Stage) exitBtn.getScene().getWindow();
        stage.hide();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Beneficiary dropdown — dashboard pattern
    // ═════════════════════════════════════════════════════════════════════════

    private void setupBeneficiaryDropdown() {
        beneficiarySearchList.setItems(beneficiaryItems);
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

        beneficiarySearchList.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                isBeneficiarySelecting = true;
                int idx = (int) (e.getY() / beneficiarySearchList.getFixedCellSize());
                if (idx >= 0 && idx < beneficiaryItems.size()) {
                    String item = beneficiaryItems.get(idx);
                    if (item != null && !item.isBlank()) handleBeneficiarySelected(item);
                }
                e.consume();
            }
        });

        beneficiarySearchList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String sel = beneficiarySearchList.getSelectionModel().getSelectedItem();
                if (sel != null) handleBeneficiarySelected(sel);
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideBeneficiaryDropdown();
                beneficiarySearchFld.requestFocus();
                e.consume();
            }
        });

        // Text filter
        beneficiarySearchFld.textProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressBeneficiaryListener) return;
            if (selectedBeneficiary != null) selectedBeneficiary = null;

            String filter = newVal == null ? "" : newVal.trim().replaceAll("\\s+", " ").toLowerCase();

            if (filter.isEmpty()) {
                beneficiaryItems.clear();
                hideBeneficiaryDropdown();
                return;
            }

            List<String> filtered = allBeneficiaries == null ? List.of()
                    : allBeneficiaries.stream()
                    .filter(b -> matchesBeneficiary(b, filter))
                    .sorted(Comparator.comparing(b -> b.getFirstName().toLowerCase()))
                    .map(this::beneficiaryDisplay)
                    .collect(Collectors.toList());

            beneficiaryItems.setAll(filtered);
            if (!filtered.isEmpty()) showBeneficiaryDropdown();
            else                     hideBeneficiaryDropdown();
        });

        beneficiarySearchFld.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case DOWN -> {
                    if (!isBeneficiaryDropdownVisible()) showBeneficiaryDropdown();
                    beneficiarySearchList.requestFocus();
                    beneficiarySearchList.getSelectionModel().select(0);
                    e.consume();
                }
                case ESCAPE -> { hideBeneficiaryDropdown(); e.consume(); }
            }
        });

        beneficiarySearchFld.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused && !isBeneficiarySelecting) Platform.runLater(this::hideBeneficiaryDropdown);
        });
    }

    private void showBeneficiaryDropdown() {
        int    rows = Math.min(8, Math.max(1, beneficiaryItems.size()));
        double h    = rows * beneficiarySearchList.getFixedCellSize() + 2;
        beneficiarySearchList.setPrefHeight(h);
        beneficiarySearchList.setMinHeight(h);
        beneficiarySearchList.setMaxHeight(h);
        beneficiarySearchList.setVisible(true);
        beneficiarySearchList.setManaged(true);
        if (!beneficiaryInputRow.getStyleClass().contains("beneficiary-search-open"))
            beneficiaryInputRow.getStyleClass().add("beneficiary-search-open");
    }

    private void hideBeneficiaryDropdown() {
        beneficiarySearchList.setVisible(false);
        beneficiarySearchList.setManaged(false);
        beneficiaryInputRow.getStyleClass().remove("beneficiary-search-open");
    }

    private boolean isBeneficiaryDropdownVisible() {
        return beneficiarySearchList.isVisible();
    }

    private void handleBeneficiarySelected(String chosen) {
        isBeneficiarySelecting = false;
        if (chosen == null || chosen.isBlank()) return;

        BeneficiaryModel found = allBeneficiaries == null ? null
                : allBeneficiaries.stream()
                .filter(b -> chosen.equals(beneficiaryDisplay(b)))
                .findFirst().orElse(null);
        if (found == null) return;

        selectedBeneficiary = found;
        suppressBeneficiaryListener = true;
        beneficiarySearchFld.setText(beneficiaryDisplay(found));
        suppressBeneficiaryListener = false;

        hideBeneficiaryDropdown();
        beneficiaryItems.clear();
        Platform.runLater(() -> disasterSearchFld.requestFocus());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Disaster dropdown — same pattern
    // ═════════════════════════════════════════════════════════════════════════

    private void setupDisasterDropdown() {
        disasterSearchList.setItems(disasterItems);
        disasterSearchList.setFixedCellSize(40);
        disasterSearchList.setCellFactory(lv -> new ListCell<>() {
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

        disasterSearchList.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                isDisasterSelecting = true;
                int idx = (int) (e.getY() / disasterSearchList.getFixedCellSize());
                if (idx >= 0 && idx < disasterItems.size()) {
                    String item = disasterItems.get(idx);
                    if (item != null && !item.isBlank()) handleDisasterSelected(item);
                }
                e.consume();
            }
        });

        disasterSearchList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String sel = disasterSearchList.getSelectionModel().getSelectedItem();
                if (sel != null) handleDisasterSelected(sel);
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideDisasterDropdown();
                disasterSearchFld.requestFocus();
                e.consume();
            }
        });

        // Text filter
        disasterSearchFld.textProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressDisasterListener) return;
            if (selectedDisaster != null) selectedDisaster = null;

            String filter = newVal == null ? "" : newVal.trim().replaceAll("\\s+", " ").toLowerCase();

            if (filter.isEmpty()) {
                disasterItems.clear();
                hideDisasterDropdown();
                return;
            }

            List<String> filtered = allDisasters == null ? List.of()
                    : allDisasters.stream()
                    .filter(d -> matchesDisaster(d, filter))
                    .sorted(Comparator.comparing(d -> d.getDisasterType().toLowerCase()))
                    .map(this::disasterDisplay)
                    .collect(Collectors.toList());

            disasterItems.setAll(filtered);
            if (!filtered.isEmpty()) showDisasterDropdown();
            else                     hideDisasterDropdown();
        });

        disasterSearchFld.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case DOWN -> {
                    if (!isDisasterDropdownVisible()) showDisasterDropdown();
                    disasterSearchList.requestFocus();
                    disasterSearchList.getSelectionModel().select(0);
                    e.consume();
                }
                case ESCAPE -> { hideDisasterDropdown(); e.consume(); }
            }
        });

        disasterSearchFld.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused && !isDisasterSelecting) Platform.runLater(this::hideDisasterDropdown);
        });
    }

    private void showDisasterDropdown() {
        int    rows = Math.min(8, Math.max(1, disasterItems.size()));
        double h    = rows * disasterSearchList.getFixedCellSize() + 2;
        disasterSearchList.setPrefHeight(h);
        disasterSearchList.setMinHeight(h);
        disasterSearchList.setMaxHeight(h);
        disasterSearchList.setVisible(true);
        disasterSearchList.setManaged(true);
        if (!disasterInputRow.getStyleClass().contains("beneficiary-search-open"))
            disasterInputRow.getStyleClass().add("beneficiary-search-open");
    }

    private void hideDisasterDropdown() {
        disasterSearchList.setVisible(false);
        disasterSearchList.setManaged(false);
        disasterInputRow.getStyleClass().remove("beneficiary-search-open");
    }

    private boolean isDisasterDropdownVisible() {
        return disasterSearchList.isVisible();
    }

    private void handleDisasterSelected(String chosen) {
        isDisasterSelecting = false;
        if (chosen == null || chosen.isBlank()) return;

        DisasterModel found = allDisasters == null ? null
                : allDisasters.stream()
                .filter(d -> chosen.equals(disasterDisplay(d)))
                .findFirst().orElse(null);
        if (found == null) return;

        selectedDisaster = found;
        suppressDisasterListener = true;
        disasterSearchFld.setText(disasterDisplay(found));
        suppressDisasterListener = false;

        hideDisasterDropdown();
        disasterItems.clear();
        Platform.runLater(() -> damageSeverityFld.requestFocus());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Search predicates + display strings
    // ═════════════════════════════════════════════════════════════════════════

    private boolean matchesBeneficiary(BeneficiaryModel b, String f) {
        String id    = String.valueOf(b.getBeneficiaryId());
        String first = b.getFirstName()  != null ? b.getFirstName().toLowerCase()  : "";
        String mid   = b.getMiddlename() != null ? b.getMiddlename().toLowerCase() : "";
        String last  = b.getLastname()   != null ? b.getLastname().toLowerCase()   : "";
        String full  = (first + " " + mid + " " + last).replaceAll("\\s+", " ").trim();
        return id.contains(f) || first.contains(f) || mid.contains(f)
                || last.contains(f) || full.contains(f)
                || beneficiaryDisplay(b).toLowerCase().contains(f);
    }

    private String beneficiaryDisplay(BeneficiaryModel b) {
        if (b == null) return "";
        return b.getBeneficiaryId() + " - "
                + b.getFirstName() + " "
                + (b.getMiddlename() != null ? b.getMiddlename() : "") + " "
                + (b.getLastname()   != null ? b.getLastname()   : "");
    }

    private boolean matchesDisaster(DisasterModel d, String f) {
        String id   = String.valueOf(d.getDisasterId());
        String type = d.getDisasterType() != null ? d.getDisasterType().toLowerCase() : "";
        String name = d.getDisasterName() != null ? d.getDisasterName().toLowerCase() : "";
        return id.contains(f) || type.contains(f) || name.contains(f)
                || disasterDisplay(d).toLowerCase().contains(f);
    }

    private String disasterDisplay(DisasterModel d) {
        if (d == null) return "";
        return d.getDisasterId() + " - " + d.getDisasterType() + " - " + d.getDisasterName();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Load data
    // ═════════════════════════════════════════════════════════════════════════

    private void loadBeneficiaries() {
        try {
            allBeneficiaries = disasterDamageService.getAllBeneficiaries();
            allBeneficiaries.sort(Comparator.comparing(b -> b.getFirstName().toLowerCase()));
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error", "Error loading beneficiaries: " + e.getMessage());
        }
    }

    private void loadDisasters() {
        try {
            allDisasters = disasterDamageService.getALlDisaster();
            allDisasters.sort(Comparator.comparing(d -> d.getDisasterType().toLowerCase()));
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error", "Error loading disasters: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Photo upload
    // ═════════════════════════════════════════════════════════════════════════

    private void handleImageUpload() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Damage Photo");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files",
                        "*.png", "*.jpg", "*.jpeg",
                        "*.PNG", "*.JPG", "*.JPEG",
                        "*.bmp", "*.BMP", "*.gif", "*.GIF"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        String home = System.getProperty("user.home");
        File oneDrive = new File(home + "/OneDrive/Pictures");
        File pictures = new File(home + "/Pictures");
        if      (oneDrive.exists()) chooser.setInitialDirectory(oneDrive);
        else if (pictures.exists()) chooser.setInitialDirectory(pictures);
        else                        chooser.setInitialDirectory(new File(home));

        File file = chooser.showOpenDialog(uploadPhotoBtn.getScene().getWindow());
        if (file == null) return;

        try {
            if (file.length() > 5 * 1024 * 1024) {
                AlertDialogManager.showWarning("File Too Large", "Please select an image under 5MB.");
                return;
            }

            selectedImageBytes = java.nio.file.Files.readAllBytes(file.toPath());

            damagePhotoView.setImage(new Image(file.toURI().toString()));
            imagePlaceholder.setVisible(false);
            imagePlaceholder.setManaged(false);
            damagePhotoView.setVisible(true);
            damagePhotoView.setManaged(true);
            previewBadge.setVisible(true);
            previewBadge.setManaged(true);
            removePhotoBtn.setVisible(true);
            removePhotoBtn.setManaged(true);

        } catch (Exception ex) {
            AlertDialogManager.showError("Error", "Failed to read image: " + ex.getMessage());
        }
    }

    private void handleRemoveImage() {
        selectedImageBytes = null;
        damagePhotoView.setImage(null);
        damagePhotoView.setVisible(false);
        damagePhotoView.setManaged(false);
        imagePlaceholder.setVisible(true);
        imagePlaceholder.setManaged(true);
        previewBadge.setVisible(false);
        previewBadge.setManaged(false);
        removePhotoBtn.setVisible(false);
        removePhotoBtn.setManaged(false);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Save
    // ═════════════════════════════════════════════════════════════════════════

    private void addDisasterDamage() {
        try {
            if (!validateInput()) return;

            String damageSeverity  = damageSeverityFld.getValue();
            String verifiedBy      = verifiedByFld.getText().trim();
            String notes           = notesFld.getText().trim();
            String assessmentDate  = assessmentDatePicker.getValue() != null
                    ? assessmentDatePicker.getValue().toString() : "";
            String regDate         = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            DisasterDamageModel record = new DisasterDamageModel(
                    selectedBeneficiary.getBeneficiaryId(),
                    selectedDisaster.getDisasterId(),
                    damageSeverity, assessmentDate,
                    verifiedBy, notes, regDate);
            record.setImage(selectedImageBytes);

            boolean success = disasterDamageService.createDisasterDamage(record);
            if (success) {
                int beneficiaryId = selectedBeneficiary.getBeneficiaryId();
                boolean cascadeOk = new UpdateTrigger()
                        .triggerCascadeUpdateWithDisaster(beneficiaryId, selectedDisaster.getDisasterId());

                if (cascadeOk) {
                    AlertDialogManager.showSuccess("Success",
                            "Disaster damage record has been successfully added.");
                } else {
                    AlertDialogManager.showWarning("Partial Success",
                            "Record added, but score recalculation encountered issues. Check logs.");
                }
                handleRemoveImage();
                disasterDamageController.loadTable();
                clearFields();
            } else {
                AlertDialogManager.showError("Error",
                        "Failed to add disaster damage record. Please try again.");
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
            beneficiarySearchFld.requestFocus(); return false;
        }
        if (selectedDisaster == null) {
            AlertDialogManager.showWarning("Validation Error",
                    "Please search and select a disaster.");
            disasterSearchFld.requestFocus(); return false;
        }
        if (damageSeverityFld.getValue() == null
                || damageSeverityFld.getValue().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Damage severity is required.");
            damageSeverityFld.requestFocus(); return false;
        }
        if (assessmentDatePicker.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Assessment date is required.");
            assessmentDatePicker.requestFocus(); return false;
        }
        if (verifiedByFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Verified by is required.");
            verifiedByFld.requestFocus(); return false;
        }
        return true;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Clear fields
    // ═════════════════════════════════════════════════════════════════════════

    private void clearFields() {
        suppressBeneficiaryListener = true;
        beneficiarySearchFld.clear();
        suppressBeneficiaryListener = false;
        selectedBeneficiary = null;
        hideBeneficiaryDropdown();
        beneficiaryItems.clear();

        suppressDisasterListener = true;
        disasterSearchFld.clear();
        suppressDisasterListener = false;
        selectedDisaster = null;
        hideDisasterDropdown();
        disasterItems.clear();

        damageSeverityFld.getSelectionModel().clearSelection();
        assessmentDatePicker.setValue(null);
        verifiedByFld.clear();
        notesFld.clear();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Drag
    // ═════════════════════════════════════════════════════════════════════════

    private void makeDraggable() {
        root.setOnMousePressed(e -> {
            if (isBeneficiarySelecting || isDisasterSelecting) return;
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        root.setOnMouseDragged(e -> {
            if (isBeneficiarySelecting || isDisasterSelecting) return;
            Stage s = (Stage) root.getScene().getWindow();
            s.setX(e.getScreenX() - xOffset);
            s.setY(e.getScreenY() - yOffset);
        });
    }
}