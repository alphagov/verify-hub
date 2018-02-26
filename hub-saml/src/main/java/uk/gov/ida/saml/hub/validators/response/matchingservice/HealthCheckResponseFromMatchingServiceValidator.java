package uk.gov.ida.saml.hub.validators.response.matchingservice;

import com.google.common.base.Strings;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.xmlsec.signature.Signature;
import uk.gov.ida.saml.core.domain.SamlStatusCode;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.hub.validators.response.common.IssuerValidator;
import uk.gov.ida.saml.hub.validators.response.common.RequestIdValidator;
import uk.gov.ida.saml.security.validators.signature.SamlSignatureUtil;

public class HealthCheckResponseFromMatchingServiceValidator {

    public void validate(Response response) {
        IssuerValidator.validate(response);
        RequestIdValidator.validate(response);
        validateResponse(response);
    }

    private void validateResponse(Response response) {
        if (Strings.isNullOrEmpty(response.getID())) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingId();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        Signature signature = response.getSignature();
        if (signature == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingSignature();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
        if (!SamlSignatureUtil.isSignaturePresent(signature)) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.signatureNotSigned();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
        validateStatusAndSubStatus(response);
    }

    protected void validateStatusAndSubStatus(Response response) {

        StatusCode statusCode = response.getStatus().getStatusCode();

        if(statusCode.getValue().equals(StatusCode.REQUESTER)) return;

        boolean responseHasNoSubStatus = statusCode.getStatusCode() == null;

        if (responseHasNoSubStatus) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingSubStatus();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        final String statusCodeValue = statusCode.getValue();
        boolean statusWasSuccess = statusCodeValue.equals(StatusCode.SUCCESS);

        final String subStatusCodeValue = statusCode.getStatusCode().getValue();
        boolean subStatusWasHealthy = subStatusCodeValue.equals(SamlStatusCode.HEALTHY);

        if (!statusWasSuccess) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.invalidStatusCode(statusCodeValue);
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (!subStatusWasHealthy) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.invalidSubStatusCode(subStatusCodeValue, StatusCode.SUCCESS);
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }
}
