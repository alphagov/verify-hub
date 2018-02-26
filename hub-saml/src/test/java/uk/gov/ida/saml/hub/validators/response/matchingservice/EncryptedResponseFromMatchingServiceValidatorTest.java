package uk.gov.ida.saml.hub.validators.response.matchingservice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.domain.SamlStatusCode;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;

import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;
import static uk.gov.ida.saml.core.test.builders.StatusBuilder.aStatus;
import static uk.gov.ida.saml.core.test.builders.StatusCodeBuilder.aStatusCode;

@RunWith(OpenSAMLMockitoRunner.class)
public class EncryptedResponseFromMatchingServiceValidatorTest {

    private Status happyStatus;

    private EncryptedResponseFromMatchingServiceValidator validator;

    @Before
    public void setUp() throws Exception {
        happyStatus = aStatus().withStatusCode(aStatusCode().withValue(StatusCode.SUCCESS).withSubStatusCode(aStatusCode().withValue(SamlStatusCode.MATCH).build()).build()).build();
        validator = new EncryptedResponseFromMatchingServiceValidator();
    }

    @Test
    public void validate_shouldThrowExceptionIfIdIsMissing() throws Exception {
        Response response = aResponse().withId(null).build();

        assertValidationFailureSamlExceptionMessage(SamlTransformationErrorFactory.missingId(), response);
    }

    @Test
    public void validate_shouldThrowInvalidSamlExceptionIfIssuerElementIsMissing() throws Exception {
        Response response = aResponse().withIssuer(null).build();
        assertValidationFailureSamlExceptionMessage(SamlTransformationErrorFactory.missingIssuer(), response);
    }

    @Test
    public void validate_shouldThrowInvalidSamlExceptionIfIssuerIdIsMissing() throws Exception {
        Response response = aResponse().withIssuer(anIssuer().withIssuerId(null).build()).build();
        assertValidationFailureSamlExceptionMessage(SamlTransformationErrorFactory.emptyIssuer(), response);
    }

    @Test
    public void validateRequest_shouldDoNothingIfResponseIsSigned() throws Exception {
        Response response = buildResponseWithAssertionFromMatchingServiceWithStatusAndSubStatus(StatusCode.SUCCESS, SamlStatusCode.MATCH);

        validator.validate(response);
    }

    @Test
    public void validateRequest_shouldThrowExceptionIfResponseDoesNotContainASignature() throws Exception {
        Response response = aResponse().withoutSignatureElement().build();

        assertValidationFailureSamlExceptionMessage(SamlTransformationErrorFactory.missingSignature(), response);
    }

    @Test
    public void validateRequest_shouldThrowExceptionIfResponseIsNotSigned() throws Exception {
        Response response = aResponse().withoutSigning().build();

        assertValidationFailureSamlExceptionMessage(SamlTransformationErrorFactory.signatureNotSigned(), response);
    }

    @Test
    public void validateIssuer_shouldThrowExceptionIfFormatAttributeHasInvalidValue() throws Exception {
        String invalidFormat = "goo";
        Response response = aResponse().withIssuer(anIssuer().withFormat(invalidFormat).build()).build();

        assertValidationFailureSamlExceptionMessage(SamlTransformationErrorFactory.illegalIssuerFormat(invalidFormat, NameIDType.ENTITY), response);
    }

    @Test
    public void validateIssuer_shouldDoNothingIfFormatAttributeIsMissing() throws Exception {
        Response response = aResponse().withIssuer(anIssuer().withFormat(null).build()).withStatus(happyStatus).build();
        validator.validate(response);
    }

    @Test
    public void validateIssuer_shouldDoNothingIfFormatAttributeHasValidValue() throws Exception {
        Response response = aResponse().withIssuer(anIssuer().withFormat(NameIDType.ENTITY).build()).withStatus(happyStatus).build();

        validator.validate(response);
    }

    @Test
    public void validateResponse_shouldThrowExceptionIfResponseHasUnencryptedAssertion() throws Exception {
        Response response = aResponse().withStatus(happyStatus).addAssertion(anAssertion().buildUnencrypted()).build();

        assertValidationFailureSamlExceptionMessage(SamlTransformationErrorFactory.unencryptedAssertion(), response);
    }

    @Test
    public void validateResponse_shouldThrowExceptionForSuccessResponsesWithNoAssertions() throws Exception {
        Response response = aResponse().withStatus(happyStatus).withNoDefaultAssertion().build();

        assertValidationFailureSamlExceptionMessage(SamlTransformationErrorFactory.missingSuccessUnEncryptedAssertions(), response);
    }

    @Test
    public void validateResponse_shouldThrowExceptionForFailureResponsesWithAssertions() throws Exception {
        Response response = buildResponseWithAssertionFromMatchingServiceWithStatusAndSubStatus(StatusCode.RESPONDER, SamlStatusCode.NO_MATCH);

        assertValidationFailureSamlExceptionMessage(SamlTransformationErrorFactory.nonSuccessHasUnEncryptedAssertions(), response);

    }

