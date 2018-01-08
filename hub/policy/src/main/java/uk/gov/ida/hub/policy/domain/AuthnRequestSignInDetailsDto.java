package uk.gov.ida.hub.policy.domain;

public class AuthnRequestSignInDetailsDto {

    private String requestIssuerId;
    private boolean transactionSupportsEidas;

    @SuppressWarnings("unused")//Needed by JAXB
    private AuthnRequestSignInDetailsDto() { }

    public AuthnRequestSignInDetailsDto(
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
