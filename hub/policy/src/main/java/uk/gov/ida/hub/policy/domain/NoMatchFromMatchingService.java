package uk.gov.ida.hub.policy.domain;

public class NoMatchFromMatchingService extends ResponseFromMatchingService {

    @SuppressWarnings("unused")//Needed by JAXB
    private NoMatchFromMatchingService() {
    }

    public NoMatchFromMatchingService(String issuer, String inResponseTo) {
        super(issuer, inResponseTo);
    }
}
