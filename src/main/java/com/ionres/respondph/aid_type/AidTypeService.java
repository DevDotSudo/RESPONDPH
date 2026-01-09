package com.ionres.respondph.aid_type;





import java.util.List;

public interface AidTypeService {
    boolean createAidType(AidTypeModel atm);
    List<AidTypeModel> getAllAidType();
    boolean deleteAidType(AidTypeModel atm);
    boolean updateAidType(AidTypeModel atm);
    AidTypeModel getAidTypeById(int id);
    public List<AidTypeModel> searchAidType(String searchTxt);
}
