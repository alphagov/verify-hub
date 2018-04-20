package uk.gov.ida.saml.core.security;

import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;

public class RelayStateValidator {

    private static final int MAXIMUM_NUMBER_OF_CHARACTERS = 80;
    private static final String[] INVALID_CHARACTERS = new String[]{"<", ">", "'", ";", "\"","%", "&"};

    public void validate(String relayState) {
        if (relayState == null){
            return;
        }

        if (relayState.length() > MAXIMUM_NUMBER_OF_CHARACTERS) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.invalidRelayState(relayState);
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        for (String invalidCharacter : INVALID_CHARACTERS) {
            if (relayState.contains(invalidCharacter)) {
                SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.relayStateContainsInvalidCharacter(invalidCharacter, relayState);
                throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
            }
        }
    }
}
