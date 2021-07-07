package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import uk.gov.ida.hub.policy.builder.state.AuthnFailedErrorStateBuilder;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.AbstractSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.NonMatchingJourneySuccessState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppExtension.PolicyClient;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;

import static uk.gov.ida.hub.policy.builder.state.IdpSelectedStateBuilder.anIdpSelectedState;
import static uk.gov.ida.hub.policy.builder.state.NonMatchingJourneySuccessStateBuilder.aNonMatchingJourneySuccessStateBuilder;
import static uk.gov.ida.hub.policy.builder.state.SuccessfulMatchStateBuilder.aSuccessfulMatchState;

public class TestSessionResourceHelper {

    public static Response createSessionInIdpSelectedState(
            SessionId sessionId,
            String issuerId,
            String idpEntityId,
            PolicyClient client,
            URI uri) {

        IdpSelectedState idpSelectedState = anIdpSelectedState()
                .withRequestIssuerEntityId(issuerId)
                .withIdpEntityId(idpEntityId)
                .withSessionId(sessionId)
                .withRegistration(true)
                .build();

        TestSessionDto testSessionDto = new TestSessionDto(
                sessionId,
                idpSelectedState.getRequestId(),
                idpSelectedState.getSessionExpiryTimestamp(),
                idpSelectedState.getIdpEntityId(),
                null,
                idpSelectedState.getRelayState().orElse(null),
                idpSelectedState.getRequestIssuerEntityId(),
                idpSelectedState.getAssertionConsumerServiceUri(),
                idpSelectedState.getLevelsOfAssurance(),
                idpSelectedState.getUseExactComparisonType(),
                idpSelectedState.isRegistering(),
                idpSelectedState.getRequestedLoa(),
                idpSelectedState.getForceAuthentication().orElse(null),
                idpSelectedState.getAvailableIdentityProviders(),
                null,
                null
        );

        return client.postTargetMain(uri, testSessionDto);
    }

    public static Response createSessionInSuccessfulMatchState(
            SessionId sessionId,
            String requestIssuerEntityId,
            String idpEntityId,
            PolicyClient client,
            URI uri) {
        SuccessfulMatchState successfulMatchState = aSuccessfulMatchState()
                .withSessionId(sessionId)
                .withIdentityProviderEntityId(idpEntityId)
                .withRequestIssuerEntityId(requestIssuerEntityId)
                .build();

        TestSessionDto testSessionDto = createASuccessfulMatchStateTestSessionDto(successfulMatchState, sessionId);

        return client.postTargetMain(uri, testSessionDto);
    }

    private static TestSessionDto createASuccessfulMatchStateTestSessionDto(AbstractSuccessfulMatchState state, SessionId sessionId) {

        return new TestSessionDto(
                sessionId,
                state.getRequestId(),
                state.getSessionExpiryTimestamp(),
                state.getIdentityProviderEntityId(),
                state.getRelayState().orElse(null),
                state.getRequestIssuerEntityId(),
                state.getMatchingServiceAssertion(),
                state.getAssertionConsumerServiceUri(),
                Collections.singletonList(state.getLevelOfAssurance()),
                false,
                null
        );
    }

    public static Response createSessionInAuthnFailedErrorState(SessionId sessionId, PolicyClient client, URI uri) {
        AuthnFailedErrorState state = AuthnFailedErrorStateBuilder.anAuthnFailedErrorState().build();
        TestSessionDto testSessionDto = new TestSessionDto(
                sessionId,
                state.getRequestId(),
                state.getSessionExpiryTimestamp(),
                state.getIdpEntityId(),
                state.getRelayState().orElse(null),
                null,
                null,
                state.getAssertionConsumerServiceUri(),
                Collections.emptyList(),
                false,
                null
        );

        return client.postTargetMain(uri, testSessionDto);
    }

    public static Response createSessionInNonMatchingJourneySuccessState(SessionId sessionId, Client client, URI uri, String rpEntityId) {
        NonMatchingJourneySuccessState nonMatchingJourneySuccessState = aNonMatchingJourneySuccessStateBuilder()
                .withSessionId(sessionId)
                .withRequestIssuerEntityId(rpEntityId)
                .build();

        TestSessionDto testSessionDto = new TestSessionDto(
                nonMatchingJourneySuccessState.getSessionId(),
                nonMatchingJourneySuccessState.getRequestId(),
                nonMatchingJourneySuccessState.getSessionExpiryTimestamp(),
                null,
                null,
                nonMatchingJourneySuccessState.getRelayState().orElse(null),
                rpEntityId,
                nonMatchingJourneySuccessState.getAssertionConsumerServiceUri(),
                null,
                null,
                false,
                LevelOfAssurance.LEVEL_2,
                false,
                null,
                nonMatchingJourneySuccessState.getEncryptedAssertions(),
                null
        );

        return client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(testSessionDto));
    }
}
