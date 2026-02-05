package com.ionres.respondph.aid;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AidPrintService {

    private static final String PRINTER_NAME = "EPSON L3210 Series";
    private static final String ORG_NAME = "RESPONDPH";
    private static final String ORG_ADDRESS = "Cagayan de Oro City, Northern Mindanao, Philippines";
    private static final String ORG_CONTACT = "Tel: (088) XXX-XXXX | Email: info@respondph.gov.ph";

    // Paper dimensions - Using LEGAL (8.5" x 14") which is closest to Short Bond (8.5" x 13")
    // Note: Short Bond is not a standard paper size in JavaFX
    private static final double PAGE_WIDTH = 612; // 8.5 inches * 72 points/inch
    private static final double MARGIN = 36; // 0.5 inch * 72 points/inch
    private static final double CONTENT_WIDTH = PAGE_WIDTH - (2 * MARGIN); // 540 points (7.5 inches)

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
        content.setPrefWidth(CONTENT_WIDTH); // 540 points (7.5 inches)
        content.setMaxWidth(CONTENT_WIDTH);
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: white;");

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
        content.setPrefWidth(CONTENT_WIDTH); // 540 points (7.5 inches)
        content.setMaxWidth(CONTENT_WIDTH);
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: white;");

        VBox section = createReportSection(disasterName, aidName, aidRecords);
        content.getChildren().add(section);

        return content;
    }

    private VBox createReportSection(String disasterName, String aidName, List<AidModel> aidRecords) {
        VBox section = new VBox();
        section.setPrefWidth(CONTENT_WIDTH);
        section.setMaxWidth(CONTENT_WIDTH);
        section.setPadding(new Insets(20, 0, 20, 0));
        section.setAlignment(Pos.TOP_CENTER);
        section.setStyle("-fx-background-color: white;");

        // Header Section with Organization Info
        VBox headerBox = createHeaderSection();
        section.getChildren().add(headerBox);

        // Document Info Section
        VBox docInfoBox = createDocumentInfoSection(disasterName, aidName, aidRecords.size());
        section.getChildren().add(docInfoBox);

        // Separator Line
        Line separator = createSeparatorLine();
        VBox.setMargin(separator, new Insets(15, 0, 15, 0));
        section.getChildren().add(separator);

        // Beneficiary Table
        VBox tableBox = createBeneficiaryTable(aidRecords);
        section.getChildren().add(tableBox);

        // Summary Section
        VBox summaryBox = createSummarySection(aidRecords);
        VBox.setMargin(summaryBox, new Insets(20, 0, 30, 0));
        section.getChildren().add(summaryBox);


        return section;
    }

    private VBox createHeaderSection() {
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));

        // Organization Logo/Name
        Text orgName = new Text(ORG_NAME);
        orgName.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        orgName.setStyle("-fx-fill: #2c3e50;");

        Text tagline = new Text("AID DISTRIBUTION MANAGEMENT SYSTEM");
        tagline.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 12));
        tagline.setStyle("-fx-fill: #7f8c8d;");

        Text address = new Text(ORG_ADDRESS);
        address.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        address.setStyle("-fx-fill: #95a5a6;");

        Text contact = new Text(ORG_CONTACT);
        contact.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        contact.setStyle("-fx-fill: #95a5a6;");

        header.getChildren().addAll(orgName, tagline, address, contact);
        return header;
    }

    private VBox createDocumentInfoSection(String disasterName, String aidName, int totalBeneficiaries) {
        VBox docInfo = new VBox(8);
        docInfo.setPrefWidth(CONTENT_WIDTH);
        docInfo.setMaxWidth(CONTENT_WIDTH);
        docInfo.setPadding(new Insets(10, 15, 10, 15));
        docInfo.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 5;");

        // Document Title
        Text title = new Text("OFFICIAL DISTRIBUTION REPORT");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        title.setStyle("-fx-fill: #2c3e50;");

        // Reference Number
        String refNumber = generateReferenceNumber();
        Text refNo = new Text("Reference No: " + refNumber);
        refNo.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 8));
        refNo.setStyle("-fx-fill: #34495e;");

        // Disaster Name (Highlighted)
        HBox disasterBox = new HBox(10);
        disasterBox.setAlignment(Pos.CENTER_LEFT);
        Text disasterLabel = new Text("Disaster Event:");
        disasterLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        disasterLabel.setStyle("-fx-fill: #e74c3c;");

        Text disasterValue = new Text(disasterName.toUpperCase());
        disasterValue.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        disasterValue.setStyle("-fx-fill: #c0392b;");
        disasterBox.getChildren().addAll(disasterLabel, disasterValue);

        // Aid Type
        HBox aidBox = new HBox(10);
        aidBox.setAlignment(Pos.CENTER_LEFT);
        Text aidLabel = new Text("Aid Type:");
        aidLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 12));
        Text aidValue = new Text(aidName);
        aidValue.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        aidValue.setStyle("-fx-fill: #2980b9;");
        aidBox.getChildren().addAll(aidLabel, aidValue);

        // Date and Count
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
        HBox metaBox = new HBox(40);

        HBox dateBox = new HBox(5);
        Text dateLabel = new Text("Issue Date:");
        dateLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 11));
        Text dateValue = new Text(dateStr);
        dateValue.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
        dateBox.getChildren().addAll(dateLabel, dateValue);

        HBox countBox = new HBox(5);
        Text countLabel = new Text("Total Beneficiaries:");
        countLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 11));
        Text countValue = new Text(String.valueOf(totalBeneficiaries));
        countValue.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        countValue.setStyle("-fx-fill: #27ae60;");
        countBox.getChildren().addAll(countLabel, countValue);

        metaBox.getChildren().addAll(dateBox, countBox);

        docInfo.getChildren().addAll(title, refNo, disasterBox, aidBox, metaBox);
        return docInfo;
    }

    private VBox createBeneficiaryTable(List<AidModel> aidRecords) {
        VBox table = new VBox(2);
        table.setPrefWidth(CONTENT_WIDTH);
        table.setMaxWidth(CONTENT_WIDTH);

        // Table Header
        HBox headerRow = new HBox();
        headerRow.setPrefWidth(CONTENT_WIDTH);
        headerRow.setPadding(new Insets(10, 10, 10, 10));
        headerRow.setStyle("-fx-background-color: #34495e; -fx-background-radius: 3 3 0 0;");

        Text colNo = createTableHeaderText("No.", 50);
        Text colName = createTableHeaderText("Beneficiary Name", 260);
        Text colDate = createTableHeaderText("Date Received", 120);
        Text colCost = createTableHeaderText("Amount (₱)", 90);

        headerRow.getChildren().addAll(colNo, colName, colDate, colCost);
        table.getChildren().add(headerRow);

        // Table Rows
        int counter = 1;
        for (AidModel aid : aidRecords) {
            HBox row = new HBox();
            row.setPrefWidth(CONTENT_WIDTH);
            row.setPadding(new Insets(8, 10, 8, 10));

            // Alternate row colors
            if (counter % 2 == 0) {
                row.setStyle("-fx-background-color: #f8f9fa;");
            } else {
                row.setStyle("-fx-background-color: white;");
            }

            String dateReceived = aid.getDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));

            Text no = createTableCellText(String.valueOf(counter), 50);
            Text name = createTableCellText(aid.getBeneficiaryName(), 260);
            Text date = createTableCellText(dateReceived, 120);
            Text cost = createTableCellText(String.format("%.2f", aid.getCost()), 90);

            row.getChildren().addAll(no, name, date, cost);
            table.getChildren().add(row);
            counter++;
        }

        // Table Border
        table.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-border-radius: 3;");

        return table;
    }

    private Text createTableHeaderText(String content, double width) {
        Text text = new Text(content);
        text.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        text.setStyle("-fx-fill: white;");
        text.setWrappingWidth(width);
        return text;
    }

    private Text createTableCellText(String content, double width) {
        Text text = new Text(content);
        text.setFont(Font.font("Arial", FontWeight.NORMAL, 8));
        text.setStyle("-fx-fill: #2c3e50;");
        text.setWrappingWidth(width);
        return text;
    }

    private VBox createSummarySection(List<AidModel> aidRecords) {
        VBox summary = new VBox(8);
        summary.setPrefWidth(CONTENT_WIDTH);
        summary.setMaxWidth(CONTENT_WIDTH);
        summary.setPadding(new Insets(15, 20, 15, 20));
        summary.setStyle("-fx-background-color: #e8f5e9; -fx-border-color: #27ae60; -fx-border-width: 2; -fx-border-radius: 3; -fx-background-radius: 3;");

        double totalCost = aidRecords.stream().mapToDouble(AidModel::getCost).sum();

        HBox summaryRow = new HBox(20);
        summaryRow.setAlignment(Pos.CENTER_RIGHT);

        Text label = new Text("TOTAL DISTRIBUTION COST:");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        label.setStyle("-fx-fill: #2c3e50;");

        Text amount = new Text(String.format("₱ %,.2f", totalCost));
        amount.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        amount.setStyle("-fx-fill: #27ae60;");

        summaryRow.getChildren().addAll(label, amount);
        summary.getChildren().add(summaryRow);

        return summary;
    }

    private Line createSeparatorLine() {
        Line line = new Line(0, 0, CONTENT_WIDTH, 0);
        line.setStroke(Color.web("#bdc3c7"));
        line.setStrokeWidth(1);
        return line;
    }

    private String generateReferenceNumber() {
        String dateCode = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomCode = String.format("%04d", new Random().nextInt(10000));
        return "AID-" + dateCode + "-" + randomCode;
    }

    private void addPageBreak(VBox content) {
        Region spacer = new Region();
        spacer.setPrefHeight(20);
        content.getChildren().add(spacer);
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

            // Use LEGAL paper (8.5" x 14") which is the closest standard size to Short Bond (8.5" x 13")
            // Configure page layout with 0.5 inch (36 points) margins on all sides
            PageLayout pageLayout = printer.createPageLayout(
                    Paper.LEGAL,
                    PageOrientation.PORTRAIT,
                    MARGIN, MARGIN, MARGIN, MARGIN  // left, right, top, bottom margins (in points)
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