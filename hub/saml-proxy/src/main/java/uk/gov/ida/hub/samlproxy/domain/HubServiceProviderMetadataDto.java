package uk.gov.ida.hub.samlproxy.domain;

import org.joda.time.DateTime;
import uk.gov.ida.common.shared.security.Certificate;
import uk.gov.ida.saml.metadata.domain.AssertionConsumerServiceEndpointDto;
import uk.gov.ida.saml.metadata.domain.ContactPersonDto;
import uk.gov.ida.saml.metadata.domain.MetadataDto;
import uk.gov.ida.saml.metadata.domain.OrganisationDto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class HubServiceProviderMetadataDto extends MetadataDto {

    private List<AssertionConsumerServiceEndpointDto> assertionConsumerServiceBindings = new ArrayList<>();

    @SuppressWarnings("unused") // needed for JAXB
    private HubServiceProviderMetadataDto() {
    }

    public HubServiceProviderMetadataDto(
            String entityId,
            DateTime validUntil,
            OrganisationDto organisation,
            Collection<ContactPersonDto> contactPersons,
            Certificate signingCertificate,
            List<Certificate> encryptionCertificates,
            List<AssertionConsumerServiceEndpointDto> assertionConsumerServiceBindings) {

        this(entityId, validUntil, organisation, contactPersons, newArrayList(signingCertificate),encryptionCertificates, assertionConsumerServiceBindings);
    }

    public HubServiceProviderMetadataDto(
            String entityId,
            DateTime validUntil,
            OrganisationDto organisation,
            Collection<ContactPersonDto> contactPersons,
            List<Certificate> hubSigningCertificates,
            List<Certificate> encryptionCertificates,
            List<AssertionConsumerServiceEndpointDto> assertionConsumerServiceBindings) {

        super(entityId, validUntil, organisation, contactPersons, hubSigningCertificates, encryptionCertificates);

        this.assertionConsumerServiceBindings = assertionConsumerServiceBindings;
    }

    public List<AssertionConsumerServiceEndpointDto> getAssertionConsumerServiceBindings() {
        return assertionConsumerServiceBindings;
    }
}
