package uk.gov.ida.saml.core.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import static java.util.Arrays.asList;

@ExtendWith(MockitoExtension.class)
public class RelayStateValidatorTest {

    private static RelayStateValidator relayStateValidator;

    @BeforeAll
    public static void setUp() throws Exception {
        relayStateValidator = new RelayStateValidator();
    }

    @Test
    public void validate_shouldCheckRelayStateLengthIsLessThanEightyOneCharactersOrRaiseException() {
        final String aStringMoreThanEightyCharacters = "a".repeat(82);

        SamlTransformationErrorManagerTestHelper.validateFail(
                () -> relayStateValidator.validate(aStringMoreThanEightyCharacters),
                SamlTransformationErrorFactory.invalidRelayState(aStringMoreThanEightyCharacters)
        );

    }

    @Test
    public void validate_shouldCheckRelayStateForValidStringAndNotThrowAnException() {
        String aStringLessThanEightyCharacters = "short string";

        relayStateValidator.validate(aStringLessThanEightyCharacters);
    }

    @Test
    public void validate_shouldCheckForInvalidCharacters() {
        final String aString = "aStringWith";

        for (final String i : asList(">", "<", "'", "\"", "%", "&", ";")) {

            SamlTransformationErrorManagerTestHelper.validateFail(
                    () -> relayStateValidator.validate(aString + i),
                    SamlTransformationErrorFactory.relayStateContainsInvalidCharacter(i, aString + i)
            );
        }
    }
}
