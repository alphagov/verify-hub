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
}