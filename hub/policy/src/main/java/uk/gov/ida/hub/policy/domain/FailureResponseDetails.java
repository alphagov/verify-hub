package uk.gov.ida.hub.policy.domain;

@SuppressWarnings("unused")
public class FailureResponseDetails {
    private String idpEntityId;

    private String rpEntityId;

    private FailureResponseDetails(){}

    public FailureResponseDetails(String idpEntityId, String rpEntityId) {
        this.idpEntityId = idpEntityId;
        this.rpEntityId = rpEntityId;
    }

    public String getRpEntityId() { return rpEntityId; }

    public String getIdpEntityId() {
        return idpEntityId;
    }

}
