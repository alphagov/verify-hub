package uk.gov.ida.saml.metadata.domain;

import org.joda.time.DateTime;
import uk.gov.ida.common.shared.security.Certificate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class HubIdentityProviderMetadataDto extends MetadataDto {

    private Collection<Certificate> idpSigningCertificates;
    private Collection<SamlEndpointDto> singleSignOnEndpoints = new ArrayList<>();

    public HubIdentityProviderMetadataDto(
            Collection<SamlEndpointDto> singleSignOnEndpoints,
            String entityId,
            OrganisationDto organisation,
            Collection<ContactPersonDto> contactPersons,
            Collection<Certificate> idpSigningCertificates,
            DateTime validUntil,
            List<Certificate> hubSigningCertificates,
            Certificate hubEncryptionCertificate) {

        super(entityId, validUntil, organisation, contactPersons, hubSigningCertificates, Arrays.asList(hubEncryptionCertificate));

        this.singleSignOnEndpoints = singleSignOnEndpoints;

        this.idpSigningCertificates = idpSigningCertificates;
    }

    public Collection<Certificate> getIdpSigningCertificates() {
        return idpSigningCertificates;
    }

    public Collection<SamlEndpointDto> getSingleSignOnEndpoints() {
        return singleSignOnEndpoints;
    }
}
