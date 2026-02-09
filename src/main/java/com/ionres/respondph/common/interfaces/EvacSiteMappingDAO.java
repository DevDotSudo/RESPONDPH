package com.ionres.respondph.common.interfaces;

import com.ionres.respondph.common.model.EvacSiteModel;

import java.util.List;

public interface EvacSiteMappingDAO {
    List<EvacSiteModel> getAllEvacSites();
    EvacSiteModel getEvacSiteById(int evacId);
}
