package uk.gov.ida.saml.hub.validators.response.idp.components;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.xmlsec.signature.support.SignatureException;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToIdpIdaStatusMappingsFactory;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.*;
import static uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper.validateFail;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;
import static uk.gov.ida.saml.core.test.builders.StatusCodeBuilder.aStatusCode;
import static uk.gov.ida.saml.hub.validators.response.helpers.ResponseValidatorTestHelper.createStatus;
import static uk.gov.ida.saml.hub.validators.response.helpers.ResponseValidatorTestHelper.createSubStatusCode;
import static uk.gov.ida.saml.hub.validators.response.helpers.ResponseValidatorTestHelper.getResponseBuilderWithTwoAssertions;

@RunWith(OpenSAMLMockitoRunner.class)
public class EncryptedResponseFromIdpValidatorTest {

    private EncryptedResponseFromIdpValidator validator;

    @Before
    public void setup() {
        validator = new EncryptedResponseFromIdpValidator(new SamlStatusToIdpIdaStatusMappingsFactory());
    }

    @Test
    public void validate_shouldThrowExceptionIfIdIsMissing() throws Exception {
        Response response = aResponse().withId(null).build();

        assertValidationFailure(response, missingId());
    }

    @Test
    public void validate_shouldThrowExceptionIfIssuerElementIsMissing() throws Exception {
        Response response = aResponse().withIssuer(null).build();

        assertValidationFailure(response, missingIssuer());
    }

    @Test
    public void validate_shouldThrowExceptionIfIssuerIdIsMissing() throws Exception {
        Issuer issuer = anIssuer().withIssuerId(null).build();
        Response response = aResponse().withIssuer(issuer).build();

        assertValidationFailure(response, emptyIssuer());
    }

    @Test
    public void validate_shouldThrowExceptionIfIssueInstantIsMissing() throws Exception {
        String responseId = "test";
        Response response = aResponse().withId(responseId).withIssueInstant(null).build();

        assertValidationFailure(response, missingIssueInstant(responseId));
    }

    @Test
    public void validateRequest_shouldNotErrorIfRequestIsSigned() throws Exception {
        Response response = getResponseBuilderWithTwoAssertions().build();

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
        Issuer issuer = anIssuer().withFormat(invalidFormat).build();
        Response response = aResponse().withIssuer(issuer).build();

        assertValidationFailure(response, illegalIssuerFormat(
            invalidFormat,
            NameIDType.ENTITY
        ));
    }

    @Test
    public void validateIssuer_shouldNotErrorIfFormatAttributeIsMissing() throws Exception {
        Issuer issuer = anIssuer().withFormat(null).build();
        Response response = getResponseBuilderWithTwoAssertions().withIssuer(issuer).build();

        validator.validate(response);
    }

    @Test
    public void validateIssuer_shouldNotErrorIfFormatAttributeHasValidValue() throws Exception {
        Issuer issuer = anIssuer().withFormat(NameIDType.ENTITY).build();
        Response response = getResponseBuilderWithTwoAssertions().withIssuer(issuer).build();

        validator.validate(response);
    }

    @Test
    public void validateResponse_shouldThrowExceptionIfResponseHasUnencryptedAssertion() throws Exception {
        Response response = aResponse().addAssertion(anAssertion().buildUnencrypted()).build();

        assertValidationFailure(response, SamlTransformationErrorFactory.unencryptedAssertion());
    }

    @Test
    public void validateResponse_shouldThrowExceptionForSuccessResponsesWithNoAssertions() throws Exception {
        Response response = aResponse().withNoDefaultAssertion().build();

        assertValidationFailure(response, SamlTransformationErrorFactory.missingSuccessUnEncryptedAssertions());
    }

    @Test
    public void validateResponse_shouldThrowExceptionForFailureResponsesWithAssertions() throws Exception {
        Status status = createStatus(StatusCode.RESPONDER, createSubStatusCode(StatusCode.AUTHN_FAILED));
        Response response = aResponse().withStatus(status).build();

        assertValidationFailure(response, nonSuccessHasUnEncryptedAssertions());
    }

