package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class EidasCycle0And1MatchRequestSentStateBuilderTest {
    private static final DateTime NOW = DateTime.now(DateTimeZone.UTC);
    private static final String ENCRYPTED_DIFFERENT_IDENTITY_ASSERTION = "encryptedDifferentIdentityAssertion";
    private static final PersistentId DIFFERENT_NAME_ID = new PersistentId("differentNameId");

    @Before
    public void setUp() {
        DateTimeUtils.setCurrentMillisFixed(NOW.getMillis());
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void anEidasCycle0And1MatchRequestSentState() {
        assertThat(EidasCycle0And1MatchRequestSentStateBuilder.anEidasCycle0And1MatchRequestSentState()).isInstanceOf(EidasCycle0And1MatchRequestSentStateBuilder.class);
    }

    @Test
    public void build() {
        EidasCycle0And1MatchRequestSentState state = EidasCycle0And1MatchRequestSentStateBuilder.anEidasCycle0And1MatchRequestSentState().build();

        assertThat(state.getRequestId()).isEqualTo("requestId");
        assertThat(state.getIdentityProviderEntityId()).isEqualTo("identityProviderEntityId");
        assertThat(state.getSessionExpiryTimestamp()).isEqualTo(DateTime.now(DateTimeZone.UTC).plusMinutes(10));
        assertThat(state.getRelayState()).isEqualTo(Optional.empty());
        assertThat(state.getRequestIssuerEntityId()).isEqualTo("requestIssuerId");
        assertThat(state.getEncryptedIdentityAssertion()).isEqualTo("encryptedIdentityAssertion");
        assertThat(state.getAssertionConsumerServiceUri()).isEqualTo(URI.create("assertionConsumerServiceUri"));
        assertThat(state.getMatchingServiceAdapterEntityId()).isEqualTo("matchingServiceAdapterEntityId");
        assertThat(state.getSessionId()).isEqualTo(new SessionId("sessionId"));
        assertThat(state.getIdpLevelOfAssurance()).isEqualTo(LevelOfAssurance.LEVEL_2);
        assertThat(state.getPersistentId()).isEqualTo(new PersistentId("default-name-id"));
        assertThat(state.getTransactionSupportsEidas()).isEqualTo(true);
        assertThat(state.getForceAuthentication().orElse(null)).isEqualTo(false);
    }

    @Test
    public void withEncryptedIdentityAssertion() {
        EidasCycle0And1MatchRequestSentState state = EidasCycle0And1MatchRequestSentStateBuilder.anEidasCycle0And1MatchRequestSentState()
            .withEncryptedIdentityAssertion(ENCRYPTED_DIFFERENT_IDENTITY_ASSERTION)
            .build();

        assertThat(state.getEncryptedIdentityAssertion()).isEqualTo(ENCRYPTED_DIFFERENT_IDENTITY_ASSERTION);
    }

    @Test
    public void withPersistentId() {
        EidasCycle0And1MatchRequestSentState state = EidasCycle0And1MatchRequestSentStateBuilder.anEidasCycle0And1MatchRequestSentState()
            .withPersistentId(DIFFERENT_NAME_ID)
            .build();

        assertThat(state.getPersistentId()).isEqualTo(DIFFERENT_NAME_ID);
    }
}
