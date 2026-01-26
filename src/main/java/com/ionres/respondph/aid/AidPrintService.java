package com.ionres.respondph.aid;

import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AidPrintService {

    private static final String PRINTER_NAME = "EPSON L3210 Series"; // Adjust if needed


    public boolean printAidDistributionReport(List<AidModel> aidRecords) {
        if (aidRecords == null || aidRecords.isEmpty()) {
            showError("No Records", "No aid records to print.");
            return false;
        }

        Map<String, Map<String, List<AidModel>>> groupedData = groupByDisasterAndAid(aidRecords);

        VBox printContent = createPrintContent(groupedData);

        return printContent(printContent);
    }


    public boolean printSpecificReport(String disasterName, String aidName, List<AidModel> aidRecords) {
        if (aidRecords == null || aidRecords.isEmpty()) {
            showError("No Records", "No beneficiaries found for this disaster and aid type.");
            return false;
        }

        VBox printContent = createSpecificPrintContent(disasterName, aidName, aidRecords);
        return printContent(printContent);
    }


    private Map<String, Map<String, List<AidModel>>> groupByDisasterAndAid(List<AidModel> aidRecords) {
        Map<String, Map<String, List<AidModel>>> grouped = new LinkedHashMap<>();

        for (AidModel aid : aidRecords) {
            String disasterName = aid.getDisasterName();
            String aidName = aid.getName();

            grouped.computeIfAbsent(disasterName, k -> new LinkedHashMap<>())
                    .computeIfAbsent(aidName, k -> new ArrayList<>())
                    .add(aid);
        }

        return grouped;
    }


    private VBox createPrintContent(Map<String, Map<String, List<AidModel>>> groupedData) {
        VBox content = new VBox(20);
        content.setPrefWidth(595); // A4 width in points (8.27 inches)
        content.setStyle("-fx-padding: 40; -fx-background-color: white;");

        for (Map.Entry<String, Map<String, List<AidModel>>> disasterEntry : groupedData.entrySet()) {
            String disasterName = disasterEntry.getKey();

            for (Map.Entry<String, List<AidModel>> aidEntry : disasterEntry.getValue().entrySet()) {
                String aidName = aidEntry.getKey();
                List<AidModel> beneficiaries = aidEntry.getValue();

                VBox section = createReportSection(disasterName, aidName, beneficiaries);
                content.getChildren().add(section);

                addPageBreak(content);
            }
        }

        return content;
    }


    private VBox createSpecificPrintContent(String disasterName, String aidName, List<AidModel> aidRecords) {
        VBox content = new VBox(20);
        content.setPrefWidth(595); // A4 width in points
        content.setStyle("-fx-padding: 40; -fx-background-color: white;");

        VBox section = createReportSection(disasterName, aidName, aidRecords);
        content.getChildren().add(section);

        return content;
    }


    private VBox createReportSection(String disasterName, String aidName, List<AidModel> aidRecords) {
        VBox section = new VBox(15);

        Text header = new Text("RESPONDPH AID DISTRIBUTION SYSTEM");
        header.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        header.setTextAlignment(TextAlignment.CENTER);
        header.setStyle("-fx-fill: #1a1a1a;");

        Text title = new Text(disasterName.toUpperCase());
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextAlignment(TextAlignment.CENTER);
        title.setStyle("-fx-fill: #2c3e50;");

        Text subtitle = new Text("Aid Type: " + aidName);
        subtitle.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 16));
        subtitle.setTextAlignment(TextAlignment.CENTER);
        subtitle.setStyle("-fx-fill: #34495e;");

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
        Text info = new Text(String.format("Date: %s  |  Total Beneficiaries: %d", dateStr, aidRecords.size()));
        info.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        info.setStyle("-fx-fill: #7f8c8d;");

        Text divider = new Text("═".repeat(80));
        divider.setFont(Font.font("Courier New", 10));

        Text listHeader = new Text(String.format("%-5s %-40s %-15s", "No.", "Beneficiary Name", "Date Received"));
        listHeader.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
        listHeader.setStyle("-fx-fill: #2c3e50;");

        section.getChildren().addAll(header, title, subtitle, info, divider, listHeader);

        int counter = 1;
        for (AidModel aid : aidRecords) {
            String dateReceived = aid.getDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            Text beneficiary = new Text(String.format("%-5d %-40s %-15s",
                    counter++,
                    aid.getBeneficiaryName(),
                    dateReceived));
            beneficiary.setFont(Font.font("Courier New", 10));
            section.getChildren().add(beneficiary);
        }

        Text footerDivider = new Text("═".repeat(80));
        footerDivider.setFont(Font.font("Courier New", 10));
        section.getChildren().add(footerDivider);

        double totalCost = aidRecords.stream().mapToDouble(AidModel::getCost).sum();
        Text summary = new Text(String.format("Total Cost: ₱%.2f", totalCost));
        summary.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        summary.setStyle("-fx-fill: #27ae60;");
        section.getChildren().add(summary);

        VBox signatures = createSignatureSection();
        section.getChildren().add(signatures);

        return section;
    }


    private VBox createSignatureSection() {
        VBox sigSection = new VBox(30);
        sigSection.setStyle("-fx-padding: 40 0 0 0;");

        Text prepared = new Text("Prepared by: _______________________________");
        prepared.setFont(Font.font("Arial", 11));

        Text approved = new Text("Approved by: _______________________________");
        approved.setFont(Font.font("Arial", 11));

        sigSection.getChildren().addAll(prepared, approved);
        return sigSection;
    }


    private void addPageBreak(VBox content) {
        Text pageBreak = new Text("\n--- Page Break ---\n");
        pageBreak.setFont(Font.font("Arial", FontWeight.LIGHT, 8));
        pageBreak.setStyle("-fx-fill: #bdc3c7;");
        content.getChildren().add(pageBreak);
    }

    private boolean printContent(Node content) {
        try {
            Printer printer = findPrinter(PRINTER_NAME);
            if (printer == null) {
                printer = Printer.getDefaultPrinter();
                if (printer == null) {
                    showError("No Printer", "No printer found. Please check printer connection.");
                    return false;
                }
            }

            PrinterJob job = PrinterJob.createPrinterJob(printer);
            if (job == null) {
                showError("Print Error", "Failed to create print job.");
                return false;
            }

            // Configure page layout for bond paper (Letter size: 8.5" x 11")
            PageLayout pageLayout = printer.createPageLayout(
                    Paper.NA_LETTER,
                    PageOrientation.PORTRAIT,
                    Printer.MarginType.DEFAULT
            );

            job.getJobSettings().setPageLayout(pageLayout);

            boolean proceed = job.showPrintDialog(null);
            if (!proceed) {
                job.cancelJob();
                return false;
            }

            boolean success = job.printPage(pageLayout, content);
            if (success) {
                job.endJob();
                showSuccess("Print Success", "Document sent to printer successfully!");
                return true;
            } else {
                showError("Print Failed", "Failed to print document.");
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Print Error", "An error occurred while printing: " + e.getMessage());
            return false;
        }
    }


    private Printer findPrinter(String printerName) {
        for (Printer printer : Printer.getAllPrinters()) {
            if (printer.getName().contains(printerName)) {
                return printer;
            }
        }
        return null;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}