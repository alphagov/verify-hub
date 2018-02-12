package uk.gov.ida.hub.policy.domain;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class IdpSelected {

    @NotEmpty
    private String selectedIdpEntityId;
    @NotEmpty
    private String principalIpAddress;
    @NotNull
    private Boolean registration;
    @NotNull
    private LevelOfAssurance requestedLoa;

    // Required for JAXB
    @SuppressWarnings("unused")
    private IdpSelected() {
    }

    public IdpSelected(String selectedIdpEntityId, String principalIpAddress, Boolean registration, LevelOfAssurance requestedLoa) {
        this.selectedIdpEntityId = selectedIdpEntityId;
        this.principalIpAddress = principalIpAddress;
        this.registration = registration;
        this.requestedLoa = requestedLoa;
    }

    public String getSelectedIdpEntityId() {
        return selectedIdpEntityId;
    }

    public String getPrincipalIpAddress() {
        return principalIpAddress;
    }

    public Boolean isRegistration() { return registration; }

    public LevelOfAssurance getRequestedLoa() { return requestedLoa; }
}
