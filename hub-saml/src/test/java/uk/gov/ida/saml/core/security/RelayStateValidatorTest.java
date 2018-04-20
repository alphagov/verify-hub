package uk.gov.ida.saml.core.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import static java.util.Arrays.asList;

@RunWith(MockitoJUnitRunner.class)
public class RelayStateValidatorTest {

    private RelayStateValidator relayStateValidator;

    @Before
    public void setUp() throws Exception {
        relayStateValidator = new RelayStateValidator();
    }

    @Test
    public void validate_shouldCheckRelayStateLengthIsLessThanEightyOneCharactersOrRaiseException() {
        final String aStringMoreThanEightyCharacters = generateLongString();

        SamlTransformationErrorManagerTestHelper.validateFail(
                new SamlTransformationErrorManagerTestHelper.Action() {
                    @Override
                    public void execute() {
                        relayStateValidator.validate(aStringMoreThanEightyCharacters);
                    }
                },
                SamlTransformationErrorFactory.invalidRelayState(aStringMoreThanEightyCharacters)
        );

    }

    @Test
    public void validate_shouldCheckRelayStateForValidStringAndNotThrowAnException() {
        String aStringLessThanEightyCharacters = generateShortString();

        relayStateValidator.validate(aStringLessThanEightyCharacters);
    }

    @Test
    public void validate_shouldCheckForInvalidCharacters() {
        final String aString = "aStringWith";

        for (final String i : asList(">", "<", "'", "\"", "%", "&", ";")) {

            SamlTransformationErrorManagerTestHelper.validateFail(
                    new SamlTransformationErrorManagerTestHelper.Action() {
                        @Override
                        public void execute() {
                            relayStateValidator.validate(aString + i);
                        }
                    },
                    SamlTransformationErrorFactory.relayStateContainsInvalidCharacter(i, aString + i)
            );
        }
    }

    private String generateShortString() {
        return "short string";
    }


    private String generateLongString() {
        String longString = "";
        for (int i = 0; i < 82; i++) {
            longString = longString + "a";
        }
        return longString;
    }
}
