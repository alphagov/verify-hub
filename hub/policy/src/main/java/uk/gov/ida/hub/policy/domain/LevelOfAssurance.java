package uk.gov.ida.hub.policy.domain;

import java.util.List;

// Do not change the ordering of this enum
public enum LevelOfAssurance {
    LEVEL_X,
    LEVEL_1,
    LEVEL_2,
    LEVEL_3,
    LEVEL_4;

    public boolean equalOrGreaterThan(LevelOfAssurance levelOfAssurance) {
        return this.ordinal() >= levelOfAssurance.ordinal();
    }

    public boolean greaterThan(LevelOfAssurance levelOfAssurance) {
        return this.ordinal()>levelOfAssurance.ordinal();
    }

    public boolean lessThan(LevelOfAssurance levelOfAssurance) {
        return this.ordinal()<levelOfAssurance.ordinal();
    }

    public static LevelOfAssurance min(List<LevelOfAssurance> assurances) {
        if (assurances == null || assurances.size() == 0) { return null; }
        if (assurances.size() == 1) { return assurances.get(0); }

        LevelOfAssurance min = assurances.get(0);
        for (int i = 1; i < assurances.size(); i++) {
            if (assurances.get(i).lessThan(min)) { min = assurances.get(i); }
        }
        return min;
    }

    public static LevelOfAssurance max(List<LevelOfAssurance> assurances) {
        if (assurances == null || assurances.size() == 0) { return null; }
        if (assurances.size() == 1) { return assurances.get(0); }

        LevelOfAssurance max = assurances.get(0);
        for (int i = 1; i < assurances.size(); i++) {
            if (assurances.get(i).greaterThan(max)) { max = assurances.get(i); }
        }
        return max;
    }
}