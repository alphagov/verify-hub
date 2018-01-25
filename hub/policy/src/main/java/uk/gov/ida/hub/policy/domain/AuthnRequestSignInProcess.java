package uk.gov.ida.hub.policy.domain;

import java.util.List;

public class AuthnRequestSignInProcess {

    private List<String> availableIdentityProviderEntityIds;
    private String requestIssuerId;
    private boolean transactionSupportsEidas;

    @SuppressWarnings("unused")//Needed by JAXB
    private AuthnRequestSignInProcess() {
    }

    public AuthnRequestSignInProcess(
            List<String> availableIdentityProviderEntityIds,
            String requestIssuerId,
            boolean transactionSupportsEidas) {

        this.availableIdentityProviderEntityIds = availableIdentityProviderEntityIds;
        this.requestIssuerId = requestIssuerId;
        this.transactionSupportsEidas = transactionSupportsEidas;
    }

    public List<String> getAvailableIdentityProviderEntityIds() {
        return availableIdentityProviderEntityIds;
    }

    public String getRequestIssuerId() {
        return requestIssuerId;
    }

    public boolean getTransactionSupportsEidas() {
        return transactionSupportsEidas;
    }
}
