package uk.gov.ida.hub.policy.domain;

public class FraudDetectedDetails {
    private String idpFraudEventId;
    private String fraudIndicator;

    @SuppressWarnings("unused") //Needed for JAXB
    private FraudDetectedDetails(){}

    public FraudDetectedDetails(String idpFraudEventId, String fraudIndicator) {
        this.idpFraudEventId = idpFraudEventId;
        this.fraudIndicator = fraudIndicator;
    }

    public String getIdpFraudEventId() {
        return idpFraudEventId;
    }

    public String getFraudIndicator() {
        return fraudIndicator;
    }
}
