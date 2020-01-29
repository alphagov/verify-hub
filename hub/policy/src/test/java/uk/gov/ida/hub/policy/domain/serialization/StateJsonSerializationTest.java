package uk.gov.ida.hub.policy.domain.serialization;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.Cycle3DataInputCancelledState;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.EidasAuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectedState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.MatchingServiceRequestErrorState;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;
import uk.gov.ida.hub.policy.domain.state.PausedRegistrationState;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.TimeoutState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.policy.builder.state.AuthnFailedErrorStateBuilder.anAuthnFailedErrorState;
import static uk.gov.ida.hub.policy.builder.state.AwaitingCycle3DataStateBuilder.anAwaitingCycle3DataState;
import static uk.gov.ida.hub.policy.builder.state.Cycle0And1MatchRequestSentStateBuilder.aCycle0And1MatchRequestSentState;
import static uk.gov.ida.hub.policy.builder.state.Cycle3DataInputCancelledStateBuilder.aCycle3DataInputCancelledState;
import static uk.gov.ida.hub.policy.builder.state.Cycle3MatchRequestSentStateBuilder.aCycle3MatchRequestSentState;
import static uk.gov.ida.hub.policy.builder.state.EidasAuthnFailedErrorStateBuilder.anEidasAuthnFailedErrorState;
import static uk.gov.ida.hub.policy.builder.state.EidasAwaitingCycle3DataStateBuilder.anEidasAwaitingCycle3DataState;
import static uk.gov.ida.hub.policy.builder.state.EidasCountrySelectedStateBuilder.anEidasCountrySelectedState;
import static uk.gov.ida.hub.policy.builder.state.EidasCycle0And1MatchRequestSentStateBuilder.anEidasCycle0And1MatchRequestSentState;
import static uk.gov.ida.hub.policy.builder.state.EidasCycle3MatchRequestSentStateBuilder.anEidasCycle3MatchRequestSentState;
import static uk.gov.ida.hub.policy.builder.state.EidasSuccessfulMatchStateBuilder.anEidasSuccessfulMatchState;
import static uk.gov.ida.hub.policy.builder.state.EidasUserAccountCreationFailedStateBuilder.aEidasUserAccountCreationFailedState;
import static uk.gov.ida.hub.policy.builder.state.EidasUserAccountCreationRequestSentStateBuilder.anEidasUserAccountCreationRequestSentState;
import static uk.gov.ida.hub.policy.builder.state.FraudEventDetectedStateBuilder.aFraudEventDetectedState;
import static uk.gov.ida.hub.policy.builder.state.IdpSelectedStateBuilder.anIdpSelectedState;
import static uk.gov.ida.hub.policy.builder.state.MatchingServiceRequestErrorStateBuilder.aMatchingServiceRequestErrorState;
import static uk.gov.ida.hub.policy.builder.state.NoMatchStateBuilder.aNoMatchState;
import static uk.gov.ida.hub.policy.builder.state.PausedRegistrationStateBuilder.aPausedRegistrationState;
import static uk.gov.ida.hub.policy.builder.state.RequesterErrorStateBuilder.aRequesterErrorState;
import static uk.gov.ida.hub.policy.builder.state.SessionStartedStateBuilder.aSessionStartedState;
import static uk.gov.ida.hub.policy.builder.state.SuccessfulMatchStateBuilder.aSuccessfulMatchState;
import static uk.gov.ida.hub.policy.builder.state.TimeoutStateBuilder.aTimeoutState;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreatedStateBuilder.aUserAccountCreatedState;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreationFailedStateBuilder.aUserAccountCreationFailedState;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreationRequestSentStateBuilder.aUserAccountCreationRequestSentState;

/**
 * Tests that check that the "State" JSON which we store in redis has not changed.
 *
 * These tests are intended to catch the situation where a change is made to a State that changes the format of the serialized
 * JSON, which may cause problems when it is subsequently deserialized to the state objects. This could cause issues during
 * deployment, since there would then be existing States in the data store which the new code may not handle.
 *
 * If you change a State in a way that breaks these tests you should first see if your change is actually breaking, or
 * if there's something else going on (e.g. changing the default in a builder).
 *
 * If it looks like your changes are breaking you should see if you can make the changes in a non-breaking way.
 * If your changes are non breaking, update these tests with new expected values.
 * 
 * If you cannot make the changes non-breaking / Zero Downtime Deployment, you may need to plan for an outage.
 * 
 */
