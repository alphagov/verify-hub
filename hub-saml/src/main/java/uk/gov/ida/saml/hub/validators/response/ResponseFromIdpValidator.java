package uk.gov.ida.saml.hub.validators.response;

import com.google.common.base.Strings;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.xmlsec.signature.Signature;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;
import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToIdaStatusCodeMapper;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToIdpIdaStatusMappingsFactory;
import uk.gov.ida.saml.security.validators.signature.SamlSignatureUtil;

import java.util.Optional;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.invalidStatusCode;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.invalidSubStatusCode;

public abstract class ResponseFromIdpValidator {

    private static final int SUB_STATUS_CODE_LIMIT = 1;

    private SamlStatusToIdaStatusCodeMapper statusCodeMapper;

    protected ResponseFromIdpValidator(
            final SamlStatusToIdpIdaStatusMappingsFactory statusMappingsFactory) {

        this.statusCodeMapper = new SamlStatusToIdaStatusCodeMapper(
                statusMappingsFactory.getSamlToIdpIdaStatusMappings()
        );
    }

    public void validate(Response response) {
        validateRequestIdAndIssuerId(response);
        validateResponse(response);
    }

    protected abstract void validateAssertionPresence(Response response);

    private void validateRequestIdAndIssuerId(Response response) {
        Issuer issuer = response.getIssuer();
        if (issuer == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingIssuer();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        String issuerId = issuer.getValue();
        if (Strings.isNullOrEmpty(issuerId)) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.emptyIssuer();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (response.getInResponseTo() == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingInResponseTo();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
        if (response.getInResponseTo().isEmpty()) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.emptyInResponseTo();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
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

    private void validateIssuer(Issuer issuer) {
        if (issuer.getFormat() != null && !issuer.getFormat().equals(NameIDType.ENTITY)) {
            String format = issuer.getFormat();
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.illegalIssuerFormat(format, NameIDType.ENTITY);
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }
}
