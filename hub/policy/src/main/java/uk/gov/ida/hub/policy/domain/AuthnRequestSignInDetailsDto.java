package uk.gov.ida.hub.policy.domain;

import java.util.List;

public class AuthnRequestSignInDetailsDto {

    private List<IdpConfigDto> availableIdentityProviders;
    private List<String> availableIdentityProviderEntityIds;
    private String requestIssuerId;
    private boolean transactionSupportsEidas;

    @SuppressWarnings("unused")//Needed by JAXB
    private AuthnRequestSignInDetailsDto() {
    }

    public AuthnRequestSignInDetailsDto(
            List<IdpConfigDto> availableIdentityProviders,
            List<String> availableIdentityProviderEntityIds,
            String requestIssuerId,
            boolean transactionSupportsEidas) {

        this.availableIdentityProviders = availableIdentityProviders;
        this.availableIdentityProviderEntityIds = availableIdentityProviderEntityIds;
        this.requestIssuerId = requestIssuerId;
        this.transactionSupportsEidas = transactionSupportsEidas;
    }

    public List<IdpConfigDto> getAvailableIdentityProviders() {
        return availableIdentityProviders;
    }

    public String getRequestIssuerId() {
        return requestIssuerId;
    }

    public boolean getTransactionSupportsEidas() {
        return transactionSupportsEidas;
    }

    public List<String> getAvailableIdentityProviderEntityIds() {
        return availableIdentityProviderEntityIds;
    }
}
