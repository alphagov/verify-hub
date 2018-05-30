package uk.gov.ida.hub.samlproxy.resources;

import com.codahale.metrics.annotation.Timed;
import org.jboss.logging.MDC;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.slf4j.event.Level;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.samlproxy.contracts.SamlRequestDto;
import uk.gov.ida.hub.samlproxy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.hub.samlproxy.domain.SamlAuthnResponseContainerDto;
import uk.gov.ida.hub.samlproxy.factories.EidasValidatorFactory;
import uk.gov.ida.hub.samlproxy.logging.ProtectiveMonitoringLogger;
import uk.gov.ida.hub.samlproxy.proxy.SessionProxy;
import uk.gov.ida.hub.samlproxy.repositories.Direction;
import uk.gov.ida.hub.samlproxy.repositories.SignatureStatus;
import uk.gov.ida.saml.core.security.RelayStateValidator;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationResponse;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.security.SamlMessageSignatureValidator;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Path(Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT)
public class SamlMessageReceiverApi {

    private final RelayStateValidator relayStateValidator;
    private final StringToOpenSamlObjectTransformer<AuthnRequest> stringSamlAuthnRequestTransformer;
    private final StringToOpenSamlObjectTransformer<org.opensaml.saml.saml2.core.Response> stringSamlResponseTransformer;
    private final SamlMessageSignatureValidator authnRequestSignatureValidator;
    private final SamlMessageSignatureValidator authnResponseSignatureValidator;
    private final Optional<EidasValidatorFactory> eidasValidatorFactory;
    private final ProtectiveMonitoringLogger protectiveMonitoringLogger;
    private final SessionProxy sessionProxy;

    @Inject
    public SamlMessageReceiverApi(RelayStateValidator relayStateValidator,
                                  StringToOpenSamlObjectTransformer<AuthnRequest> stringSamlAuthnRequestTransformer,
                                  StringToOpenSamlObjectTransformer<org.opensaml.saml.saml2.core.Response> stringSamlResponseTransformer,
                                  @Named("authnRequestSignatureValidator") SamlMessageSignatureValidator authnRequestSignatureValidator,
                                  @Named("authnResponseSignatureValidator") SamlMessageSignatureValidator authnResponseSignatureValidator,
                                  Optional<EidasValidatorFactory> eidasValidatorFactory,
                                  ProtectiveMonitoringLogger protectiveMonitoringLogger,
                                  SessionProxy sessionProxy) {
        this.relayStateValidator = relayStateValidator;
        this.stringSamlAuthnRequestTransformer = stringSamlAuthnRequestTransformer;
        this.stringSamlResponseTransformer = stringSamlResponseTransformer;
        this.authnRequestSignatureValidator = authnRequestSignatureValidator;
        this.authnResponseSignatureValidator = authnResponseSignatureValidator;
        this.eidasValidatorFactory = eidasValidatorFactory;
        this.protectiveMonitoringLogger = protectiveMonitoringLogger;
        this.sessionProxy = sessionProxy;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response handleRequestPost(SamlRequestDto samlRequestDto) {

        relayStateValidator.validate(samlRequestDto.getRelayState());

        AuthnRequest authnRequest = stringSamlAuthnRequestTransformer.apply(samlRequestDto.getSamlRequest());

        SamlValidationResponse signatureValidationResponse = authnRequestSignatureValidator.validate(authnRequest, SPSSODescriptor.DEFAULT_ELEMENT_NAME);

        protectiveMonitoringLogger.logAuthnRequest(authnRequest, Direction.INBOUND, SignatureStatus.fromValidationResponse(signatureValidationResponse));

        if (!signatureValidationResponse.isOK()) {
            SamlValidationSpecificationFailure failure = signatureValidationResponse.getSamlValidationSpecificationFailure();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), signatureValidationResponse.getCause(), Level.ERROR);
        }

        SamlAuthnRequestContainerDto samlAuthnRequestContainerDto = new SamlAuthnRequestContainerDto(samlRequestDto.getSamlRequest(), Optional.ofNullable(samlRequestDto.getRelayState()), samlRequestDto.getPrincipalIpAsSeenByFrontend());

        SessionId sessionId = sessionProxy.createSession(samlAuthnRequestContainerDto);
        return Response.ok(sessionId).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path(Urls.SamlProxyUrls.RESPONSE_POST_PATH)
    @Timed
    public Response handleResponsePost(SamlRequestDto samlRequestDto) {

        final SessionId sessionId = new SessionId(samlRequestDto.getRelayState());
        MDC.put("SessionId", sessionId);

        relayStateValidator.validate(samlRequestDto.getRelayState());

        org.opensaml.saml.saml2.core.Response samlResponse = stringSamlResponseTransformer.apply(samlRequestDto.getSamlRequest());

        SamlValidationResponse signatureValidationResponse = authnResponseSignatureValidator.validate(samlResponse, IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
        protectiveMonitoringLogger.logAuthnResponse(
                samlResponse,
                Direction.INBOUND,
                SignatureStatus.fromValidationResponse(signatureValidationResponse));

        if (!signatureValidationResponse.isOK()) {
            SamlValidationSpecificationFailure failure = signatureValidationResponse.getSamlValidationSpecificationFailure();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), signatureValidationResponse.getCause(), Level.ERROR);
        }

        final SamlAuthnResponseContainerDto authnResponseDto = new SamlAuthnResponseContainerDto(
                samlRequestDto.getSamlRequest(),
                sessionId,
                samlRequestDto.getPrincipalIpAsSeenByFrontend()
        );

        return Response.ok(sessionProxy.receiveAuthnResponseFromIdp(authnResponseDto, sessionId)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path(Urls.SamlProxyUrls.EIDAS_RESPONSE_POST_PATH)
    @Timed
    public Response handleEidasResponsePost(SamlRequestDto samlRequestDto) {

        if (eidasValidatorFactory.isPresent()) {
            final SessionId sessionId = new SessionId(samlRequestDto.getRelayState());
            MDC.put("SessionId", sessionId);

            relayStateValidator.validate(samlRequestDto.getRelayState());

            org.opensaml.saml.saml2.core.Response samlResponse = stringSamlResponseTransformer.apply(samlRequestDto.getSamlRequest());

            eidasValidatorFactory.get().getValidatedResponse(samlResponse);

            protectiveMonitoringLogger.logAuthnResponse(
                samlResponse,
                Direction.INBOUND,
                SignatureStatus.VALID_SIGNATURE);

            final SamlAuthnResponseContainerDto authnResponseDto = new SamlAuthnResponseContainerDto(
                samlRequestDto.getSamlRequest(),
                sessionId,
                samlRequestDto.getPrincipalIpAsSeenByFrontend()
            );
            return Response.ok(sessionProxy.receiveAuthnResponseFromCountry(authnResponseDto, sessionId)).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
