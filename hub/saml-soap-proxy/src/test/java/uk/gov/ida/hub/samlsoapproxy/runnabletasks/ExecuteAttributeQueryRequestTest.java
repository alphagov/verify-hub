package uk.gov.ida.hub.samlsoapproxy.runnabletasks;

import org.glassfish.jersey.internal.util.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.w3c.dom.Element;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.common.shared.security.PrivateKeyFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.hub.samlsoapproxy.client.AttributeQueryRequestClient;
import uk.gov.ida.hub.samlsoapproxy.domain.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlsoapproxy.logging.ProtectiveMonitoringLogger;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.builders.StatusBuilder;
import uk.gov.ida.saml.core.test.builders.StatusMessageBuilder;
import uk.gov.ida.saml.core.validation.SamlValidationResponse;
import uk.gov.ida.saml.security.SamlMessageSignatureValidator;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.samlsoapproxy.builders.AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.UNCHAINED_PRIVATE_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.UNCHAINED_PUBLIC_CERT;
import static uk.gov.ida.saml.core.test.builders.AttributeQueryBuilder.anAttributeQuery;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;

@RunWith(OpenSAMLMockitoRunner.class)
public class ExecuteAttributeQueryRequestTest {

    private static final QName HUB_ROLE = SPSSODescriptor.DEFAULT_ELEMENT_NAME;
    @Mock
    private AttributeQueryRequestClient attributeQueryRequestClient;
    @Mock
    private SamlMessageSignatureValidator matchingRequestSignatureValidator;
    @Mock
    private SamlMessageSignatureValidator matchingResponseSignatureValidator;
    @Mock
    private Function<Element, Response> elementToResponseTransformer;
    @Mock
    private Function<Element, AttributeQuery> elementToAttributeQueryTransformer;
    @Mock
    private ProtectiveMonitoringLogger protectiveMonitoringLogger;
    @Mock
    private Element matchingServiceResponse;

    private ExecuteAttributeQueryRequest executeAttributeQueryRequest;
    private URI matchingServiceUri = URI.create("/another-uri");
    private AttributeQueryContainerDto attributeQueryContainerDto;
    private SessionId sessionId = SessionId.createNewSessionId();
    private final AttributeQuery attributeQuery = anAttributeQuery().build();

    @Before
    public void setUp() {
        attributeQueryContainerDto = anAttributeQueryContainerDto(anAttributeQuery().build())
                .withMatchingServiceUri(matchingServiceUri)
                .build();

        executeAttributeQueryRequest = new ExecuteAttributeQueryRequest(
                elementToAttributeQueryTransformer,
                elementToResponseTransformer,
                matchingRequestSignatureValidator,
                matchingResponseSignatureValidator,
                attributeQueryRequestClient,
                protectiveMonitoringLogger);

       DateTimeFreezer.freezeTime();

        when(matchingRequestSignatureValidator.validate(any(AttributeQuery.class), eq(HUB_ROLE))).thenReturn(SamlValidationResponse.aValidResponse());
        when(matchingResponseSignatureValidator.validate(any(Response.class), eq(AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME))).thenReturn(SamlValidationResponse.aValidResponse());
        when(elementToAttributeQueryTransformer.apply(any(Element.class))).thenReturn(attributeQuery);
    }

    @After
    public void tearDown() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void run_shouldUseCorrectSignatureValidators() throws Exception {
        when(attributeQueryRequestClient.sendQuery(any(Element.class), anyString(), any(SessionId.class), any(URI.class))).thenReturn(matchingServiceResponse);
        final Response response = aResponse().build();
        when(elementToResponseTransformer.apply(matchingServiceResponse)).thenReturn(response);
        executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto);

