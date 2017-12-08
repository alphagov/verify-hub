package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import com.google.common.base.Optional;
import uk.gov.ida.hub.policy.builder.state.AuthnFailedErrorStateBuilder;
import uk.gov.ida.hub.policy.builder.state.CountrySelectedStateBuilder;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.CountrySelectingState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

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
                idpSelectedState.registering(),
                idpSelectedState.getForceAuthentication(),
                idpSelectedState.getAvailableIdentityProviderEntityIds(),
                idpSelectedState.getTransactionSupportsEidas());

        return  client
                .target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(testSessionDto));
    }

    public static Response createSessionInSuccessfulMatchState(SessionId sessionId, String idpEntityId, Client client, URI uri) {
        SuccessfulMatchState successfulMatchState = aSuccessfulMatchState().withSessionId(sessionId).withIdentityProviderEntityId(idpEntityId).build();
        TestSessionDto testSessionDto = new TestSessionDto(sessionId,
                successfulMatchState.getRequestId(),
                successfulMatchState.getSessionExpiryTimestamp(),
                successfulMatchState.getIdentityProviderEntityId(),
                successfulMatchState.getMatchingServiceAssertion(),
                successfulMatchState.getRelayState(),
                successfulMatchState.getIdentityProviderEntityId(),
                null,
                successfulMatchState.getAssertionConsumerServiceUri(),
                Arrays.asList(successfulMatchState.getLevelOfAssurance()),
                false,
                successfulMatchState.getTransactionSupportsEidas());
        return client.target(uri)
                     .request(MediaType.APPLICATION_JSON_TYPE)
                     .post(Entity.json(testSessionDto));
    }

    public static Response createSessionInAuthnFailedErrorState(SessionId sessionId, Client client, URI uri) {
        AuthnFailedErrorState state = AuthnFailedErrorStateBuilder.anAuthnFailedErrorState().build();
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
