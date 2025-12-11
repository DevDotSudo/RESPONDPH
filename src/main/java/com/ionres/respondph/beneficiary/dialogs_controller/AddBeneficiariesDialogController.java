package com.ionres.respondph.beneficiary.dialogs_controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Pagination;

public class AddBeneficiariesDialogController {

    @FXML
    private Pagination bene_pagination;

    @FXML
    private Button backBtn;

    @FXML
    private Button nextBtn;

    @FXML
    public void initialize() {
        bene_pagination.setPageFactory(this::createPage);
        bene_pagination.setCurrentPageIndex(0);

        nextBtn.setOnAction(event -> nextPage());
        backBtn.setOnAction(event -> prevPage());
    }

    private Node createPage(int pageIndex) {
        try {
            switch (pageIndex) {
                case 0:
                    return FXMLLoader.load(getClass().getResource(
                            "/view/pages/add_beneficiaries_pagination/BeneficiaryProfile.fxml"));
                case 1:
                    return FXMLLoader.load(getClass().getResource(
                            "/view/pages/add_beneficiaries_pagination/VulnerabilityIndicators.fxml"));
                case 2:
                    return FXMLLoader.load(getClass().getResource(
                            "/view/pages/add_beneficiaries_pagination/HousingAndInfrastracture.fxml"));
                case 3:
                    return FXMLLoader.load(getClass().getResource(
                            "/view/pages/add_beneficiaries_pagination/SocioEconomicStatus.fxml"));
                default:
                    return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Button("Error loading page");
        }
    }

    private void nextPage() {
        int current = bene_pagination.getCurrentPageIndex();
        int total = bene_pagination.getPageCount();

        if (current < total - 1) {
            bene_pagination.setCurrentPageIndex(current + 1);
        }
    }

    private void prevPage() {
        int current = bene_pagination.getCurrentPageIndex();

        if (current > 0) {
            bene_pagination.setCurrentPageIndex(current - 1);
        }
    }
}
