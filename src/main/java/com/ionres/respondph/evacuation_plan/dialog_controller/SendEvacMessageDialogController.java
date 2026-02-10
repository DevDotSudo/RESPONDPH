package com.ionres.respondph.evacuation_plan.dialog_controller;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.disaster.DisasterModel;
import com.ionres.respondph.evacuation_plan.EvacuationPlanDAO;
import com.ionres.respondph.evacuation_plan.EvacuationPlanDAOImpl;
import com.ionres.respondph.evac_site.EvacSiteModel;
import com.ionres.respondph.sendsms.SmsService;
import com.ionres.respondph.sendsms.SmsServiceImpl;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.SMSSender;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.List;

public class SendEvacMessageDialogController {

    @FXML private VBox root;
    @FXML private Button closeButton;
    @FXML private ComboBox<EvacSiteModel> cbEvacSite;
    @FXML private ComboBox<DisasterModel> cbDisaster;
    @FXML private Label lblBeneficiaryCount;
    @FXML private TextArea txtMessage;
    @FXML private Label lblCharCount;
    @FXML private CheckBox chkAppendSiteName;
    @FXML private Button btnPreview;
    @FXML private RadioButton rbGsm;
    @FXML private RadioButton rbApi;
    @FXML private HBox gsmPortBox;
    @FXML private ComboBox<String> cbGsmPort;
    @FXML private Button btnRefreshPorts;
    @FXML private Label lblConnectionStatus;
    @FXML private Button btnCancel;
    @FXML private Button btnSend;

    private Stage dialogStage;
    private EvacuationPlanDAO evacPlanDAO;
    private SmsService smsService;
    private SMSSender smsSender;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        evacPlanDAO = new EvacuationPlanDAOImpl(DBConnection.getInstance());
        smsService = new SmsServiceImpl();
        smsSender = SMSSender.getInstance();

