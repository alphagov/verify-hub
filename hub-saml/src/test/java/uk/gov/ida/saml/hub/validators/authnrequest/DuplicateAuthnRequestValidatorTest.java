package uk.gov.ida.saml.hub.validators.authnrequest;

import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.hub.configuration.SamlDuplicateRequestValidationConfiguration;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(OpenSAMLMockitoRunner.class)
public class DuplicateAuthnRequestValidatorTest {

    private DuplicateAuthnRequestValidator duplicateAuthnRequestValidator;
    private final int EXPIRATION_HOURS = 2;

    @Before
    public void initialiseTestSubject() {
        SamlDuplicateRequestValidationConfiguration samlEngineConfiguration = new SamlDuplicateRequestValidationConfiguration() {
            @Override
            public Duration getAuthnRequestIdExpirationDuration() {
                return Duration.hours(EXPIRATION_HOURS);
            }
        };
        ConcurrentMap<AuthnRequestIdKey, DateTime> duplicateIds = new ConcurrentHashMap<>();
        duplicateAuthnRequestValidator = new DuplicateAuthnRequestValidator(duplicateIds, samlEngineConfiguration);
    }

    @Before
    public void freezeTime() {
        DateTimeFreezer.freezeTime();
    }

    @After
    public void unfreezeTime() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void valid_shouldThrowAnExceptionIfTheAuthnRequestIsADuplicateOfAPreviousOne() throws Exception {
        final String duplicateRequestId = "duplicate-id";
        duplicateAuthnRequestValidator.valid(duplicateRequestId);

        boolean isValid = duplicateAuthnRequestValidator.valid(duplicateRequestId);
        assertThat(isValid).isEqualTo(false);
    }

    @Test
    public void valid_shouldPassIfTheAuthnRequestIsNotADuplicateOfAPreviousOne() throws Exception {
        duplicateAuthnRequestValidator.valid("some-request-id");

        boolean isValid = duplicateAuthnRequestValidator.valid("another-request-id");
        assertThat(isValid).isEqualTo(true);
    }


    @Test
    public void valid_shouldPassIfTwoAuthnRequestsHaveTheSameIdButTheFirstAssertionHasExpired() throws Exception {
        final String duplicateRequestId = "duplicate-id";
        duplicateAuthnRequestValidator.valid(duplicateRequestId);

        DateTimeFreezer.freezeTime(DateTime.now().plusHours(EXPIRATION_HOURS).plusMinutes(1));

        boolean isValid = duplicateAuthnRequestValidator.valid(duplicateRequestId);
        assertThat(isValid).isEqualTo(true);
    }

    @Test
    public void valid_shouldFailIfAuthnRequestsReceivedWithSameIdAndFirstIdHasNotExpired() throws Exception {
        final String duplicateRequestId = "duplicate-id";
        duplicateAuthnRequestValidator.valid(duplicateRequestId);

        DateTimeFreezer.freezeTime(DateTime.now().plusHours(EXPIRATION_HOURS).minusMinutes(1));

        boolean isValid = duplicateAuthnRequestValidator.valid(duplicateRequestId);
        assertThat(isValid).isEqualTo(false);
    }
}
