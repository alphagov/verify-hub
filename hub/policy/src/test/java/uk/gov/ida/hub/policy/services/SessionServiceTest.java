package uk.gov.ida.hub.policy.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.policy.builder.SamlAuthnRequestContainerDtoBuilder;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.policy.contracts.SamlMessageDto;
import uk.gov.ida.hub.policy.contracts.SamlRequestDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.controllogic.AuthnRequestFromTransactionHandler;
import uk.gov.ida.hub.policy.domain.AuthnRequestFromHub;
import uk.gov.ida.hub.policy.domain.AuthnRequestFromHubContainerDto;
import uk.gov.ida.hub.policy.domain.IdaAuthnRequestFromHubDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResourceLocation;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.exception.SessionCreationFailureException;
import uk.gov.ida.hub.policy.domain.exception.SessionNotFoundException;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectedState;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.AuthnResponseFromHubContainerDtoBuilder.anAuthnResponseFromHubContainerDto;
import static uk.gov.ida.hub.policy.builder.domain.AuthnRequestFromHubBuilder.anAuthnRequestFromHub;
import static uk.gov.ida.hub.policy.builder.domain.ResponseFromHubBuilder.aResponseFromHubDto;
import static uk.gov.ida.hub.policy.domain.SessionId.createNewSessionId;
import static uk.gov.ida.hub.policy.proxy.SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto;

@RunWith(MockitoJUnitRunner.class)
public class SessionServiceTest {
    @Mock
    private AuthnRequestFromTransactionHandler authnRequestHandler;
    @Mock
    private SamlEngineProxy samlEngineProxy;
    @Mock
    private TransactionsConfigProxy configProxy;
    @Mock
    private SessionRepository sessionRepository;

    private SessionService service;

    private final SamlAuthnRequestContainerDto requestDto = SamlAuthnRequestContainerDtoBuilder.aSamlAuthnRequestContainerDto().build();

    @Before
    public void setUp() {
        service = new SessionService(samlEngineProxy, configProxy, authnRequestHandler, sessionRepository);
    }

    @Test
    public void shouldCreateASessionFromInputs() {
        // Given
        SamlResponseWithAuthnRequestInformationDto samlResponse = aSamlResponseWithAuthnRequestInformationDto().build();
        URI assertionConsumerServiceUri = UriBuilder.fromUri(UUID.randomUUID().toString()).build();

        final SessionId sessionId = SessionIdBuilder.aSessionId().with("coffee-pasta").build();

        givenSamlEngineTranslatesRequest(samlResponse);
        givenConfigReturnsAssertionConsumerServiceURLFor(samlResponse, assertionConsumerServiceUri);
        givenSessionIsCreated(samlResponse, assertionConsumerServiceUri, sessionId, false);

        // When
        SessionId result = service.create(requestDto);

        // Then
        assertThat(result, is(sessionId));
    }

    @Test(expected = SessionCreationFailureException.class)
    public void shouldThrowSessionCreationFailureExceptionIfCallToConfigServiceThrowsExceptionBecauseAssertionConsumerServiceUriIsInvalid() {
        SamlResponseWithAuthnRequestInformationDto samlResponse = aSamlResponseWithAuthnRequestInformationDto().build();

        givenSamlEngineTranslatesRequest(samlResponse);
        when(configProxy.getAssertionConsumerServiceUri(samlResponse.getIssuer(), samlResponse.getAssertionConsumerServiceIndex()))
                .thenThrow(new WebApplicationException());

        service.create(requestDto);
    }

    @Test(expected = SessionCreationFailureException.class)
    public void shouldThrowSessionCreationFailureExceptionIfProvidedAssertionConsumerServiceUrlDoesntMatch() {
        SamlResponseWithAuthnRequestInformationDto samlResponse = aSamlResponseWithAuthnRequestInformationDto()
                .withAssertionConsumerServiceUrl(URI.create("http://wrongurl"))
                .build();
        URI assertionConsumerServiceUri = UriBuilder.fromUri(UUID.randomUUID().toString()).build();

        final SessionId sessionId = SessionIdBuilder.aSessionId().with("coffee-pasta").build();

        givenSamlEngineTranslatesRequest(samlResponse);
        givenConfigReturnsAssertionConsumerServiceURLFor(samlResponse, assertionConsumerServiceUri);
        givenSessionIsCreated(samlResponse, assertionConsumerServiceUri, sessionId, false);

        service.create(requestDto);
    }

    @Test
    public void shouldCreateSessionIfProvidedAssertionConsumerServiceUrlMatches() {
        URI assertionConsumerServiceUri = UriBuilder.fromUri(UUID.randomUUID().toString()).build();
        SamlResponseWithAuthnRequestInformationDto samlResponse = aSamlResponseWithAuthnRequestInformationDto()
                .withAssertionConsumerServiceUrl(assertionConsumerServiceUri)
                .build();

        final SessionId sessionId = SessionIdBuilder.aSessionId().with("coffee-pasta").build();

        givenSamlEngineTranslatesRequest(samlResponse);
        givenConfigReturnsAssertionConsumerServiceURLFor(samlResponse, assertionConsumerServiceUri);
        givenSessionIsCreated(samlResponse, assertionConsumerServiceUri, sessionId, false);

        SessionId result = service.create(requestDto);

        assertThat(result, is(sessionId));
    }

