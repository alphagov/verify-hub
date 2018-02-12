package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.CountrySelectedState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.EidasAttributeQueryRequestDtoBuilder.anEidasAttributeQueryRequestDto;
import static uk.gov.ida.hub.policy.builder.state.CountrySelectedStateBuilder.aCountrySelectedState;

@RunWith(MockitoJUnitRunner.class)
public class CountrySelectedStateControllerTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private EventSinkHubEventLogger eventSinkHubEventLogger;

    @Mock
    private StateTransitionAction stateTransitionAction;

    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;

    private static final String MSA_ID = "msa-id";
    private static final String COUNTRY_ENTITY_ID = "foo";
    private static final DateTime NOW = DateTime.now(DateTimeZone.UTC);
    private CountrySelectedState state = aCountrySelectedState()
            .withSelectedCountry(COUNTRY_ENTITY_ID)
            .withLevelOfAssurance(ImmutableList.of(LevelOfAssurance.LEVEL_2))
            .build();
    private CountrySelectedStateController controller;

    @Before
    public void setUp() {
        DateTimeUtils.setCurrentMillisFixed(NOW.getMillis());
        controller = new CountrySelectedStateController(state, eventSinkHubEventLogger, stateTransitionAction, transactionsConfigProxy);
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldThrowIfCountryNotInListSupplied() {
        exception.expect(StateProcessingValidationException.class);
        exception.expectMessage(String.format("Country with entity id %s is not enabled for eidas", COUNTRY_ENTITY_ID));
        List<EidasCountryDto> enabledCountries = ImmutableList.of(new EidasCountryDto("ID1", "ID1", true), new EidasCountryDto("ID2", "ID2", true));

        controller.validateCountryIsIn(enabledCountries);
    }

    @Test
    public void shouldNotThrowIfCountryInListSupplied() {
        List<EidasCountryDto> enabledCountries = ImmutableList.of(new EidasCountryDto(COUNTRY_ENTITY_ID, COUNTRY_ENTITY_ID, true), new EidasCountryDto("ID2", "ID2", true));

        controller.validateCountryIsIn(enabledCountries);
    }

    @Test
    public void shouldReturnMatchingServiceEntityIdWhenAsked() throws Exception {
        when(transactionsConfigProxy.getMatchingServiceEntityId(COUNTRY_ENTITY_ID)).thenReturn(MSA_ID);
        controller.getMatchingServiceEntityId();
        verify(transactionsConfigProxy).getMatchingServiceEntityId(state.getRequestIssuerEntityId());
    }

    @Test
    public void shouldValidateAbsentLoa() {
        exception.expect(StateProcessingValidationException.class);
        exception.expectMessage(String.format("Level of assurance in the response does not match level of assurance in the request. Was [null] but expected [%s].", ImmutableList.of(LevelOfAssurance.LEVEL_2)));

        controller.validateLevelOfAssurance(Optional.empty());
    }

    @Test
    public void shouldValidateIncorrectLoa() {
        exception.expect(StateProcessingValidationException.class);
        exception.expectMessage(String.format("Level of assurance in the response does not match level of assurance in the request. Was [%s] but expected [%s].", LevelOfAssurance.LEVEL_1, ImmutableList.of(LevelOfAssurance.LEVEL_2)));

        controller.validateLevelOfAssurance(Optional.of(LevelOfAssurance.LEVEL_1));
    }

    @Test
    public void shouldNotThrowWhenValidatingCorrectLoa() {
        controller.validateLevelOfAssurance(Optional.of(LevelOfAssurance.LEVEL_2));
    }

    @Test
    public void shouldTransitionToEidasCycle0And1MatchRequestSentState() {
        final String ipAddress = "ip-address";
        when(transactionsConfigProxy.getMatchingServiceEntityId(state.getRequestIssuerEntityId())).thenReturn(MSA_ID);
        EidasAttributeQueryRequestDto eidasAttributeQueryRequestDto = anEidasAttributeQueryRequestDto().build();
        EidasCycle0And1MatchRequestSentState eidasCycle0And1MatchRequestSentState = new EidasCycle0And1MatchRequestSentState(
            state.getRequestId(),
            state.getRequestIssuerEntityId(),
            state.getSessionExpiryTimestamp(),
            state.getAssertionConsumerServiceUri(),
            new SessionId(state.getSessionId().getSessionId()),
            state.getTransactionSupportsEidas(),
            COUNTRY_ENTITY_ID,
            state.getRelayState().orNull(),
            eidasAttributeQueryRequestDto.getLevelOfAssurance(),
            MSA_ID,
            eidasAttributeQueryRequestDto.getEncryptedIdentityAssertion(),
            eidasAttributeQueryRequestDto.getPersistentId()
        );

        controller.transitionToEidasCycle0And1MatchRequestSentState(eidasAttributeQueryRequestDto, ipAddress, COUNTRY_ENTITY_ID);

        verify(eventSinkHubEventLogger).logIdpAuthnSucceededEvent(
            state.getSessionId(),
            state.getSessionExpiryTimestamp(),
            state.getCountryEntityId(),
            state.getRequestIssuerEntityId(),
            eidasAttributeQueryRequestDto.getPersistentId(),
            state.getRequestId(),
            state.getLevelsOfAssurance().get(0),
            state.getLevelsOfAssurance().get(state.getLevelsOfAssurance().size() - 1),
            eidasAttributeQueryRequestDto.getLevelOfAssurance(),
            com.google.common.base.Optional.absent(),
            ipAddress);
        verify(stateTransitionAction).transitionTo(eidasCycle0And1MatchRequestSentState);
    }
}
