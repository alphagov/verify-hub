package uk.gov.ida.hub.samlsoapproxy.domain;

public class CertificateDto {
    private String issuerId;
    private String certificate;
    private KeyUse keyUse;
    private FederationEntityType federationEntityType;

    @SuppressWarnings("unused")//Needed by JAXB
    private CertificateDto() {
    }

    public CertificateDto(String issuerId, String certificate, KeyUse keyUse, FederationEntityType federationEntityType) {
        this.issuerId = issuerId;
        this.certificate = certificate;
        this.keyUse = keyUse;
        this.federationEntityType = federationEntityType;
    }

    public String getIssuerId() {
        return issuerId;
    }

    public String getCertificate() {
        return certificate;
    }

    public KeyUse getKeyUse() {
        return keyUse;
    }

    public FederationEntityType getFederationEntityType() { return federationEntityType; }

    public enum KeyUse {
        Signing("SIGNING"),
        Encryption("ENCRYPTION");
        private String description;

        KeyUse(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
