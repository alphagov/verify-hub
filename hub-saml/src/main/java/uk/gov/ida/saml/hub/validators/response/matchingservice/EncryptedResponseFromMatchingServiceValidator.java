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

public class EncryptedResponseFromMatchingServiceValidator {

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
        validateIssuer(response.getIssuer());
        validateStatusAndSubStatus(response);
        validateAssertionPresence(response);
    }

    protected void validateStatusAndSubStatus(Response response) {

        StatusCode statusCode = response.getStatus().getStatusCode();

        if(statusCode.getValue().equals(StatusCode.REQUESTER)){
            return;
        }

        boolean responseHasNoSubStatus = statusCode.getStatusCode() == null;

        if (responseHasNoSubStatus) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingSubStatus();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        boolean statusWasResponder = response.getStatus().getStatusCode().getValue().equals(StatusCode.RESPONDER);
        boolean statusWasSuccess = response.getStatus().getStatusCode().getValue().equals(StatusCode.SUCCESS);

        boolean subStatusWasNoMatch = response.getStatus().getStatusCode().getStatusCode().getValue().equals(SamlStatusCode.NO_MATCH);
        boolean subStatusWasMatch = response.getStatus().getStatusCode().getStatusCode().getValue().equals(SamlStatusCode.MATCH);
        boolean subStatusWasMultiMatch = response.getStatus().getStatusCode().getStatusCode().getValue().equals(SamlStatusCode.MULTI_MATCH);
        boolean subStatusWasCreated = response.getStatus().getStatusCode().getStatusCode().getValue().equals(SamlStatusCode.CREATED);
        boolean subStatusWasCreateFailure = response.getStatus().getStatusCode().getStatusCode().getValue().equals(SamlStatusCode.CREATE_FAILURE);

        if (statusWasResponder) {
            if (!subStatusWasNoMatch && !subStatusWasMultiMatch && !subStatusWasCreateFailure) {
                SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.subStatusMustBeOneOf("Responder", "No Match", "Multi Match", "Create Failure");
                throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
            }
        } else {
            if (statusWasSuccess && !(subStatusWasMatch || subStatusWasNoMatch || subStatusWasCreated)) {
                SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.subStatusMustBeOneOf("Success", "Match", "No Match", "Created");
                throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
            }
        }
    }

    private void validateIssuer(Issuer issuer) {
        if (issuer.getFormat() != null && !issuer.getFormat().equals(NameIDType.ENTITY)) {
            String format = issuer.getFormat();
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.illegalIssuerFormat(format, NameIDType.ENTITY);
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }

    protected void validateAssertionPresence(Response response) {
        if (!response.getAssertions().isEmpty()) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.unencryptedAssertion();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        boolean responseWasSuccessful = response.getStatus().getStatusCode().getValue().equals(StatusCode.SUCCESS);
        boolean responseHasNoAssertions = response.getEncryptedAssertions().isEmpty();

        if (responseWasSuccessful && responseHasNoAssertions) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingSuccessUnEncryptedAssertions();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (!responseWasSuccessful && !responseHasNoAssertions) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.nonSuccessHasUnEncryptedAssertions();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (response.getEncryptedAssertions().size() > 1) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.unexpectedNumberOfAssertions(1, response.getEncryptedAssertions().size());
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }
}
