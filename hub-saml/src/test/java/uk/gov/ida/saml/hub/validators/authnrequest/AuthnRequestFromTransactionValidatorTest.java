package uk.gov.ida.saml.hub.validators.authnrequest;

import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameIDType;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.hub.configuration.SamlAuthnRequestValidityDurationConfiguration;
import uk.gov.ida.saml.hub.configuration.SamlDuplicateRequestValidationConfiguration;
import uk.gov.ida.saml.security.validators.issuer.IssuerValidator;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.ida.saml.core.IdaConstants.SAML_VERSION_NUMBER;
import static uk.gov.ida.saml.core.test.AuthnRequestIdGenerator.generateRequestId;
import static uk.gov.ida.saml.core.test.builders.AuthnRequestBuilder.anAuthnRequest;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;
import static uk.gov.ida.saml.core.test.builders.NameIdPolicyBuilder.aNameIdPolicy;
import static uk.gov.ida.saml.core.test.builders.ScopingBuilder.aScoping;

@ExtendWith(OpenSAMLExtension.class)
public class AuthnRequestFromTransactionValidatorTest {

    private static AuthnRequestFromTransactionValidator validator;

    @BeforeAll
    public static void setup() {
        SamlDuplicateRequestValidationConfiguration samlDuplicateRequestValidationConfiguration = () -> Duration.hours(2);
        SamlAuthnRequestValidityDurationConfiguration samlAuthnRequestValidityDurationConfiguration = () -> Duration.minutes(5);
        IdExpirationCache idExpirationCache = new ConcurrentMapIdExpirationCache(new ConcurrentHashMap<>());
        validator = new AuthnRequestFromTransactionValidator(
                new IssuerValidator(),
                new DuplicateAuthnRequestValidator(idExpirationCache, samlDuplicateRequestValidationConfiguration),
                new AuthnRequestIssueInstantValidator(samlAuthnRequestValidityDurationConfiguration)
        );
    }

