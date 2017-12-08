package uk.gov.ida.hub.samlengine.validation.country;


import org.slf4j.event.Level;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;

import java.text.MessageFormat;

public class EidasProfileValidationSpecification {

    public static final SamlTransformationErrorException authnResponseAssertionIssuerFormatMismatch(String responseIssuerFormat, String assertionIssuerFormat) {
        return new SamlTransformationErrorException(
            format("The Authn Response issuer format [{0}] does not match the assertion issuer format [{1}]", responseIssuerFormat, assertionIssuerFormat),
            Level.ERROR
        );
    }

    public static final SamlTransformationErrorException authnResponseAssertionIssuerValueMismatch(String responseIssuer, String assertionIssuer) {
        return new SamlTransformationErrorException(
            format("The Authn Response issuer [{0}] does not match the assertion issuer [{1}]", responseIssuer, assertionIssuer),
            Level.ERROR
        );
    }

    private static final String format(String errorMessage, Object... params) {
        return MessageFormat.format(errorMessage, params);
    }
}
