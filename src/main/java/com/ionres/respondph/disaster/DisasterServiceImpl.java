package com.ionres.respondph.disaster;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.exception.ExceptionFactory;
import com.ionres.respondph.util.Cryptography;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DisasterServiceImpl implements DisasterService {
    Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

    private final DisasterDAO disasterDAO;

    public DisasterServiceImpl(DBConnection dbConnection) {
        this.disasterDAO = new DisasterDAOImpl(dbConnection);
    }

    @Override
    public List<DisasterModel> getAllDisaster() {
        return disasterDAO.getAll();
    }

    @Override
    public boolean createDisaster(DisasterModel dm) {
        try {
            String encryptedDisasterType = cs.encryptWithOneParameter(dm.getDisasterType());
            String encryptedDisasterName = cs.encryptWithOneParameter(dm.getDisasterName());
            String encryptedDate         = cs.encryptWithOneParameter(dm.getDate());
            String encryptedNotes        = cs.encryptWithOneParameter(dm.getNotes());
            String encryptedRegDate      = cs.encryptWithOneParameter(dm.getRegDate());

            String encryptedLat    = (dm.getLat()    != null && !dm.getLat().trim().isEmpty())
                    ? cs.encryptWithOneParameter(dm.getLat()) : null;
            String encryptedLongi  = (dm.getLongi()  != null && !dm.getLongi().trim().isEmpty())
                    ? cs.encryptWithOneParameter(dm.getLongi()) : null;
            String encryptedRadius = (dm.getRadius() != null && !dm.getRadius().trim().isEmpty())
                    ? cs.encryptWithOneParameter(dm.getRadius()) : null;

            // ── NEW: encrypt poly_lat_long if present ────────────────────────
            String encryptedPoly = (dm.getPolyLatLong() != null && !dm.getPolyLatLong().trim().isEmpty())
                    ? cs.encryptWithOneParameter(dm.getPolyLatLong()) : null;
            // ─────────────────────────────────────────────────────────────────

            DisasterModel encryptedModel = new DisasterModel(
                    encryptedDisasterType, encryptedDisasterName, encryptedDate,
                    encryptedLat, encryptedLongi, encryptedRadius,
                    encryptedNotes, encryptedRegDate, dm.isBanateArea()
            );
            encryptedModel.setPolyLatLong(encryptedPoly); // ── NEW
            encryptedModel.setLocationType(dm.getLocationType()); // ── NEW

            boolean flag = disasterDAO.saving(encryptedModel);

            if (!flag) {
                throw ExceptionFactory.failedToCreate("Disaster");
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
    public boolean deleteDisaster(DisasterModel dm) {
        try {
            if (dm == null || dm.getDisasterId() <= 0) {
                throw ExceptionFactory.missingField("Disaster ID");
            }
            boolean deleted = disasterDAO.delete(dm);
            if (!deleted) throw ExceptionFactory.failedToDelete("Disaster");
            return deleted;
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateDisaster(DisasterModel dm) {
        try {
            if (dm == null || dm.getDisasterId() <= 0) {
                throw ExceptionFactory.missingField("Disaster ID");
            }

            String encryptedDisasterType = cs.encryptWithOneParameter(dm.getDisasterType());
            String encryptedDisasterName = cs.encryptWithOneParameter(dm.getDisasterName());
            String encryptedDate         = cs.encryptWithOneParameter(dm.getDate());
            String encryptedLat          = (dm.getLat() != null && !dm.getLat().trim().isEmpty())
                    ? cs.encryptWithOneParameter(dm.getLat()) : null;
            String encryptedLongi        = (dm.getLongi() != null && !dm.getLongi().trim().isEmpty())
                    ? cs.encryptWithOneParameter(dm.getLongi()) : null;
            String encryptedRadius       = (dm.getRadius() != null && !dm.getRadius().trim().isEmpty())
                    ? cs.encryptWithOneParameter(dm.getRadius()) : null;
            String encryptedNotes        = cs.encryptWithOneParameter(dm.getNotes());
            String encryptedRegDate      = cs.encryptWithOneParameter(dm.getRegDate());

            // ── NEW: encrypt poly_lat_long if present ────────────────────────
            String encryptedPoly = (dm.getPolyLatLong() != null && !dm.getPolyLatLong().trim().isEmpty())
                    ? cs.encryptWithOneParameter(dm.getPolyLatLong()) : null;
            // ─────────────────────────────────────────────────────────────────

            DisasterModel encryptedDm = new DisasterModel(
                    encryptedDisasterType, encryptedDisasterName, encryptedDate,
                    encryptedLat, encryptedLongi, encryptedRadius,
                    encryptedNotes, encryptedRegDate
            );
            encryptedDm.setDisasterId(dm.getDisasterId());
            encryptedDm.setPolyLatLong(encryptedPoly); // ── NEW
            encryptedDm.setIsBanateArea(dm.isBanateArea()); // ── NEW

            boolean updated = disasterDAO.update(encryptedDm);
            if (!updated) throw ExceptionFactory.failedToUpdate("Disaster");
            return updated;

        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public DisasterModel getDisasterById(int id) {
        try {
            DisasterModel dm = disasterDAO.getById(id);
            if (dm == null) {
                System.out.println("Disaster not found with ID: " + id);
            }
            return dm;
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            return null;
        }
    }

    @Override
    public List<DisasterModel> searchDisaster(String searchTxt) {
        List<DisasterModel> allDisaster = getAllDisaster();
        List<DisasterModel> filteredDisaster = new ArrayList<>();

        for (DisasterModel disasterModel : allDisaster) {
            if (disasterModel.getDisasterType().toLowerCase().contains(searchTxt.toLowerCase()) ||
                    disasterModel.getDisasterName().toLowerCase().contains(searchTxt.toLowerCase())) {
                filteredDisaster.add(disasterModel);
            }
        }
        return filteredDisaster;
    }
}