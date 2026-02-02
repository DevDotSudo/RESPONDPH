package com.ionres.respondph.evac_site;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.exception.ExceptionFactory;
import com.ionres.respondph.util.Cryptography;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EvacSiteServiceImpl implements EvacSiteService {
    Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

    private final EvacSiteDAO evacSiteDAO;

    public EvacSiteServiceImpl(DBConnection dbConnection) {
        this.evacSiteDAO = new EvacSiteDAOServiceImpl(dbConnection);
    }

    @Override
    public List<EvacSiteModel> getAllEvacSites() {
        return evacSiteDAO.getAll();
    }

    @Override
    public boolean createEvacSite(EvacSiteModel evacSite) {
        try {
            // Only encrypt name and notes, not numeric fields
            String encryptedName = cs.encryptWithOneParameter(evacSite.getName());
            String encryptedNotes = cs.encryptWithOneParameter(evacSite.getNotes());

            EvacSiteModel encryptedEvacSite = new EvacSiteModel();
            encryptedEvacSite.setName(encryptedName);
            encryptedEvacSite.setCapacity(evacSite.getCapacity()); // store as-is
            encryptedEvacSite.setLat(evacSite.getLat()); // store as-is
            encryptedEvacSite.setLongi(evacSite.getLongi()); // store as-is
            encryptedEvacSite.setNotes(encryptedNotes);

            boolean flag = evacSiteDAO.saving(encryptedEvacSite);

            if (!flag) {
                throw ExceptionFactory.failedToCreate("Evacuation Site");
            }
            return flag;
        } catch (SQLException ex) {
            System.out.println("Error: " + ex);
            return false;
        } catch (Exception ex) {
            System.out.println("Error: " + ex);
            return false;
        }
    }

    @Override
    public boolean deleteEvacSite(EvacSiteModel evacSite) {
        try {
            if (evacSite == null || evacSite.getEvacId() <= 0) {
                throw ExceptionFactory.missingField("Evacuation Site ID");
            }

            boolean deleted = evacSiteDAO.delete(evacSite);

            if (!deleted) {
                throw ExceptionFactory.failedToDelete("Evacuation Site");
            }

            return deleted;

        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateEvacSite(EvacSiteModel evacSite) {
        try {
            if (evacSite == null || evacSite.getEvacId() <= 0) {
                throw ExceptionFactory.missingField("Evacuation Site ID");
            }

            // Only encrypt name and notes
            String encryptedName = cs.encryptWithOneParameter(evacSite.getName());
            String encryptedNotes = cs.encryptWithOneParameter(evacSite.getNotes());

            EvacSiteModel encryptedEvacSite = new EvacSiteModel();
            encryptedEvacSite.setEvacId(evacSite.getEvacId());
            encryptedEvacSite.setName(encryptedName);
            encryptedEvacSite.setCapacity(evacSite.getCapacity()); // store as-is
            encryptedEvacSite.setLat(evacSite.getLat()); // store as-is
            encryptedEvacSite.setLongi(evacSite.getLongi()); // store as-is
            encryptedEvacSite.setNotes(encryptedNotes);

            boolean updated = evacSiteDAO.update(encryptedEvacSite);

            if (!updated) {
                throw ExceptionFactory.failedToUpdate("Evacuation Site");
            }

            return updated;

        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public EvacSiteModel getEvacSiteById(int id) {
        try {
            EvacSiteModel evacSite = evacSiteDAO.getById(id);
            if (evacSite == null) {
                System.out.println("Evacuation site not found with ID: " + id);
            }
            return evacSite;
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            return null;
        }
    }

    @Override
    public List<EvacSiteModel> searchEvacSite(String searchTxt) {
        List<EvacSiteModel> allEvacSites = getAllEvacSites();
        List<EvacSiteModel> filteredEvacSites = new ArrayList<>();

        for (EvacSiteModel evacSite : allEvacSites) {
            if (evacSite.getName().toLowerCase().contains(searchTxt.toLowerCase())) {
                filteredEvacSites.add(evacSite);
            }
        }
        return filteredEvacSites;
    }
}