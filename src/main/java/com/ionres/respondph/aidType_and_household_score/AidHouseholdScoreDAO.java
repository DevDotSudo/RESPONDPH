package com.ionres.respondph.aidType_and_household_score;

public interface AidHouseholdScoreDAO {

    HouseholdScoreData getHouseholdScoresWithDisaster(int beneficiaryId, int disasterId);
    HouseholdScoreData getHouseholdScores(int beneficiaryId);

    AidTypeWeights getAidTypeWeights(int aidTypeId);

    HouseholdMemberCounts countHouseholdMembers(int beneficiaryId);

    boolean saveAidHouseholdScoreWithDisaster(int beneficiaryId, int aidTypeId, int adminId, int disasterId,
                                              HouseholdScoreData household, double finalScore,
                                              String scoreCategory, HouseholdMemberCounts memberCounts);

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
        public int totalMembers;
        public int ableBodyMembers;
        public int vulnerableMembers;
    }
}
