package com.ionres.respondph.aid;

import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AidPrintService {

    private static final String PRINTER_NAME = "EPSON L3210 Series";
    private static final String ORG_NAME = "RESPONDPH";
    private static final String ORG_ADDRESS = "Cagayan de Oro City, Northern Mindanao, Philippines";
    private static final String ORG_CONTACT = "Tel: (088) XXX-XXXX | Email: info@respondph.gov.ph";

    // Paper dimensions
    private static final double PAGE_WIDTH = 612; // 8.5 inches * 72 points/inch
    private static final double MARGIN = 36; // 0.5 inch * 72 points/inch
    private static final double CONTENT_WIDTH = PAGE_WIDTH - (2 * MARGIN); // 540 points (7.5 inches)

    // Print settings
    private String paperSize = "A4 (210mm x 297mm)";
    private boolean landscape = false;
    private int copies = 1;
    private boolean includeHeader = true;
    private boolean includeFooter = true;
    private boolean includePageNumbers = true;

    // =========================================================================
    // SETTERS
    // =========================================================================

    public void setPaperSize(String paperSize) {
        this.paperSize = paperSize;
    }

    public void setLandscape(boolean landscape) {
        this.landscape = landscape;
    }

    public void setCopies(int copies) {
        this.copies = copies;
    }

    public void setIncludeHeader(boolean includeHeader) {
        this.includeHeader = includeHeader;
    }

    public void setIncludeFooter(boolean includeFooter) {
        this.includeFooter = includeFooter;
    }

    public void setIncludePageNumbers(boolean includePageNumbers) {
        this.includePageNumbers = includePageNumbers;
    }

    // =========================================================================
    // PUBLIC PRINT METHODS
    // =========================================================================

    /**
     * Print a beneficiary list report
     */
    public boolean printBeneficiaryList(String disasterName, String aidName, List<AidModel> records) {
        if (records == null || records.isEmpty()) {
            showError("No Records", "No beneficiary records to print.");
            return false;
        }

        VBox content = buildBeneficiaryListContent(disasterName, aidName, records);
        return printContent(content);
    }

    /**
     * Print a distribution summary report
     */
    public boolean printDistributionSummary(String disasterName, String aidName, List<AidModel> records) {
        if (records == null || records.isEmpty()) {
            showError("No Records", "No distribution records to print.");
            return false;
        }

        VBox content = buildDistributionSummaryContent(disasterName, aidName, records);
        return printContent(content);
    }

    /**
     * Build beneficiary list content for preview
     */
    public VBox buildBeneficiaryList(String disasterName, String aidName, List<AidModel> records) {
        return buildBeneficiaryListContent(disasterName, aidName, records);
    }

    /**
     * Build distribution summary content for preview
     */
    public VBox buildDistributionSummary(String disasterName, String aidName, List<AidModel> records) {
        return buildDistributionSummaryContent(disasterName, aidName, records);
    }

    /**
     * Print a complete aid distribution report (grouped by disaster and aid type)
     */
    public boolean printAidDistributionReport(List<AidModel> aidRecords) {
        if (aidRecords == null || aidRecords.isEmpty()) {
            showError("No Records", "No aid records to print.");
            return false;
        }

        Map<String, Map<String, List<AidModel>>> groupedData = groupByDisasterAndAid(aidRecords);
        VBox printContent = createCompleteReportContent(groupedData);
        return printContent(printContent);
    }

    /**
     * Print a specific report (legacy method)
     */
    public boolean printSpecificReport(String disasterName, String aidName, List<AidModel> aidRecords) {
        if (aidRecords == null || aidRecords.isEmpty()) {
            showError("No Records", "No beneficiaries found for this disaster and aid type.");
            return false;
        }

        VBox printContent = createSpecificPrintContent(disasterName, aidName, aidRecords);
        return printContent(printContent);
    }

    // =========================================================================
    // REPORT BUILDERS
    // =========================================================================

    /**
     * Build beneficiary list content with centered layout and quantity column
     */
    private VBox buildBeneficiaryListContent(String disasterName, String aidName, List<AidModel> records) {
        VBox root = createPrintPage();
        root.setAlignment(Pos.TOP_CENTER);

        if (includeHeader)
            root.getChildren().add(buildHeader("BENEFICIARY LIST", disasterName, aidName));

        root.getChildren().add(createSeparator());

        // Create a centered table container
        VBox tableContainer = new VBox();
        tableContainer.setAlignment(Pos.CENTER);
        tableContainer.setMaxWidth(CONTENT_WIDTH);

        // Column header row - centered
        HBox colHeader = createStyledRow(true);
        colHeader.setAlignment(Pos.CENTER);
        colHeader.getChildren().addAll(
                createColumnCell("#", 35, true),
                createColumnCell("Beneficiary Name", 200, true),
                createColumnCell("Date Received", 90, true),
                createColumnCell("Qty", 40, true),
                createColumnCell("Cost (₱)", 70, true)
        );
        tableContainer.getChildren().add(colHeader);

        // Data rows
        for (int i = 0; i < records.size(); i++) {
            AidModel aid = records.get(i);
            HBox row = createStyledRow(i % 2 == 0);
            row.setAlignment(Pos.CENTER);
            row.getChildren().addAll(
                    createColumnCell(String.valueOf(i + 1), 35, false),
                    createColumnCell(nullToDash(aid.getBeneficiaryName()), 200, false),
                    createColumnCell(aid.getDate() != null
                            ? aid.getDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                            : "—", 90, false),
                    createColumnCell(String.format("%.0f", aid.getQuantity()), 40, false),
                    createColumnCell(String.format("%.2f", aid.getCost()), 70, false)
            );
            tableContainer.getChildren().add(row);
        }

        root.getChildren().add(tableContainer);
        root.getChildren().add(createSeparator());

        // Summary section - centered
        VBox summaryBox = new VBox(5);
        summaryBox.setAlignment(Pos.CENTER);

        double totalCost = records.stream().mapToDouble(AidModel::getCost).sum();
        double totalQuantity = records.stream().mapToDouble(AidModel::getQuantity).sum();

        Label summaryLabel = new Label(String.format(
                "Total Beneficiaries: %d     Total Quantity: %.0f     Total Cost: ₱ %,.2f",
                records.size(), totalQuantity, totalCost));
        summaryLabel.setStyle("-fx-font-size: 10; -fx-font-weight: bold; -fx-alignment: center;");
        summaryBox.getChildren().add(summaryLabel);

        root.getChildren().add(summaryBox);

        if (includeFooter)
            root.getChildren().add(buildFooter(records.size()));

        if (includePageNumbers)
            root.getChildren().add(createPageNumber(1));

        return root;
    }

    /**
     * Build distribution summary content
     */
    private VBox buildDistributionSummaryContent(String disasterName, String aidName, List<AidModel> records) {
        VBox root = createPrintPage();
        root.setAlignment(Pos.TOP_CENTER);

        if (includeHeader)
            root.getChildren().add(buildHeader("DISTRIBUTION SUMMARY", disasterName, aidName));

        root.getChildren().add(createSeparator());

        double totalCost = records.stream().mapToDouble(AidModel::getCost).sum();
        double totalQuantity = records.stream().mapToDouble(AidModel::getQuantity).sum();
        double avgCost = records.isEmpty() ? 0 : totalCost / records.size();
        double maxCost = records.stream().mapToDouble(AidModel::getCost).max().orElse(0);
        double minCost = records.stream().mapToDouble(AidModel::getCost).min().orElse(0);

        VBox statsContainer = new VBox(5);
        statsContainer.setAlignment(Pos.CENTER);
        statsContainer.getChildren().addAll(
                createStatRow("Aid Name", aidName),
                createStatRow("Disaster Event", disasterName),
                createStatRow("Total Beneficiaries", records.size() + " persons"),
                createStatRow("Total Quantity", String.format("%.0f units", totalQuantity)),
                createStatRow("Total Cost", String.format("₱ %,.2f", totalCost)),
                createStatRow("Average Cost", String.format("₱ %,.2f", avgCost)),
                createStatRow("Highest Amount", String.format("₱ %,.2f", maxCost)),
                createStatRow("Lowest Amount", String.format("₱ %,.2f", minCost))
        );
        root.getChildren().add(statsContainer);

        root.getChildren().add(createSeparator());

        // Visual cost bar
        double barWidth = 500.0;
        StackPane bar = createCostBar(barWidth, totalCost);
        HBox barBox = new HBox(bar);
        barBox.setAlignment(Pos.CENTER);
        barBox.setPadding(new Insets(10, 0, 10, 0));
        root.getChildren().add(barBox);

        root.getChildren().add(createSeparator());

        Label noteLabel = new Label("✔ Distribution completed successfully.");
        noteLabel.setStyle("-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: #1a7a4a; -fx-alignment: center;");
        noteLabel.setAlignment(Pos.CENTER);
        root.getChildren().add(noteLabel);

        if (includeFooter)
            root.getChildren().add(buildFooter(records.size()));

        if (includePageNumbers)
            root.getChildren().add(createPageNumber(1));

        return root;
    }

    /**
     * Create complete report with multiple sections
     */
    private VBox createCompleteReportContent(Map<String, Map<String, List<AidModel>>> groupedData) {
        VBox content = createPrintPage();
        content.setSpacing(20);

        int pageNumber = 1;
        boolean firstSection = true;

        for (Map.Entry<String, Map<String, List<AidModel>>> disasterEntry : groupedData.entrySet()) {
            String disasterName = disasterEntry.getKey();

            for (Map.Entry<String, List<AidModel>> aidEntry : disasterEntry.getValue().entrySet()) {
                String aidName = aidEntry.getKey();
                List<AidModel> beneficiaries = aidEntry.getValue();

                if (!firstSection) {
                    content.getChildren().add(createPageBreak());
                }

                VBox section = createReportSection(disasterName, aidName, beneficiaries, pageNumber++);
                content.getChildren().add(section);
                firstSection = false;
            }
        }

        return content;
    }

    /**
     * Create specific report section
     */
    private VBox createSpecificPrintContent(String disasterName, String aidName, List<AidModel> aidRecords) {
        VBox content = createPrintPage();
        VBox section = createReportSection(disasterName, aidName, aidRecords, 1);
        content.getChildren().add(section);
        return content;
    }

    /**
     * Create a single report section
     */
    private VBox createReportSection(String disasterName, String aidName, List<AidModel> aidRecords, int pageNum) {
        VBox section = new VBox();
        section.setPrefWidth(CONTENT_WIDTH);
        section.setMaxWidth(CONTENT_WIDTH);
        section.setPadding(new Insets(20, 0, 20, 0));
        section.setAlignment(Pos.TOP_CENTER);
        section.setStyle("-fx-background-color: white;");

        // Header Section with Organization Info
        VBox headerBox = createOrganizationHeader();
        section.getChildren().add(headerBox);

        // Document Info Section
        VBox docInfoBox = createDocumentInfoSection(disasterName, aidName, aidRecords.size());
        section.getChildren().add(docInfoBox);

        // Separator Line
        Region separator = createSeparator();
        VBox.setMargin(separator, new Insets(15, 0, 15, 0));
        section.getChildren().add(separator);

        // Beneficiary Table
        VBox tableBox = createBeneficiaryTable(aidRecords);
        section.getChildren().add(tableBox);

        // Summary Section
        VBox summaryBox = createSummarySection(aidRecords);
        VBox.setMargin(summaryBox, new Insets(20, 0, 30, 0));
        section.getChildren().add(summaryBox);

        if (includeFooter)
            section.getChildren().add(buildFooter(aidRecords.size()));

        if (includePageNumbers)
            section.getChildren().add(createPageNumber(pageNum));

        return section;
    }

    // =========================================================================
    // COMPONENT BUILDERS
    // =========================================================================

    /**
     * Create a basic print page container
     */
    private VBox createPrintPage() {
        VBox page = new VBox(6);
        page.setPrefWidth(CONTENT_WIDTH);
        page.setMaxWidth(CONTENT_WIDTH);
        page.setPadding(new Insets(25));
        page.setStyle("-fx-background-color: white;");
        page.setAlignment(Pos.TOP_CENTER);
        return page;
    }

    /**
     * Build report header
     */
    private VBox buildHeader(String reportTitle, String disasterName, String aidName) {
        VBox header = new VBox(3);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 10, 0));

        Label republic = new Label("Republic of the Philippines");
        republic.setStyle("-fx-font-size: 9; -fx-text-fill: #555; -fx-alignment: center;");

        Label title = new Label("MUNICIPAL OF BANATE DISASTER RISK REDUCTION AND MANAGEMENT");
        title.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-alignment: center;");

        Label reportTypeLabel = new Label(reportTitle);
        reportTypeLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; " +
                "-fx-text-fill: #1a3a6b; -fx-padding: 2 0 0 0; -fx-alignment: center;");

        Label disasterLabel = new Label("Disaster: " + disasterName);
        disasterLabel.setStyle("-fx-font-size: 10; -fx-font-weight: bold; " +
                "-fx-text-fill: #c0392b; -fx-alignment: center;");

        Label aidLabel = new Label("Aid Type: " + aidName);
        aidLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #333; -fx-alignment: center;");

        Label dateLabel = new Label("Generated: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy  HH:mm")));
        dateLabel.setStyle("-fx-font-size: 9; -fx-text-fill: #777; -fx-alignment: center;");

        header.getChildren().addAll(republic, title, reportTypeLabel,
                disasterLabel, aidLabel, dateLabel);
        return header;
    }

    /**
     * Create organization header (legacy style)
     */
    private VBox createOrganizationHeader() {
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));

        Text orgName = new Text(ORG_NAME);
        orgName.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        orgName.setStyle("-fx-fill: #2c3e50;");

        Text tagline = new Text("AID DISTRIBUTION MANAGEMENT SYSTEM");
        tagline.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 11));
        tagline.setStyle("-fx-fill: #7f8c8d;");

        Text address = new Text(ORG_ADDRESS);
        address.setFont(Font.font("Arial", FontWeight.NORMAL, 9));
        address.setStyle("-fx-fill: #95a5a6;");

        Text contact = new Text(ORG_CONTACT);
        contact.setFont(Font.font("Arial", FontWeight.NORMAL, 9));
        contact.setStyle("-fx-fill: #95a5a6;");

        header.getChildren().addAll(orgName, tagline, address, contact);
        return header;
    }

    /**
     * Create document info section
     */
    private VBox createDocumentInfoSection(String disasterName, String aidName, int totalBeneficiaries) {
        VBox docInfo = new VBox(8);
        docInfo.setPrefWidth(CONTENT_WIDTH);
        docInfo.setMaxWidth(CONTENT_WIDTH);
        docInfo.setPadding(new Insets(10, 15, 10, 15));
        docInfo.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 5;");
        docInfo.setAlignment(Pos.CENTER);

        // Document Title
        Text title = new Text("OFFICIAL DISTRIBUTION REPORT");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        title.setStyle("-fx-fill: #2c3e50;");

        // Reference Number
        String refNumber = generateReferenceNumber();
        Text refNo = new Text("Reference No: " + refNumber);
        refNo.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 8));
        refNo.setStyle("-fx-fill: #34495e;");

        // Disaster Name (Highlighted)
        HBox disasterBox = new HBox(10);
        disasterBox.setAlignment(Pos.CENTER);
        Text disasterLabel = new Text("Disaster Event:");
        disasterLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        disasterLabel.setStyle("-fx-fill: #e74c3c;");

        Text disasterValue = new Text(disasterName.toUpperCase());
        disasterValue.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        disasterValue.setStyle("-fx-fill: #c0392b;");
        disasterBox.getChildren().addAll(disasterLabel, disasterValue);

        // Aid Type
        HBox aidBox = new HBox(10);
        aidBox.setAlignment(Pos.CENTER);
        Text aidLabel = new Text("Aid Type:");
        aidLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 11));
        Text aidValue = new Text(aidName);
        aidValue.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
        aidValue.setStyle("-fx-fill: #2980b9;");
        aidBox.getChildren().addAll(aidLabel, aidValue);

        // Date and Count
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
        HBox metaBox = new HBox(30);
        metaBox.setAlignment(Pos.CENTER);

        HBox dateBox = new HBox(5);
        dateBox.setAlignment(Pos.CENTER);
        Text dateLabel = new Text("Issue Date:");
        dateLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 10));
        Text dateValue = new Text(dateStr);
        dateValue.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        dateBox.getChildren().addAll(dateLabel, dateValue);

        HBox countBox = new HBox(5);
        countBox.setAlignment(Pos.CENTER);
        Text countLabel = new Text("Total Beneficiaries:");
        countLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 10));
        Text countValue = new Text(String.valueOf(totalBeneficiaries));
        countValue.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        countValue.setStyle("-fx-fill: #27ae60;");
        countBox.getChildren().addAll(countLabel, countValue);

        metaBox.getChildren().addAll(dateBox, countBox);

        docInfo.getChildren().addAll(title, refNo, disasterBox, aidBox, metaBox);
        return docInfo;
    }

    /**
     * Create beneficiary table
     */
    private VBox createBeneficiaryTable(List<AidModel> aidRecords) {
        VBox table = new VBox(2);
        table.setPrefWidth(CONTENT_WIDTH);
        table.setMaxWidth(CONTENT_WIDTH);
        table.setAlignment(Pos.CENTER);

        // Table Header
        HBox headerRow = new HBox();
        headerRow.setPrefWidth(CONTENT_WIDTH);
        headerRow.setPadding(new Insets(8, 10, 8, 10));
        headerRow.setStyle("-fx-background-color: #34495e; -fx-background-radius: 3 3 0 0;");
        headerRow.setAlignment(Pos.CENTER);

        Text colNo = createTableHeaderText("No.", 40);
        Text colName = createTableHeaderText("Beneficiary Name", 220);
        Text colDate = createTableHeaderText("Date Received", 100);
        Text colQty = createTableHeaderText("Qty", 45);
        Text colCost = createTableHeaderText("Amount (₱)", 80);

        headerRow.getChildren().addAll(colNo, colName, colDate, colQty, colCost);
        table.getChildren().add(headerRow);

        // Table Rows
        int counter = 1;
        for (AidModel aid : aidRecords) {
            HBox row = new HBox();
            row.setPrefWidth(CONTENT_WIDTH);
            row.setPadding(new Insets(6, 10, 6, 10));
            row.setAlignment(Pos.CENTER);

            // Alternate row colors
            if (counter % 2 == 0) {
                row.setStyle("-fx-background-color: #f8f9fa;");
            } else {
                row.setStyle("-fx-background-color: white;");
            }

            String dateReceived = aid.getDate() != null
                    ? aid.getDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                    : "—";

            Text no = createTableCellText(String.valueOf(counter), 40);
            Text name = createTableCellText(nullToDash(aid.getBeneficiaryName()), 220);
            Text date = createTableCellText(dateReceived, 100);
            Text qty = createTableCellText(String.format("%.0f", aid.getQuantity()), 45);
            Text cost = createTableCellText(String.format("%.2f", aid.getCost()), 80);

            row.getChildren().addAll(no, name, date, qty, cost);
            table.getChildren().add(row);
            counter++;
        }

        // Table Border
        table.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-border-radius: 3;");

        return table;
    }


    private Text createTableHeaderText(String content, double width) {
        Text text = new Text(content);
        text.setFont(Font.font("Arial", FontWeight.BOLD, 9));
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
        summary.setPadding(new Insets(12, 20, 12, 20));
        summary.setStyle("-fx-background-color: #e8f5e9; -fx-border-color: #27ae60; " +
                "-fx-border-width: 2; -fx-border-radius: 3; -fx-background-radius: 3;");
        summary.setAlignment(Pos.CENTER);

        double totalCost = aidRecords.stream().mapToDouble(AidModel::getCost).sum();
        double totalQuantity = aidRecords.stream().mapToDouble(AidModel::getQuantity).sum();

        HBox summaryRow = new HBox(20);
        summaryRow.setAlignment(Pos.CENTER);

        Text label = new Text("TOTAL DISTRIBUTION:");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        label.setStyle("-fx-fill: #2c3e50;");

        Text qtyText = new Text(String.format("%.0f units", totalQuantity));
        qtyText.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        qtyText.setStyle("-fx-fill: #2980b9;");

        Text amount = new Text(String.format("₱ %,.2f", totalCost));
        amount.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        amount.setStyle("-fx-fill: #27ae60;");

        summaryRow.getChildren().addAll(label, qtyText, amount);
        summary.getChildren().add(summaryRow);

        return summary;
    }


    private StackPane createCostBar(double barWidth, double totalCost) {
        StackPane bar = new StackPane();
        bar.setMaxWidth(barWidth);
        bar.setMinWidth(barWidth);
        bar.setMinHeight(20);
        bar.setMaxHeight(20);
        bar.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 4;");

        HBox fill = new HBox();
        fill.setMinWidth(barWidth);
        fill.setMaxWidth(barWidth);
        fill.setMinHeight(20);
        fill.setMaxHeight(20);
        fill.setStyle("-fx-background-color: #1a7a4a; -fx-background-radius: 4;");
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        Label centerLabel = new Label(String.format("₱ %,.2f total distributed", totalCost));
        centerLabel.setStyle("-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: white;");

        bar.getChildren().addAll(fill, centerLabel);
        return bar;
    }


    private HBox buildFooter(int totalRecords) {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(8, 0, 0, 0));

        Label left = new Label("Total Records: " + totalRecords);
        left.setStyle("-fx-font-size: 9; -fx-text-fill: #555; -fx-alignment: center;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String addedBy = SessionManager.getInstance().getCurrentAdminFullName();

        Label right = new Label("Prepared by : " + addedBy);
        right.setStyle("-fx-font-size: 9; -fx-text-fill: #555; -fx-alignment: center;");

        footer.getChildren().addAll(left, spacer, right);
        return footer;
    }


    private Label createPageNumber(int page) {
        Label lbl = new Label("Page " + page);
        lbl.setStyle("-fx-font-size: 8; -fx-text-fill: #aaa; -fx-alignment: center;");
        lbl.setAlignment(Pos.CENTER);
        HBox.setHgrow(lbl, Priority.ALWAYS);
        return lbl;
    }


    private Region createSeparator() {
        Region sep = new Region();
        sep.setMinHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: #bdc3c7;");
        sep.setMaxWidth(CONTENT_WIDTH - 100);
        VBox.setMargin(sep, new Insets(8, 0, 8, 0));
        return sep;
    }


    private Region createPageBreak() {
        Region spacer = new Region();
        spacer.setPrefHeight(30);
        spacer.setStyle("-fx-background-color: transparent;");
        return spacer;
    }

    /**
     * Create styled row for tables
     */
    private HBox createStyledRow(boolean shaded) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(3, 5, 3, 5));
        row.setStyle("-fx-background-color: " + (shaded ? "#f2f3f4" : "white") + ";");
        return row;
    }

    /**
     * Create column cell
     */
    private Label createColumnCell(String text, double width, boolean header) {
        Label lbl = new Label(text);
        lbl.setMinWidth(width);
        lbl.setMaxWidth(width);
        lbl.setAlignment(Pos.CENTER);
        lbl.setWrapText(true);
        lbl.setStyle(header
                ? "-fx-font-size: 9; -fx-font-weight: bold; -fx-text-fill: #1a3a6b; -fx-alignment: center;"
                : "-fx-font-size: 8; -fx-alignment: center;");
        return lbl;
    }

    /**
     * Create stat row for summary
     */
    private HBox createStatRow(String label, String value) {
        HBox row = new HBox(15);
        row.setPadding(new Insets(3, 0, 3, 0));
        row.setAlignment(Pos.CENTER);

        Label lbl = new Label(label + ":");
        lbl.setMinWidth(150);
        lbl.setStyle("-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: #444; -fx-alignment: right;");
        lbl.setAlignment(Pos.CENTER_RIGHT);

        Label val = new Label(value);
        val.setStyle("-fx-font-size: 10; -fx-alignment: left;");
        val.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(lbl, val);
        return row;
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    /**
     * Group aid records by disaster and aid type
     */
    private Map<String, Map<String, List<AidModel>>> groupByDisasterAndAid(List<AidModel> aidRecords) {
        Map<String, Map<String, List<AidModel>>> grouped = new LinkedHashMap<>();

        for (AidModel aid : aidRecords) {
            String disasterName = aid.getDisasterName() != null ? aid.getDisasterName() : "General Aid";
            String aidName = aid.getName();

            grouped.computeIfAbsent(disasterName, k -> new LinkedHashMap<>())
                    .computeIfAbsent(aidName, k -> new ArrayList<>())
                    .add(aid);
        }

        return grouped;
    }

    /**
     * Generate reference number
     */
    private String generateReferenceNumber() {
        String dateCode = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomCode = String.format("%04d", new Random().nextInt(10000));
        return "AID-" + dateCode + "-" + randomCode;
    }

    /**
     * Extract score info from notes
     */
    private String extractScoreInfo(String notes) {
        if (notes == null || notes.isEmpty()) return "N/A";
        try {
            java.util.regex.Matcher sm = java.util.regex.Pattern.compile("Score:\\s*([0-9]+\\.?[0-9]*)")
                    .matcher(notes);
            java.util.regex.Matcher cm = java.util.regex.Pattern.compile("Cluster:\\s*([0-9]+)")
                    .matcher(notes);
            java.util.regex.Matcher pm = java.util.regex.Pattern.compile("Priority:\\s*([A-Za-z]+)")
                    .matcher(notes);
            String score = sm.find() ? sm.group(1) : "N/A";
            String cluster = cm.find() ? cm.group(1) : "N/A";
            String priority = pm.find() ? pm.group(1) : "N/A";
            return String.format("%s (C%s) - %s", score, cluster, priority);
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * Convert null to dash
     */
    private String nullToDash(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    // =========================================================================
    // PRINTING METHODS
    // =========================================================================

    /**
     * Print content with current settings
     */
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

            // Apply page layout
            PageLayout pageLayout = createPageLayout(printer);
            job.getJobSettings().setPageLayout(pageLayout);

            boolean proceed = job.showPrintDialog(null);
            if (!proceed) {
                job.cancelJob();
                return false;
            }

            boolean allSuccess = true;
            for (int i = 0; i < copies; i++) {
                if (!job.printPage(pageLayout, content)) {
                    allSuccess = false;
                    break;
                }
            }

            if (allSuccess) {
                job.endJob();
                showSuccess("Print Success",
                        String.format("Document sent to printer successfully!\n%d %s printed.",
                                copies, copies == 1 ? "copy" : "copies"));
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

    /**
     * Create page layout based on current settings
     */
    private PageLayout createPageLayout(Printer printer) {
        Paper paper = resolvePaper();
        PageOrientation orientation = landscape ? PageOrientation.LANDSCAPE : PageOrientation.PORTRAIT;
        return printer.createPageLayout(paper, orientation, Printer.MarginType.DEFAULT);
    }

    /**
     * Resolve paper based on selected size
     */
    private Paper resolvePaper() {
        if (paperSize == null) return Paper.A4;
        if (paperSize.startsWith("Letter")) return Paper.NA_LETTER;
        if (paperSize.startsWith("Legal")) return Paper.LEGAL;
        return Paper.A4;
    }

    /**
     * Find printer by name
     */
    private Printer findPrinter(String printerName) {
        for (Printer printer : Printer.getAllPrinters()) {
            if (printer.getName().contains(printerName)) {
                return printer;
            }
        }
        return null;
    }

    // =========================================================================
    // DIALOG HELPERS
    // =========================================================================

    private void showError(String title, String message) {
        AlertDialogManager.showError(title, message);
    }

    private void showSuccess(String title, String message) {
        AlertDialogManager.showSuccess(title, message);
    }
}