    @Test
    public void validateResponse_shouldThrowExceptionIfThereIsNoInResponseToAttribute() throws Exception {
        Response response = aResponse().withInResponseTo(null).build();

        assertValidationFailure(response, SamlTransformationErrorFactory.missingInResponseTo());
    }

    @Test
    public void validateStatus_shouldNotErrorIfStatusIsResponderWithSubStatusAuthnFailed() throws Exception {
        Status status = createStatus(StatusCode.RESPONDER, createSubStatusCode(StatusCode.AUTHN_FAILED));
        Response response = aResponse().withStatus(status).withNoDefaultAssertion().build();

        validator.validate(response);
    }

    @Test
    public void validateStatus_shouldNotErrorIfStatusIsResponderWithSubStatusNoAuthnContext() throws Exception {
        Status status = createStatus(StatusCode.RESPONDER, createSubStatusCode(StatusCode.NO_AUTHN_CONTEXT));
        Response response = aResponse().withStatus(status).withNoDefaultAssertion().build();

        validator.validate(response);
    }

    @Test
    public void validateStatus_shouldNotErrorIfStatusIsRequesterWithNoSubStatus() throws Exception {
        Status status = createStatus(StatusCode.REQUESTER);
        Response response = aResponse().withStatus(status).withNoDefaultAssertion().build();

        validator.validate(response);
    }

    @Test
    public void validateStatus_shouldNotErrorIfStatusIsSuccessWithNoSubStatus() throws Exception {
        Status status = createStatus(StatusCode.SUCCESS);
        Response response = getResponseBuilderWithTwoAssertions().withStatus(status).build();

        validator.validate(response);
    }

    @Test
    public void validateStatus_shouldThrowExceptionIfTheStatusIsInvalid() throws Exception {
        String anInvalidStatusCode = "This is wrong";
        Status status = createStatus(anInvalidStatusCode);
        Response response = aResponse().withStatus(status).build();

        assertValidationFailure(response, invalidStatusCode(anInvalidStatusCode));
    }

    @Test
    public void validateStatus_shouldThrowExceptionIfSuccessHasASubStatus() throws Exception {
        StatusCode subStatusCode = createSubStatusCode();
        Status status = createStatus(StatusCode.SUCCESS, subStatusCode);
        Response response = aResponse().withStatus(status).build();

        assertValidationFailure(response, invalidSubStatusCode(subStatusCode.getValue(), StatusCode.SUCCESS));
    }

    @Test
    public void validateStatus_shouldThrowExceptionIfRequesterHasASubStatus() throws Exception {
        StatusCode subStatusCode = createSubStatusCode();
        Status status = createStatus(StatusCode.REQUESTER, subStatusCode);
        Response response = aResponse().withStatus(status).build();

        assertValidationFailure(response, invalidSubStatusCode(subStatusCode.getValue(), StatusCode.REQUESTER));
    }

    @Test
    public void validateStatus_shouldThrowExceptionIfAuthnFailedHasASubSubStatus() throws Exception {
        StatusCode subStatusCode = aStatusCode()
            .withValue(StatusCode.AUTHN_FAILED)
            .withSubStatusCode(createSubStatusCode())
            .build();

        Status status = createStatus(StatusCode.RESPONDER, subStatusCode);
        Response response = aResponse().withStatus(status).build();

        assertValidationFailure(response, nestedSubStatusCodesBreached(1));
    }

    @Test
    public void validate_shouldThrowIfResponseContainsTooManyAssertions() throws Exception {
        EncryptedAssertion assertion = anAssertion().build();
        Response response = getResponseBuilderWithTwoAssertions().addEncryptedAssertion(assertion).build();

        assertValidationFailure(response, unexpectedNumberOfAssertions(2, 3));
    }

    @Test
    public void validate_shouldThrowIfResponseContainsTooFewAssertions() throws Exception {
        EncryptedAssertion assertion = anAssertion().build();
        Response response = aResponse().addEncryptedAssertion(assertion).build();

        assertValidationFailure(response, unexpectedNumberOfAssertions(2, 1));
    }

    private void assertValidationFailure(Response response, SamlValidationSpecificationFailure failure) {
        validateFail(() -> validator.validate(response), failure);
    }
}
