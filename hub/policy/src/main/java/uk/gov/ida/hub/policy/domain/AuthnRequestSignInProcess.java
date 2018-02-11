package uk.gov.ida.hub.policy.domain;

public class AuthnRequestSignInProcess {

    private String requestIssuerId;
    private boolean transactionSupportsEidas;

    @SuppressWarnings("unused")//Needed by JAXB
    private AuthnRequestSignInProcess() {
    }

    public AuthnRequestSignInProcess(
            String requestIssuerId,
            boolean transactionSupportsEidas) {

        this.requestIssuerId = requestIssuerId;
        this.transactionSupportsEidas = transactionSupportsEidas;
    }

    public String getRequestIssuerId() {
        return requestIssuerId;
    }

    public boolean getTransactionSupportsEidas() {
        return transactionSupportsEidas;
    }
}
