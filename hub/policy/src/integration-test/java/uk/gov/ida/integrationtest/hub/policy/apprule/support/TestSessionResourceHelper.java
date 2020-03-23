package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import uk.gov.ida.hub.policy.builder.state.AuthnFailedErrorStateBuilder;
import uk.gov.ida.hub.policy.builder.state.EidasAuthnFailedErrorStateBuilder;
import uk.gov.ida.hub.policy.builder.state.EidasCountrySelectedStateBuilder;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.AbstractSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.EidasAuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectingState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.NonMatchingJourneySuccessState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;

import static uk.gov.ida.hub.policy.builder.state.EidasSuccessfulMatchStateBuilder.anEidasSuccessfulMatchState;
import static uk.gov.ida.hub.policy.builder.state.IdpSelectedStateBuilder.anIdpSelectedState;
import static uk.gov.ida.hub.policy.builder.state.NonMatchingJourneySuccessStateBuilder.aNonMatchingJourneySuccessStateBuilder;
import static uk.gov.ida.hub.policy.builder.state.SuccessfulMatchStateBuilder.aSuccessfulMatchState;

public class TestSessionResourceHelper {

    public static Response createSessionInIdpSelectedState(SessionId sessionId, String issuerId, String idpEntityId, Client client, URI uri) {
        return createSessionInIdpSelectedState(sessionId, issuerId, idpEntityId, client, uri, false);
    }

    public static Response createSessionInIdpSelectedState(
            SessionId sessionId,
            String issuerId,
            String idpEntityId,
            Client client,
            URI uri,
            boolean transactionSupportsEidas) {

        IdpSelectedState idpSelectedState = anIdpSelectedState()
                .withRequestIssuerEntityId(issuerId)
                .withIdpEntityId(idpEntityId)
                .withSessionId(sessionId)
                .withRegistration(true)
                .withTransactionSupportsEidas(transactionSupportsEidas)
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
                idpSelectedState.getTransactionSupportsEidas(),
                null,
                null,
                null
        );

        return client
                .target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(testSessionDto));
    }

    public static Response createSessionInSuccessfulMatchState(SessionId sessionId, String requestIssuerEntityId, String idpEntityId, Client client, URI uri) {
        SuccessfulMatchState successfulMatchState = aSuccessfulMatchState()
                .withSessionId(sessionId)
                .withIdentityProviderEntityId(idpEntityId)
                .withRequestIssuerEntityId(requestIssuerEntityId)
                .build();

        TestSessionDto testSessionDto = createASuccessfulMatchStateTestSessionDto(successfulMatchState, sessionId);

        return client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(testSessionDto));
    }

    public static Response createSessionInEidasSuccessfulMatchState(SessionId sessionId, String rpEntityId, String countryEntityId, Client client, URI uri) {
        EidasSuccessfulMatchState eidasSuccessfulMatchState = anEidasSuccessfulMatchState().withRequestIssuerId(rpEntityId).withSessionId(sessionId).withCountryEntityId(countryEntityId).build();

        TestSessionDto testSessionDto = createASuccessfulMatchStateTestSessionDto(eidasSuccessfulMatchState, sessionId);

        return client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(testSessionDto));
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
                state.getTransactionSupportsEidas(),
                null
        );
    }

    public static Response createSessionInAuthnFailedErrorState(SessionId sessionId, Client client, URI uri) {
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
                state.getTransactionSupportsEidas(),
                null
        );

        return client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(testSessionDto));
    }

    public static Response createSessionInEidasAuthnFailedErrorState(SessionId sessionId, Client client, URI uri) {
        EidasAuthnFailedErrorState state = EidasAuthnFailedErrorStateBuilder.anEidasAuthnFailedErrorState().build();
        TestSessionDto testSessionDto = new TestSessionDto(
                sessionId,
                state.getRequestId(),
                state.getSessionExpiryTimestamp(),
                state.getCountryEntityId(),
                state.getRelayState().orElse(null),
                state.getRequestIssuerEntityId(),
                null,
                state.getAssertionConsumerServiceUri(),
                state.getLevelsOfAssurance(),
                false,
                state.getTransactionSupportsEidas(),
                null
        );

        return client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(testSessionDto));
    }

    public static Response selectCountryInSession(SessionId sessionId, Client client, URI uri) {
        return client
                .target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.text(""));
    }

    public static Response createSessionInEidasCountrySelectingState(SessionId sessionId, Client client, URI uri, String rpEntityId, boolean transactionSupportsEidas) {
        EidasCountrySelectingState countrySelectedState = EidasCountrySelectedStateBuilder.anEidasCountrySelectedState()
                .withSessionId(sessionId)
                .withRequestIssuerEntityId(rpEntityId)
                .withTransactionSupportsEidas(transactionSupportsEidas)
                .build();

        TestSessionDto testSessionDto = new TestSessionDto(
                sessionId,
                countrySelectedState.getRequestId(),
                countrySelectedState.getSessionExpiryTimestamp(),
                null,
                null,
                rpEntityId,
                null,
                countrySelectedState.getAssertionConsumerServiceUri(),
                null,
                null,
                countrySelectedState.getTransactionSupportsEidas(),
                null);
        return client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(testSessionDto));
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
                nonMatchingJourneySuccessState.getTransactionSupportsEidas(),
                nonMatchingJourneySuccessState.getEncryptedAssertions(),
                nonMatchingJourneySuccessState.getCountrySignedResponseContainer().get(),
                null
        );

        return client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(testSessionDto));
    }
}
