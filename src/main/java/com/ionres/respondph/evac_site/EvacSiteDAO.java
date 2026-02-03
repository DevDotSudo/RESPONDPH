package com.ionres.respondph.evac_site;

import java.util.List;

public interface EvacSiteDAO {
    boolean saving(EvacSiteModel evacSite);
    List<EvacSiteModel> getAll();
    boolean delete(EvacSiteModel evacSite);
    boolean update(EvacSiteModel evacSite);
    EvacSiteModel getById(int id);
}