package uk.gov.ida.hub.samlproxy.repositories;

import uk.gov.ida.saml.core.validation.SamlValidationResponse;

public enum SignatureStatus {
    VALID_SIGNATURE, INVALID_SIGNATURE, NO_SIGNATURE;

    public static SignatureStatus fromValidationResponse(SamlValidationResponse samlValidationResponse) {
        return samlValidationResponse.isOK() ? VALID_SIGNATURE : INVALID_SIGNATURE;
    }

    public boolean valid() {
        return this == VALID_SIGNATURE;
    }
}
