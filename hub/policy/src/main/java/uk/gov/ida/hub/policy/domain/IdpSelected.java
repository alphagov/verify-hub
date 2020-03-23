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

    private String analyticsSessionId;

    private String journeyType;

    private String abTestVariant;

    // Required for JAXB
    @SuppressWarnings("unused")
    private IdpSelected() {
    }

    public IdpSelected(String selectedIdpEntityId, String principalIpAddress, Boolean registration, LevelOfAssurance requestedLoa, String analyticsSessionId, String journeyType, String abTestVariant) {
        this.selectedIdpEntityId = selectedIdpEntityId;
        this.principalIpAddress = principalIpAddress;
        this.registration = registration;
        this.requestedLoa = requestedLoa;
        this.analyticsSessionId = analyticsSessionId;
        this.journeyType = journeyType;
        this.abTestVariant = abTestVariant;
    }

    public String getSelectedIdpEntityId() {
        return selectedIdpEntityId;
    }

    public String getPrincipalIpAddress() {
        return principalIpAddress;
    }

    public String getAnalyticsSessionId() {
        return analyticsSessionId;
    }

    public String getJourneyType() {
        return journeyType;
    }

    public Boolean isRegistration() {
        return registration;
    }

    public LevelOfAssurance getRequestedLoa() {
        return requestedLoa;
    }

    public String getAbTestVariant() {
        return abTestVariant;
    }
}
