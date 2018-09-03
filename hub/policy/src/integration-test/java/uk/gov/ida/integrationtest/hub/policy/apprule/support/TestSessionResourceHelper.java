package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import uk.gov.ida.hub.policy.builder.state.AuthnFailedErrorStateBuilder;
import uk.gov.ida.hub.policy.builder.state.CountryAuthnFailedErrorStateBuilder;
import uk.gov.ida.hub.policy.builder.state.CountrySelectedStateBuilder;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.AbstractSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.CountryAuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.CountrySelectingState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;

import static uk.gov.ida.hub.policy.builder.state.EidasSuccessfulMatchStateBuilder.anEidasSuccessfulMatchState;
import static uk.gov.ida.hub.policy.builder.state.IdpSelectedStateBuilder.anIdpSelectedState;
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
                idpSelectedState.getRelayState().orNull(),
                idpSelectedState.getRequestIssuerEntityId(),
                idpSelectedState.getMatchingServiceEntityId(),
                idpSelectedState.getAssertionConsumerServiceUri(),
                idpSelectedState.getLevelsOfAssurance(),
                idpSelectedState.getUseExactComparisonType(),
                idpSelectedState.isRegistering(),
                idpSelectedState.getRequestedLoa(),
                idpSelectedState.getForceAuthentication().orNull(),
                idpSelectedState.getAvailableIdentityProviderEntityIds(),
                idpSelectedState.getTransactionSupportsEidas());

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
                state.getRelayState().orNull(),
                state.getRequestIssuerEntityId(),
                null,
                state.getMatchingServiceAssertion(),
                state.getAssertionConsumerServiceUri(),
                Collections.singletonList(state.getLevelOfAssurance()),
                false,
                state.getTransactionSupportsEidas());
    }

    public static Response createSessionInAuthnFailedErrorState(SessionId sessionId, Client client, URI uri) {
        AuthnFailedErrorState state = AuthnFailedErrorStateBuilder.anAuthnFailedErrorState().build();
        TestSessionDto testSessionDto = new TestSessionDto(
                sessionId,
                state.getRequestId(),
                state.getSessionExpiryTimestamp(),
                state.getIdpEntityId(),
                state.getRelayState().orNull(),
                null,
                null,
                null,
                state.getAssertionConsumerServiceUri(),
                Collections.emptyList(),
                false,
                state.getTransactionSupportsEidas());

        return client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(testSessionDto));
    }

    public static Response createSessionInCountryAuthnFailedErrorState(SessionId sessionId, Client client, URI uri) {
        CountryAuthnFailedErrorState state = CountryAuthnFailedErrorStateBuilder.aCountryAuthnFailedErrorState().build();
        TestSessionDto testSessionDto = new TestSessionDto(
                sessionId,
                state.getRequestId(),
                state.getSessionExpiryTimestamp(),
                state.getCountryEntityId(),
                state.getRelayState().orNull(),
                state.getRequestIssuerEntityId(),
                null,
                null,
                state.getAssertionConsumerServiceUri(),
                state.getLevelsOfAssurance(),
                false,
                state.getTransactionSupportsEidas());

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

    public static Response createSessionInCountrySelectingState(SessionId sessionId, Client client, URI uri, String rpEntityId, boolean transactionSupportsEidas) {
        CountrySelectingState countrySelectedState = CountrySelectedStateBuilder.aCountrySelectedState()
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
                null,
                countrySelectedState.getAssertionConsumerServiceUri(),
                null,
                null,
                countrySelectedState.getTransactionSupportsEidas());
        return client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(testSessionDto));
    }
}
