package com.ionres.respondph.common.controller;

import com.ionres.respondph.util.Mapping;

public interface ControllerListener {
    void onLocationSelected(Mapping.LatLng latLng);
}
