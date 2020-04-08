package uk.gov.ida.hub.policy.domain;

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

    public static LevelOfAssurance min(LevelOfAssurance... assurances) {
        if (assurances == null || assurances.length == 0) { return null; }
        if (assurances.length == 1) { return assurances[0]; }

        LevelOfAssurance min = assurances[0];
        for (int i = 1; i < assurances.length; i++) {
            if (assurances[i].lessThan(min)) { min = assurances[i]; }
        }
        return min;
    }

    public static LevelOfAssurance max(LevelOfAssurance... assurances) {
        if (assurances == null || assurances.length == 0) { return null; }
        if (assurances.length == 1) { return assurances[0]; }

        LevelOfAssurance max = assurances[0];
        for (int i = 1; i < assurances.length; i++) {
            if (assurances[i].greaterThan(max)) { max = assurances[i]; }
        }
        return max;
    }
}