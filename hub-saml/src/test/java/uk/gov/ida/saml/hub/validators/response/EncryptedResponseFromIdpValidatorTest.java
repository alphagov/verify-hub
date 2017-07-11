package uk.gov.ida.saml.hub.validators.response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper;
import uk.gov.ida.saml.core.test.builders.ResponseBuilder;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.validation.errors.GenericHubProfileValidationSpecification;
import uk.gov.ida.saml.core.validation.errors.ResponseProcessingValidationSpecification;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToIdpIdaStatusMappingsFactory;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.*;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;
import static uk.gov.ida.saml.core.test.builders.StatusBuilder.aStatus;
import static uk.gov.ida.saml.core.test.builders.StatusCodeBuilder.aStatusCode;

@RunWith(OpenSAMLMockitoRunner.class)
public class EncryptedResponseFromIdpValidatorTest {

    private ResponseFromIdpValidator validator;

    @Before
    public void setup() {
        validator = new EncryptedResponseFromIdpValidator(
                new SamlStatusToIdpIdaStatusMappingsFactory()
        );
    }

    @Test
    public void validate_shouldThrowExceptionIfIdIsMissing() throws Exception {
        Response response = aResponse().withId(null).build();

        assertValidationFailureSamlExceptionMessage(
                GenericHubProfileValidationSpecification.class,
                SamlTransformationErrorFactory.missingId(),
                response
        );
    }

    @Test
    public void validate_shouldThrowExceptionIfIssuerElementIsMissing() throws Exception {
        Response response = aResponse().withIssuer(null).build();

        assertValidationFailureSamlExceptionMessage(
                GenericHubProfileValidationSpecification.class,
                SamlTransformationErrorFactory.missingIssuer(),
                response
        );
    }

    @Test
    public void validate_shouldThrowExceptionIfIssuerIdIsMissing() throws Exception {
        Response response = aResponse().withIssuer(anIssuer().withIssuerId(null).build()).build();

        assertValidationFailureSamlExceptionMessage(
                GenericHubProfileValidationSpecification.class,
                SamlTransformationErrorFactory.emptyIssuer(),
                response
        );
    }

    @Test
    public void validateRequest_shouldDoNothingIfRequestIsSigned() throws Exception {
        Response response = getResponseBuilderWithTwoAssertions().build();
        validator.validate(response);
    }

    @Test
    public void validateRequest_shouldThrowExceptionIfResponseDoesNotContainASignature() throws Exception {
        Response response = aResponse().withoutSignatureElement().build();

        assertValidationFailureSamlExceptionMessage(
                GenericHubProfileValidationSpecification.class,
                SamlTransformationErrorFactory.missingSignature(),
                response
        );
    }

    @Test
    public void validateRequest_shouldThrowExceptionIfResponseIsNotSigned() throws Exception {
        Response response = aResponse().withoutSigning().build();

        assertValidationFailureSamlExceptionMessage(
                GenericHubProfileValidationSpecification.class,
                SamlTransformationErrorFactory.signatureNotSigned(),
                response
        );
    }

    @Test
    public void validateIssuer_shouldThrowExceptionIfFormatAttributeHasInvalidValue() throws Exception {
        String invalidFormat = "goo";
        Response response = aResponse().withIssuer(anIssuer().withFormat(invalidFormat).build()).build();

        assertValidationFailureSamlExceptionMessage(
                GenericHubProfileValidationSpecification.class, SamlTransformationErrorFactory.illegalIssuerFormat(
                        invalidFormat,
                        NameIDType.ENTITY
                ), response
        );
    }

    @Test
    public void validateIssuer_shouldDoNothingIfFormatAttributeIsMissing() throws Exception {
        Response response = getResponseBuilderWithTwoAssertions()
                .withIssuer(anIssuer().withFormat(null).build()).build();

        validator.validate(response);
    }

