package com.ionres.respondph.disaster;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.exception.ExceptionFactory;
import com.ionres.respondph.util.Cryptography;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DisasterServiceImpl implements  DisasterService{
    Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

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

            String encryptedDisasterType = cs.encryptWithOneParameter(dm.getDisasterType());
            String encryptedDisasterName = cs.encryptWithOneParameter(dm.getDisasterName());
            String encryptedDate = cs.encryptWithOneParameter(dm.getDate());
            String encryptedLat = cs.encryptWithOneParameter(dm.getLat());
            String encryptedLongi = cs.encryptWithOneParameter(dm.getLongi());
            String encryptedRadius = cs.encryptWithOneParameter(dm.getRadius());
            String encryptedNotes = cs.encryptWithOneParameter(dm.getNotes());
            String encryptedRegDate = cs.encryptWithOneParameter(dm.getRegDate());


            boolean flag = disasterDAO.saving(new DisasterModel(
                    encryptedDisasterType, encryptedDisasterName, encryptedDate, encryptedLat, encryptedLongi, encryptedRadius,
                    encryptedNotes, encryptedRegDate
            ));

            if (!flag) {
                throw ExceptionFactory.failedToCreate("Disaster");
            }
            return flag;
        } catch (SQLException ex) {
            System.out.println("Error : " + ex);
            return  false;

        } catch (Exception ex) {
            System.out.println("Error: " + ex);
            return  false;
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
            String encryptedDate = cs.encryptWithOneParameter(dm.getDate());
            String encryptedLat = cs.encryptWithOneParameter(dm.getLat());
            String encryptedLongi = cs.encryptWithOneParameter(dm.getLongi());
            String encryptedRadius = cs.encryptWithOneParameter(dm.getRadius());
            String encryptedNotes = cs.encryptWithOneParameter(dm.getNotes());
            String encryptedRegDate = cs.encryptWithOneParameter(dm.getRegDate());

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
                    disasterModel.getDisasterName().toLowerCase().contains(searchTxt.toLowerCase())
            ) {
                filteredDisaster.add(disasterModel);
            }
        }
        return filteredDisaster;
    }
}