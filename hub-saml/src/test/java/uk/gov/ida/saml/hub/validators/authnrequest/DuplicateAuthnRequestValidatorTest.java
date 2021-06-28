package uk.gov.ida.saml.hub.validators.authnrequest;

import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;
import uk.gov.ida.saml.hub.configuration.SamlDuplicateRequestValidationConfiguration;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OpenSAMLExtension.class)
public class DuplicateAuthnRequestValidatorTest {

    private static DuplicateAuthnRequestValidator duplicateAuthnRequestValidator;
    private static final int EXPIRATION_HOURS = 2;

    @BeforeAll
    public static void initialiseTestSubject() {
        SamlDuplicateRequestValidationConfiguration samlEngineConfiguration = () -> Duration.hours(EXPIRATION_HOURS);
        ConcurrentMap<AuthnRequestIdKey, DateTime> duplicateIds = new ConcurrentHashMap<>();
        IdExpirationCache idExpirationCache = new ConcurrentMapIdExpirationCache(duplicateIds);
        duplicateAuthnRequestValidator = new DuplicateAuthnRequestValidator(idExpirationCache, samlEngineConfiguration);
    }

    @BeforeEach
    public void freezeTime() {
        DateTimeFreezer.freezeTime();
    }

    @AfterEach
    public void unfreezeTime() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void valid_shouldThrowAnExceptionIfTheAuthnRequestIsADuplicateOfAPreviousOne() {
        final String duplicateRequestId = "duplicate-id";
        duplicateAuthnRequestValidator.valid(duplicateRequestId);

        boolean isValid = duplicateAuthnRequestValidator.valid(duplicateRequestId);
        assertThat(isValid).isEqualTo(false);
    }

    @Test
    public void valid_shouldPassIfTheAuthnRequestIsNotADuplicateOfAPreviousOne() {
        duplicateAuthnRequestValidator.valid("some-request-id");

        boolean isValid = duplicateAuthnRequestValidator.valid("another-request-id");
        assertThat(isValid).isEqualTo(true);
    }


    @Test
    public void valid_shouldPassIfTwoAuthnRequestsHaveTheSameIdButTheFirstAssertionHasExpired() {
        final String duplicateRequestId = "duplicate-id";
        duplicateAuthnRequestValidator.valid(duplicateRequestId);

        DateTimeFreezer.freezeTime(DateTime.now().plusHours(EXPIRATION_HOURS).plusMinutes(1));

        boolean isValid = duplicateAuthnRequestValidator.valid(duplicateRequestId);
        assertThat(isValid).isEqualTo(true);
    }

    @Test
    public void valid_shouldFailIfAuthnRequestsReceivedWithSameIdAndFirstIdHasNotExpired() {
        final String duplicateRequestId = "duplicate-id";
        duplicateAuthnRequestValidator.valid(duplicateRequestId);

        DateTimeFreezer.freezeTime(DateTime.now().plusHours(EXPIRATION_HOURS).minusMinutes(1));

        boolean isValid = duplicateAuthnRequestValidator.valid(duplicateRequestId);
        assertThat(isValid).isEqualTo(false);
    }
}
