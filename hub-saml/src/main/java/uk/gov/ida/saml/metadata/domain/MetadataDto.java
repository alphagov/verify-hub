package uk.gov.ida.saml.metadata.domain;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import uk.gov.ida.common.shared.security.Certificate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class MetadataDto {
    protected List<Certificate> encryptionCertificates;
    private DateTime validUntil;
    private String entityId;
    private OrganisationDto organisation;
    private Collection<ContactPersonDto> contactPersons = new ArrayList<>();
    private List<Certificate> hubSigningCertificates;

    protected MetadataDto() {}

    public MetadataDto(
            String entityId,
            DateTime validUntil,
            OrganisationDto organisation,
            Collection<ContactPersonDto> contactPersons,
            List<Certificate> hubSigningCertificates, List<Certificate> encryptionCertificates) {
        this.entityId = entityId;
        this.validUntil = validUntil;
        this.organisation = organisation;
        this.contactPersons = contactPersons;
        this.hubSigningCertificates = hubSigningCertificates;
        this.encryptionCertificates = encryptionCertificates;
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

    public List<Certificate> getSigningCertificates() {
        return hubSigningCertificates;
    }

    public Collection<Certificate> getCertificates() {
        return ImmutableList.<Certificate>builder()
            .addAll(hubSigningCertificates)
            .addAll(encryptionCertificates)
            .build();
    }

    public List<Certificate> getEncryptionCertificates() {
        return encryptionCertificates;
    }
}
