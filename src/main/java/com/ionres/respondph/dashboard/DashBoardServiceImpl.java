package com.ionres.respondph.dashboard;

import com.ionres.respondph.common.model.*;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.CryptographyManager;
import com.ionres.respondph.util.NameDecryptionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashBoardServiceImpl implements DashBoardService {
    private static final Logger LOGGER = Logger.getLogger(DashBoardServiceImpl.class.getName());
    private final DashBoardDAO dashBoardDAO;
    private static final Cryptography CRYPTO = CryptographyManager.getInstance();

    public DashBoardServiceImpl(DBConnection connection) {
        this.dashBoardDAO = new DashBoardDAOImpl(connection);
    }
    @Override
    public int fetchTotalBeneficiary() {
        return dashBoardDAO.getTotalBeneficiaries();
    }

    @Override
    public int fetchTotalDisasters() {
        return dashBoardDAO.getTotalDisasters();
    }

    @Override
    public int fetchTotalAids() {
        return dashBoardDAO.getTotalAids();
    }

    @Override
    public int fetchTotalEvacuationSites() { return dashBoardDAO.getTotalEvacutaionSites(); }

    @Override
    public List<DisasterCircleInfo> getCircles() {
        List<DisasterCircleInfo> result = new ArrayList<>();

        for (DisasterCircleEncrypted e : dashBoardDAO.fetchAllEncrypted()) {
            try {
                double lat = Double.parseDouble(CRYPTO.decryptWithOneParameter(e.lat));
                double lon = Double.parseDouble(CRYPTO.decryptWithOneParameter(e.lon));
                double radius = Double.parseDouble(CRYPTO.decryptWithOneParameter(e.radius));

                // Dashboard doesn't need disaster name/type, so we pass empty strings
                result.add(new DisasterCircleInfo(lat, lon, radius, "", ""));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error decrypting circle coordinates", ex);
            }
        }
        return result;
    }

    @Override
    public List<BeneficiaryMarker> getBeneficiaries() {
        List<BeneficiaryMarker> result = new ArrayList<>();

        for (BeneficiariesMappingModel b : dashBoardDAO.fetchAllBeneficiaries()) {
            try {
                double lat = Double.parseDouble(CRYPTO.decryptWithOneParameter(b.getLat()));
                double lon = Double.parseDouble(CRYPTO.decryptWithOneParameter(b.getLng()));
                String fullName = NameDecryptionUtils.decryptFullName(b.getBeneficiaryName());

                result.add(new BeneficiaryMarker(b.getId(), fullName, lat, lon));

            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error decrypting beneficiary (ID: " + b.getId() + ")", ex);
            }
        }

        return result;
    }

    @Override
    public List<EvacSiteMarker> getEvacSites() {
        List<EvacSiteMarker> result = new ArrayList<>();

        for (EvacSiteMappingModel e : dashBoardDAO.fetchAllEvacSites()) {
            try {
                // Evac sites store coordinates as plain doubles in the database (not encrypted)
                double lat = Double.parseDouble(e.getLat());
                double lon = Double.parseDouble(e.getLng());

                // Decrypt the name if it's encrypted
                String name = CRYPTO.decryptWithOneParameter(e.getName());
                int capacity = Integer.parseInt(e.getCapacity());

                result.add(new EvacSiteMarker(e.getEvacId(), lat, lon, name, capacity));

            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error processing evac site (ID: " + e.getEvacId() + ")", ex);
            }
        }

        return result;
    }
}