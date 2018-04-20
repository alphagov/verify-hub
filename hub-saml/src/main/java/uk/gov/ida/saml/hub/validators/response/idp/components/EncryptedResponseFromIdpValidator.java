package uk.gov.ida.saml.hub.validators.response.idp.components;

import com.google.common.base.Strings;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.xmlsec.signature.Signature;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;
import uk.gov.ida.saml.hub.exception.SamlValidationException;
import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToIdaStatusCodeMapper;
import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToIdpIdaStatusMappingsFactory;
import uk.gov.ida.saml.hub.validators.response.common.IssuerValidator;
import uk.gov.ida.saml.hub.validators.response.common.RequestIdValidator;

import java.util.List;
import java.util.Optional;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.*;
import static uk.gov.ida.saml.security.validators.signature.SamlSignatureUtil.isSignaturePresent;

public class EncryptedResponseFromIdpValidator {
    private static final int SUB_STATUS_CODE_LIMIT = 1;
    private SamlStatusToIdaStatusCodeMapper statusCodeMapper;

    public EncryptedResponseFromIdpValidator(final SamlStatusToIdpIdaStatusMappingsFactory statusMappingsFactory) {
        this.statusCodeMapper = new SamlStatusToIdaStatusCodeMapper(
            statusMappingsFactory.getSamlToIdpIdaStatusMappings()
        );
    }

    protected void validateAssertionPresence(Response response) {
        if (!response.getAssertions().isEmpty()) throw new SamlValidationException(unencryptedAssertion());

        boolean responseWasSuccessful = response.getStatus().getStatusCode().getValue().equals(StatusCode.SUCCESS);
        List<EncryptedAssertion> encryptedAssertions = response.getEncryptedAssertions();

        if (responseWasSuccessful && encryptedAssertions.isEmpty()) {
            throw new SamlValidationException(missingSuccessUnEncryptedAssertions());
        }

        if (!responseWasSuccessful && !encryptedAssertions.isEmpty()) {
            throw new SamlValidationException(nonSuccessHasUnEncryptedAssertions());
        }

        if (responseWasSuccessful && encryptedAssertions.size() != 2) {
            throw new SamlValidationException(unexpectedNumberOfAssertions(2, encryptedAssertions.size()));
        }
    }

    public void validate(Response response) {
        IssuerValidator.validate(response);
        RequestIdValidator.validate(response);
        validateResponse(response);
    }

    private void validateResponse(Response response) {
        if (Strings.isNullOrEmpty(response.getID())) throw new SamlValidationException(missingId());
        if (response.getIssueInstant() == null) throw new SamlValidationException(missingIssueInstant(response.getID()));

        Signature signature = response.getSignature();
        if (signature == null) throw new SamlValidationException(missingSignature());
        if (!isSignaturePresent(signature)) throw new SamlValidationException(signatureNotSigned());

        validateStatus(response.getStatus());
        validateAssertionPresence(response);
    }

    private void validateStatus(Status status) {
        validateStatusCode(status.getStatusCode(), 0);

        Optional<IdpIdaStatus.Status> mappedStatus = statusCodeMapper.map(status);
        if (!mappedStatus.isPresent()) fail(status);
    }

    private void fail(Status status) {
        StatusCode statusCode = status.getStatusCode();
        StatusCode subStatusCode = statusCode.getStatusCode();

        if (subStatusCode == null) throw new SamlValidationException(invalidStatusCode(statusCode.getValue()));

        SamlValidationSpecificationFailure failure = invalidSubStatusCode(
                subStatusCode.getValue(),
                statusCode.getValue()
        );
        throw new SamlValidationException(failure);
    }

    private void validateStatusCode(StatusCode statusCode, int subStatusCount) {
        if (subStatusCount > SUB_STATUS_CODE_LIMIT) {
            throw new SamlValidationException(nestedSubStatusCodesBreached(SUB_STATUS_CODE_LIMIT));
        }

        StatusCode subStatus = statusCode.getStatusCode();
        if (subStatus != null) validateStatusCode(subStatus, subStatusCount + 1);
    }
}
