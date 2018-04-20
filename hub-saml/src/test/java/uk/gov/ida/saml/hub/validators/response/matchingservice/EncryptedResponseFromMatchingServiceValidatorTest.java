package uk.gov.ida.saml.hub.validators.response.matchingservice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.domain.SamlStatusCode;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.*;
import static uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper.validateFail;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;
import static uk.gov.ida.saml.core.test.builders.StatusBuilder.aStatus;
import static uk.gov.ida.saml.core.test.builders.StatusCodeBuilder.aStatusCode;
import static uk.gov.ida.saml.hub.validators.response.helpers.ResponseValidatorTestHelper.createStatus;
import static uk.gov.ida.saml.hub.validators.response.helpers.ResponseValidatorTestHelper.createSubStatusCode;

@RunWith(OpenSAMLMockitoRunner.class)
public class EncryptedResponseFromMatchingServiceValidatorTest {

    private Status happyStatus;

    private EncryptedResponseFromMatchingServiceValidator validator;

    @Before
    public void setUp() throws Exception {
        happyStatus = createStatus(StatusCode.SUCCESS, createSubStatusCode(SamlStatusCode.MATCH));
        validator = new EncryptedResponseFromMatchingServiceValidator();
    }

    @Test
    public void validate_shouldThrowExceptionIfIdIsMissing() throws Exception {
        Response response = aResponse().withId(null).build();

        assertValidationFailure(response, missingId());
    }

    @Test
    public void validate_shouldThrowInvalidSamlExceptionIfIssuerElementIsMissing() throws Exception {
        Response response = aResponse().withIssuer(null).build();

        assertValidationFailure(response, missingIssuer());
    }

    @Test
    public void validate_shouldThrowInvalidSamlExceptionIfIssuerIdIsMissing() throws Exception {
        Issuer issuer = anIssuer().withIssuerId(null).build();
        Response response = aResponse().withIssuer(issuer).build();

        assertValidationFailure(response, emptyIssuer());
    }

    @Test
    public void validateRequest_shouldDoNothingIfResponseIsSigned() throws Exception {
        Response response = aResponse().withStatus(happyStatus).build();

        validator.validate(response);
    }

    @Test
    public void validateRequest_shouldThrowExceptionIfResponseDoesNotContainASignature() throws Exception {
        Response response = aResponse().withoutSignatureElement().build();

        assertValidationFailure(response, missingSignature());
    }

    @Test
    public void validateRequest_shouldThrowExceptionIfResponseIsNotSigned() throws Exception {
        Response response = aResponse().withoutSigning().build();

        assertValidationFailure(response, signatureNotSigned());
    }

    @Test
    public void validateIssuer_shouldThrowExceptionIfFormatAttributeHasInvalidValue() throws Exception {
        String invalidFormat = "goo";
        Response response = aResponse().withIssuer(anIssuer().withFormat(invalidFormat).build()).build();

        assertValidationFailure(response, illegalIssuerFormat(invalidFormat, NameIDType.ENTITY));
    }

    @Test
    public void validateIssuer_shouldDoNothingIfFormatAttributeIsMissing() throws Exception {
        Issuer issuer = anIssuer().withFormat(null).build();
        Response response = aResponse().withIssuer(issuer).withStatus(happyStatus).build();

        validator.validate(response);
    }

    @Test
    public void validateIssuer_shouldDoNothingIfFormatAttributeHasValidValue() throws Exception {
        Issuer issuer = anIssuer().withFormat(NameIDType.ENTITY).build();
        Response response = aResponse().withIssuer(issuer).withStatus(happyStatus).build();

        validator.validate(response);
    }

    @Test
    public void validateResponse_shouldThrowExceptionIfResponseHasUnencryptedAssertion() throws Exception {
        Assertion assertion = anAssertion().buildUnencrypted();
        Response response = aResponse().withStatus(happyStatus).addAssertion(assertion).build();

        assertValidationFailure(response, unencryptedAssertion());
    }

