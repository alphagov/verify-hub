package uk.gov.ida.saml.metadata.transformers;

import org.opensaml.saml.saml2.metadata.Organization;
import uk.gov.ida.saml.metadata.domain.OrganisationDto;

public class OrganizationMarshaller {

    public OrganisationDto toDto(Organization organization) {
        String organisationName = organization.getDisplayNames().get(0).getValue();
        String orgFriendlyName = organization.getOrganizationNames().get(0).getValue();
        String orgUrl = organization.getURLs().get(0).getValue();
        return new OrganisationDto(organisationName, orgFriendlyName, orgUrl);
    }
}
