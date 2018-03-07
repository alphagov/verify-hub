package uk.gov.ida.saml.metadata.domain;

public class OrganisationDto {
    private String displayName;
    private String name;
    private String url;

    @SuppressWarnings("unused")//Needed by JAXB
    private OrganisationDto() {}

    public OrganisationDto(String displayName, String name, String url) {
        this.displayName = displayName;
        this.name = name;
        this.url = url;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
