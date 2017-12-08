package uk.gov.ida.hub.config.domain;

/**
 * This enum is *only* used when loading the attribute names from config files, to ensure that we are
 * using an expected attribute. Config will fail to start if the cycle3 attribute name is not in this
 * enum.
 */
public enum Cycle3AttributeName {
    DrivingLicenceNumber,
    NationalInsuranceNumber,
    SaUniqueTaxpayerReference,
    sbiPiVendorNo,
    LandRegistryBorrowerReference,
    CICAEmailAddress
}
