package uk.gov.ida.hub.policy.services;

import uk.gov.ida.hub.policy.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.policy.contracts.RequestForErrorResponseFromHubDto;
import uk.gov.ida.hub.policy.contracts.SamlMessageDto;
import uk.gov.ida.hub.policy.contracts.SamlRequestDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.controllogic.AuthnRequestFromTransactionHandler;
import uk.gov.ida.hub.policy.domain.AuthnRequestFromHub;
import uk.gov.ida.hub.policy.domain.AuthnRequestFromHubContainerDto;
import uk.gov.ida.hub.policy.domain.IdaAuthnRequestFromHubDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.exception.SessionCreationFailureException;
import uk.gov.ida.hub.policy.domain.exception.SessionNotFoundException;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectedState;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.net.URI;
import java.util.Optional;

public class SessionService {
    private final SamlEngineProxy samlEngineProxy;
    private final TransactionsConfigProxy configProxy;
    private final AuthnRequestFromTransactionHandler authnRequestHandler;
    private final SessionRepository sessionRepository;

    @Inject
    public SessionService(SamlEngineProxy samlEngineProxy,
                          TransactionsConfigProxy configProxy,
                          AuthnRequestFromTransactionHandler authnRequestHandler,
                          SessionRepository sessionRepository) {
        this.samlEngineProxy = samlEngineProxy;
        this.configProxy = configProxy;
        this.authnRequestHandler = authnRequestHandler;
        this.sessionRepository = sessionRepository;
    }

    public SessionId create(final SamlAuthnRequestContainerDto requestDto) {
        SamlResponseWithAuthnRequestInformationDto samlResponse = samlEngineProxy.translate(requestDto.getSamlRequest());
        URI assertionConsumerServiceUri = getAssertionConsumerServiceUri(samlResponse);
        boolean transactionSupportsEidas = getTransactionSupportsEidas(samlResponse.getIssuer());

        return authnRequestHandler.handleRequestFromTransaction(
            samlResponse,
            requestDto.getRelayState(),
            requestDto.getPrincipalIPAddressAsSeenByHub(),
            assertionConsumerServiceUri,
            transactionSupportsEidas
        );
    }

    private boolean getTransactionSupportsEidas(String issuer) {
        try {
            return configProxy.getEidasSupportedForEntity(issuer);
        } catch (WebApplicationException e) {
            throw SessionCreationFailureException.configServiceException(e);
        }
    }

    public SessionId getSessionIfItExists(SessionId sessionId) {
        if (sessionRepository.sessionExists(sessionId)) {
            return sessionId;
        }

        throw new SessionNotFoundException(sessionId);
    }

    public Optional<LevelOfAssurance> getLevelOfAssurance(SessionId sessionId){
        getSessionIfItExists(sessionId);
        return sessionRepository.getLevelOfAssuranceFromIdp(sessionId);
    }

    public AuthnRequestFromHubContainerDto getIdpAuthnRequest(SessionId sessionId) {
        getSessionIfItExists(sessionId);
        final AuthnRequestFromHub request = authnRequestHandler.getIdaAuthnRequestFromHub(sessionId);
        final IdaAuthnRequestFromHubDto authnRequestFromHub = new IdaAuthnRequestFromHubDto(
                request.getId(),
                request.getForceAuthentication(),
                request.getSessionExpiryTimestamp(),
                request.getRecipientEntityId(),
                request.getLevelsOfAssurance(),
                request.getUseExactComparisonType(),
                request.getOverriddenSsoUrl());

        boolean countryIdentityProvider = sessionRepository.isSessionInState(sessionId, EidasCountrySelectedState.class);
        final SamlRequestDto samlRequest = countryIdentityProvider ? samlEngineProxy.generateCountryAuthnRequestFromHub(authnRequestFromHub) :
                samlEngineProxy.generateIdpAuthnRequestFromHub(authnRequestFromHub);

        return new AuthnRequestFromHubContainerDto(samlRequest.getSamlRequest(), samlRequest.getSsoUri(), request.getRegistering());
    }

    private URI getAssertionConsumerServiceUri(SamlResponseWithAuthnRequestInformationDto samlResponse) {
        try {
            URI uri = configProxy.getAssertionConsumerServiceUri(samlResponse.getIssuer(), samlResponse.getAssertionConsumerServiceIndex()).getTarget();
            if (doesNotMatchProvidedAssertionConsumerServiceUrl(samlResponse.getAssertionConsumerServiceUrl(), uri)) {
                throw SessionCreationFailureException.assertionConsumerServiceUrlNotMatching(
                    samlResponse.getAssertionConsumerServiceUrl().map(URI::toString).orElse("unknown"),
                    uri.toString(),
                    samlResponse.getIssuer()
                );
            }
            return uri;
        } catch (WebApplicationException e) {
            throw SessionCreationFailureException.configServiceException(e);
        }
    }

    public AuthnResponseFromHubContainerDto getRpAuthnResponse(SessionId sessionId) {
        getSessionIfItExists(sessionId);
        ResponseFromHub responseFromHub = authnRequestHandler.getResponseFromHub(sessionId);
        return samlEngineProxy.generateRpAuthnResponse(responseFromHub);
    }

    public AuthnResponseFromHubContainerDto getRpErrorResponse(SessionId sessionId) {
        getSessionIfItExists(sessionId);
        final ResponseFromHub errorResponseFromHub = authnRequestHandler.getErrorResponseFromHub(sessionId);
        final RequestForErrorResponseFromHubDto requestForErrorResponseFromHubDto =
                new RequestForErrorResponseFromHubDto(errorResponseFromHub.getAuthnRequestIssuerEntityId(),
                        errorResponseFromHub.getResponseId(),
                        errorResponseFromHub.getInResponseTo(),
                        errorResponseFromHub.getAssertionConsumerServiceUri(),
                        errorResponseFromHub.getStatus());
        final SamlMessageDto samlMessageDto = samlEngineProxy.generateErrorResponseFromHub(requestForErrorResponseFromHubDto);
        final AuthnResponseFromHubContainerDto authnResponseFromHubContainerDto =
                new AuthnResponseFromHubContainerDto(samlMessageDto.getSamlMessage(),
                        errorResponseFromHub.getAssertionConsumerServiceUri(),
                        errorResponseFromHub.getRelayState(),
                        errorResponseFromHub.getResponseId());
        return authnResponseFromHubContainerDto;
    }

    private boolean doesNotMatchProvidedAssertionConsumerServiceUrl(Optional<URI> providedUrl, URI configUrl) {
        return providedUrl.isPresent() && !providedUrl.get().equals(configUrl);
    }
}
