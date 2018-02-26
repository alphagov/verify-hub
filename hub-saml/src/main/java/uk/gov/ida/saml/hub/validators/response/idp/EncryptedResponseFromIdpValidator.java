package uk.gov.ida.saml.hub.validators.response.idp;

import com.google.common.base.Strings;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.xmlsec.signature.Signature;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;
import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToIdaStatusCodeMapper;
import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToIdpIdaStatusMappingsFactory;
import uk.gov.ida.saml.hub.validators.response.common.IssuerValidator;
import uk.gov.ida.saml.hub.validators.response.common.RequestIdValidator;
import uk.gov.ida.saml.security.validators.signature.SamlSignatureUtil;

import java.util.Optional;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.invalidStatusCode;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.invalidSubStatusCode;

public class EncryptedResponseFromIdpValidator {

    public EncryptedResponseFromIdpValidator(final SamlStatusToIdpIdaStatusMappingsFactory statusMappingsFactory) {
        this.statusCodeMapper = new SamlStatusToIdaStatusCodeMapper(
            statusMappingsFactory.getSamlToIdpIdaStatusMappings()
        );
    }

    protected void validateAssertionPresence(Response response) {
        if (!response.getAssertions().isEmpty()) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.unencryptedAssertion();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        boolean responseWasSuccessful = response.getStatus().getStatusCode().getValue().equals(StatusCode.SUCCESS);
        if (responseWasSuccessful && response.getEncryptedAssertions().isEmpty()) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingSuccessUnEncryptedAssertions();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (!responseWasSuccessful && !response.getEncryptedAssertions().isEmpty()) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.nonSuccessHasUnEncryptedAssertions();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (responseWasSuccessful && response.getEncryptedAssertions().size() != 2) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.unexpectedNumberOfAssertions(2, response.getEncryptedAssertions().size());
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }

    private static final int SUB_STATUS_CODE_LIMIT = 1;

    private SamlStatusToIdaStatusCodeMapper statusCodeMapper;

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
        validateStatus(response.getStatus());
        validateAssertionPresence(response);
    }

    private void validateStatus(Status status) {
        validateStatusCode(status.getStatusCode(), 0);

        final Optional<IdpIdaStatus.Status> mappedStatus = statusCodeMapper.map(status);
        if (!mappedStatus.isPresent()) {
            fail(status);
        }
    }

    private void fail(final Status status) {
        final StatusCode statusCode = status.getStatusCode();
        final StatusCode subStatusCode = statusCode.getStatusCode();
        if (subStatusCode != null) {
            SamlValidationSpecificationFailure failure = invalidSubStatusCode(
                    subStatusCode.getValue(),
                    statusCode.getValue()
            );
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
        else {
            SamlValidationSpecificationFailure failure = invalidStatusCode(statusCode.getValue());
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }

    private void validateStatusCode(StatusCode statusCode, int subStatusCount) {
        if (subStatusCount > SUB_STATUS_CODE_LIMIT) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.nestedSubStatusCodesBreached(SUB_STATUS_CODE_LIMIT);
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        StatusCode subStatus = statusCode.getStatusCode();

        if (subStatus != null) {
            validateStatusCode(subStatus, subStatusCount + 1);
        }
    }
}
