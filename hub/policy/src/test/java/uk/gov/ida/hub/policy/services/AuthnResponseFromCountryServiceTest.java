package uk.gov.ida.hub.policy.services;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.builder.SamlAuthnResponseTranslatorDtoBuilder;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseContainerDto;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.policy.domain.CountryAuthenticationStatus.Status;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.InboundResponseFromCountry;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.ResponseAction;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.controller.EidasCountrySelectedStateController;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectedState;
import uk.gov.ida.hub.policy.exception.InvalidSessionStateException;
import uk.gov.ida.hub.policy.factories.SamlAuthnResponseTranslatorDtoFactory;
import uk.gov.ida.hub.policy.proxy.AttributeQueryRequest;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;
import uk.gov.ida.hub.policy.proxy.SamlSoapProxyProxy;

import java.net.URI;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.SamlAuthnResponseContainerDtoBuilder.aSamlAuthnResponseContainerDto;
import static uk.gov.ida.hub.policy.domain.LevelOfAssurance.LEVEL_2;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.NON_MATCHING_JOURNEY_SUCCESS;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.OTHER;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.SUCCESS;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_ONE;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

@RunWith(MockitoJUnitRunner.class)
public class AuthnResponseFromCountryServiceTest {

    private static final DateTime TIMESTAMP = DateTime.now();
    private static final URI ASSERTION_CONSUMER_SERVICE_URI = URI.create("assertion-consumer-service-uri");
    private static final SessionId SESSION_ID = SessionIdBuilder.aSessionId().build();
    private static final String REQUEST_ID = "requestId";
    private static final String PID = "pid";
    private static final String BLOB = "blob";
    private static final String SAML_REQUEST = "SAML";
    private static final URI MSA_URI = URI.create("matching-service-uri");
    private static final boolean IS_ONBOARDING = true;
    private static final Duration MATCHING_SERVICE_RESPONSE_WAIT_PERIOD = Duration.standardMinutes(60);
    private static final Duration ASSERTION_EXPIRY = Duration.standardMinutes(60);
    private static final String COUNTRY_ENTITY_ID = "country_entity_id";
    private static final EidasCountryDto EIDAS_COUNTRY_DTO = new EidasCountryDto(COUNTRY_ENTITY_ID, "country_x", true);
    private static final String ANALYTICS_SESSION_ID = "some-session-id";
    private static final String JOURNEY_TYPE = "some-journey-type";
    private static final SamlAuthnResponseContainerDto SAML_AUTHN_RESPONSE_CONTAINER_DTO = aSamlAuthnResponseContainerDto().withSessionId(SESSION_ID).withPrincipalIPAddressAsSeenByHub("1.1.1.1").withAnalyticsSessionId(ANALYTICS_SESSION_ID).withJourneyType(JOURNEY_TYPE).build();
    private static final SamlAuthnResponseTranslatorDto SAML_AUTHN_RESPONSE_TRANSLATOR_DTO = SamlAuthnResponseTranslatorDtoBuilder.aSamlAuthnResponseTranslatorDto().build();
    private static final SamlAuthnResponseTranslatorDto NON_MATCHING_SAML_AUTHN_RESPONSE_TRANSLATOR_DTO = SamlAuthnResponseTranslatorDtoBuilder.aSamlAuthnResponseTranslatorDto().withMatchingServiceEntityId(TEST_RP).build();

    private static final InboundResponseFromCountry INBOUND_RESPONSE_FROM_COUNTRY = new InboundResponseFromCountry(
        Status.Success,
        Optional.absent(),
        STUB_IDP_ONE,
        Optional.of(BLOB),
        Optional.of(PID),
        Optional.of(LEVEL_2),
        Optional.absent());

    private static final EidasAttributeQueryRequestDto EIDAS_ATTRIBUTE_QUERY_REQUEST_DTO = new EidasAttributeQueryRequestDto(
        REQUEST_ID,
        TEST_RP,
        ASSERTION_CONSUMER_SERVICE_URI,
        TIMESTAMP.plus(ASSERTION_EXPIRY),
        TEST_RP_MS,
        MSA_URI,
        TIMESTAMP.plus(MATCHING_SERVICE_RESPONSE_WAIT_PERIOD),
        IS_ONBOARDING,
        LEVEL_2,
        new PersistentId(PID),
        Optional.absent(),
        Optional.absent(),
        BLOB
    );

    private static final AttributeQueryContainerDto ATTRIBUTE_QUERY_CONTAINER_DTO = new AttributeQueryContainerDto(
        SAML_REQUEST,
        MSA_URI,
        REQUEST_ID,
        TIMESTAMP,
        TEST_RP,
        IS_ONBOARDING);

