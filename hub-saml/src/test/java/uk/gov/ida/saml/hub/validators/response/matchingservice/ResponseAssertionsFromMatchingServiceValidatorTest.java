package uk.gov.ida.saml.hub.validators.response.matchingservice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.validators.assertion.IdentityProviderAssertionValidator;
import uk.gov.ida.saml.core.validators.subjectconfirmation.BasicAssertionSubjectConfirmationValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.AuthnStatementBuilder.anAuthnStatement;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;
import static uk.gov.ida.saml.core.test.builders.StatusBuilder.aStatus;
import static uk.gov.ida.saml.core.test.builders.StatusCodeBuilder.aStatusCode;

@RunWith(OpenSAMLMockitoRunner.class)
public class ResponseAssertionsFromMatchingServiceValidatorTest {

    @Mock
    private IdentityProviderAssertionValidator assertionValidator;

    private ResponseAssertionsFromMatchingServiceValidator validator;

    @Before
    public void setUp() throws Exception {
        validator = new ResponseAssertionsFromMatchingServiceValidator(assertionValidator, TestEntityIds.HUB_ENTITY_ID);
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void validate_shouldThrowExceptionIfAssertionDoesNotContainAnAuthnContext() throws Exception {
        String requestId = "some-request-id";
        final Response response = aResponse()
                .withInResponseTo(requestId)
                .build();
        final Assertion assertion = anAssertion().addAuthnStatement(anAuthnStatement().withAuthnContext(null).build()).buildUnencrypted();

        validateThrows(response, assertion, SamlTransformationErrorFactory.authnContextMissingError());
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void validate_shouldThrowExceptionIfAssertionDoesNotContainAnAuthnStatement() throws Exception {
        String requestId = "some-request-id";
        final Response response = aResponse()
                .withInResponseTo(requestId)
                .build();
        validateThrows(response, anAssertion().buildUnencrypted(), SamlTransformationErrorFactory.missingAuthnStatement());
    }

    @Test
    public void validate_shouldNotThrowExceptionIfResponseIsANoMatch() throws Exception {
        String requestId = "some-request-id";
        final Response response = aResponse()
                .withStatus(aStatus().withStatusCode(aStatusCode().withValue(StatusCode.RESPONDER).build()).build())
                .withInResponseTo(requestId)
                .build();

        validator.validate(new ValidatedResponse(response), new ValidatedAssertions(singletonList(anAssertion().buildUnencrypted())));
    }

    private void validateThrows(Response response, Assertion assertion, SamlValidationSpecificationFailure samlValidationSpecificationFailure) {
        try {
            validator.validate(new ValidatedResponse(response), new ValidatedAssertions(singletonList(assertion)));
        } catch (SamlTransformationErrorException e) {
            assertThat(e.getMessage()).isEqualTo(samlValidationSpecificationFailure.getErrorMessage());
            assertThat(e.getLogLevel()).isEqualTo(samlValidationSpecificationFailure.getLogLevel());
            throw e;
        }
    }
}
