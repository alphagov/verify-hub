package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import com.google.common.base.Optional;
import uk.gov.ida.hub.policy.builder.state.AuthnFailedErrorStateBuilder;
import uk.gov.ida.hub.policy.builder.state.CountrySelectedStateBuilder;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.AbstractSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorStateTransitional;
import uk.gov.ida.hub.policy.domain.state.CountrySelectingState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchStateTransitional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import static uk.gov.ida.hub.policy.builder.state.EidasSuccessfulMatchStateBuilder.aEidasSuccessfulMatchState;
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

        IdpSelectedStateTransitional idpSelectedState = anIdpSelectedState()
                .withRequestIssuerEntityId(issuerId)
                .withIdpEntityId(idpEntityId)
                .withSessionId(sessionId)
                .withRegistration(true)
                .withTransactionSupportsEidas(transactionSupportsEidas)
                .build();
        
        TestSessionDto testSessionDto = new TestSessionDto(sessionId,
                idpSelectedState.getRequestId(),
                idpSelectedState.getSessionExpiryTimestamp(),
                idpSelectedState.getIdpEntityId(),
                idpSelectedState.getRelayState(),
                idpSelectedState.getRequestIssuerEntityId(),
                idpSelectedState.getMatchingServiceEntityId(),
                idpSelectedState.getAssertionConsumerServiceUri(),
                idpSelectedState.getLevelsOfAssurance(),
                idpSelectedState.getUseExactComparisonType(),
                idpSelectedState.isRegistering(),
                idpSelectedState.getRequestedLoa(),
                idpSelectedState.getForceAuthentication(),
                idpSelectedState.getAvailableIdentityProviderEntityIds(),
                idpSelectedState.getTransactionSupportsEidas());

        return  client
                .target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(testSessionDto));
    }

    public static Response createSessionInSuccessfulMatchState(SessionId sessionId, String requestIssuerEntityId, String idpEntityId, Client client, URI uri) {
        SuccessfulMatchStateTransitional successfulMatchState = aSuccessfulMatchState().withSessionId(sessionId)
                .withIdentityProviderEntityId(idpEntityId)
                .withRequestIssuerEntityId(requestIssuerEntityId)
                .build();

        TestSessionDto testSessionDto = createASuccessfulMatchStateTestSessionDto(successfulMatchState, sessionId);

        return client.target(uri)
                     .request(MediaType.APPLICATION_JSON_TYPE)
                     .post(Entity.json(testSessionDto));
    }

    public static Response createSessionInEidasSuccessfulMatchState(SessionId sessionId, String rpEntityId, String countryEntityId, Client client, URI uri) {
        EidasSuccessfulMatchState eidasSuccessfulMatchState = aEidasSuccessfulMatchState().withRequestIssuerId(rpEntityId).withSessionId(sessionId).withIdentityProviderEntityId(countryEntityId).build();

        TestSessionDto testSessionDto = createASuccessfulMatchStateTestSessionDto(eidasSuccessfulMatchState, sessionId);

        return client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(testSessionDto));
    }

    private static TestSessionDto createASuccessfulMatchStateTestSessionDto(AbstractSuccessfulMatchState state, SessionId sessionId){

        return  new TestSessionDto(sessionId,
                state.getRequestId(),
                state.getSessionExpiryTimestamp(),
                state.getIdentityProviderEntityId(),
                state.getMatchingServiceAssertion(),
                state.getRelayState(),
                state.getRequestIssuerEntityId(),
                null,
                state.getAssertionConsumerServiceUri(),
                Arrays.asList(state.getLevelOfAssurance()),
                false,
                state.getTransactionSupportsEidas());
    }

    public static Response createSessionInAuthnFailedErrorState(SessionId sessionId, Client client, URI uri) {
        AuthnFailedErrorStateTransitional state = AuthnFailedErrorStateBuilder.anAuthnFailedErrorState().build();
        TestSessionDto testSessionDto = new TestSessionDto(sessionId,
                state.getRequestId(),
                state.getSessionExpiryTimestamp(),
                state.getIdpEntityId(),
                null,
                state.getRelayState(),
                state.getIdpEntityId(),
                null,
                state.getAssertionConsumerServiceUri(),
                Collections.EMPTY_LIST,
                false,
                state.getTransactionSupportsEidas());
        return client.target(uri)
                     .request(MediaType.APPLICATION_JSON_TYPE)
                     .post(Entity.json(testSessionDto));
    }

    public static Response selectCountryInSession(SessionId sessionId, Client client, URI uri) {
        return  client
            .target(uri)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.text(""));
    }

    public static Response createSessionInCountrySelectingState(SessionId sessionId, Client client, URI uri, String rpEntityId, boolean transactionSupportsEidas) {
        CountrySelectingState countrySelectedState = CountrySelectedStateBuilder.aCountrySelectedState()
            .withSessionId(sessionId)
            .withTransactionSupportsEidas(transactionSupportsEidas)
            .build();

        TestSessionDto testSessionDto = new TestSessionDto(sessionId,
            countrySelectedState.getRequestId(),
            countrySelectedState.getSessionExpiryTimestamp(),
            null,
            null,
            Optional.absent(),
            rpEntityId,
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