        verify(matchingRequestSignatureValidator).validate(attributeQuery, HUB_ROLE);
        verify(matchingResponseSignatureValidator).validate(response, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME);
    }

    @Test
    public void run_shouldSendToTheCorrectUri() throws Exception {
        when(attributeQueryRequestClient.sendQuery(any(Element.class), anyString(), any(SessionId.class), any(URI.class))).thenReturn(matchingServiceResponse);
        when(elementToResponseTransformer.apply(matchingServiceResponse)).thenReturn(aResponse().build());
        executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto);

        final ArgumentCaptor<URI> uriArgumentCaptor = ArgumentCaptor.forClass(URI.class);
        verify(attributeQueryRequestClient).sendQuery(any(Element.class), anyString(), any(SessionId.class), uriArgumentCaptor.capture());

        final URI uri = uriArgumentCaptor.getValue();
        assertThat(uri).isEqualTo(matchingServiceUri);
    }

    @Test
    public void run_shouldCallInboundMessageValidatorWithResponse() throws Exception {
        when(attributeQueryRequestClient.sendQuery(any(Element.class), anyString(), any(SessionId.class), any(URI.class))).thenReturn(matchingServiceResponse);
        Response response = aResponse().build();
        when(elementToResponseTransformer.apply(matchingServiceResponse)).thenReturn(response);

        executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto);

        verify(matchingResponseSignatureValidator).validate(response, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME);
    }

    @Test
    public void run_shouldCallInboundMessageValidatorWithAttributeQuery() throws Exception {
        when(attributeQueryRequestClient.sendQuery(any(Element.class), anyString(), any(SessionId.class), any(URI.class))).thenReturn(matchingServiceResponse);
        Response response = aResponse().build();
        when(elementToResponseTransformer.apply(matchingServiceResponse)).thenReturn(response);

        executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto);

        verify(matchingRequestSignatureValidator).validate(attributeQuery, HUB_ROLE);
    }

    @Test
    public void run_shouldCallSamlMessageSignatureValidatorWithResponse() throws Exception {
        when(attributeQueryRequestClient.sendQuery(any(Element.class), anyString(), any(SessionId.class), any(URI.class))).thenReturn(matchingServiceResponse);
        Response response = aResponse().withIssuer(anIssuer().withIssuerId("issuer-id").build()).build();
        when(elementToResponseTransformer.apply(matchingServiceResponse)).thenReturn(response);
        executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto);

        verify(matchingResponseSignatureValidator).validate(response, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME);
    }

    @Test
    public void run_shouldCallSamlMessageSignatureValidatorWithRequest() throws Exception {
        when(attributeQueryRequestClient.sendQuery(any(Element.class), anyString(), any(SessionId.class), any(URI.class))).thenReturn(matchingServiceResponse);
        Response response = aResponse().build();
        when(elementToResponseTransformer.apply(matchingServiceResponse)).thenReturn(response);

        executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto);

        verify(matchingRequestSignatureValidator).validate(attributeQuery, HUB_ROLE);
    }

    @Test
    public void run_shouldLogProtectiveMonitoringCorrectly() throws Exception {
        final Element matchingServiceResponse = mock(Element.class);
        when(attributeQueryRequestClient.sendQuery(any(Element.class), anyString(), any(SessionId.class), any(URI.class))).thenReturn(matchingServiceResponse);
        Response response = aResponse().build();
        when(elementToResponseTransformer.apply(matchingServiceResponse)).thenReturn(response);

        executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto);

        verify(protectiveMonitoringLogger).logAttributeQuery(attributeQuery.getID(), attributeQueryContainerDto.getMatchingServiceUri().toASCIIString(), attributeQuery.getIssuer().getValue(), true);
        verify(protectiveMonitoringLogger).logAttributeQueryResponse(response.getID(), response.getInResponseTo(), response.getIssuer().getValue(), true, response.getStatus().getStatusCode().getValue(), "");
    }

    @Test
    public void run_shouldLogStatusMessageIfItExists() throws MarshallingException, SignatureException {
        final Element matchingServiceResponse = mock(Element.class);
        when(attributeQueryRequestClient.sendQuery(any(Element.class), anyString(), any(SessionId.class), any(URI.class))).thenReturn(matchingServiceResponse);
        String message = "Some message";
        StatusMessage statusMessage = StatusMessageBuilder.aStatusMessage().withMessage(message).build();
        Response response = aResponse().withStatus(StatusBuilder.aStatus().withMessage(statusMessage).build()).build();
        when(elementToResponseTransformer.apply(matchingServiceResponse)).thenReturn(response);

        executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto);

        verify(protectiveMonitoringLogger).logAttributeQueryResponse(response.getID(), response.getInResponseTo(), response.getIssuer().getValue(), true, response.getStatus().getStatusCode().getValue(), message);
    }

    @Test
    public void run_shouldThrowCertChainValidationExceptionOnResponse() throws Exception {
        when(attributeQueryRequestClient.sendQuery(any(Element.class), anyString(), any(SessionId.class), any(URI.class))).thenReturn(matchingServiceResponse);
        final BasicX509Credential x509Credential = new BasicX509Credential(
                new X509CertificateFactory().createCertificate(UNCHAINED_PUBLIC_CERT),
                new PrivateKeyFactory().createPrivateKey(Base64.decode(UNCHAINED_PRIVATE_KEY.getBytes())));
        Response response = aResponse().withSigningCredential(x509Credential).withIssuer(anIssuer().withIssuerId("issuer-id").build()).build();
        when(elementToResponseTransformer.apply(matchingServiceResponse)).thenReturn(response);
        executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto);

        verify(matchingResponseSignatureValidator).validate(response, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME);
    }
}
