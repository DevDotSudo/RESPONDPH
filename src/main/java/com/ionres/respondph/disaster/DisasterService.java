package com.ionres.respondph.disaster;

import java.util.List;

public interface DisasterService {
    List<DisasterModel> getAllDisaster();
    boolean createDisaster(DisasterModel dm);
    boolean deleteDisaster(DisasterModel dm);
    boolean updateDisaster(DisasterModel dm);
    DisasterModel getDisasterById(int id);
    public List<DisasterModel> searchDisaster(String searchTxt);
}