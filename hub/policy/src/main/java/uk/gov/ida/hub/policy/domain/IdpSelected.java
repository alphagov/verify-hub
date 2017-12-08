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

    // Required for JAXB
    @SuppressWarnings("unused")
    private IdpSelected() {
    }

    public IdpSelected(String selectedIdpEntityId, String principalIpAddress) {
        this.selectedIdpEntityId = selectedIdpEntityId;
        this.principalIpAddress = principalIpAddress;
    }

    public IdpSelected(String selectedIdpEntityId, String principalIpAddress, Boolean registration) {
        this.selectedIdpEntityId = selectedIdpEntityId;
        this.principalIpAddress = principalIpAddress;
        this.registration = registration;
    }

    public String getSelectedIdpEntityId() {
        return selectedIdpEntityId;
    }

    public String getPrincipalIpAddress() {
        return principalIpAddress;
    }

    public Boolean isRegistration() { return registration; }
}
