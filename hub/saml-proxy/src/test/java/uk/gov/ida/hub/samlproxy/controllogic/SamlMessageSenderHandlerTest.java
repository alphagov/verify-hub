package uk.gov.ida.hub.samlproxy.controllogic;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.xmlsec.signature.support.SignatureException;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.hub.samlproxy.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.samlproxy.controllogic.SamlMessageSenderHandler.SamlMessage;
import uk.gov.ida.hub.samlproxy.domain.AuthnRequestFromHubContainerDto;
import uk.gov.ida.hub.samlproxy.logging.ExternalCommunicationEventLogger;
import uk.gov.ida.hub.samlproxy.logging.ProtectiveMonitoringLogger;
import uk.gov.ida.hub.samlproxy.proxy.SessionProxy;
import uk.gov.ida.hub.samlproxy.repositories.Direction;
import uk.gov.ida.hub.samlproxy.repositories.SignatureStatus;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationResponse;
import uk.gov.ida.saml.core.validation.errors.SamlValidationSpecification;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.security.SamlMessageSignatureValidator;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.AuthnRequestBuilder.anAuthnRequest;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;


@RunWith(OpenSAMLMockitoRunner.class)
public class SamlMessageSenderHandlerTest {

    @Mock
    private StringToOpenSamlObjectTransformer<Response> responseTransformer;
    @Mock
    private SamlMessageSignatureValidator samlMessageSignatureValidator;
    @Mock
    private EventSinkProxy eventSinkProxy;
    @Mock
    private ExternalCommunicationEventLogger externalCommunicationEventLogger;
    @Mock
    private StringToOpenSamlObjectTransformer<AuthnRequest> authnRequestTransformer;
    @Mock
    private ProtectiveMonitoringLogger protectiveMonitoringLogger;
    @Mock
    private SessionProxy sessionProxy;
    @Captor
    private ArgumentCaptor<String> transitionMessageCaptor;

    public SamlMessageSenderHandler samlMessageSenderHandler;

    private static final String samlRequest = "some-saml-request";
    private static final URI postEndPoint = URI.create("http://someurl.com");
    private static final String principalIpAddressAsSeenByHub = "a-principal-ip-address";
    private static final Optional<String> relayState = Optional.fromNullable("some-relay-state");

    @Before
    public void setUp() throws Exception {
        samlMessageSenderHandler = new SamlMessageSenderHandler(
                responseTransformer,
                authnRequestTransformer,
                samlMessageSignatureValidator,
                externalCommunicationEventLogger,
                protectiveMonitoringLogger,
                sessionProxy);
        when(samlMessageSignatureValidator.validate(any(AuthnRequest.class), any(QName.class))).thenReturn(SamlValidationResponse.aValidResponse());
        when(samlMessageSignatureValidator.validate(any(Response.class), any(QName.class))).thenReturn(SamlValidationResponse.aValidResponse());
    }

    @Test
    public void generateAuthnRequestFromHub_shouldAddExternalCommunicationEvent() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        String expectedSamlMessageId = UUID.randomUUID().toString();

        when(sessionProxy.getAuthnRequestFromHub(any(SessionId.class))).thenReturn(new AuthnRequestFromHubContainerDto(samlRequest, postEndPoint, true));

        AuthnRequest authnRequest = anAuthnRequest().withId(expectedSamlMessageId).build();
        when(authnRequestTransformer.apply(samlRequest)).thenReturn(authnRequest);

        SamlMessage authnResponse = samlMessageSenderHandler.generateAuthnRequestFromHub(sessionId, principalIpAddressAsSeenByHub);
        assertThat(authnResponse.getSamlMessage()).isEqualTo(samlRequest);
        assertThat(authnResponse.getPostEndpoint()).isEqualTo(postEndPoint.toString());
        assertThat(authnResponse.getRegistration().isPresent()).isTrue();
        assertThat(authnResponse.getRegistration().get()).isTrue();
        assertThat(authnResponse.getSamlMessageType()).isEqualTo(SamlMessageType.SAML_REQUEST);
        assertThat(authnResponse.getRelayState().isPresent()).isTrue();
        assertThat(authnResponse.getRelayState().get()).isEqualTo(sessionId.getSessionId());

