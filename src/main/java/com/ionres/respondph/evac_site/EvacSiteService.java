package com.ionres.respondph.evac_site;

import java.util.List;

public interface EvacSiteService {
    List<EvacSiteModel> getAllEvacSites();
    boolean createEvacSite(EvacSiteModel evacSite);
    boolean deleteEvacSite(EvacSiteModel evacSite);
    boolean updateEvacSite(EvacSiteModel evacSite);
    EvacSiteModel getEvacSiteById(int id);
    List<EvacSiteModel> searchEvacSite(String searchTxt);
}