package com.ionres.respondph.household_score;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.disaster_damage.DisasterDamageModel;
import com.ionres.respondph.familymembers.FamilyMembersModel;
import com.ionres.respondph.vulnerability_indicator.VulnerabilityIndicatorScoreModel;

import java.util.List;

public interface HouseholdScoreDAO {

    VulnerabilityIndicatorScoreModel getVulnerabilityScores();
    BeneficiaryModel getBeneficiaryById(int beneficiaryId);

    List<FamilyMembersModel> getFamilyMembersByBeneficiaryId(int beneficiaryId);

    List<DisasterDamageModel> getDisasterDamageById(int beneficiaryId);

    boolean saveHouseholdScore(HouseholdScoreModel score);
     boolean saveHouseholdScoreWithDisaster(HouseholdScoreModel score);

    List<Integer> getAllBeneficiaryIdsWithHouseholdScores();

    boolean updateNullDisasterIdToSpecificDisaster(int beneficiaryId, int disasterId);

}