    @AfterEach
    public void unfreezeTime() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void validate_shouldThrowExceptionIfIdIsInvalid() {
        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(anAuthnRequest().withId("6135ce2c-fe0d-413a-9d12-2ae1063153bd").build())),
                SamlTransformationErrorFactory.invalidRequestID()
        );
    }

    @Test
    public void validate_shouldDoNothingIfIdIsValid() {
        validator.validate(anAuthnRequest().withId("a43qif88dsfv").build());

        validator.validate(anAuthnRequest().withId("_443qif88dsfv").build());
    }

    @Test
    public void validateRequest_shouldDoNothingIfRequestIsSigned() {

        validator.validate(anAuthnRequest().build());
    }

    @Test
    public void validateRequest_shouldThrowExceptionIfRequestDoesNotContainASignature() {
        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(anAuthnRequest().withoutSignatureElement().build())),
                SamlTransformationErrorFactory.missingSignature()
        );
    }

    @Test
    public void validateRequest_shouldThrowExceptionIfRequestIsNotSigned() {
        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(anAuthnRequest().withoutSigning().build())),
                SamlTransformationErrorFactory.signatureNotSigned()
        );
    }

    @Test
    public void validateIssuer_shouldThrowExceptionIfFormatAttributeHasInvalidValue() {
        String invalidFormat = "goo";
        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(anAuthnRequest().withIssuer(anIssuer().withFormat(invalidFormat).build()).build())),
                SamlTransformationErrorFactory.illegalIssuerFormat(invalidFormat, NameIDType.ENTITY)
        );
    }

    @Test
    public void validateIssuer_shouldDoNothingIfFormatAttributeIsMissing() {
        validator.validate(anAuthnRequest().withIssuer(anIssuer().withFormat(null).build()).build());

    }

    @Test
    public void validateIssuer_shouldDoNothingIfFormatAttributeHasValidValue() {
        validator.validate(anAuthnRequest().withIssuer(anIssuer().withFormat(NameIDType.ENTITY).build()).build());

    }

    @Test
    public void validateNameIdPolicy_shouldDoNothingIfNameIdPolicyIsMissing() {
        validator.validate(anAuthnRequest().build());

    }

    @Test
    public void validateNameIdPolicy_shouldDoNothingIfNameIdPolicyFormatHasValidValue() {
        validator.validate(anAuthnRequest().withNameIdPolicy(aNameIdPolicy().withFormat(NameIDType.PERSISTENT).build()).build());

    }

    @Test
    public void validateRequest_shouldThrowExceptionIfScopingIsPresent() {
        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(anAuthnRequest().withScoping(aScoping().build()).build())),
                SamlTransformationErrorFactory.scopingNotAllowed()
        );
    }

    @Test
    public void validateProtocolBinding_shouldDoNothingIfProtocolBindingHasValidValue() {
        validator.validate(anAuthnRequest().withProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI).build());

    }

    @Test
    public void validateProtocolBinding_shouldThrowExceptionIfProtocolBindingHasInvalidValue() {
        String invalidValue = "goo";
        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(anAuthnRequest().withProtocolBinding(invalidValue).build())),
                SamlTransformationErrorFactory.illegalProtocolBindingError(invalidValue, SAMLConstants.SAML2_POST_BINDING_URI)
        );
    }

    @Test
    public void validateRequest_shouldThrowExceptionIfIsPassiveIsPresent() {
        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(anAuthnRequest().withIsPassive(true).build())),
                SamlTransformationErrorFactory.isPassiveNotAllowed()
        );
    }

    @Test
    public void validateRequest_shouldThrowExceptionIfIsDuplicateRequestIdIsPresent() {
        final String requestId = generateRequestId();
        final String oneIssuerId = "some-issuer-id";
        final String anotherIssuerId = "some-other-issuer-id";
        final AuthnRequest authnRequest = anAuthnRequest().withId(requestId).withIssuer(anIssuer().withIssuerId(oneIssuerId).build()).build();

        validator.validate(authnRequest);
        final AuthnRequest duplicateIdAuthnRequest = anAuthnRequest().withId(requestId).withIssuer(anIssuer().withIssuerId(anotherIssuerId).build()).build();
    
        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(duplicateIdAuthnRequest)),
                SamlTransformationErrorFactory.duplicateRequestId(requestId, duplicateIdAuthnRequest.getIssuer().getValue())
        );
    }

    @Test
    public void validateRequest_shouldThrowExceptionIfRequestIsTooOld() {
        DateTimeFreezer.freezeTime();

        String requestId = generateRequestId();
        DateTime issueInstant = DateTime.now().minusMinutes(5).minusSeconds(1);

        final AuthnRequest authnRequest = anAuthnRequest().withId(requestId).withIssueInstant(issueInstant).build();

        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(authnRequest)),
                SamlTransformationErrorFactory.requestTooOld(requestId, issueInstant.withZone(DateTimeZone.UTC), DateTime.now())
        );
    }

    @Test
    public void validate_shouldThrowExceptionIfIdIsMissing() {
        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(anAuthnRequest().withId(null).build())),
                SamlTransformationErrorFactory.missingRequestId()
        );
    }

    @Test
    public void validate_shouldThrowExceptionIfIssuerElementIsMissing() {
        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(anAuthnRequest().withIssuer(null).build())),
                SamlTransformationErrorFactory.missingIssuer()
        );
    }

    @Test
    public void validate_shouldThrowExceptionIfIssuerIdIsMissing() {
        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(anAuthnRequest().withIssuer(anIssuer().withIssuerId(null).build()).build())),
                SamlTransformationErrorFactory.emptyIssuer()
        );
    }

    @Test
    public void validate_shouldThrowExceptionIfIssueInstantIsMissing() {
        AuthnRequest authnRequest = anAuthnRequest().withIssueInstant(null).build();
        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(authnRequest)),
                SamlTransformationErrorFactory.missingRequestIssueInstant(authnRequest.getID())
        );
    }

    @Test
    public void validate_shouldThrowExceptionIfVersionNumberIsMissing() {
        AuthnRequest authnRequest = anAuthnRequest().withVersionNumber(null).build();
        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(authnRequest)),
                SamlTransformationErrorFactory.missingRequestVersion(authnRequest.getID())
        );
    }

    @Test
    public void validate_shouldThrowExceptionIfVersionNumberIsNotTwoPointZero() {
        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(anAuthnRequest().withVersionNumber("1.0").build())),
                SamlTransformationErrorFactory.illegalRequestVersionNumber()
        );
    }

    @Test
    public void validate_shouldDoNothingIfVersionNumberIsTwoPointZero() {
        validator.validate(anAuthnRequest().withVersionNumber(SAML_VERSION_NUMBER).build());
    }
    
    private void validateException(SamlTransformationErrorException e, SamlValidationSpecificationFailure samlValidationSpecificationFailure) {
        assertThat(e.getMessage()).isEqualTo(samlValidationSpecificationFailure.getErrorMessage());
        assertThat(e.getLogLevel()).isEqualTo(samlValidationSpecificationFailure.getLogLevel());
    }
}