        verify(externalCommunicationEventLogger).logIdpAuthnRequest(expectedSamlMessageId, sessionId, postEndPoint, principalIpAddressAsSeenByHub);
    }

    @Test
    public void generateAuthnResponseFromHub_shouldAddExternalCommunicationEvent() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        String expectedSamlMessageId = UUID.randomUUID().toString();

        Response openSamlResponse = setUpAuthnResponseFromHub(sessionId, expectedSamlMessageId);

        SamlMessage authnResponse = samlMessageSenderHandler.generateAuthnResponseFromHub(sessionId, principalIpAddressAsSeenByHub);
        assertThat(authnResponse.getSamlMessage()).isEqualTo(samlRequest);
        assertThat(authnResponse.getPostEndpoint()).isEqualTo(postEndPoint.toString());
        assertThat(authnResponse.getRegistration().isPresent()).isFalse();
        assertThat(authnResponse.getRelayState().isPresent()).isTrue();
        assertThat(authnResponse.getRelayState().get()).isEqualTo(relayState.get());
        assertThat(authnResponse.getSamlMessageType()).isEqualTo(SamlMessageType.SAML_RESPONSE);

        verify(externalCommunicationEventLogger).logResponseFromHub(expectedSamlMessageId, sessionId, postEndPoint, principalIpAddressAsSeenByHub);
        verify(protectiveMonitoringLogger).logAuthnResponse(openSamlResponse, Direction.OUTBOUND, SignatureStatus.VALID_SIGNATURE);
    }

    @Test
    public void generateErrorResponseFromHub_shouldAddExternalCommunicationEvent() throws MarshallingException, SignatureException {
        SessionId sessionId = SessionId.createNewSessionId();
        String responseId = UUID.randomUUID().toString();

        when(sessionProxy.getErrorResponseFromHub(sessionId)).thenReturn(new AuthnResponseFromHubContainerDto(samlRequest, postEndPoint, relayState, responseId));
        Response samlResponse = setUpErrorResponseFromHub(sessionId, responseId);
        when(responseTransformer.apply(samlRequest)).thenReturn(samlResponse);

        SamlMessage samlMessage = samlMessageSenderHandler.generateErrorResponseFromHub(sessionId, principalIpAddressAsSeenByHub);
        assertThat(samlMessage.getSamlMessage()).isEqualTo(samlRequest);
        assertThat(samlMessage.getPostEndpoint()).isEqualTo(postEndPoint.toString());
        assertThat(samlMessage.getRegistration().isPresent()).isFalse();
        assertThat(samlMessage.getSamlMessageType()).isEqualTo(SamlMessageType.SAML_RESPONSE);
        assertThat(samlMessage.getRelayState().isPresent()).isTrue();
        assertThat(samlMessage.getRelayState()).isEqualTo(relayState);

        verify(externalCommunicationEventLogger).logResponseFromHub(responseId, sessionId, postEndPoint, principalIpAddressAsSeenByHub);
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void generateAuthRequestFromHub_shouldThrowSamlTransformationException() throws MarshallingException, SignatureException {
        SessionId sessionId = SessionId.createNewSessionId();
        String expectedSamlMessageId = UUID.randomUUID().toString();
        when(sessionProxy.getAuthnRequestFromHub(sessionId)).thenReturn(new AuthnRequestFromHubContainerDto(samlRequest, postEndPoint, true));
        AuthnRequest authnRequest = anAuthnRequest().withId(expectedSamlMessageId).build();
        when(authnRequestTransformer.apply(samlRequest)).thenReturn(authnRequest);
        when(samlMessageSignatureValidator.validate(authnRequest, SPSSODescriptor.DEFAULT_ELEMENT_NAME)).thenReturn(SamlValidationResponse.anInvalidResponse(new SamlValidationSpecification("bad", true)));

        samlMessageSenderHandler.generateAuthnRequestFromHub(sessionId, principalIpAddressAsSeenByHub);
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void generateAuthResponseFromHub_shouldThrowSamlTransformationException() throws MarshallingException, SignatureException {
        SessionId sessionId = SessionId.createNewSessionId();
        String expectedSamlMessageId = UUID.randomUUID().toString();
        Response openSamlResponse = setUpAuthnResponseFromHub(sessionId, expectedSamlMessageId);

        when(samlMessageSignatureValidator.validate(openSamlResponse, SPSSODescriptor.DEFAULT_ELEMENT_NAME)).thenReturn(SamlValidationResponse.anInvalidResponse(new SamlValidationSpecification("bad", true)));

        samlMessageSenderHandler.generateAuthnResponseFromHub(sessionId, principalIpAddressAsSeenByHub);
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void generateErrorResponseFromHub_shouldThrowSamlTransformationException() throws MarshallingException, SignatureException {
        SessionId sessionId = SessionId.createNewSessionId();
        String expectedSamlMessageId = UUID.randomUUID().toString();
        Response openSamlResponse = setUpErrorResponseFromHub(sessionId, expectedSamlMessageId);
        when(samlMessageSignatureValidator.validate(openSamlResponse, SPSSODescriptor.DEFAULT_ELEMENT_NAME)).thenReturn(SamlValidationResponse.anInvalidResponse(new SamlValidationSpecification("bad", true)));

        samlMessageSenderHandler.generateErrorResponseFromHub(sessionId, principalIpAddressAsSeenByHub);
    }

    private Response setUpAuthnResponseFromHub(SessionId sessionId, String expectedSamlMessageId) throws MarshallingException, SignatureException {
        AuthnResponseFromHubContainerDto hubContainerDto = new AuthnResponseFromHubContainerDto(samlRequest, postEndPoint, relayState, expectedSamlMessageId);
        when(sessionProxy.getAuthnResponseFromHub(sessionId)).thenReturn(hubContainerDto);
        Response openSamlResponse = aResponse().withId(expectedSamlMessageId).build();
        when(responseTransformer.apply(anyString())).thenReturn(openSamlResponse);
        return openSamlResponse;
    }

    private Response setUpErrorResponseFromHub(SessionId sessionId, String expectedSamlMessageId) throws MarshallingException, SignatureException {
        AuthnResponseFromHubContainerDto hubContainerDto = new AuthnResponseFromHubContainerDto(samlRequest, postEndPoint, relayState, expectedSamlMessageId);
        when(sessionProxy.getErrorResponseFromHub(sessionId)).thenReturn(hubContainerDto);
        Response openSamlResponse = aResponse().withId(expectedSamlMessageId).build();
        when(responseTransformer.apply(anyString())).thenReturn(openSamlResponse);
        return openSamlResponse;
    }

}