    private static final AttributeQueryRequest ATTRIBUTE_QUERY_REQUEST = new AttributeQueryRequest(
        REQUEST_ID,
        TEST_RP,
        SAML_REQUEST,
        MSA_URI,
        TIMESTAMP,
        IS_ONBOARDING);

    private AuthnResponseFromCountryService service;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private SamlAuthnResponseTranslatorDtoFactory samlAuthnResponseTranslatorDtoFactory;

    @Mock
    private SamlEngineProxy samlEngineProxy;

    @Mock
    private SamlSoapProxyProxy samlSoapProxyProxy;

    @Mock
    private EidasCountrySelectedStateController stateController;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private CountriesService countriesService;

    @Before
    public void setup() {
        DateTimeUtils.setCurrentMillisFixed(TIMESTAMP.getMillis());
        service = new AuthnResponseFromCountryService(
            samlEngineProxy,
            samlSoapProxyProxy,
            sessionRepository,
            samlAuthnResponseTranslatorDtoFactory,
            countriesService);
        when(sessionRepository.getStateController(SESSION_ID, EidasCountrySelectedState.class)).thenReturn(stateController);
        when(stateController.getMatchingServiceEntityId()).thenReturn(TEST_RP_MS);
        when(stateController.getCountryEntityId()).thenReturn(COUNTRY_ENTITY_ID);
        when(stateController.getRequestIssuerId()).thenReturn(TEST_RP);
        when(stateController.isMatchingJourney()).thenReturn(true);
        when(stateController.getEidasAttributeQueryRequestDto(any())).thenReturn(EIDAS_ATTRIBUTE_QUERY_REQUEST_DTO);
        when(countriesService.getCountries(any())).thenReturn(Collections.singletonList(EIDAS_COUNTRY_DTO));
        when(samlAuthnResponseTranslatorDtoFactory.fromSamlAuthnResponseContainerDto(SAML_AUTHN_RESPONSE_CONTAINER_DTO, TEST_RP_MS)).thenReturn(SAML_AUTHN_RESPONSE_TRANSLATOR_DTO);
        when(samlAuthnResponseTranslatorDtoFactory.fromSamlAuthnResponseContainerDto(SAML_AUTHN_RESPONSE_CONTAINER_DTO, TEST_RP)).thenReturn(NON_MATCHING_SAML_AUTHN_RESPONSE_TRANSLATOR_DTO);
        when(samlEngineProxy.translateAuthnResponseFromCountry(SAML_AUTHN_RESPONSE_TRANSLATOR_DTO)).thenReturn(INBOUND_RESPONSE_FROM_COUNTRY);
        when(samlEngineProxy.generateEidasAttributeQuery(EIDAS_ATTRIBUTE_QUERY_REQUEST_DTO)).thenReturn(ATTRIBUTE_QUERY_CONTAINER_DTO);
    }

    @After
    public void teardown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldCheckAnEidasResponseIsExpectedWhenSuccessfulResponseIsReceived() {
        ResponseAction responseAction = service.receiveAuthnResponseFromCountry(SESSION_ID, SAML_AUTHN_RESPONSE_CONTAINER_DTO);

        verify(sessionRepository).getStateController(SESSION_ID, EidasCountrySelectedState.class);
        verify(samlAuthnResponseTranslatorDtoFactory).fromSamlAuthnResponseContainerDto(SAML_AUTHN_RESPONSE_CONTAINER_DTO, TEST_RP_MS);
        verify(samlEngineProxy).generateEidasAttributeQuery(EIDAS_ATTRIBUTE_QUERY_REQUEST_DTO);
        verify(stateController).handleMatchingJourneySuccessResponseFromCountry(INBOUND_RESPONSE_FROM_COUNTRY, SAML_AUTHN_RESPONSE_CONTAINER_DTO.getPrincipalIPAddressAsSeenByHub(), ANALYTICS_SESSION_ID, JOURNEY_TYPE);
        verify(samlSoapProxyProxy).sendHubMatchingServiceRequest(SESSION_ID, ATTRIBUTE_QUERY_REQUEST);
        ResponseAction expectedResponseAction = ResponseAction.success(SESSION_ID, false, LEVEL_2, null);
        assertThat(responseAction).isEqualToComparingFieldByField(expectedResponseAction);
    }

