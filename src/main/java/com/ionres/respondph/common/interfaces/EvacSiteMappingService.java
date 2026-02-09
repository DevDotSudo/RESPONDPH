package com.ionres.respondph.common.interfaces;

import com.ionres.respondph.common.model.EvacSiteMarker;

import java.util.List;

public interface EvacSiteMappingService {

    List<EvacSiteMarker> getAllEvacSites();

    EvacSiteMarker getEvacSiteById(int evacId);
}
