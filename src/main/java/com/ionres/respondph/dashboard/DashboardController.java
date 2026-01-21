package com.ionres.respondph.dashboard;

import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.DashboardRefresher;
import com.ionres.respondph.util.Mapping;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class DashboardController {
    private final DashBoardService dashBoardService = AppContext.dashBoardService;
    private final Mapping mapping = new Mapping();
    @FXML private Pane mapContainer;
    @FXML private Label totalBeneficiaryLabel;
    @FXML private Label totalDisastersLabel;
    @FXML private Label totalAidsLabel;

    private final double[][] boundary = {
            {11.0775,122.7315},{11.1031,122.7581},{11.0925,122.7618},
            {11.0912,122.7648},{11.0897,122.7662},{11.0896,122.7796},
            {11.0756,122.7942},{11.0674,122.7957},{11.0584,122.7991},
            {11.0533,122.8023},{11.0416,122.8200},{10.9914,122.8514},
            {10.9907,122.8483},{10.9899,122.8462},{10.9904,122.8449},
            {10.9920,122.8447},{10.9951,122.8433},{10.9968,122.8443},
            {10.9966,122.8417},{10.9963,122.8340},{10.9988,122.8287},
            {10.9976,122.8156},{10.9909,122.7957},{10.9919,122.7865},
            {11.0034,122.7861},{11.0480,122.7722},{11.0613,122.7499},
            {11.0681,122.7489},{11.0719,122.7453},{11.0761,122.7454}
    };

    private final Barangay[] barangays = {
            new Barangay("Poblacion",11.0021,122.8196),
            new Barangay("Alacaygan",10.9977,122.8046),
            new Barangay("Bularan",11.0035,122.8230),
            new Barangay("Carmelo",11.0100,122.8151),
            new Barangay("Talokgangan",11.0032,122.8271),
            new Barangay("Zona Sur",11.0002,122.8148),
            new Barangay("San Salvador",10.9992,122.8356),
            new Barangay("Belen",10.9971,122.7970)
    };

    public void initialize() {
        Platform.runLater(() -> {
            mapping.init(mapContainer);
            mapping.setAfterRedraw(() -> {
                drawBoundary();
                drawBarangays();
            });
            DashboardRefresher.register(this);
            loadDashBoardData();
        });
    }

    private void drawBoundary() {
        GraphicsContext gc = mapping.getGc();
        gc.setStroke(Color.RED);
        gc.setLineWidth(3);
        gc.beginPath();

        boolean first = true;
        for (double[] c : boundary) {
            Mapping.Point p = mapping.latLonToScreen(c[0], c[1]);
            if (first) {
                gc.moveTo(p.x, p.y);
                first = false;
            } else {
                gc.lineTo(p.x, p.y);
            }
        }
        gc.closePath();
        gc.stroke();
    }

    private void drawBarangays() {
        GraphicsContext gc = mapping.getGc();
        gc.setFont(Font.font(11));

        for (Barangay b : barangays) {
            Mapping.Point p = mapping.latLonToScreen(b.lat, b.lon);

            gc.setFill(Color.DODGERBLUE);
            gc.fillOval(p.x - 6, p.y - 6, 12, 12);

            double w = textWidth(b.name, gc);
            gc.setFill(Color.WHITE);
            gc.fillRect(p.x - w / 2 - 4, p.y + 8, w + 8, 18);

            gc.setFill(Color.BLACK);
            gc.fillText(b.name, p.x - w / 2, p.y + 22);
        }
    }

    private double textWidth(String s, GraphicsContext gc) {
        Text t = new Text(s);
        t.setFont(gc.getFont());
        return t.getLayoutBounds().getWidth();
    }

    public void loadDashBoardData() {
        totalBeneficiaryLabel.setText(String.valueOf(dashBoardService.fetchTotalBeneficiary()));
        totalDisastersLabel.setText(String.valueOf(dashBoardService.fetchTotalDisasters()));
        totalAidsLabel.setText(String.valueOf(dashBoardService.fetchTotalAids()));
    }

    private static class Barangay {
        String name;
        double lat;
        double lon;
        Barangay(String n,double a,double o){
            name=n;
            lat=a;
            lon=o;
        }
    }
}
