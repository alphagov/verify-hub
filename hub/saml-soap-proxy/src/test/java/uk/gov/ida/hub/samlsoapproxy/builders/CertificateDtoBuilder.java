package uk.gov.ida.hub.samlsoapproxy.builders;

import uk.gov.ida.hub.samlsoapproxy.domain.CertificateDto;
import uk.gov.ida.hub.samlsoapproxy.domain.FederationEntityType;

import static uk.gov.ida.saml.core.test.TestCertificateStrings.PUBLIC_SIGNING_CERTS;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_ENTITY_ID;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.getPrimaryPublicEncryptionCert;

public class CertificateDtoBuilder {

    private boolean useCertificateOfIssuerId = true;
    private String issuerId = TEST_ENTITY_ID;
    private String certificate;
    private CertificateDto.KeyUse keyUse = CertificateDto.KeyUse.Signing;
    private FederationEntityType federationEntityType = FederationEntityType.RP;

    public CertificateDto build() {
        if (useCertificateOfIssuerId) {
            if (keyUse == CertificateDto.KeyUse.Signing) {
               certificate = PUBLIC_SIGNING_CERTS.get(issuerId);
            } else {
                certificate = getPrimaryPublicEncryptionCert(issuerId);
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
