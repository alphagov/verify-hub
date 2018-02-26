package uk.gov.ida.saml.hub.validators.response.matchingservice;

import com.google.common.base.Strings;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.xmlsec.signature.Signature;
import uk.gov.ida.saml.core.domain.SamlStatusCode;
import uk.gov.ida.saml.hub.exception.SamlValidationException;
import uk.gov.ida.saml.hub.validators.response.common.IssuerValidator;
import uk.gov.ida.saml.hub.validators.response.common.RequestIdValidator;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.invalidStatusCode;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.invalidSubStatusCode;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.missingId;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.missingSignature;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.missingSubStatus;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.signatureNotSigned;
import static uk.gov.ida.saml.security.validators.signature.SamlSignatureUtil.isSignaturePresent;

public class HealthCheckResponseFromMatchingServiceValidator {

    public void validate(Response response) {
        IssuerValidator.validate(response);
        RequestIdValidator.validate(response);
        validateResponse(response);
    }

    private void validateResponse(Response response) {
        if (Strings.isNullOrEmpty(response.getID())) throw new SamlValidationException(missingId());

        Signature signature = response.getSignature();
        if (signature == null) throw new SamlValidationException(missingSignature());
        if (!isSignaturePresent(signature)) throw new SamlValidationException(signatureNotSigned());

        validateStatusAndSubStatus(response);
    }

    protected void validateStatusAndSubStatus(Response response) {
        StatusCode statusCode = response.getStatus().getStatusCode();

        if(StatusCode.REQUESTER.equals(statusCode.getValue())) return;

        if (statusCode.getStatusCode() == null) throw new SamlValidationException(missingSubStatus());

        String statusCodeValue = statusCode.getValue();
        if (!StatusCode.SUCCESS.equals(statusCodeValue)) throw new SamlValidationException(invalidStatusCode(statusCodeValue));

        String subStatusCodeValue = statusCode.getStatusCode().getValue();
        if (!SamlStatusCode.HEALTHY.equals(subStatusCodeValue)) {
            throw new SamlValidationException(invalidSubStatusCode(subStatusCodeValue, StatusCode.SUCCESS));
        }
    }
}
