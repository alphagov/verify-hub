package uk.gov.ida.saml.hub.validators.response.matchingservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.validation.assertion.IdentityProviderAssertionValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.AuthnStatementBuilder.anAuthnStatement;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;
import static uk.gov.ida.saml.core.test.builders.StatusBuilder.aStatus;
import static uk.gov.ida.saml.core.test.builders.StatusCodeBuilder.aStatusCode;

@ExtendWith(OpenSAMLExtension.class)
@ExtendWith(MockitoExtension.class)
public class ResponseAssertionsFromMatchingServiceValidatorTest {

    @Mock
    private IdentityProviderAssertionValidator assertionValidator;

    private ResponseAssertionsFromMatchingServiceValidator validator;

    @BeforeEach
    public void setUp() throws Exception {
        validator = new ResponseAssertionsFromMatchingServiceValidator(assertionValidator, TestEntityIds.HUB_ENTITY_ID);
    }

    @Test
    public void validate_shouldThrowExceptionIfAssertionDoesNotContainAnAuthnContext() throws Exception {
        String requestId = "some-request-id";
        final Response response = aResponse()
                .withInResponseTo(requestId)
                .build();
        final Assertion assertion = anAssertion().addAuthnStatement(anAuthnStatement().withAuthnContext(null).build()).buildUnencrypted();

        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(new ValidatedResponse(response), new ValidatedAssertions(singletonList(assertion)))),
                SamlTransformationErrorFactory.authnContextMissingError()
        );
    }

    @Test
    public void validate_shouldThrowExceptionIfAssertionDoesNotContainAnAuthnStatement() throws Exception {
        String requestId = "some-request-id";
        final Response response = aResponse()
                .withInResponseTo(requestId)
                .build();

        validateException(
                assertThrows(SamlTransformationErrorException.class, () -> validator.validate(new ValidatedResponse(response), new ValidatedAssertions(singletonList(anAssertion().buildUnencrypted())))),
                SamlTransformationErrorFactory.missingAuthnStatement()
        );
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

    private void validateException(SamlTransformationErrorException e, SamlValidationSpecificationFailure samlValidationSpecificationFailure) {
        assertThat(e.getMessage()).isEqualTo(samlValidationSpecificationFailure.getErrorMessage());
        assertThat(e.getLogLevel()).isEqualTo(samlValidationSpecificationFailure.getLogLevel());
    }
}
