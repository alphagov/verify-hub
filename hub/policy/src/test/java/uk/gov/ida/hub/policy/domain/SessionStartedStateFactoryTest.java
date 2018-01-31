package uk.gov.ida.hub.policy.domain;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedStateFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.policy.builder.domain.ReceivedAuthnRequestBuilder.aReceivedAuthnRequest;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

@RunWith(MockitoJUnitRunner.class)
public class SessionStartedStateFactoryTest {

    private SessionStartedStateFactory systemUnderTest;

    @Before
    public void setup() {
        systemUnderTest = new SessionStartedStateFactory();
    }

    @Test
    public void build_shouldBuildState() throws Exception {
        ReceivedAuthnRequest authnRequest = aReceivedAuthnRequest().build();

        SessionStartedState sessionStartedState = systemUnderTest.build(
                authnRequest.getId(),
                authnRequest.getAssertionConsumerServiceUri(),
                authnRequest.getIssuer(),
                authnRequest.getRelayState(),
                authnRequest.getForceAuthentication(),
                DateTime.now().plusDays(6),
                aSessionId().build(), 
                false);

        assertThat(sessionStartedState.getRequestId()).isEqualTo(authnRequest.getId());
    }
}