    @Test
    public void shouldCreateSessionWithTransactionSupportsEidas() {
        // Given
        URI assertionConsumerServiceUri = UriBuilder.fromUri(UUID.randomUUID().toString()).build();
        SamlResponseWithAuthnRequestInformationDto samlResponse = aSamlResponseWithAuthnRequestInformationDto().build();
        final SessionId sessionId = SessionIdBuilder.aSessionId().with("coffee-pasta").build();

        givenSamlEngineTranslatesRequest(samlResponse);
        givenConfigReturnsAssertionConsumerServiceURLFor(samlResponse, assertionConsumerServiceUri);
        givenConfigReturnsTransactionSupportsEidas(samlResponse, true);
        givenSessionIsCreated(samlResponse, assertionConsumerServiceUri, sessionId, true);

        // When
        SessionId result = service.create(requestDto);

        // Then
        assertThat(result, is(sessionId));
        verify(authnRequestHandler).handleRequestFromTransaction(samlResponse, requestDto.getRelayState(), requestDto.getPrincipalIPAddressAsSeenByHub(), assertionConsumerServiceUri, true);

    }

    @Test
    public void shouldGetACountryAuthnRequestWithOverriddenSsoUrl() {
        // Given
        SessionId sessionId = createNewSessionId();
        when(sessionRepository.sessionExists(sessionId)).thenReturn(true);
        when(sessionRepository.isSessionInState(sessionId, EidasCountrySelectedState.class)).thenReturn(true);
        AuthnRequestFromHub authnRequestFromHub = anAuthnRequestFromHub().withSsoUrl(URI.create("/theSsoUri")).build();
        when(authnRequestHandler.getIdaAuthnRequestFromHub(sessionId)).thenReturn(authnRequestFromHub);
        URI ssoUri = UriBuilder.fromUri(UUID.randomUUID().toString()).build();
        SamlRequestDto samlRequest = new SamlRequestDto("samlRequest", ssoUri);
        when(samlEngineProxy.generateCountryAuthnRequestFromHub(any(IdaAuthnRequestFromHubDto.class))).thenReturn(samlRequest);

        // When
        AuthnRequestFromHubContainerDto countryAuthnRequest = service.getIdpAuthnRequest(sessionId);

        // Then
        AuthnRequestFromHubContainerDto expected = new AuthnRequestFromHubContainerDto(samlRequest.getSamlRequest(), ssoUri, authnRequestFromHub.getRegistering());
        assertThat(countryAuthnRequest).isEqualToComparingFieldByField(expected);

        ArgumentCaptor<IdaAuthnRequestFromHubDto> requestFromHubDtoArgumentCaptor = ArgumentCaptor.forClass(IdaAuthnRequestFromHubDto.class);
        verify(samlEngineProxy).generateCountryAuthnRequestFromHub(requestFromHubDtoArgumentCaptor.capture());
        assertThat(requestFromHubDtoArgumentCaptor.getValue().getOverriddenSsoUrl(), notNullValue());
    }

    @Test
    public void getSession_ReturnSessionIdWhenSessionExists() {
        SessionId sessionId = createNewSessionId();
        when(sessionRepository.sessionExists(sessionId)).thenReturn(true);
        assertThat(service.getSessionIfItExists(sessionId)).isEqualTo(sessionId);
    }

    @Test(expected = SessionNotFoundException.class)
    public void getSession_ThrowsExceptionWhenSessionDoesNotExists() {
        SessionId sessionId = createNewSessionId();
        when(sessionRepository.sessionExists(sessionId)).thenReturn(false);
        assertThat(service.getSessionIfItExists(sessionId)).isEqualTo(sessionId);
    }

    @Test(expected = SessionNotFoundException.class)
    public void shouldThrowSessionNotFoundWhenSessionDoesNotExistAndAResponseFromHubIsRequested() {
        SessionId sessionId = createNewSessionId();
        when(sessionRepository.sessionExists(sessionId)).thenReturn(false);
        service.getRpAuthnResponse(sessionId);
    }

