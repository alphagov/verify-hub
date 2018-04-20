package uk.gov.ida.saml.metadata.domain;

import org.joda.time.DateTime;
import uk.gov.ida.common.shared.security.Certificate;

import java.util.ArrayList;
import java.util.Collection;

public abstract class FetchedMetadata {
    private DateTime validUntil;
    private String entityId;
    private OrganisationDto organisation;
    private Collection<ContactPersonDto> contactPersons = new ArrayList<>();
    private Certificate signingCertificate;

    public FetchedMetadata(
        String entityId,
        DateTime validUntil,
        OrganisationDto organisation,
        Collection<ContactPersonDto> contactPersons,
        Certificate signingCertificate) {
        this.entityId = entityId;
        this.validUntil = validUntil;
        this.organisation = organisation;
        this.contactPersons = contactPersons;
        this.signingCertificate = signingCertificate;
    }

    public DateTime getValidUntil() {
        return validUntil;
    }

    public String getEntityId() {
        return entityId;
    }

    public OrganisationDto getOrganisation() {
        return organisation;
    }

    public Collection<ContactPersonDto> getContactPersons() {
        return contactPersons;
    }

    public Certificate getSigningCertificate() {
        return signingCertificate;
    }
}