    @Test
    public void validateResponse_shouldThrowExceptionForSuccessResponsesWithNoAssertions() throws Exception {
        Response response = aResponse().withStatus(happyStatus).withNoDefaultAssertion().build();

        assertValidationFailure(response, missingSuccessUnEncryptedAssertions());
    }

    @Test
    public void validateResponse_shouldThrowExceptionForFailureResponsesWithAssertions() throws Exception {
        Status status = createStatus(StatusCode.RESPONDER, createSubStatusCode(SamlStatusCode.NO_MATCH));
        Response response = aResponse().withStatus(status).build();

        assertValidationFailure(response, nonSuccessHasUnEncryptedAssertions());
    }

    @Test
    public void validateResponse_shouldThrowExceptionIfThereIsNoInResponseToAttribute() throws Exception {
        Response response = aResponse().withInResponseTo(null).build();

        assertValidationFailure(response, missingInResponseTo());
    }

    @Test
    public void validate_shouldThrowExceptionIfSuccessResponseDoesNotContainSubStatusOfMatchOrNoMatchOrCreated() throws Exception {
        Status status = createStatus(StatusCode.SUCCESS, createSubStatusCode(SamlStatusCode.MULTI_MATCH));
        Response response = aResponse().withStatus(status).withNoDefaultAssertion().build();

        assertValidationFailure(response, subStatusMustBeOneOf("Success", "Match", "No Match", "Created"));
    }

    @Test
    public void validate_shouldDoNothingIfAResponderStatusContainsASubStatusOfNoMatch() throws Exception {
        Status status = createStatus(StatusCode.RESPONDER, createSubStatusCode(SamlStatusCode.NO_MATCH));
        Response response = aResponse().withStatus(status).withNoDefaultAssertion().build();

        validator.validate(response);
    }

    @Test
    public void validate_shouldDoNothingIfASuccessStatusContainsASubStatusOfMatch() throws Exception {
        Response response = aResponse().withStatus(happyStatus).build();

        validator.validate(response);
    }

    @Test
    public void validate_shouldDoNothingIfASuccessStatusContainsASubStatusOfNoMatch() throws Exception {
        Status status = createStatus(StatusCode.SUCCESS, createSubStatusCode(SamlStatusCode.NO_MATCH));
        Response response = aResponse().withStatus(status).build();

        validator.validate(response);
    }

    @Test
    public void validate_shouldDoNothingIfAResponderStatusContainsASubStatusOfMultiMatch() throws Exception {
        Status status = createStatus(StatusCode.RESPONDER, createSubStatusCode(SamlStatusCode.MULTI_MATCH));
        Response response = aResponse().withStatus(status).withNoDefaultAssertion().build();

        validator.validate(response);
    }

    @Test
    public void validate_shouldThrowExceptionIfAResponderStatusContainsAnInvalidSubStatus() throws Exception {
        Status status = createStatus(StatusCode.RESPONDER, createSubStatusCode("invalid, yo."));
        Response response = aResponse().withStatus(status).withNoDefaultAssertion().build();

        assertValidationFailure(response, subStatusMustBeOneOf("Responder", "No Match", "Multi Match", "Create Failure"));
    }

    @Test
    public void validate_shouldThrowExceptionIfSubStatusIsNull() throws Exception {
        Response response = aResponse().withStatus(createStatus(StatusCode.SUCCESS)).build();

        assertValidationFailure(response, SamlTransformationErrorFactory.missingSubStatus());
    }

    @Test
    public void validate_shouldThrowIfResponseContainsTooManyAssertions() throws Exception {
        Response response = aResponse().withStatus(happyStatus)
            .addEncryptedAssertion(anAssertion().build())
            .addEncryptedAssertion(anAssertion().build())
            .build();

        assertValidationFailure(response, unexpectedNumberOfAssertions(1, 2));
    }

    private void assertValidationFailure(Response response, SamlValidationSpecificationFailure failure) {
        validateFail(() -> validator.validate(response), failure);
    }
}