    @Test
    public void shouldUpdateSessionStateAndCallSamlEngineWhenResponseFromHubIsRequested() {
        // Given
        SessionId sessionId = createNewSessionId();
        when(sessionRepository.sessionExists(sessionId)).thenReturn(true);
        ResponseFromHub responseFromHub = aResponseFromHubDto().build();
        when(authnRequestHandler.getResponseFromHub(sessionId)).thenReturn(responseFromHub);
        AuthnResponseFromHubContainerDto expected = anAuthnResponseFromHubContainerDto().build();
        when(samlEngineProxy.generateRpAuthnResponse(responseFromHub)).thenReturn(expected);

        // When
        AuthnResponseFromHubContainerDto actual = service.getRpAuthnResponse(sessionId);

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldGetLevelOfAssurance() {
        SessionId sessionId = createNewSessionId();
        when(sessionRepository.sessionExists(sessionId)).thenReturn(true);
        final Optional<LevelOfAssurance> loa = Optional.of(LevelOfAssurance.LEVEL_1);
        when(sessionRepository.getLevelOfAssuranceFromIdp(sessionId)).thenReturn(loa);
        assertThat(service.getLevelOfAssurance(sessionId)).isEqualTo(loa);
    }

    @Test
    public void shouldGetIdpAuthnRequest() {
        SessionId sessionId = createNewSessionId();
        when(sessionRepository.sessionExists(sessionId)).thenReturn(true);
        AuthnRequestFromHub authnRequestFromHub = anAuthnRequestFromHub().build();
        when(authnRequestHandler.getIdaAuthnRequestFromHub(sessionId)).thenReturn(authnRequestFromHub);
        URI ssoUri = UriBuilder.fromUri(UUID.randomUUID().toString()).build();
        SamlRequestDto samlRequest = new SamlRequestDto("samlRequest", ssoUri);
        when(samlEngineProxy.generateIdpAuthnRequestFromHub(any(IdaAuthnRequestFromHubDto.class))).thenReturn(samlRequest);

        AuthnRequestFromHubContainerDto idpAuthnRequest = service.getIdpAuthnRequest(sessionId);

        AuthnRequestFromHubContainerDto expected = new AuthnRequestFromHubContainerDto(samlRequest.getSamlRequest(), ssoUri, authnRequestFromHub.getRegistering());
        assertThat(idpAuthnRequest).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void sendErrorResponseFromHub_shouldReturnDtoWithSamlRequestPostLocationAndRelayState() {
        SessionId sessionId = createNewSessionId();
        when(sessionRepository.sessionExists(sessionId)).thenReturn(true);
        ResponseFromHub responseFromHub = aResponseFromHubDto().withRelayState("relayState").build();
        when(authnRequestHandler.getErrorResponseFromHub(sessionId)).thenReturn(responseFromHub);
        final SamlMessageDto samlMessageDto = new SamlMessageDto("saml");
        when(samlEngineProxy.generateErrorResponseFromHub(any())).thenReturn(samlMessageDto);

        AuthnResponseFromHubContainerDto dto = service.getRpErrorResponse(sessionId);

        assertThat(dto.getSamlResponse()).isEqualTo(samlMessageDto.getSamlMessage());
        assertThat(dto.getPostEndpoint()).isEqualTo(responseFromHub.getAssertionConsumerServiceUri());
        assertThat(dto.getRelayState()).isEqualTo(responseFromHub.getRelayState());
        assertThat(dto.getResponseId()).isEqualTo(responseFromHub.getResponseId());
    }

    @Test(expected = ApplicationException.class)
    public void sendErrorResponseFromHub_shouldErrorWhenSamlEngineProxyReturnsAnError() {
        SessionId sessionId = createNewSessionId();
        when(sessionRepository.sessionExists(sessionId)).thenReturn(true);
        ResponseFromHub responseFromHub = aResponseFromHubDto().withRelayState("relayState").build();
        when(authnRequestHandler.getErrorResponseFromHub(sessionId)).thenReturn(responseFromHub);
        when(samlEngineProxy.generateErrorResponseFromHub(any())).thenThrow(ApplicationException.createAuditedException(ExceptionType.NETWORK_ERROR, UUID.randomUUID()));

        service.getRpErrorResponse(sessionId);
    }


    private void givenSessionIsCreated(SamlResponseWithAuthnRequestInformationDto samlResponse, URI assertionConsumerServiceUri, SessionId sessionId, boolean transactionSupportsEidas) {
        when(authnRequestHandler.handleRequestFromTransaction(samlResponse, requestDto.getRelayState(), requestDto.getPrincipalIPAddressAsSeenByHub(), assertionConsumerServiceUri, transactionSupportsEidas))
                .thenReturn(sessionId);
    }

    private void givenConfigReturnsAssertionConsumerServiceURLFor(SamlResponseWithAuthnRequestInformationDto samlResponse, URI assertionConsumerServiceUri) {
        when(configProxy.getAssertionConsumerServiceUri(samlResponse.getIssuer(), samlResponse.getAssertionConsumerServiceIndex()))
                .thenReturn(new ResourceLocation(assertionConsumerServiceUri));
    }

    private void givenConfigReturnsTransactionSupportsEidas(SamlResponseWithAuthnRequestInformationDto samlResponse, boolean transactionSupportsEidas) {
        when(configProxy.getEidasSupportedForEntity(samlResponse.getIssuer()))
                .thenReturn(transactionSupportsEidas);
    }

    private void givenSamlEngineTranslatesRequest(SamlResponseWithAuthnRequestInformationDto samlResponse) {
        when(samlEngineProxy.translate(requestDto.getSamlRequest()))
                .thenReturn(samlResponse);
    }
}
