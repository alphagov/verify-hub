package uk.gov.ida.saml.core.validators.assertion;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper;
import uk.gov.ida.saml.core.test.builders.AssertionBuilder;
import uk.gov.ida.saml.core.test.builders.AuthnContextBuilder;
import uk.gov.ida.saml.core.test.builders.AuthnContextClassRefBuilder;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.validation.errors.GenericHubProfileValidationSpecification;
import uk.gov.ida.saml.core.validation.errors.ResponseProcessingValidationSpecification;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;import uk.gov.ida.saml.core.test.builders.AuthnStatementBuilder;

@RunWith(OpenSAMLMockitoRunner.class)
public class AuthnStatementAssertionValidatorTest {

    @Mock
    private DuplicateAssertionValidator duplicateAssertionValidator;

    private AuthnStatementAssertionValidator validator;

    @Before
    public void setup() {
        validator = new AuthnStatementAssertionValidator(duplicateAssertionValidator);
    }

    @Test
    public void validate_shouldThrowExceptionIfAuthnContextIsAbsent() throws Exception {
        final AuthnStatement authnStatement = AuthnStatementBuilder.anAuthnStatement().withAuthnContext(null).build();
        final Assertion assertion = AssertionBuilder.anAssertion().addAuthnStatement(authnStatement).buildUnencrypted();

        validateException(assertion, ResponseProcessingValidationSpecification.class, SamlTransformationErrorFactory.authnContextMissingError());
    }

    @Test
    public void validate_shouldPassValidation() throws Exception {
        final AuthnStatement authnStatement = AuthnStatementBuilder.anAuthnStatement().build();
        Assertion assertion = AssertionBuilder.anAssertion().addAuthnStatement(authnStatement).buildUnencrypted();
        Mockito.when(duplicateAssertionValidator.valid(Matchers.any(Assertion.class))).thenReturn(true);

        validator.validate(assertion);
    }

    @Test
    public void validate_shouldThrowExceptionIfAuthnContextClassRefIsAbsent() throws Exception {
        final AuthnContext authnContext = AuthnContextBuilder.anAuthnContext().withAuthnContextClassRef(null).build();
        final AuthnStatement authnStatement = AuthnStatementBuilder.anAuthnStatement().withAuthnContext(authnContext).build();

        Assertion assertion = AssertionBuilder.anAssertion().addAuthnStatement(authnStatement).buildUnencrypted();

        validateException(assertion, GenericHubProfileValidationSpecification.class, SamlTransformationErrorFactory.authnContextClassRefMissing());
    }

    @Test
    public void validate_shouldThrowExceptionIfAuthnContextClassRefValueIsAbsent() throws Exception {
        final AuthnContextClassRef authnContextClassRef = AuthnContextClassRefBuilder.anAuthnContextClassRef().withAuthnContextClasRefValue(null).build();
        final AuthnContext authnContext = AuthnContextBuilder.anAuthnContext().withAuthnContextClassRef(authnContextClassRef).build();
        final AuthnStatement authnStatement = AuthnStatementBuilder.anAuthnStatement().withAuthnContext(authnContext).build();

        Assertion assertion = AssertionBuilder.anAssertion().addAuthnStatement(authnStatement).buildUnencrypted();

        validateException(assertion, GenericHubProfileValidationSpecification.class, SamlTransformationErrorFactory.authnContextClassRefValueMissing());
    }

    @Test
    public void validate_shouldThrowExceptionIfIdInTheAssertionHasBeenProcessedBefore() throws Exception {
        String id = "duplicate-id";
        Assertion assertion = AssertionBuilder.anAssertion().withId(id).addAuthnStatement(AuthnStatementBuilder.anAuthnStatement().build()).buildUnencrypted();

        Mockito.when(duplicateAssertionValidator.valid(Matchers.any(Assertion.class))).thenReturn(false);

        validateException(assertion, ResponseProcessingValidationSpecification.class, SamlTransformationErrorFactory.authnStatementAlreadyReceived(id));
    }

    @Test
    public void validate_shouldThrowExceptionIfIdInTheAssertionHasBeenProcessedBeforeTwo() throws Exception {
        Mockito.when(duplicateAssertionValidator.valid(Matchers.any(Assertion.class))).thenReturn(false);

        String id = "duplicate-id-two";
        Assertion assertion = AssertionBuilder.anAssertion().withId(id).addAuthnStatement(AuthnStatementBuilder.anAuthnStatement().build()).buildUnencrypted();
        validateException(assertion, ResponseProcessingValidationSpecification.class, SamlTransformationErrorFactory.authnStatementAlreadyReceived(id));
    }

    private void validateException(final Assertion assertion, Class<? extends SamlValidationSpecificationFailure> errorClass, SamlValidationSpecificationFailure failure) {
        SamlTransformationErrorManagerTestHelper.validateFail(
                new SamlTransformationErrorManagerTestHelper.Action() {
                    @Override
                    public void execute() {
                        validator.validate(assertion);
                    }
                },
                failure
        );
    }
}
