package uk.gov.ida.saml.hub.validators.response.common;

import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.saml.hub.exception.SamlValidationException;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.emptyInResponseTo;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.missingInResponseTo;

public class RequestIdValidator {

    public static void validate(Response response) {
        String requestId = response.getInResponseTo();
        if (requestId == null) throw new SamlValidationException(missingInResponseTo());
        if (requestId.isEmpty()) throw new SamlValidationException(emptyInResponseTo());
    }
}
