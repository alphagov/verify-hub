package uk.gov.ida.hub.policy.domain;

// do not reorder this enum - the ordinals are used for comparison
public enum LevelOfAssurance implements Comparable<LevelOfAssurance> {
    LEVEL_X,
    LEVEL_1,
    LEVEL_2,
    LEVEL_3,
    LEVEL_4;
}