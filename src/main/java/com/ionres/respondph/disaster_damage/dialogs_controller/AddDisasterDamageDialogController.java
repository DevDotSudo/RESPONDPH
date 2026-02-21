package com.ionres.respondph.disaster_damage.dialogs_controller;

import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.common.model.DisasterModel;
import com.ionres.respondph.disaster_damage.DisasterDamageController;
import com.ionres.respondph.disaster_damage.DisasterDamageModel;
import com.ionres.respondph.disaster_damage.DisasterDamageService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.SessionManager;
import com.ionres.respondph.util.UpdateTrigger;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AddDisasterDamageDialogController {

    @FXML private VBox root;
    @FXML private ComboBox<BeneficiaryModel> beneficiaryNameFld;
    @FXML private ComboBox<DisasterModel> disasterFld;
    @FXML private ComboBox<String> damageSeverityFld;
    @FXML private TextField verifiedByFld;
    @FXML private DatePicker assessmentDatePicker;
    @FXML private TextArea notesFld;
    @FXML private Button saveBtn, exitBtn;
    @FXML private Button uploadPhotoBtn;
    @FXML private Button removePhotoBtn;
    @FXML private ImageView damagePhotoView;
    @FXML private VBox imagePlaceholder;
    @FXML private StackPane previewBadge;
    @FXML private StackPane imagePreviewContainer;
    private byte[] selectedImageBytes = null;
    private List<BeneficiaryModel> allBeneficiaries;
    private List<DisasterModel> allDisaster;
    private DisasterDamageService disasterDamageService;
    private DisasterDamageController disasterDamageController;
    private boolean isSaving = false;

    public void setDisasterDamageService(DisasterDamageService disasterDamageService) {
        this.disasterDamageService = disasterDamageService;
        loadBeneficiaries();
        loadDisaster();
    }

    public void setDisasterDamageController(DisasterDamageController disasterDamageController) {
        this.disasterDamageController = disasterDamageController;
    }

    @FXML
    private void initialize() {
        setupKeyHandlers();
        EventHandler<ActionEvent> handler = this::handleActions;
        saveBtn.setOnAction(handler);
        exitBtn.setOnAction(handler);
        uploadPhotoBtn.setOnAction(handler);
        removePhotoBtn.setOnAction(handler);

        damagePhotoView.fitWidthProperty().bind(imagePreviewContainer.widthProperty().subtract(20));
        damagePhotoView.fitHeightProperty().bind(imagePreviewContainer.heightProperty().subtract(20));
    }

    private void handleActions(ActionEvent event) {
        Object src =  event.getSource();

        if(src == saveBtn){
            addDisasterDamage();
        }
        else if (src == exitBtn){
            closeDialog();
        }
        else if (src == uploadPhotoBtn){
            handleImageUpload();
        }
        else if (src == removePhotoBtn){
            handleRemoveImage();
        }
    }

    private void setupKeyHandlers() {
        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER: saveBtn.fire(); break;
                case ESCAPE: exitBtn.fire(); break;
            }
        });
        root.requestFocus();
    }


    private void closeDialog() {
        Stage stage = (Stage) exitBtn.getScene().getWindow();
        stage.hide();
    }

    private void loadBeneficiaries() {
        try {
            allBeneficiaries = disasterDamageService.getAllBeneficiaries();
            allBeneficiaries.sort(Comparator.comparing(b -> b.getFirstName().toLowerCase()));

            beneficiaryNameFld.setEditable(true);
            beneficiaryNameFld.getItems().setAll(allBeneficiaries);

            beneficiaryNameFld.setConverter(new StringConverter<>() {
                @Override
                public String toString(BeneficiaryModel b) {
                    if (b == null) return "";
                    return b.getBeneficiaryId() + " - " +
                            b.getFirstName() + " " +
                            (b.getMiddlename() != null ? b.getMiddlename() : "") + " " +
                            (b.getLastname() != null ? b.getLastname() : "");
                }
                @Override
                public BeneficiaryModel fromString(String text) {
                    if (text == null || text.trim().isEmpty()) {
                        return null;
                    }

                    String searchText = text.trim();
                    return allBeneficiaries.stream()
                            .filter(b -> {
                                String fullDisplay = b.getBeneficiaryId() + " - " +
                                        b.getFirstName() + " " +
                                        (b.getMiddlename() != null ? b.getMiddlename() : "") + " " +
                                        (b.getLastname() != null ? b.getLastname() : "");
                                return fullDisplay.equalsIgnoreCase(searchText);
                            })
                            .findFirst()
                            .orElse(null);
                }
            });

            beneficiaryNameFld.setCellFactory(cb -> new ListCell<>() {
                @Override
                protected void updateItem(BeneficiaryModel item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("");
                    } else {
                        setText(item.getBeneficiaryId() + " - " +
                                item.getFirstName() + " " +
                                (item.getMiddlename() != null ? item.getMiddlename() : "") + " " +
                                (item.getLastname() != null ? item.getLastname() : ""));
                    }
                }
            });

            beneficiaryNameFld.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(BeneficiaryModel item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("");
                    } else {
                        setText(item.getBeneficiaryId() + " - " +
                                item.getFirstName() + " " +
                                (item.getMiddlename() != null ? item.getMiddlename() : "") + " " +
                                (item.getLastname() != null ? item.getLastname() : ""));
                    }
                }
            });

            setupBeneficiarySearchFilter();

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Error loading beneficiaries: " + e.getMessage());
        }
    }

    private void handleImageUpload() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Damage Photo");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.*"),
                new FileChooser.ExtensionFilter("Image Files",
                        "*.png", "*.jpg", "*.jpeg",
                        "*.PNG", "*.JPG", "*.JPEG",
                        "*.bmp", "*.BMP", "*.gif", "*.GIF")
        );

        String userHome = System.getProperty("user.home");
        File oneDrivePictures = new File(userHome + "/OneDrive/Pictures");
        File localPictures    = new File(userHome + "/Pictures");

        if (oneDrivePictures.exists()) {
            chooser.setInitialDirectory(oneDrivePictures);
        } else if (localPictures.exists()) {
            chooser.setInitialDirectory(localPictures);
        } else {
            chooser.setInitialDirectory(new File(userHome));
        }

        File file = chooser.showOpenDialog(uploadPhotoBtn.getScene().getWindow());
        if (file != null) {
            try {

                if (file.length() > 5 * 1024 * 1024) {
                    AlertDialogManager.showWarning("File Too Large", "Please select an image under 5MB.");
                    return;
                }

                selectedImageBytes = java.nio.file.Files.readAllBytes(file.toPath());

                Image img = new Image(file.toURI().toString());
                damagePhotoView.setImage(img);

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
    }

    private void handleRemoveImage() {
        selectedImageBytes = null;
        damagePhotoView.setImage(null);

        // Hide image, show placeholder again
        damagePhotoView.setVisible(false);
        damagePhotoView.setManaged(false);

        imagePlaceholder.setVisible(true);
        imagePlaceholder.setManaged(true);

        // Hide badge and remove button
        previewBadge.setVisible(false);
        previewBadge.setManaged(false);

        removePhotoBtn.setVisible(false);
        removePhotoBtn.setManaged(false);
    }


    private void setupBeneficiarySearchFilter() {
        beneficiaryNameFld.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            String search = (newText == null) ? "" : newText.toLowerCase().trim();

            if(isSaving){
                return;
            }

            BeneficiaryModel selected = beneficiaryNameFld.getSelectionModel().getSelectedItem();
            if (selected != null && newText != null) {
                String selectedText = selected.getBeneficiaryId() + " - " +
                        selected.getFirstName() + " " +
                        (selected.getMiddlename() != null ? selected.getMiddlename() : "") + " " +
                        (selected.getLastname() != null ? selected.getLastname() : "");
                if (selectedText.equalsIgnoreCase(newText.trim())) {
                    return;
                }
            }

            List<BeneficiaryModel> filtered;
            if (search.isEmpty()) {
                filtered = allBeneficiaries;
            } else {
                filtered = allBeneficiaries.stream()
                        .filter(b -> {
                            String id = String.valueOf(b.getBeneficiaryId());
                            String firstName = (b.getFirstName() != null) ? b.getFirstName().toLowerCase() : "";
                            String middleName = (b.getMiddlename() != null) ? b.getMiddlename().toLowerCase() : "";
                            String lastName = (b.getLastname() != null) ? b.getLastname().toLowerCase() : "";

                            return id.contains(search) ||
                                    firstName.contains(search) ||
                                    middleName.contains(search) ||
                                    lastName.contains(search);
                        })
                        .sorted(Comparator.comparing(b -> b.getFirstName().toLowerCase()))
                        .collect(Collectors.toList());
            }

            beneficiaryNameFld.getItems().setAll(filtered);

            if (!beneficiaryNameFld.isShowing() && !filtered.isEmpty() && !search.isEmpty()) {
                beneficiaryNameFld.show();
            }
        });
    }

    private void loadDisaster() {
        try {
            allDisaster = disasterDamageService.getALlDisaster();
            allDisaster.sort(Comparator.comparing(d -> d.getDisasterType().toLowerCase()));

            disasterFld.setEditable(true);
            disasterFld.getItems().setAll(allDisaster);

            disasterFld.setConverter(new StringConverter<>() {
                @Override
                public String toString(DisasterModel d) {
                    if (d == null) return "";
                    return d.getDisasterId() + " - " +
                            d.getDisasterType() + " - " +
                            d.getDisasterName();
                }

                @Override
                public DisasterModel fromString(String text) {
                    return allDisaster.stream()
                            .filter(d -> (d.getDisasterId() + " - " +
                                    d.getDisasterType() + " - " +
                                    d.getDisasterName()).equalsIgnoreCase(text))
                            .findFirst()
                            .orElse(null);
                }
            });

            disasterFld.setCellFactory(cb -> new ListCell<>() {
                @Override
                protected void updateItem(DisasterModel item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" :
                            item.getDisasterId() + " - " +
                                    item.getDisasterType() + " - " +
                                    item.getDisasterName());
                }
            });

            disasterFld.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(DisasterModel item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" :
                            item.getDisasterId() + " - " +
                                    item.getDisasterType() + " - " +
                                    item.getDisasterName());
                }
            });

            setupDisasterSearchFilter();

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Error loading disasters: " + e.getMessage());
        }
    }

    private void setupDisasterSearchFilter() {
        disasterFld.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            String search = newText.toLowerCase().trim();

            if (isSaving){
                return;
            }

            List<DisasterModel> filtered;
            if (search.isEmpty()) {
                filtered = allDisaster;
            } else {
                filtered = allDisaster.stream()
                        .filter(d -> String.valueOf(d.getDisasterId()).contains(search) ||
                                d.getDisasterType().toLowerCase().contains(search) ||
                                d.getDisasterName().toLowerCase().contains(search))
                        .sorted(Comparator.comparing(d -> d.getDisasterType().toLowerCase()))
                        .collect(Collectors.toList());
            }

            disasterFld.getItems().setAll(filtered);

            if (!disasterFld.isShowing() && !filtered.isEmpty()) {
                disasterFld.show();
            }
        });
    }

    private void addDisasterDamage() {
        try {
            isSaving = true;
            beneficiaryNameFld.hide();
            disasterFld.hide();

            if (!validateInput()) {
                isSaving = false;
                return;
            }

            BeneficiaryModel beneficiary = beneficiaryNameFld.getValue();
            DisasterModel disaster = disasterFld.getValue();
            String damageSeverity = damageSeverityFld.getValue();
            String verifiedBy = verifiedByFld.getText().trim();
            String notes = notesFld.getText().trim();

            String assessmentDate = assessmentDatePicker.getValue() != null ?
                    assessmentDatePicker.getValue().toString() : "";

            String regDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            DisasterDamageModel disasterDamage = new DisasterDamageModel(
                    beneficiary.getBeneficiaryId(),
                    disaster.getDisasterId(),
                    damageSeverity,
                    assessmentDate,
                    verifiedBy,
                    notes,
                    regDate
            );
            disasterDamage.setImage(selectedImageBytes);
            boolean success = disasterDamageService.createDisasterDamage(disasterDamage);

            if (success) {

                System.out.println("========== DISASTER DAMAGE ADDED - TRIGGERING CASCADE ==========");

                int beneficiaryId = beneficiary.getBeneficiaryId();
                int adminId = SessionManager.getInstance().getCurrentAdminId();

                UpdateTrigger trigger = new UpdateTrigger();
                boolean cascadeSuccess = trigger.triggerCascadeUpdateWithDisaster(beneficiaryId, disaster.getDisasterId());

                if (cascadeSuccess) {
                    AlertDialogManager.showSuccess("Success",
                            "Disaster damage record has been successfully added.\n"
                    );
                    handleRemoveImage();
                } else {
                    AlertDialogManager.showWarning("Partial Success",
                            "Disaster damage record has been added, but score recalculation encountered issues.\n" +
                                    "Please check the console for details.");
                }

                disasterDamageController.loadTable();
                clearFields();

            } else {
                AlertDialogManager.showError("Error",
                        "Failed to add disaster damage record. Please try again.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Error",
                    "An error occurred while adding disaster damage: " + e.getMessage());
        }
        finally {
            isSaving = false;
        }
    }

    private boolean validateInput() {
        if (beneficiaryNameFld.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Beneficiary is required.");
            beneficiaryNameFld.requestFocus();
            return false;
        }

        if (disasterFld.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Disaster is required.");
            disasterFld.requestFocus();
            return false;
        }

        if (damageSeverityFld.getValue() == null || damageSeverityFld.getValue().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Damage severity is required.");
            damageSeverityFld.requestFocus();
            return false;
        }

        if (assessmentDatePicker.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Assessment date is required.");
            assessmentDatePicker.requestFocus();
            return false;
        }

        if (verifiedByFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Verified by is required.");
            verifiedByFld.requestFocus();
            return false;
        }

        return true;
    }

    private void clearFields() {
        beneficiaryNameFld.getSelectionModel().clearSelection();
        beneficiaryNameFld.hide();
        disasterFld.hide();
        disasterFld.getSelectionModel().clearSelection();
        damageSeverityFld.getSelectionModel().clearSelection();
        assessmentDatePicker.setValue(null);
        verifiedByFld.clear();
        notesFld.clear();
    }
}