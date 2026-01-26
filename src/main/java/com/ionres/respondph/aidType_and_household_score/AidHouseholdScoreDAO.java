package com.ionres.respondph.aidType_and_household_score;

public interface AidHouseholdScoreDAO {


    HouseholdScoreData getHouseholdScores(int beneficiaryId);


    AidTypeWeights getAidTypeWeights(int aidTypeId);


    HouseholdMemberCounts countHouseholdMembers(int beneficiaryId);


    boolean saveAidHouseholdScore(int beneficiaryId, int aidTypeId, int adminId,
                                  HouseholdScoreData household, double finalScore,
                                  String scoreCategory, HouseholdMemberCounts memberCounts);

    class HouseholdScoreData {
        public Double ageScore;
        public double genderScore;
        public double maritalStatusScore;
        public double soloParentScore;
        public double disabilityScore;
        public double healthConditionScore;
        public double cleanWaterScore;
        public double sanitationScore;
        public double houseConstructionScore;
        public double ownershipScore;
        public double damageSeverityScore;
        public double employmentStatusScore;
        public double monthlyIncomeScore;
        public double educationLevelScore;
        public double digitalAccessScore;
        public Double dependencyRatioScore;
    }

    class AidTypeWeights {
        public Double ageWeight;
        public double genderWeight;
        public double maritalStatusWeight;
        public double soloParentWeight;
        public double disabilityWeight;
        public double healthConditionWeight;
        public double cleanWaterWeight;
        public double sanitationWeight;
        public double houseConstructionWeight;
        public double ownershipWeight;
        public double damageSeverityWeight;
        public double employmentStatusWeight;
        public double monthlyIncomeWeight;
        public double educationLevelWeight;
        public double digitalAccessWeight;
        public Double dependencyRatioWeight;
    }

    class HouseholdMemberCounts {
        public int totalMembers = 0;
        public int ableBodyMembers = 0;
        public int vulnerableMembers = 0;
    }
}

//
//package com.ionres.respondph.aidType_and_household_score;
//
//import java.util.List;
//
//public interface AidHouseholdScoreDAO {
//
//    /**
//     * Get household scores for a beneficiary
//     */
//    HouseholdScoreData getHouseholdScores(int beneficiaryId);
//
//    /**
//     * Get scores for all family members of a beneficiary (for averaging common indicators)
//     */
//    List<HouseholdMemberScoreData> getHouseholdMemberScores(int beneficiaryId);
//
//    /**
//     * Get aid type weights for a specific aid type
//     */
//    AidTypeWeights getAidTypeWeights(int aidTypeId);
//
//    /**
//     * Count household members (total, able-bodied, vulnerable)
//     */
//    HouseholdMemberCounts countHouseholdMembers(int beneficiaryId);
//
//    /**
//     * Save calculated aid household score to database
//     */
//    boolean saveAidHouseholdScore(int beneficiaryId, int aidTypeId, int adminId,
//                                  HouseholdScoreData household, double finalScore,
//                                  String scoreCategory, HouseholdMemberCounts memberCounts);
//
//    /**
//     * Data class for beneficiary household scores
//     */
//    class HouseholdScoreData {
//        public Double ageScore;
//        public double genderScore;
//        public double maritalStatusScore;
//        public double soloParentScore;
//        public double disabilityScore;
//        public double healthConditionScore;
//        public double cleanWaterScore;
//        public double sanitationScore;
//        public double houseConstructionScore;
//        public double ownershipScore;
//        public double damageSeverityScore;
//        public double employmentStatusScore;
//        public double monthlyIncomeScore;
//        public double educationLevelScore;
//        public double digitalAccessScore;
//        public Double dependencyRatioScore;
//    }
//
//    /**
//     * Data class for family member scores (common indicators only)
//     */
//    class HouseholdMemberScoreData {
//        public Double ageScore;
//        public double genderScore;
//        public double maritalStatusScore;
//        public double disabilityScore;
//        public double healthConditionScore;
//        public double educationLevelScore;
//        public double employmentStatusScore;
//    }
//
//    /**
//     * Data class for aid type weights
//     */
//    class AidTypeWeights {
//        public Double ageWeight;
//        public double genderWeight;
//        public double maritalStatusWeight;
//        public double soloParentWeight;
//        public double disabilityWeight;
//        public double healthConditionWeight;
//        public double cleanWaterWeight;
//        public double sanitationWeight;
//        public double houseConstructionWeight;
//        public double ownershipWeight;
//        public double damageSeverityWeight;
//        public double employmentStatusWeight;
//        public double monthlyIncomeWeight;
//        public double educationLevelWeight;
//        public double digitalAccessWeight;
//        public Double dependencyRatioWeight;
//    }
//
//    /**
//     * Data class for household member counts
//     */
//    class HouseholdMemberCounts {
//        public int totalMembers = 0;
//        public int ableBodyMembers = 0;
//        public int vulnerableMembers = 0;
//    }
//}