package com.ionres.respondph.disaster;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.exception.ExceptionFactory;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.CryptographyManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DisasterServiceImpl implements DisasterService {
    private static final Logger LOGGER = Logger.getLogger(DisasterServiceImpl.class.getName());
    private static final Cryptography CRYPTO = CryptographyManager.getInstance();
    private final DisasterDAO disasterDAO;

    public DisasterServiceImpl(DBConnection dbConnection) {
        this.disasterDAO = new DisasterDAOImpl(dbConnection);
    }
    @Override
    public List<DisasterModel> getAllDisaster() {
        List<DisasterModel> disasterModels = disasterDAO.getAll();
        return  disasterModels;
    }

    @Override
    public boolean createDisaster(DisasterModel dm) {
        try {

            String encryptedDisasterType = CRYPTO.encryptWithOneParameter(dm.getDisasterType());
            String encryptedDisasterName = CRYPTO.encryptWithOneParameter(dm.getDisasterName());
            String encryptedDate = CRYPTO.encryptWithOneParameter(dm.getDate());
            String encryptedLat = CRYPTO.encryptWithOneParameter(dm.getLat());
            String encryptedLongi = CRYPTO.encryptWithOneParameter(dm.getLongi());
            String encryptedRadius = CRYPTO.encryptWithOneParameter(dm.getRadius());
            String encryptedNotes = CRYPTO.encryptWithOneParameter(dm.getNotes());
            String encryptedRegDate = CRYPTO.encryptWithOneParameter(dm.getRegDate());


            boolean flag = disasterDAO.saving(new DisasterModel(
                    encryptedDisasterType, encryptedDisasterName, encryptedDate, encryptedLat, encryptedLongi, encryptedRadius,
                    encryptedNotes, encryptedRegDate
            ));

            if (!flag) {
                throw ExceptionFactory.failedToCreate("Disaster");
            }
            return flag;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQL error creating disaster", ex);
            return false;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error creating disaster", ex);
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

            if (!deleted) {
                throw ExceptionFactory.failedToDelete("Disaster");
            }

            return deleted;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error deleting disaster", ex);
            return false;
        }
    }

    @Override
    public boolean updateDisaster(DisasterModel dm) {
        try {
            if (dm == null || dm.getDisasterId() <= 0) {
                throw ExceptionFactory.missingField("Disaster ID");
            }

            String encryptedDisasterType = CRYPTO.encryptWithOneParameter(dm.getDisasterType());
            String encryptedDisasterName = CRYPTO.encryptWithOneParameter(dm.getDisasterName());
            String encryptedDate = CRYPTO.encryptWithOneParameter(dm.getDate());
            String encryptedLat = CRYPTO.encryptWithOneParameter(dm.getLat());
            String encryptedLongi = CRYPTO.encryptWithOneParameter(dm.getLongi());
            String encryptedRadius = CRYPTO.encryptWithOneParameter(dm.getRadius());
            String encryptedNotes = CRYPTO.encryptWithOneParameter(dm.getNotes());
            String encryptedRegDate = CRYPTO.encryptWithOneParameter(dm.getRegDate());

            DisasterModel encryptedDm = new DisasterModel(
                    encryptedDisasterType, encryptedDisasterName, encryptedDate,
                    encryptedLat, encryptedLongi, encryptedRadius,
                    encryptedNotes, encryptedRegDate
            );
            encryptedDm.setDisasterId(dm.getDisasterId());

            boolean updated = disasterDAO.update(encryptedDm);

            if (!updated) {
                throw ExceptionFactory.failedToUpdate("Disaster");
            }

            return updated;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error deleting disaster", ex);
            return false;
        }
    }

    @Override
    public DisasterModel getDisasterById(int id) {
        try {
            DisasterModel dm = disasterDAO.getById(id);
            if (dm == null) {
                LOGGER.warning("Disaster not found with ID: " + id);
            }
            return dm;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error fetching disaster by ID: " + id, ex);
            return null;
        }
    }

    @Override
    public List<DisasterModel> searchDisaster(String searchTxt) {
        List<DisasterModel> allDisaster = getAllDisaster();
        List<DisasterModel> filteredDisaster = new ArrayList<>();

        for (DisasterModel disasterModel : allDisaster) {
            if (disasterModel.getDisasterType().toLowerCase().contains(searchTxt.toLowerCase()) ||
                    disasterModel.getDisasterName().toLowerCase().contains(searchTxt.toLowerCase())
            ) {
                filteredDisaster.add(disasterModel);
            }
        }
        return filteredDisaster;
    }


}