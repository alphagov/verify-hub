package uk.gov.ida.hub.policy.domain;

public abstract class ResponseFromMatchingService {

    private String issuer;
    private String inResponseTo;

    @SuppressWarnings("unused")//Needed by JAXB
    protected ResponseFromMatchingService() {
    }

    protected ResponseFromMatchingService(String issuer, String inResponseTo) {
        this.issuer = issuer;
        this.inResponseTo = inResponseTo;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getInResponseTo() {
        return inResponseTo;
    }
}