    @Test(expected = InvalidSessionStateException.class)
    public void shouldThrowAnExceptionWhenSuccessfulResponseIsReceivedAndIsInInvalidState() {
        when(sessionRepository.getStateController(SESSION_ID, EidasCountrySelectedState.class)).thenThrow(InvalidSessionStateException.class);
        service.receiveAuthnResponseFromCountry(SESSION_ID, SAML_AUTHN_RESPONSE_CONTAINER_DTO);
    }

    @Test(expected = StateProcessingValidationException.class)
    public void shouldThrowAnExceptionWhenSuccessfulResponseIsReceivedAndCountryIsDisabled() {
        when(countriesService.getCountries(any())).thenReturn(Collections.emptyList());

        service.receiveAuthnResponseFromCountry(SESSION_ID, SAML_AUTHN_RESPONSE_CONTAINER_DTO);
    }

    @Test
    public void shouldReturnSuccessResponseIfTranslationResponseFromSamlEngineIsSuccessfulAndUsingMatching() {
        final InboundResponseFromCountry inboundResponseFromCountry =
                new InboundResponseFromCountry(Status.Success,
                                               Optional.of("status"),
                                               "issuer",
                                               Optional.of("blob"),
                                               Optional.of("pid"),
                                               Optional.of(LEVEL_2),
                                               Optional.absent());

        when(samlEngineProxy.translateAuthnResponseFromCountry(SAML_AUTHN_RESPONSE_TRANSLATOR_DTO))
                .thenReturn(inboundResponseFromCountry);

        ResponseAction responseAction = service.receiveAuthnResponseFromCountry(SESSION_ID, SAML_AUTHN_RESPONSE_CONTAINER_DTO);

        verify(stateController).handleMatchingJourneySuccessResponseFromCountry(inboundResponseFromCountry, SAML_AUTHN_RESPONSE_CONTAINER_DTO.getPrincipalIPAddressAsSeenByHub(), ANALYTICS_SESSION_ID, JOURNEY_TYPE);
        assertThat(responseAction.getResult()).isEqualTo(SUCCESS);
    }

    @Test
    public void shouldReturnNonMatchingJourneySuccessResponseIfTranslationResponseFromSamlEngineIsSuccessfulAndNotUsingMatching() {
        final InboundResponseFromCountry inboundResponseFromCountry =
                new InboundResponseFromCountry(Status.Success, Optional.of("status"), "issuer", Optional.of("blob-encrypted-for-test-rp"), Optional.of("pid"), Optional.of(LEVEL_2), Optional.absent());

        when(stateController.isMatchingJourney()).thenReturn(false);
        when(samlEngineProxy.translateAuthnResponseFromCountry(NON_MATCHING_SAML_AUTHN_RESPONSE_TRANSLATOR_DTO))
                .thenReturn(inboundResponseFromCountry);

        ResponseAction responseAction = service.receiveAuthnResponseFromCountry(SESSION_ID, SAML_AUTHN_RESPONSE_CONTAINER_DTO);

        verify(samlAuthnResponseTranslatorDtoFactory).fromSamlAuthnResponseContainerDto(any(), eq(TEST_RP));
        verify(stateController).handleNonMatchingJourneySuccessResponseFromCountry(inboundResponseFromCountry, SAML_AUTHN_RESPONSE_CONTAINER_DTO.getPrincipalIPAddressAsSeenByHub(), ANALYTICS_SESSION_ID, JOURNEY_TYPE);
        assertThat(responseAction.getResult()).isEqualTo(NON_MATCHING_JOURNEY_SUCCESS);
    }

    @Test
    public void shouldReturnOtherResponseIfTranslationResponseFromSamlEngineIsFailure() {
        when(samlEngineProxy.translateAuthnResponseFromCountry(SAML_AUTHN_RESPONSE_TRANSLATOR_DTO))
                .thenReturn(new InboundResponseFromCountry(Status.Failure,
                                                           Optional.of("status"),
                                                           "issuer",
                                                           Optional.of("blob"),
                                                           Optional.of("pid"),
                                                           Optional.of(LEVEL_2),
                                                           Optional.absent()));

        ResponseAction responseAction = service.receiveAuthnResponseFromCountry(SESSION_ID, SAML_AUTHN_RESPONSE_CONTAINER_DTO);

        verify(stateController).handleAuthenticationFailedResponseFromCountry(SAML_AUTHN_RESPONSE_CONTAINER_DTO.getPrincipalIPAddressAsSeenByHub(), ANALYTICS_SESSION_ID, JOURNEY_TYPE);
        assertThat(responseAction.getResult()).isEqualTo(OTHER);
    }
}
