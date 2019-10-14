package uk.gov.ida.hub.policy.domain.controller;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.common.shared.security.IdGenerator;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.Cycle3AttributeRequestData;
import uk.gov.ida.hub.policy.domain.Cycle3Dataset;
import uk.gov.ida.hub.policy.domain.MatchingProcess;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingStatus;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.TransactionIdaStatus;
import uk.gov.ida.hub.policy.domain.state.Cycle3DataInputCancelledState;
import uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.net.URI;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.MatchingServiceConfigEntityDataDtoBuilder.aMatchingServiceConfigEntityDataDto;
import static uk.gov.ida.hub.policy.builder.state.EidasAwaitingCycle3DataStateBuilder.anEidasAwaitingCycle3DataState;

@RunWith(MockitoJUnitRunner.class)
public class EidasAwaitingCycle3DataStateControllerTest {

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;

    @Mock
    private HubEventLogger hubEventLogger;

    @Mock
    private StateTransitionAction stateTransitionAction;

    @Mock
    private PolicyConfiguration policyConfiguration;

    @Mock
    private AssertionRestrictionsFactory assertionRestrictionsFactory;

    @Mock
    private MatchingServiceConfigProxy matchingServiceConfigProxy;

    private static final DateTime NOW = DateTime.now();
    private static final String CYCLE_3_ATTRIBUTE_NAME = "cycle3AttributeName";
    private static final String RESPONSE_ID = "responseId";
    private EidasAwaitingCycle3DataStateController controller;
    private EidasAwaitingCycle3DataState state;

    @Before
    public void setUp() {
        DateTimeUtils.setCurrentMillisFixed(NOW.getMillis());
        ResponseFromHubFactory responseFromHubFactory = new ResponseFromHubFactory(idGenerator);
        state = anEidasAwaitingCycle3DataState().build();
        controller = new EidasAwaitingCycle3DataStateController(
            state,
            hubEventLogger,
            stateTransitionAction,
            transactionsConfigProxy,
            responseFromHubFactory,
            policyConfiguration,
            assertionRestrictionsFactory,
            matchingServiceConfigProxy);
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void getResponseProcessingDetails() {
        ResponseProcessingDetails expectedResponse = new ResponseProcessingDetails(
            state.getSessionId(),
            ResponseProcessingStatus.GET_C3_DATA,
            state.getRequestIssuerEntityId()
        );

        ResponseProcessingDetails actualResponse = controller.getResponseProcessingDetails();

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    public void getErrorResponse() {
        when(idGenerator.getId()).thenReturn(RESPONSE_ID);
        ResponseFromHub expectedResponse = new ResponseFromHub(
            RESPONSE_ID,
            state.getRequestId(),
            state.getRequestIssuerEntityId(),
            emptyList(),
            Optional.of("relayState"),
            URI.create("assertionConsumerServiceUri"),
            TransactionIdaStatus.NoAuthenticationContext
        );
        ResponseFromHub actualResponse = controller.getErrorResponse();

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    public void getCycle3AttributeRequestData() {
        final MatchingProcess matchingProcess = new MatchingProcess(Optional.of(CYCLE_3_ATTRIBUTE_NAME));
        when(transactionsConfigProxy.getMatchingProcess(state.getRequestIssuerEntityId())).thenReturn(matchingProcess);
        final Cycle3AttributeRequestData expectedData = new Cycle3AttributeRequestData(CYCLE_3_ATTRIBUTE_NAME, state.getRequestIssuerEntityId());

        final Cycle3AttributeRequestData actualData = controller.getCycle3AttributeRequestData();

        assertThat(actualData).isEqualTo(expectedData);
    }

    @Test
    public void handleCancellation() {
        doNothing().when(hubEventLogger).logCycle3DataInputCancelled(
            state.getSessionId(),
            state.getRequestIssuerEntityId(),
            state.getSessionExpiryTimestamp(),
            state.getRequestId());
        final Cycle3DataInputCancelledState expectedState = new Cycle3DataInputCancelledState(
            state.getRequestId(),
            state.getSessionExpiryTimestamp(),
            state.getRelayState().orElse(null),
            state.getRequestIssuerEntityId(),
            state.getAssertionConsumerServiceUri(),
            new SessionId(state.getSessionId().getSessionId()),
            state.getTransactionSupportsEidas());

        controller.handleCancellation();

        verify(stateTransitionAction).transitionTo(refEq(expectedState));
    }

    @Test
    public void createAttributeQuery() {
        final Cycle3Dataset cycle3Dataset = Cycle3Dataset.createFromData("attribute", "attributeValue");
        final MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =  aMatchingServiceConfigEntityDataDto()
            .withEntityId(state.getMatchingServiceEntityId())
            .build();
        when(matchingServiceConfigProxy.getMatchingService(state.getMatchingServiceEntityId())).thenReturn(matchingServiceConfigEntityDataDto);
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(Duration.standardMinutes(60));
        when(assertionRestrictionsFactory.getAssertionExpiry()).thenReturn(DateTime.now().plusHours(2));
        final EidasAttributeQueryRequestDto expectedDto = new EidasAttributeQueryRequestDto(
            state.getRequestId(),
            state.getRequestIssuerEntityId(),
            state.getAssertionConsumerServiceUri(),
            assertionRestrictionsFactory.getAssertionExpiry(),
            state.getMatchingServiceEntityId(),
            matchingServiceConfigEntityDataDto.getUri(),
            DateTime.now().plus(policyConfiguration.getMatchingServiceResponseWaitPeriod()),
            matchingServiceConfigEntityDataDto.isOnboarding(),
            state.getLevelOfAssurance(),
            state.getPersistentId(),
            Optional.of(cycle3Dataset),
            Optional.empty(),
            state.getEncryptedIdentityAssertion(),
            Optional.empty()
        );

        EidasAttributeQueryRequestDto actualDto = controller.createAttributeQuery(cycle3Dataset);

        assertThat(actualDto).isEqualTo(expectedDto);
    }

    @Test
    public void shouldTransitionToEidasCycle3MatchRequestSentState() {
        final String principalIpAddressAsSeenByHub = "principalIpAddressAsSeenByHub";
        doNothing().when(hubEventLogger).logCycle3DataObtained(
            state.getSessionId(),
            state.getRequestIssuerEntityId(),
            state.getSessionExpiryTimestamp(),
            state.getRequestId(),
            principalIpAddressAsSeenByHub
        );
        final EidasCycle3MatchRequestSentState expectedState = new EidasCycle3MatchRequestSentState(
            state.getRequestId(),
            state.getRequestIssuerEntityId(),
            state.getSessionExpiryTimestamp(),
            state.getAssertionConsumerServiceUri(),
            state.getSessionId(),
            state.getTransactionSupportsEidas(),
            state.getIdentityProviderEntityId(),
            state.getRelayState().orElse(null),
            state.getLevelOfAssurance(),
            state.getMatchingServiceEntityId(),
            state.getEncryptedIdentityAssertion(),
            state.getPersistentId(),
            state.getForceAuthentication().orElse(null)
        );

        controller.handleCycle3DataSubmitted(principalIpAddressAsSeenByHub);

        verify(stateTransitionAction).transitionTo(expectedState);
    }
}
