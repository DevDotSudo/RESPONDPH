package com.ionres.respondph.util;

import com.ionres.respondph.mapping.MappingController;
import javafx.application.Platform;

public final class DashboardRefresher {

    private static MappingController controller;

    private DashboardRefresher() {}

    public static void register(MappingController ctrl) {
        controller = ctrl;
    }

    public static void refresh() {
        if (controller != null) {
            Platform.runLater(controller::loadDashBoardData);
        }
    }
}