        setupRadioButtons();
        setupComboBoxes();
        setupTextArea();
        makeDraggable();
        loadEvacuationSites();
        loadDisasters();
        setupSelectionListeners();
        populateGsmPorts();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    private void setupRadioButtons() {
        ToggleGroup group = new ToggleGroup();
        rbGsm.setToggleGroup(group);
        rbApi.setToggleGroup(group);

        rbApi.setSelected(true);

        rbApi.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                gsmPortBox.setVisible(false);
                gsmPortBox.setManaged(false);
                updateSendButtonState();
            }
        });

        rbGsm.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                gsmPortBox.setVisible(true);
                gsmPortBox.setManaged(true);
                populateGsmPorts();
                updateSendButtonState();
            }
        });
    }

    private void setupComboBoxes() {
        cbEvacSite.setConverter(new StringConverter<EvacSiteModel>() {
            @Override
            public String toString(EvacSiteModel evacSite) {
                return evacSite == null ? "" : evacSite.getName();
            }

            @Override
            public EvacSiteModel fromString(String string) {
                return null;
            }
        });

        // Disaster ComboBox
        cbDisaster.setConverter(new StringConverter<DisasterModel>() {
            @Override
            public String toString(DisasterModel disaster) {
                return disaster == null ? "" : disaster.getDisasterName() + " (" + disaster.getDate() + ")";
            }

            @Override
            public DisasterModel fromString(String string) {
                return null;
            }
        });
    }

    private void setupTextArea() {
        txtMessage.textProperty().addListener((obs, oldVal, newVal) -> {
            int length = newVal == null ? 0 : newVal.length();
            lblCharCount.setText(length + "/160 characters");

            if (length > 160) {
                lblCharCount.setStyle("-fx-text-fill: #ef4444;");
            } else {
                lblCharCount.setStyle("-fx-text-fill: #6b7280;");
            }
        });
    }

    private void setupSelectionListeners() {
        cbEvacSite.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateBeneficiaryCount();
            updateSendButtonState();
        });

        cbDisaster.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateBeneficiaryCount();
            updateSendButtonState();
        });
    }

    private void loadEvacuationSites() {
        try {
            List<EvacSiteModel> evacSites = AppContext.evacSiteService.getAllEvacSites();
            cbEvacSite.setItems(FXCollections.observableArrayList(evacSites));
        } catch (Exception e) {
            System.err.println("Error loading evacuation sites: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadDisasters() {
        try {
            List<DisasterModel> disasters = AppContext.disasterService.getAllDisaster();
            cbDisaster.setItems(FXCollections.observableArrayList(disasters));
        } catch (Exception e) {
            System.err.println("Error loading disasters: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateBeneficiaryCount() {
        EvacSiteModel selectedSite = cbEvacSite.getValue();
        DisasterModel selectedDisaster = cbDisaster.getValue();

        if (selectedSite == null || selectedDisaster == null) {
            lblBeneficiaryCount.setText("Beneficiaries: 0");
            return;
        }

        try {
            List<BeneficiaryModel> beneficiaries = getBeneficiariesForEvacSite(
                    selectedSite.getEvacId(), selectedDisaster.getDisasterId());
            lblBeneficiaryCount.setText("Beneficiaries: " + beneficiaries.size());
        } catch (Exception e) {
            System.err.println("Error counting beneficiaries: " + e.getMessage());
            lblBeneficiaryCount.setText("Beneficiaries: 0");
        }
    }

    private List<BeneficiaryModel> getBeneficiariesForEvacSite(int evacSiteId, int disasterId) {
        return evacPlanDAO.getBeneficiariesByEvacSiteAndDisaster(evacSiteId, disasterId);
    }

    private void populateGsmPorts() {
        if (cbGsmPort == null) return;

        try {
            List<String> availablePorts = smsSender.getAvailablePorts();

            if (availablePorts.isEmpty()) {
                cbGsmPort.setItems(FXCollections.observableArrayList("No ports available"));
                cbGsmPort.setDisable(true);
                cbGsmPort.getSelectionModel().selectFirst();
                lblConnectionStatus.setText("Disconnected - No ports found");
                lblConnectionStatus.setStyle("-fx-text-fill: red;");
            } else {
                cbGsmPort.setItems(FXCollections.observableArrayList(availablePorts));
                cbGsmPort.setDisable(false);

                String connectedPort = smsSender.getConnectedPort();
                if (connectedPort != null && availablePorts.contains(connectedPort)) {
                    cbGsmPort.getSelectionModel().select(connectedPort);
                    lblConnectionStatus.setText("Connected to " + connectedPort);
                    lblConnectionStatus.setStyle("-fx-text-fill: green;");
                } else {
                    cbGsmPort.getSelectionModel().selectFirst();
                    lblConnectionStatus.setText("Disconnected");
                    lblConnectionStatus.setStyle("-fx-text-fill: orange;");
                }

                cbGsmPort.valueProperty().addListener((obs, oldPort, newPort) -> {
                    if (newPort != null && !newPort.equals(oldPort) && rbGsm.isSelected()) {
                        connectToPort(newPort);
                    }
                    updateSendButtonState();
                });
            }
        } catch (Exception e) {
            System.err.println("Error detecting GSM ports: " + e.getMessage());
            cbGsmPort.setItems(FXCollections.observableArrayList("Error detecting ports"));
            cbGsmPort.setDisable(true);
            lblConnectionStatus.setText("Error: " + e.getMessage());
            lblConnectionStatus.setStyle("-fx-text-fill: red;");
        }
    }

    private void connectToPort(String portName) {
        if (portName == null || portName.isEmpty() ||
                portName.contains("No ports") || portName.contains("Error")) {
            return;
        }

        new Thread(() -> {
            boolean connected = smsSender.connectToPort(portName, 5000);

            Platform.runLater(() -> {
                if (connected) {
                    lblConnectionStatus.setText("Connected to " + portName);
                    lblConnectionStatus.setStyle("-fx-text-fill: green;");
                } else {
                    lblConnectionStatus.setText("Failed to connect");
                    lblConnectionStatus.setStyle("-fx-text-fill: red;");
                }
                updateSendButtonState();
            });
        }).start();
    }

    private void updateSendButtonState() {
        boolean canSend = cbEvacSite.getValue() != null &&
                cbDisaster.getValue() != null &&
                txtMessage.getText() != null &&
                !txtMessage.getText().trim().isEmpty();

        if (rbGsm.isSelected()) {
            String selectedPort = cbGsmPort.getValue();
            boolean validPort = selectedPort != null &&
                    !selectedPort.contains("No ports") &&
                    !selectedPort.contains("Error");
            canSend = canSend && validPort && smsSender.isConnected();
        }

        btnSend.setDisable(!canSend);
    }

    @FXML
    private void handlePreview() {
        String message = buildMessage();
        if (message == null) return;

        Alert preview = new Alert(Alert.AlertType.INFORMATION);
        preview.setTitle("Message Preview");
        preview.setHeaderText("Preview of message to be sent:");
        preview.setContentText(message);
        preview.showAndWait();
    }

    @FXML
    private void handleRefreshPorts() {
        populateGsmPorts();
    }

    @FXML
    private void handleSend() {
        if (!validateInput()) {
            return;
        }

        EvacSiteModel selectedSite = cbEvacSite.getValue();
        DisasterModel selectedDisaster = cbDisaster.getValue();
        String message = buildMessage();

        if (message == null) {
            return;
        }

        List<BeneficiaryModel> beneficiaries = getBeneficiariesForEvacSite(
                selectedSite.getEvacId(), selectedDisaster.getDisasterId());

        if (beneficiaries.isEmpty()) {
            AlertDialogManager.showWarning("No Recipients",
                    "No beneficiaries found assigned to " + selectedSite.getName() +
                            " for the selected disaster.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Send");
        confirm.setHeaderText("Send SMS to " + beneficiaries.size() + " beneficiary(ies)?");
        confirm.setContentText("Evacuation Site: " + selectedSite.getName() + "\n" +
                "Message: " + message.substring(0, Math.min(50, message.length())) + "...");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                sendMessages(beneficiaries, message);
            }
        });
    }

    private String buildMessage() {
        String message = txtMessage.getText().trim();

        if (message.isEmpty()) {
            return null;
        }

        if (chkAppendSiteName.isSelected()) {
            EvacSiteModel selectedSite = cbEvacSite.getValue();
            if (selectedSite != null) {
                message = message + " " + selectedSite.getName();
            }
        }

        return message;
    }

    private boolean validateInput() {
        if (cbEvacSite.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error",
                    "Please select an evacuation site.");
            cbEvacSite.requestFocus();
            return false;
        }

        if (cbDisaster.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error",
                    "Please select a disaster.");
            cbDisaster.requestFocus();
            return false;
        }

        if (txtMessage.getText() == null || txtMessage.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error",
                    "Please enter a message.");
            txtMessage.requestFocus();
            return false;
        }

        if (rbGsm.isSelected()) {
            String selectedPort = cbGsmPort.getValue();
            if (selectedPort == null || selectedPort.contains("No ports") ||
                    selectedPort.contains("Error")) {
                AlertDialogManager.showWarning("Validation Error",
                        "Please select a valid GSM port.");
                return false;
            }

            if (!smsSender.isConnected()) {
                AlertDialogManager.showWarning("Connection Error",
                        "GSM modem is not connected. Please connect first.");
                return false;
            }
        }

        return true;
    }

    private void sendMessages(List<BeneficiaryModel> beneficiaries, String message) {
        btnSend.setDisable(true);
        String method = rbApi.isSelected() ? "API" : "GSM";

        Alert progress = new Alert(Alert.AlertType.INFORMATION);
        progress.setTitle("Sending SMS");
        progress.setHeaderText("Sending messages...");
        progress.setContentText("Please wait...");
        progress.show();

        new Thread(() -> {
            int sent = smsService.sendBulkSMS(beneficiaries, message, method);

            Platform.runLater(() -> {
                progress.close();
                btnSend.setDisable(false);

                AlertDialogManager.showSuccess("SMS Sent",
                        "Successfully sent " + sent + " out of " + beneficiaries.size() + " messages.");

                closeDialog();
            });
        }).start();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    @FXML
    private void handleClose() {
        closeDialog();
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.hide();
        }
    }

    private void makeDraggable() {
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            if (dialogStage != null) {
                dialogStage.setX(event.getScreenX() - xOffset);
                dialogStage.setY(event.getScreenY() - yOffset);
            }
        });
    }
}
