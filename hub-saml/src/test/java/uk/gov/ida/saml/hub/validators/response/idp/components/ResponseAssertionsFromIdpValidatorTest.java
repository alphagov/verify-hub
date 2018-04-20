package uk.gov.ida.saml.hub.validators.response.idp.components;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.validators.assertion.AuthnStatementAssertionValidator;
import uk.gov.ida.saml.core.validators.assertion.IPAddressValidator;
import uk.gov.ida.saml.core.validators.assertion.IdentityProviderAssertionValidator;
import uk.gov.ida.saml.core.validators.assertion.MatchingDatasetAssertionValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.AuthnStatementBuilder.anAuthnStatement;
import static uk.gov.ida.saml.core.test.builders.MatchingDatasetAttributeStatementBuilder_1_1.aMatchingDatasetAttributeStatement_1_1;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;

@RunWith(OpenSAMLMockitoRunner.class)
public class ResponseAssertionsFromIdpValidatorTest {

    @Mock
    private IdentityProviderAssertionValidator assertionValidator;
    @Mock
    private MatchingDatasetAssertionValidator matchingDatasetAssertionValidator;
    @Mock
    private AuthnStatementAssertionValidator authnStatementValidator;
    @Mock
    private IPAddressValidator ipAddressValidator;

    private ResponseAssertionsFromIdpValidator validator;

    @Before
    public void setUp() throws Exception {
        validator = new ResponseAssertionsFromIdpValidator(
                assertionValidator,
                matchingDatasetAssertionValidator,
                authnStatementValidator,
                ipAddressValidator,
                TestEntityIds.HUB_ENTITY_ID
        );
    }

    @Test
    public void validate_shouldDelegateAuthnStatementAssertionValidation() throws Exception {
        Response response = aResponse()
                .addEncryptedAssertion(anAssertion().addAttributeStatement(aMatchingDatasetAttributeStatement_1_1().build()).build())
                .addEncryptedAssertion(anAssertion().build())
                .build();
        Assertion authNAssertion = anAssertion().buildUnencrypted();
        Assertion mdsAssertion = anAssertion().addAttributeStatement(aMatchingDatasetAttributeStatement_1_1().build()).buildUnencrypted();
        List<Assertion> assertions = asList(mdsAssertion, authNAssertion);

        validator.validate(new ValidatedResponse(response), new ValidatedAssertions(assertions));

        verify(authnStatementValidator).validate(authNAssertion);
    }

    @Test
    public void validate_shouldDelegateMatchingDatasetAssertionValidation() throws Exception {
        Response response = aResponse()
                .addEncryptedAssertion(anAssertion().addAttributeStatement(aMatchingDatasetAttributeStatement_1_1().build()).build())
                .addEncryptedAssertion(anAssertion().build())
                .build();
        Assertion authNAssertion = anAssertion().buildUnencrypted();
        Assertion mdsAssertion = anAssertion().addAttributeStatement(aMatchingDatasetAttributeStatement_1_1().build()).buildUnencrypted();
        List<Assertion> assertions = asList(mdsAssertion, authNAssertion);

        validator.validate(new ValidatedResponse(response), new ValidatedAssertions(assertions));

        verify(matchingDatasetAssertionValidator).validate(mdsAssertion, response.getIssuer().getValue());
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void validate_shouldThrowExceptionIfMatchingDatasetStatementElementIsMissing() throws Exception {
        final Response response = aResponse()
                .addEncryptedAssertion(anAssertion().addAuthnStatement(anAuthnStatement().build()).build())
                .addEncryptedAssertion(anAssertion().build()).build();
        List<Assertion> assertions = asList(anAssertion().addAuthnStatement(anAuthnStatement().build()).buildUnencrypted(), anAssertion().buildUnencrypted());

        validateThrows(response, assertions, SamlTransformationErrorFactory.missingMatchingMds());
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void validate_shouldThrowExceptionIfAuthnStatementAssertionIsMissing() throws Exception {
        Response response = aResponse()
                .addEncryptedAssertion(anAssertion().addAttributeStatement(aMatchingDatasetAttributeStatement_1_1().build()).build())
                .addEncryptedAssertion(anAssertion().addAttributeStatement(aMatchingDatasetAttributeStatement_1_1().build()).build())
                .build();
        List<Assertion> assertions = asList(
                anAssertion().addAttributeStatement(aMatchingDatasetAttributeStatement_1_1().build()).buildUnencrypted(),
                anAssertion().addAttributeStatement(aMatchingDatasetAttributeStatement_1_1().build()).buildUnencrypted()
        );
        validateThrows(response, assertions, SamlTransformationErrorFactory.missingAuthnStatement());
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void validate_shouldThrowExceptionIfThereAreMultipleAuthnStatementsWithinTheAuthnStatementAssertionPresent() throws Exception {
        Response response = aResponse()
                .addEncryptedAssertion(anAssertion().addAttributeStatement(aMatchingDatasetAttributeStatement_1_1().build()).build())
                .addEncryptedAssertion(anAssertion().addAuthnStatement(anAuthnStatement().build()).addAuthnStatement(anAuthnStatement().build()).build())
                .build();
        List<Assertion> assertions = asList(
                anAssertion().addAttributeStatement(aMatchingDatasetAttributeStatement_1_1().build()).buildUnencrypted(),
                anAssertion().addAuthnStatement(anAuthnStatement().build()).addAuthnStatement(anAuthnStatement().build()).buildUnencrypted()
        );
        validateThrows(response, assertions, SamlTransformationErrorFactory.multipleAuthnStatements());
    }

    @Test
    public void validate_shouldDelegateToIpAddressValidator() throws Exception {
        Assertion authnStatementAssertion = anAssertion().addAuthnStatement(anAuthnStatement().build()).buildUnencrypted();
        Response response = aResponse()
                .addEncryptedAssertion(anAssertion().addAttributeStatement(aMatchingDatasetAttributeStatement_1_1().build()).build())
                .addEncryptedAssertion(anAssertion().addAuthnStatement(anAuthnStatement().build()).build())
                .build();
        List<Assertion> assertions = asList(anAssertion().addAttributeStatement(aMatchingDatasetAttributeStatement_1_1().build()).buildUnencrypted(), authnStatementAssertion);
        validator.validate(new ValidatedResponse(response), new ValidatedAssertions(assertions));
        verify(ipAddressValidator).validate(authnStatementAssertion);
    }

    private void validateThrows(Response response, List<Assertion> assertions, SamlValidationSpecificationFailure samlValidationSpecificationFailure) {
        try {
            validator.validate(new ValidatedResponse(response), new ValidatedAssertions(assertions));
        } catch (SamlTransformationErrorException e) {
            assertThat(e.getMessage()).isEqualTo(samlValidationSpecificationFailure.getErrorMessage());
            assertThat(e.getLogLevel()).isEqualTo(samlValidationSpecificationFailure.getLogLevel());
            throw e;
        }
    }
}
