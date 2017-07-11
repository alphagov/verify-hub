package uk.gov.ida.saml.hub.validators.response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.test.builders.StatusBuilder;
import uk.gov.ida.saml.core.validators.assertion.AssertionValidator;
import uk.gov.ida.saml.core.test.builders.ResponseBuilder;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.AuthnStatementBuilder.anAuthnStatement;
import static uk.gov.ida.saml.core.test.builders.MatchingDatasetAttributeStatementBuilder_1_1.aMatchingDatasetAttributeStatement_1_1;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;

@RunWith(OpenSAMLMockitoRunner.class)
public class ResponseAssertionsValidatorTest {

    private AssertionValidator assertionValidator = mock(AssertionValidator.class);
    private ResponseAssertionsValidator validator = new ResponseAssertionsValidator(assertionValidator, TestEntityIds.HUB_ENTITY_ID);

    @Test
    public void validate_shouldDoNothingIfAllExpectedStatementElementsArePresent() throws Exception {
        Response response = aValidIdpResponse().build();
        validator.validate(new ValidatedResponse(response), new ValidatedAssertions(response.getAssertions()));
    }

    @Test
    public void validate_shouldValidateAssertionUsingAssertionValidator() throws Exception {
        Response response = aValidIdpResponse().build();
        final List<Assertion> assertions = response.getAssertions();

        validator.validate(new ValidatedResponse(response), new ValidatedAssertions(assertions));

        for (Assertion assertion : assertions) {
            verify(assertionValidator).validate(eq(assertion), anyString(), anyString());
        }
    }

    @Test
    public void validate_shouldValidateAssertionIssuerUsingAssertionIssuerValidator() throws Exception {
        Response response = aValidIdpResponse().build();
        final List<Assertion> assertions = response.getAssertions();

        validator.validate(new ValidatedResponse(response), new ValidatedAssertions(assertions));

        for (Assertion assertion : assertions) {
            verify(assertionValidator).validate(eq(assertion), anyString(), anyString());
        }
    }

    private ResponseBuilder aValidIdpResponse() {
        return aResponse()
                .withStatus(StatusBuilder.aStatus().build())
                .addAssertion(anAssertion().addAttributeStatement(aMatchingDatasetAttributeStatement_1_1().build()).buildUnencrypted())
                .addAssertion(anAssertion().addAuthnStatement(anAuthnStatement().build()).buildUnencrypted());
    }
}
