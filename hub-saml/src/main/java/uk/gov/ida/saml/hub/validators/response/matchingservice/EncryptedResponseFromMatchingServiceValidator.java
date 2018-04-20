package uk.gov.ida.saml.hub.validators.response.matchingservice;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.xmlsec.signature.Signature;
import uk.gov.ida.saml.core.domain.SamlStatusCode;
import uk.gov.ida.saml.hub.exception.SamlValidationException;
import uk.gov.ida.saml.hub.validators.response.common.IssuerValidator;
import uk.gov.ida.saml.hub.validators.response.common.RequestIdValidator;
import uk.gov.ida.saml.security.validators.signature.SamlSignatureUtil;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.missingId;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.missingSignature;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.missingSubStatus;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.missingSuccessUnEncryptedAssertions;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.nonSuccessHasUnEncryptedAssertions;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.signatureNotSigned;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.subStatusMustBeOneOf;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.unencryptedAssertion;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.unexpectedNumberOfAssertions;
import static uk.gov.ida.saml.security.validators.signature.SamlSignatureUtil.isSignaturePresent;

public class EncryptedResponseFromMatchingServiceValidator {

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
        validateAssertionPresence(response);
    }

    protected void validateStatusAndSubStatus(Response response) {
        StatusCode statusCode = response.getStatus().getStatusCode();
        String statusCodeValue = statusCode.getValue();

        StatusCode subStatusCode = statusCode.getStatusCode();

        if (StatusCode.REQUESTER.equals(statusCodeValue)) return;

        if (subStatusCode == null) throw new SamlValidationException(missingSubStatus());

        String subStatusCodeValue = subStatusCode.getValue();

        if (!StatusCode.RESPONDER.equals(statusCodeValue)) {
            validateSuccessResponse(statusCodeValue, subStatusCodeValue);
        } else {
            validateResponderError(subStatusCodeValue);
        }
    }

    private void validateResponderError(String subStatusCodeValue) {
        if (ImmutableList.of(
            SamlStatusCode.NO_MATCH,
            SamlStatusCode.MULTI_MATCH,
            SamlStatusCode.CREATE_FAILURE).contains(subStatusCodeValue)) {
            return;
        }

        throw new SamlValidationException(subStatusMustBeOneOf("Responder", "No Match", "Multi Match", "Create Failure"));
    }

    private void validateSuccessResponse(String statusCodeValue, String subStatusCodeValue) {
        if (!StatusCode.SUCCESS.equals(statusCodeValue)) return;
        if (ImmutableList.of(
                SamlStatusCode.MATCH,
                SamlStatusCode.NO_MATCH,
                SamlStatusCode.CREATED).contains(subStatusCodeValue)) {
            return;
        }

        throw new SamlValidationException(subStatusMustBeOneOf("Success", "Match", "No Match", "Created"));
    }

    protected void validateAssertionPresence(Response response) {
        if (!response.getAssertions().isEmpty()) throw new SamlValidationException(unencryptedAssertion());

        boolean responseWasSuccessful = StatusCode.SUCCESS.equals(response.getStatus().getStatusCode().getValue());
        boolean responseHasNoAssertions = response.getEncryptedAssertions().isEmpty();

        if (responseWasSuccessful && responseHasNoAssertions)
            throw new SamlValidationException(missingSuccessUnEncryptedAssertions());

        if (!responseWasSuccessful && !responseHasNoAssertions) {
            throw new SamlValidationException(nonSuccessHasUnEncryptedAssertions());
        }

        if (response.getEncryptedAssertions().size() > 1) {
            throw new SamlValidationException(unexpectedNumberOfAssertions(1, response.getEncryptedAssertions().size()));
        }
    }
}
