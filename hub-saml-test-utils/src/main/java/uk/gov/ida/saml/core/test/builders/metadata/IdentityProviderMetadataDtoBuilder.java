package uk.gov.ida.saml.core.test.builders.metadata;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import uk.gov.ida.common.shared.security.Certificate;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.test.builders.CertificateBuilder;
import uk.gov.ida.saml.core.test.builders.ContactPersonDtoBuilder;
import uk.gov.ida.saml.core.test.builders.OrganisationDtoBuilder;
import uk.gov.ida.saml.metadata.domain.ContactPersonDto;
import uk.gov.ida.saml.metadata.domain.HubIdentityProviderMetadataDto;
import uk.gov.ida.saml.metadata.domain.OrganisationDto;
import uk.gov.ida.saml.metadata.domain.SamlEndpointDto;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class IdentityProviderMetadataDtoBuilder {

    private List<SamlEndpointDto> singleSignOnServiceEndpoints = new ArrayList<>();
    private String entityId = TestEntityIds.HUB_ENTITY_ID;
    private OrganisationDto organisation = OrganisationDtoBuilder.anOrganisationDto().build();
    private List<ContactPersonDto> contactPersons = new ArrayList<>();
    private List<Certificate> signingCertificates = null;
    private DateTime validUntil = DateTime.now().plus(Duration.standardDays(365));
    private List<Certificate> idpSigningCertificates = new ArrayList<>();
    private Certificate encryptionCertificate = null;

    private boolean useDefaultSingleSignOnServiceEndpoints = true;
    private boolean useDefaultContactPerson = true;

    public static IdentityProviderMetadataDtoBuilder anIdentityProviderMetadataDto() {
        return new IdentityProviderMetadataDtoBuilder();
    }

    public HubIdentityProviderMetadataDto build() {
        populateListsWithDefaults();

        return new HubIdentityProviderMetadataDto(
                singleSignOnServiceEndpoints,
                entityId,
                organisation,
                contactPersons,
                idpSigningCertificates,
                validUntil,
                signingCertificates,
                encryptionCertificate);
    }

    private void populateListsWithDefaults() {
        if (useDefaultSingleSignOnServiceEndpoints) {
            this.singleSignOnServiceEndpoints.add(SamlEndpointDto.createPostBinding(URI.create("https://hub.ida.gov.uk/SAML2/SSO/POST")));
        }
        if (useDefaultContactPerson) {
            this.contactPersons.add(ContactPersonDtoBuilder.aContactPersonDto().build());
        }
        if (signingCertificates == null && entityId != null) {
            this.withSigningCertificate(CertificateBuilder.aCertificate().withKeyUse(Certificate.KeyUse.Signing).withIssuerId(entityId).build());
        }
        if (idpSigningCertificates.isEmpty()) {
            this.idpSigningCertificates.add(CertificateBuilder.aCertificate().build());
        }
        if (encryptionCertificate == null && entityId != null) {
            this.encryptionCertificate = CertificateBuilder.aCertificate().withKeyUse(Certificate.KeyUse.Encryption).withIssuerId(entityId).build();
        }
    }

    public IdentityProviderMetadataDtoBuilder addSingleSignOnServiceEndpoint(SamlEndpointDto samlEndpoint) {
        this.singleSignOnServiceEndpoints.add(samlEndpoint);
        this.useDefaultSingleSignOnServiceEndpoints = false;
        return this;
    }

    public IdentityProviderMetadataDtoBuilder withEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public IdentityProviderMetadataDtoBuilder addContactPerson(ContactPersonDto contactPerson) {
        this.contactPersons.add(contactPerson);
        this.useDefaultContactPerson = false;
        return this;
    }

    public IdentityProviderMetadataDtoBuilder withSigningCertificate(Certificate certificate) {
        this.signingCertificates = ImmutableList.of(certificate);
        return this;
    }

    public IdentityProviderMetadataDtoBuilder withSigningCertificates(List<Certificate> signingCertificates) {
        this.signingCertificates = signingCertificates;
        return this;
    }

    public IdentityProviderMetadataDtoBuilder withValidUntil(DateTime validUntil) {
        this.validUntil = validUntil;
        return this;
    }

    public IdentityProviderMetadataDtoBuilder addIdpSigningCertificate(Certificate certificate) {
        this.idpSigningCertificates.add(certificate);
        return this;
    }

    public IdentityProviderMetadataDtoBuilder withHubEncryptionCertificate(Certificate certificate) {
        this.encryptionCertificate = certificate;
        return this;
    }
}