public class StateJsonSerializationTest {

    private static final SessionId SESSION_ID = new SessionId("some-session-id");
    private static final String REQUEST_ID = "some-request-id";

    private ObjectMapper objectMapper = getRedisObjectMapper();

    @Before
    public void setUp() {
        // Some of the states and some of the builders call DateTime.now() in their constructor.
        // That means they don't serialize deterministically unless we freeze time.
        DateTimeFreezer.freezeTime(new DateTime(1988, 1, 1, 0, 0));
    }

    @After
    public void tearDown() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void shouldSerializeSessionStartedState() throws JsonProcessingException {
        SessionStartedState expectedState = aSessionStartedState().withSessionId(SESSION_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.SessionStartedState\",\"requestId\":\"requestId\",\"relayState\":null,\"assertionConsumerServiceUri\":null,\"forceAuthentication\":false,\"sessionExpiryTimestamp\":568425600000,\"sessionId\":{\"sessionId\":\"some-session-id\"},\"transactionSupportsEidas\":false,\"requestIssuerEntityId\":\"requestIssuerId\"}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeEidasCountrySelectedState() throws JsonProcessingException {
        EidasCountrySelectedState expectedState = anEidasCountrySelectedState().withSessionId(SESSION_ID).withRequestId(REQUEST_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.EidasCountrySelectedState\",\"countryEntityId\":null,\"relayState\":null,\"requestId\":\"some-request-id\",\"sessionExpiryTimestamp\":567994200000,\"assertionConsumerServiceUri\":\"/default-service-index\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"transactionSupportsEidas\":false,\"levelsOfAssurance\":null,\"forceAuthentication\":false,\"requestIssuerEntityId\":\"requestIssuerId\"}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeEidasAwaitingCycle3DataState() throws JsonProcessingException  {
        EidasAwaitingCycle3DataState expectedState = anEidasAwaitingCycle3DataState().withSessionId(SESSION_ID).withCountrySignedResponseContainer().build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState\",\"requestId\":\"requestId\",\"sessionExpiryTimestamp\":567994200000,\"assertionConsumerServiceUri\":\"assertionConsumerServiceUri\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"transactionSupportsEidas\":true,\"identityProviderEntityId\":\"identityProviderEntityId\",\"relayState\":\"relayState\",\"persistentId\":{\"nameId\":\"nameId\"},\"levelOfAssurance\":\"LEVEL_2\",\"encryptedIdentityAssertion\":\"encryptedIdentityAssertion\",\"forceAuthentication\":null,\"countrySignedResponseContainer\":{\"base64SamlResponse\":\"MIEBASE64STRING==\",\"base64encryptedKeys\":[\"MIEKEY==\"],\"countryEntityId\":\"http://country.com\"},\"requestIssuerEntityId\":\"requestIssuerId\",\"matchingServiceEntityId\":\"matchingServiceAdapterEntityId\"}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeAwaitingCycle3DataState() throws JsonProcessingException  {
        AwaitingCycle3DataState expectedState = anAwaitingCycle3DataState().withSessionId(SESSION_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState\",\"requestId\":\"request-id\",\"identityProviderEntityId\":\"idp entity-id\",\"sessionExpiryTimestamp\":567994200000,\"encryptedMatchingDatasetAssertion\":\"encrypted-matching-dataset-assertion\",\"authnStatementAssertion\":\"aPassthroughAssertion().buildAuthnStatementAssertion()\",\"relayState\":null,\"assertionConsumerServiceUri\":\"/default-service-uri\",\"matchingServiceEntityId\":\"matchingServiceEntityId\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"persistentId\":{\"nameId\":\"default-name-id\"},\"levelOfAssurance\":\"LEVEL_1\",\"registering\":false,\"transactionSupportsEidas\":false,\"requestIssuerEntityId\":\"transaction entity id\",\"forceAuthentication\":null}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeUserAccountCreatedState() throws JsonProcessingException { 
        UserAccountCreatedState expectedState = aUserAccountCreatedState().withSessionId(SESSION_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState\",\"requestId\":\"request-id\",\"sessionExpiryTimestamp\":567994200000,\"assertionConsumerServiceUri\":\"http://assertionconsumeruri\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"identityProviderEntityId\":\"identity-provider-id\",\"matchingServiceAssertion\":\"aPassthroughAssertion().buildMatchingServiceAssertion()\",\"relayState\":null,\"levelOfAssurance\":\"LEVEL_2\",\"registering\":false,\"transactionSupportsEidas\":false,\"requestIssuerEntityId\":\"request issuer id\",\"forceAuthentication\":null}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeMatchingServiceRequestErrorState() throws JsonProcessingException { 
        MatchingServiceRequestErrorState expectedState = aMatchingServiceRequestErrorState().withSessionId(SESSION_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.MatchingServiceRequestErrorState\",\"requestId\":\"requestId\",\"sessionExpiryTimestamp\":567994200000,\"assertionConsumerServiceUri\":\"/default-service-index\",\"identityProviderEntityId\":\"identityProviderEntityId\",\"relayState\":null,\"sessionId\":{\"sessionId\":\"some-session-id\"},\"transactionSupportsEidas\":false,\"requestIssuerEntityId\":\"requestIssuerId\",\"forceAuthentication\":null}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeEidasSuccessfulMatchState() throws JsonProcessingException { 
        EidasSuccessfulMatchState expectedState = anEidasSuccessfulMatchState().withSessionId(SESSION_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState\",\"requestId\":\"request-id\",\"sessionExpiryTimestamp\":567994200000,\"identityProviderEntityId\":\"country-entity-id\",\"matchingServiceAssertion\":\"aPassthroughAssertion().buildMatchingServiceAssertion()\",\"relayState\":\"relay state\",\"assertionConsumerServiceUri\":\"http://assertionconsumeruri\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"levelOfAssurance\":\"LEVEL_2\",\"transactionSupportsEidas\":true,\"requestIssuerEntityId\":\"request issuer id\",\"forceAuthentication\":null}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeSuccessfulMatchState() throws JsonProcessingException { 
        SuccessfulMatchState expectedState = aSuccessfulMatchState().withSessionId(SESSION_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState\",\"requestId\":\"request-id\",\"sessionExpiryTimestamp\":567994200000,\"identityProviderEntityId\":\"idp-entity-id\",\"matchingServiceAssertion\":\"aPassthroughAssertion().buildMatchingServiceAssertion()\",\"relayState\":\"relay state\",\"assertionConsumerServiceUri\":\"http://assertionconsumeruri\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"levelOfAssurance\":\"LEVEL_2\",\"isRegistering\":false,\"transactionSupportsEidas\":false,\"requestIssuerEntityId\":\"request issuer id\",\"forceAuthentication\":null,\"registering\":false}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeNoMatchState() throws JsonProcessingException { 
        NoMatchState expectedState = aNoMatchState().build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.NoMatchState\",\"requestId\":\"request ID\",\"identityProviderEntityId\":\"idp entity id\",\"sessionExpiryTimestamp\":567994200000,\"assertionConsumerServiceUri\":\"/someUri\",\"relayState\":null,\"sessionId\":{\"sessionId\":\"sessionId\"},\"transactionSupportsEidas\":false,\"requestIssuerEntityId\":\"requestIssuerId\",\"forceAuthentication\":null}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeUserAccountCreationFailedState() throws JsonProcessingException { 
        UserAccountCreationFailedState expectedState = aUserAccountCreationFailedState().withSessionId(SESSION_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.UserAccountCreationFailedState\",\"requestId\":\"requestId\",\"sessionExpiryTimestamp\":567994200000,\"assertionConsumerServiceUri\":\"/default-service-index\",\"relayState\":\"relayState\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"transactionSupportsEidas\":false,\"requestIssuerEntityId\":\"requestIssuerId\",\"forceAuthentication\":null}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeEidasUserAccountCreationFailedState() throws JsonProcessingException { 
        EidasUserAccountCreationFailedState expectedState = aEidasUserAccountCreationFailedState().withSessionId(SESSION_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationFailedState\",\"requestId\":\"requestId\",\"sessionExpiryTimestamp\":567994200000,\"assertionConsumerServiceUri\":\"/default-service-index\",\"relayState\":null,\"sessionId\":{\"sessionId\":\"some-session-id\"},\"forceAuthentication\":false,\"requestIssuerEntityId\":\"requestIssuerId\",\"transactionSupportsEidas\":true}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeIdpSelectedState() throws JsonProcessingException { 
        IdpSelectedState expectedState = anIdpSelectedState().withSessionId(SESSION_ID).withRequestId(REQUEST_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.IdpSelectedState\",\"requestId\":\"some-request-id\",\"idpEntityId\":\"idp-entity-id\",\"levelsOfAssurance\":[\"LEVEL_1\",\"LEVEL_2\"],\"useExactComparisonType\":false,\"forceAuthentication\":null,\"assertionConsumerServiceUri\":\"/default-service-uri\",\"relayState\":null,\"sessionExpiryTimestamp\":568425600000,\"registering\":false,\"requestedLoa\":\"LEVEL_2\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"availableIdentityProviders\":[\"idp-a\",\"idp-b\",\"idp-c\"],\"transactionSupportsEidas\":false,\"requestIssuerEntityId\":\"transaction-entity-id\"}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeRequesterErrorState() throws JsonProcessingException { 
        RequesterErrorState expectedState = aRequesterErrorState().withSessionId(SESSION_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.RequesterErrorState\",\"requestId\":\"requestId\",\"sessionExpiryTimestamp\":567997200000,\"assertionConsumerServiceUri\":\"assertionConsumerServiceUri\",\"relayState\":\"relayState\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"forceAuthentication\":false,\"transactionSupportsEidas\":false,\"requestIssuerEntityId\":\"authnRequestIssuerEntityId\"}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeCycle3DataInputCancelledState() throws JsonProcessingException { 
        Cycle3DataInputCancelledState expectedState = aCycle3DataInputCancelledState().withSessionId(SESSION_ID).withRequestId(REQUEST_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.Cycle3DataInputCancelledState\",\"requestId\":\"some-request-id\",\"sessionExpiryTimestamp\":567994200000,\"relayState\":null,\"assertionConsumerServiceUri\":\"/default-service-index\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"transactionSupportsEidas\":false,\"requestIssuerEntityId\":\"requestIssuerId\",\"forceAuthentication\":null}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeEidasCycle0And1MatchRequestSentState() throws JsonProcessingException { 
        EidasCycle0And1MatchRequestSentState expectedState = anEidasCycle0And1MatchRequestSentState().withCountrySignedResponseContainer().build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState\",\"requestId\":\"requestId\",\"requestIssuerEntityId\":\"requestIssuerId\",\"sessionExpiryTimestamp\":567994200000,\"assertionConsumerServiceUri\":\"assertionConsumerServiceUri\",\"sessionId\":{\"sessionId\":\"sessionId\"},\"transactionSupportsEidas\":true,\"identityProviderEntityId\":\"identityProviderEntityId\",\"relayState\":null,\"idpLevelOfAssurance\":\"LEVEL_2\",\"matchingServiceAdapterEntityId\":\"matchingServiceAdapterEntityId\",\"encryptedIdentityAssertion\":\"encryptedIdentityAssertion\",\"persistentId\":{\"nameId\":\"default-name-id\"},\"forceAuthentication\":false,\"countrySignedResponseContainer\":{\"base64SamlResponse\":\"MIEBASE64STRING==\",\"base64encryptedKeys\":[\"MIEKEY==\"],\"countryEntityId\":\"http://country.com\"},\"requestSentTime\":567993600000}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeEidasCycle3MatchRequestSentState() throws JsonProcessingException { 
        EidasCycle3MatchRequestSentState expectedState = anEidasCycle3MatchRequestSentState().withCountrySignedResponseContainer().build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.EidasCycle3MatchRequestSentState\",\"requestId\":\"requestId\",\"requestIssuerEntityId\":\"requestIssuerId\",\"sessionExpiryTimestamp\":567994200000,\"assertionConsumerServiceUri\":\"assertionConsumerServiceUri\",\"sessionId\":{\"sessionId\":\"sessionId\"},\"transactionSupportsEidas\":true,\"identityProviderEntityId\":\"identityProviderEntityId\",\"relayState\":null,\"idpLevelOfAssurance\":\"LEVEL_2\",\"matchingServiceAdapterEntityId\":\"matchingServiceAdapterEntityId\",\"encryptedIdentityAssertion\":\"encryptedIdentityAssertion\",\"persistentId\":{\"nameId\":\"default-name-id\"},\"forceAuthentication\":false,\"countrySignedResponseContainer\":{\"base64SamlResponse\":\"MIEBASE64STRING==\",\"base64encryptedKeys\":[\"MIEKEY==\"],\"countryEntityId\":\"http://country.com\"},\"requestSentTime\":567993600000}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeUserAccountCreationRequestSentState() throws JsonProcessingException { 
        UserAccountCreationRequestSentState expectedState = aUserAccountCreationRequestSentState().withSessionId(SESSION_ID).withRequestId(REQUEST_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState\",\"requestId\":\"some-request-id\",\"requestIssuerEntityId\":\"request issuer id\",\"sessionExpiryTimestamp\":567994200000,\"assertionConsumerServiceUri\":\"/default-service-index\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"transactionSupportsEidas\":false,\"identityProviderEntityId\":\"idp entity id\",\"relayState\":null,\"idpLevelOfAssurance\":\"LEVEL_1\",\"registering\":false,\"matchingServiceAdapterEntityId\":\"matchingServiceEntityId\",\"forceAuthentication\":null,\"requestSentTime\":567993600000}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeEidasUserAccountCreationRequestSentState() throws JsonProcessingException { 
        EidasUserAccountCreationRequestSentState expectedState = anEidasUserAccountCreationRequestSentState().withSessionId(SESSION_ID).withRequestId(REQUEST_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationRequestSentState\",\"requestId\":\"some-request-id\",\"requestIssuerEntityId\":\"request issuer id\",\"sessionExpiryTimestamp\":567994200000,\"assertionConsumerServiceUri\":\"/default-service-index\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"identityProviderEntityId\":\"idp entity id\",\"relayState\":null,\"idpLevelOfAssurance\":\"LEVEL_2\",\"matchingServiceAdapterEntityId\":\"matchingServiceEntityId\",\"forceAuthentication\":null,\"transactionSupportsEidas\":true,\"requestSentTime\":567993600000}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeCycle0And1MatchRequestSentState() throws JsonProcessingException { 
        Cycle0And1MatchRequestSentState expectedState = aCycle0And1MatchRequestSentState().withSessionId(SESSION_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState\",\"requestId\":\"requestId\",\"requestIssuerEntityId\":\"request-issuer-id\",\"sessionExpiryTimestamp\":567994200000,\"assertionConsumerServiceUri\":\"default-service-uri\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"transactionSupportsEidas\":false,\"registering\":false,\"identityProviderEntityId\":\"idp-entity-id\",\"relayState\":null,\"idpLevelOfAssurance\":\"LEVEL_1\",\"matchingServiceAdapterEntityId\":\"matching-service-entityId\",\"encryptedMatchingDatasetAssertion\":\"encrypted-matching-dataset-assertion\",\"authnStatementAssertion\":\"aPassthroughAssertion().buildAuthnStatementAssertion()\",\"persistentId\":{\"nameId\":\"default-name-id\"},\"forceAuthentication\":null,\"requestSentTime\":567993600000}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeCycle3MatchRequestSentState() throws JsonProcessingException { 
        Cycle3MatchRequestSentState expectedState = aCycle3MatchRequestSentState().withRequestId(REQUEST_ID).withSessionId(SESSION_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentState\",\"requestId\":\"some-request-id\",\"requestIssuerEntityId\":\"request issuer id\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"transactionSupportsEidas\":false,\"identityProviderEntityId\":\"idp entity id\",\"relayState\":null,\"idpLevelOfAssurance\":\"LEVEL_1\",\"registering\":false,\"matchingServiceAdapterEntityId\":\"matchingServiceEntityId\",\"encryptedMatchingDatasetAssertion\":\"encrypted-matching-dataset-assertion\",\"authnStatementAssertion\":\"aPassthroughAssertion().buildAuthnStatementAssertion()\",\"persistentId\":{\"nameId\":\"default-name-id\"},\"sessionExpiryTimestamp\":567994200000,\"assertionConsumerServiceUri\":\"/default-service-index\",\"forceAuthentication\":null,\"requestSentTime\":567993600000}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializePausedRegistrationState() throws JsonProcessingException { 
        PausedRegistrationState expectedState = aPausedRegistrationState().build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.PausedRegistrationState\",\"requestId\":\"some-request-id\",\"sessionExpiryTimestamp\":567993600000,\"assertionConsumerServiceUri\":\"urn:some:assertion:consumer:service\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"transactionSupportsEidas\":true,\"relayState\":\"some-relay-state\",\"requestIssuerEntityId\":\"some-request-issuer-id\",\"forceAuthentication\":null}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeTimeoutState() throws JsonProcessingException { 
        TimeoutState expectedState = aTimeoutState().withSessionId(SESSION_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.TimeoutState\",\"requestId\":\"requestId\",\"sessionExpiryTimestamp\":567997200000,\"assertionConsumerServiceUri\":\"assertionConsumerServiceUri\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"transactionSupportsEidas\":false,\"requestIssuerEntityId\":\"requestId\",\"forceAuthentication\":null,\"relayState\":null}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeAuthnFailedErrorState() throws JsonProcessingException { 
        AuthnFailedErrorState expectedState = anAuthnFailedErrorState().withRequestId(REQUEST_ID).withSessionId(SESSION_ID).withForceAuthentication(null).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState\",\"requestId\":\"some-request-id\",\"sessionExpiryTimestamp\":567994200000,\"assertionConsumerServiceUri\":\"/default-service-index\",\"relayState\":null,\"sessionId\":{\"sessionId\":\"some-session-id\"},\"idpEntityId\":\"IDP Entity ID\",\"forceAuthentication\":null,\"transactionSupportsEidas\":false,\"requestIssuerEntityId\":\"requestIssuerId\"}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeCountryAuthnFailedErrorState() throws JsonProcessingException { 
        EidasAuthnFailedErrorState expectedState = anEidasAuthnFailedErrorState().withRequestId(REQUEST_ID).withSessionId(SESSION_ID).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.EidasAuthnFailedErrorState\",\"requestId\":\"some-request-id\",\"sessionExpiryTimestamp\":567994200000,\"assertionConsumerServiceUri\":\"/default-service-index\",\"relayState\":null,\"sessionId\":{\"sessionId\":\"some-session-id\"},\"countryEntityId\":\"https://stub_country.acme.eu/stub-country-one/ServiceMetadata\",\"levelsOfAssurance\":[\"LEVEL_2\"],\"forceAuthentication\":false,\"requestIssuerEntityId\":\"requestIssuerId\",\"transactionSupportsEidas\":true}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeFraudEventDetectedState() throws JsonProcessingException { 
        FraudEventDetectedState expectedState = aFraudEventDetectedState().withSessionId(SESSION_ID).withForceAuthentication(null).build();
        String actual = objectMapper.writeValueAsString(expectedState);
        String expected = "{\"@class\":\"uk.gov.ida.hub.policy.domain.state.FraudEventDetectedState\",\"requestId\":\"requestId\",\"sessionExpiryTimestamp\":567997200000,\"assertionConsumerServiceUri\":\"assertionConsumerServiceUri\",\"relayState\":\"relayState\",\"sessionId\":{\"sessionId\":\"some-session-id\"},\"idpEntityId\":\"idpEntityId\",\"forceAuthentication\":null,\"transactionSupportsEidas\":false,\"requestIssuerEntityId\":\"requestId\"}";
        assertThat(actual).isEqualTo(expected);
    }

    private ObjectMapper getRedisObjectMapper() {
        return new ObjectMapper()
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModule(new JodaModule())
                .registerModule(new Jdk8Module())
                .registerModule(new GuavaModule());
    }
}
