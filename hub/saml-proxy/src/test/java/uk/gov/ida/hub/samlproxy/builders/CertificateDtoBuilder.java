package uk.gov.ida.hub.samlproxy.builders;

import uk.gov.ida.hub.samlproxy.domain.CertificateDto;
import uk.gov.ida.hub.samlproxy.domain.FederationEntityType;
import uk.gov.ida.saml.core.test.TestCertificateStrings;

public class CertificateDtoBuilder {

    private boolean useCertificateOfIssuerId = true;
    private String issuerId = TestCertificateStrings.TEST_ENTITY_ID;
    private String certificate = null;
    private CertificateDto.KeyUse keyUse = CertificateDto.KeyUse.Signing;
    private FederationEntityType federationEntityType = FederationEntityType.RP;

    public static CertificateDtoBuilder aCertificateDto() {
        return new CertificateDtoBuilder();
    }

    public CertificateDto build() {
        if (useCertificateOfIssuerId) {
            if (keyUse == CertificateDto.KeyUse.Signing) {
                certificate = TestCertificateStrings.PUBLIC_SIGNING_CERTS.get(issuerId);
            } else {
                certificate = TestCertificateStrings.getPrimaryPublicEncryptionCert(issuerId);
            }
            if (certificate == null) {
                certificate = "Some certificate";
            }
        }
        return new CertificateDto(issuerId, certificate, keyUse, federationEntityType);
    }

    public CertificateDtoBuilder withIssuerId(String issuerID) {
        this.issuerId = issuerID;
        return this;
    }

    public CertificateDtoBuilder withCertificate(String certificate) {
        useCertificateOfIssuerId = false;
        this.certificate = certificate;
        return this;
    }

    public CertificateDtoBuilder withKeyUse(CertificateDto.KeyUse keyUse) {
        this.keyUse = keyUse;
        return this;
    }
}
