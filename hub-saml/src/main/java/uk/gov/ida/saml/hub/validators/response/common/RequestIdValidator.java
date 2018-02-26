package uk.gov.ida.saml.hub.validators.response.common;

import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.emptyInResponseTo;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.missingInResponseTo;

public class RequestIdValidator {

    public static void validate(Response response) {
        String requestId = response.getInResponseTo();
        if (requestId == null) throwError(missingInResponseTo());
        if (requestId.isEmpty()) throwError(emptyInResponseTo());
    }

    private static void throwError(SamlValidationSpecificationFailure failure) {
        throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
    }
}