    @Test
    public void validateIssuer_shouldDoNothingIfFormatAttributeHasValidValue() throws Exception {
        Response response = getResponseBuilderWithTwoAssertions()
                .withIssuer(anIssuer().withFormat(NameIDType.ENTITY).build()).build();

        validator.validate(response);
    }

    @Test
    public void validateResponse_shouldThrowExceptionIfResponseHasUnencryptedAssertion() throws Exception {
        Response response = aResponse().addAssertion(anAssertion().buildUnencrypted()).build();

        assertValidationFailureSamlExceptionMessage(
                ResponseProcessingValidationSpecification.class,
                SamlTransformationErrorFactory.unencryptedAssertion(),
                response
        );
    }

    @Test
    public void validateResponse_shouldThrowExceptionForSuccessResponsesWithNoAssertions() throws Exception {
        Response response = aResponse().withNoDefaultAssertion().build();

        assertValidationFailureSamlExceptionMessage(
                ResponseProcessingValidationSpecification.class,
                SamlTransformationErrorFactory.missingSuccessUnEncryptedAssertions(),
                response
        );
    }

    @Test
    public void validateResponse_shouldThrowExceptionForFailureResponsesWithAssertions() throws Exception {
        Response response = aResponse()
                .withStatus(
                        aStatus()
                                .withStatusCode(
                                        aStatusCode()
                                                .withValue(StatusCode.RESPONDER)
                                                .withSubStatusCode(
                                                        aStatusCode().withValue(StatusCode.AUTHN_FAILED).build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        assertValidationFailureSamlExceptionMessage(
                ResponseProcessingValidationSpecification.class,
                nonSuccessHasUnEncryptedAssertions(),
                response
        );
    }

    @Test
    public void validateResponse_shouldThrowExceptionIfThereIsNoInResponseToAttribute() throws Exception {
        Response response = aResponse().withInResponseTo(null).build();

        assertValidationFailureSamlExceptionMessage(
                GenericHubProfileValidationSpecification.class,
                SamlTransformationErrorFactory.missingInResponseTo(),
                response
        );
    }

    @Test
    public void validateStatus_shouldDoNothingIfStatusIsResponderWithSubStatusAuthnFailed() throws Exception {
        Response response = aResponse().withStatus(
                aStatus().withStatusCode(
                        aStatusCode()
                                .withValue(StatusCode.RESPONDER)
                                .withSubStatusCode(aStatusCode().withValue(StatusCode.AUTHN_FAILED).build()).build())
                        .build())
                .withNoDefaultAssertion().build();

        validator.validate(response);
    }

    @Test
    public void validateStatus_shouldDoNothingIfStatusIsResponderWithSubStatusNoAuthnContext() throws Exception {
        Response response = aResponse().withStatus(
                aStatus().withStatusCode(
                        aStatusCode()
                                .withValue(StatusCode.RESPONDER)
                                .withSubStatusCode(aStatusCode().withValue(StatusCode.NO_AUTHN_CONTEXT).build())
                                .build())
                        .build())
                .withNoDefaultAssertion()
                .build();

        validator.validate(response);
    }

    @Test
    public void validateStatus_shouldDoNothingIfStatusIsRequesterWithNoSubStatus() throws Exception {
        Response response = aResponse().withStatus(
                aStatus()
                        .withStatusCode(aStatusCode().withValue(StatusCode.REQUESTER).build())
                        .build())
                .withNoDefaultAssertion()
                .build();

        validator.validate(response);
    }

    @Test
    public void validateStatus_shouldDoNothingIfStatusIsSuccessWithNoSubStatus() throws Exception {
        Response response = getResponseBuilderWithTwoAssertions().withStatus(
                aStatus()
                        .withStatusCode(
                                aStatusCode()
                                        .withValue(StatusCode.SUCCESS)
                                        .build()
                        )
                        .build()
        ).build();

        validator.validate(response);
    }

    @Test
    public void validateStatus_shouldThrowExceptionIfTheStatusIsInvalid() throws Exception {
        final String anInvalidStatusCode = "This is wrong";
        Response response = aResponse().withStatus(
                aStatus()
                        .withStatusCode(
                                aStatusCode()
                                        .withValue(anInvalidStatusCode)
                                        .build()
                        )
                        .build()
        ).build();

        assertValidationFailureSamlExceptionMessage(
                GenericHubProfileValidationSpecification.class,
                invalidStatusCode(anInvalidStatusCode),
                response
        );
    }

    @Test
    public void validateStatus_shouldThrowExceptionIfSuccessHasASubStatus() throws Exception {
        final StatusCode subStatusCode = aStatusCode().build();
        Response response = aResponse().withStatus(
                aStatus().withStatusCode(
                        aStatusCode()
                                .withValue(StatusCode.SUCCESS)
                                .withSubStatusCode(subStatusCode)
                                .build()
                ).build()
        ).build();

        assertValidationFailureSamlExceptionMessage(
                GenericHubProfileValidationSpecification.class,
                invalidSubStatusCode(subStatusCode.getValue(), StatusCode.SUCCESS),
                response
        );
    }

    @Test
    public void validateStatus_shouldThrowExceptionIfRequesterHasASubStatus() throws Exception {
        final StatusCode subStatusCode = aStatusCode().build();
        final StatusCode statusCode = aStatusCode()
                .withValue(StatusCode.REQUESTER)
                .withSubStatusCode(subStatusCode)
                .build();
        Status status = aStatus().withStatusCode(statusCode).build();
        Response response = aResponse().withStatus(status).build();

        assertValidationFailureSamlExceptionMessage(
                GenericHubProfileValidationSpecification.class,
                invalidSubStatusCode(subStatusCode.getValue(), StatusCode.REQUESTER),
                response
        );
    }

    @Test
    public void validateStatus_shouldThrowExceptionIfAuthnFailedHasASubSubStatus() throws Exception {
        final StatusCode statusCode = aStatusCode()
                .withValue(StatusCode.RESPONDER)
                .withSubStatusCode(
                        aStatusCode().withValue(StatusCode.AUTHN_FAILED)
                                .withSubStatusCode(aStatusCode().build())
                                .build()
                )
                .build();
        Status status = aStatus().withStatusCode(statusCode).build();
        Response response = aResponse().withStatus(status).build();

        assertValidationFailureSamlExceptionMessage(
                GenericHubProfileValidationSpecification.class,
                nestedSubStatusCodesBreached(1),
                response
        );
    }

    @Test
    public void validate_shouldThrowIfResponseContainsTooManyAssertions() throws Exception {
        Response response = getResponseBuilderWithTwoAssertions()
                .addEncryptedAssertion(anAssertion().build())
                .build();

        assertValidationFailureSamlExceptionMessage(
                GenericHubProfileValidationSpecification.class,
                SamlTransformationErrorFactory.unexpectedNumberOfAssertions(2, 3),
                response
        );
    }


    @Test
    public void validate_shouldThrowIfResponseContainsTooFewAssertions() throws Exception {
        Response response = aResponse().addEncryptedAssertion(anAssertion().build()).build();

        assertValidationFailureSamlExceptionMessage(
                GenericHubProfileValidationSpecification.class,
                SamlTransformationErrorFactory.unexpectedNumberOfAssertions(2, 1),
                response
        );
    }

    private void assertValidationFailureSamlExceptionMessage(Class<? extends SamlValidationSpecificationFailure> errorClass, SamlValidationSpecificationFailure failure, final Response response) {
        SamlTransformationErrorManagerTestHelper.validateFail(
                () -> validator.validate(response),
                failure
        );
    }

    private ResponseBuilder getResponseBuilderWithTwoAssertions() {
        return aResponse().addEncryptedAssertion(anAssertion().build()).addEncryptedAssertion(anAssertion().build());
    }
}
