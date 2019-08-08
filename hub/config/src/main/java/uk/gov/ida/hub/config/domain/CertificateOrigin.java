package uk.gov.ida.hub.config.domain;

public enum CertificateOrigin {
    SELFSERVICE (false),
    FEDERATION (true);


    private final boolean shouldCheckTrustChain;

    CertificateOrigin(boolean shouldcheckTrustChain) {
        this.shouldCheckTrustChain = shouldcheckTrustChain;
    }


    public boolean shouldCheckTrustChain() {
        return shouldCheckTrustChain;
    }
}