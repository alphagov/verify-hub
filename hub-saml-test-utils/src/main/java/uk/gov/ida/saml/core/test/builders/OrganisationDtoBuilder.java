package uk.gov.ida.saml.core.test.builders;


import uk.gov.ida.saml.metadata.domain.OrganisationDto;

public class OrganisationDtoBuilder {

    private String organisationDisplayName = "Display Name";
    private String organisationName = "MegaCorp";

    public static OrganisationDtoBuilder anOrganisationDto() {
        return new OrganisationDtoBuilder();
    }

    public OrganisationDto build() {
        return new OrganisationDto(
                organisationDisplayName,
                organisationName,
                "https://hub.ida.gov.uk");
    }

    public OrganisationDtoBuilder withDisplayName(String organisationDisplayName) {
        this.organisationDisplayName = organisationDisplayName;
        return this;
    }

    public OrganisationDtoBuilder withName(String organisationName) {
        this.organisationName = organisationName;
        return this;
    }
}