    @Test
    public void validateResponse_shouldThrowExceptionIfThereIsNoInResponseToAttribute() throws Exception {
        Response response = aResponse().withInResponseTo(null).build();

        assertValidationFailureSamlExceptionMessage(SamlTransformationErrorFactory.missingInResponseTo(), response);

    }

    @Test
    public void validate_shouldThrowExceptionIfSuccessResponseDoesNotContainSubStatusOfMatchOrNoMatchOrCreated() throws Exception {
        Response response = buildResponseFromMatchingServiceWithStatusAndSubStatus(StatusCode.SUCCESS, SamlStatusCode.MULTI_MATCH);
        assertValidationFailureSamlExceptionMessage(SamlTransformationErrorFactory.subStatusMustBeOneOf("Success", "Match", "No Match", "Created"), response);
    }

    @Test
    public void validate_shouldDoNothingIfAResponderStatusContainsASubStatusOfNoMatch() throws Exception {
        Response response = buildResponseFromMatchingServiceWithStatusAndSubStatus(StatusCode.RESPONDER, SamlStatusCode.NO_MATCH);
        validator.validate(response);
    }

    @Test
    public void validate_shouldDoNothingIfASuccessStatusContainsASubStatusOfMatch() throws Exception {
        Response response = aResponse().withStatus(
                aStatus().withStatusCode(
                        aStatusCode().withValue(StatusCode.SUCCESS).withSubStatusCode(
                                aStatusCode().withValue(SamlStatusCode.MATCH).build()
                        ).build()
                ).build()
        ).build();
        validator.validate(response);
    }

    @Test
    public void validate_shouldDoNothingIfASuccessStatusContainsASubStatusOfNoMatch() throws Exception {
        Response response =
                aResponse().withStatus(
                        aStatus().withStatusCode(
                                aStatusCode().withValue(StatusCode.SUCCESS).withSubStatusCode(
                                        aStatusCode().withValue(SamlStatusCode.NO_MATCH).build()
                                ).build()
                        ).build()
                ).build();
        validator.validate(response);
    }

    @Test
    public void validate_shouldDoNothingIfAResponderStatusContainsASubStatusOfMultiMatch() throws Exception {
        Response response = buildResponseFromMatchingServiceWithStatusAndSubStatus(StatusCode.RESPONDER, SamlStatusCode.MULTI_MATCH);
        validator.validate(response);
    }

    @Test
    public void validate_shouldThrowExceptionIfAResponderStatusContainsAnInvalidSubStatus() throws Exception {
        Response response = buildResponseFromMatchingServiceWithStatusAndSubStatus(StatusCode.RESPONDER, "invalid, yo.");
        assertValidationFailureSamlExceptionMessage(SamlTransformationErrorFactory.subStatusMustBeOneOf("Responder", "No Match", "Multi Match", "Create Failure"), response);
    }

    @Test
    public void validate_shouldThrowExceptionIfSubStatusIsNull() throws Exception {
        Response response =
                aResponse().withStatus(
                        aStatus().withStatusCode(
                                aStatusCode().withValue(StatusCode.SUCCESS)
                                        .build()
                        ).build()
                ).build();
        assertValidationFailureSamlExceptionMessage(SamlTransformationErrorFactory.missingSubStatus(), response);
    }

    @Test
    public void validate_shouldThrowIfResponseContainsTooManyAssertions() throws Exception {
        Response response = aResponse().withStatus(aStatus().withStatusCode(
                aStatusCode().withValue(StatusCode.SUCCESS).withSubStatusCode(
                        aStatusCode().withValue(SamlStatusCode.MATCH).build()
                ).build()).build())
                .addEncryptedAssertion(anAssertion().build())
                .addEncryptedAssertion(anAssertion().build())
                .build();

        assertValidationFailureSamlExceptionMessage(
                SamlTransformationErrorFactory.unexpectedNumberOfAssertions(1, 2),
                response
        );
    }

    private Response buildResponseFromMatchingServiceWithStatusAndSubStatus(String status, String subStatus) throws Exception {
        return
                aResponse().withNoDefaultAssertion().withStatus(
                        aStatus().withStatusCode(
                                aStatusCode().withValue(status).withSubStatusCode(
                                        aStatusCode().withValue(subStatus).build()
                                ).build()
                        ).build()
                ).build();
    }

    private Response buildResponseWithAssertionFromMatchingServiceWithStatusAndSubStatus(String status, String subStatus) throws Exception {
        return aResponse().withStatus(
                aStatus().withStatusCode(
                        aStatusCode().withValue(status).withSubStatusCode(
                                aStatusCode().withValue(subStatus).build()
                        ).build()
                ).build()
        ).build();
    }

    private void assertValidationFailureSamlExceptionMessage(SamlValidationSpecificationFailure failure, final Response response) {
        SamlTransformationErrorManagerTestHelper.validateFail(
                new SamlTransformationErrorManagerTestHelper.Action() {
                    @Override
                    public void execute() {
                        validator.validate(response);
                    }
                },
                failure
        );
    }
}
