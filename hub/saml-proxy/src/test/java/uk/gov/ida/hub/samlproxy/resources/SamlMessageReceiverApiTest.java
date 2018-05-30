package uk.gov.ida.hub.samlproxy.resources;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.xmlsec.signature.support.SignatureException;
import uk.gov.ida.common.IpFromXForwardedForHeader;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlproxy.contracts.SamlRequestDto;
import uk.gov.ida.hub.samlproxy.domain.LevelOfAssurance;
import uk.gov.ida.hub.samlproxy.domain.ResponseActionDto;
import uk.gov.ida.hub.samlproxy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.hub.samlproxy.domain.SamlAuthnResponseContainerDto;
import uk.gov.ida.hub.samlproxy.factories.EidasValidatorFactory;
import uk.gov.ida.hub.samlproxy.logging.ProtectiveMonitoringLogger;
import uk.gov.ida.hub.samlproxy.proxy.SessionProxy;
import uk.gov.ida.hub.samlproxy.repositories.Direction;
import uk.gov.ida.hub.samlproxy.repositories.SignatureStatus;
import uk.gov.ida.saml.core.security.RelayStateValidator;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.validation.SamlValidationResponse;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.security.SamlMessageSignatureValidator;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.namespace.QName;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.AuthnRequestBuilder.anAuthnRequest;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aValidIdpResponse;

@RunWith(OpenSAMLMockitoRunner.class)
public class SamlMessageReceiverApiTest {

    @Mock
    private RelayStateValidator relayStateValidator;

    @Mock
    private StringToOpenSamlObjectTransformer<AuthnRequest> stringSamlAuthnRequestTransformer;
    @Mock
    private StringToOpenSamlObjectTransformer<org.opensaml.saml.saml2.core.Response> stringSamlResponseTransformer;

    @Mock
    private SamlMessageSignatureValidator samlMessageSignatureValidator;

    @Mock
    EidasValidatorFactory eidasValidatorFactory;

    @Mock
    private ProtectiveMonitoringLogger protectiveMonitoringLogger;

    @Mock
    private IpFromXForwardedForHeader ipFromXForwardedForHeader;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private StringToOpenSamlObjectTransformer<AuthnRequest> authnRequestTransformer; //TODO duplicate of line 52?

    @Mock
    private Function<String, org.opensaml.saml.saml2.core.Response> responseTransformer;

    @Mock
    private SessionProxy sessionProxy;

    private final SessionId SESSION_ID = SessionId.createNewSessionId();
    private final String ISSUER_ID = RandomStringUtils.randomAlphanumeric(10);
    private final String SAML_REQUEST = RandomStringUtils.randomAlphanumeric(10);
    private final String DESTINATION = RandomStringUtils.randomAlphanumeric(10);
    private final String IP = RandomStringUtils.randomNumeric(15);

    private SamlMessageReceiverApi samlMessageReceiverApi;
    private org.opensaml.saml.saml2.core.Response validSamlResponse;
    private SamlRequestDto SAML_REQUEST_DTO = new SamlRequestDto(SAML_REQUEST, SESSION_ID.getSessionId(), IP);

    @Before
    public void setUp() throws MarshallingException, SignatureException {
        samlMessageReceiverApi = new SamlMessageReceiverApi(
                relayStateValidator,
                stringSamlAuthnRequestTransformer,
                stringSamlResponseTransformer,
                samlMessageSignatureValidator,
                samlMessageSignatureValidator,
                Optional.of(eidasValidatorFactory),
                protectiveMonitoringLogger,
                sessionProxy);
        validSamlResponse = aValidIdpResponse().build();
    }

    @Test
    public void handleRequestPost_shouldReturnSessionId() throws Exception {
        AuthnRequest authnRequest = anAuthnRequest().withIssuer(anIssuer().withIssuerId(ISSUER_ID).build()).build();
        when(stringSamlAuthnRequestTransformer.apply(SAML_REQUEST)).thenReturn(authnRequest);
        when(samlMessageSignatureValidator.validate(any(AuthnRequest.class), any(QName.class))).thenReturn(SamlValidationResponse.aValidResponse());
        when(sessionProxy.createSession(any(SamlAuthnRequestContainerDto.class))).thenReturn(SESSION_ID);

        Response response = samlMessageReceiverApi.handleRequestPost(SAML_REQUEST_DTO);

        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(SESSION_ID);
    }

    @Test
    public void handleResponsePost_shouldReturnActionDtoOnSuccessfulRegistration() throws MarshallingException, SignatureException {
        ResponseActionDto responseActionDto = ResponseActionDto.success(SESSION_ID, true, LevelOfAssurance.LEVEL_2);
        when(stringSamlResponseTransformer.apply(SAML_REQUEST)).thenReturn(validSamlResponse);
        when(samlMessageSignatureValidator.validate(any(org.opensaml.saml.saml2.core.Response.class), any(QName.class))).thenReturn(SamlValidationResponse.aValidResponse());
        when(sessionProxy.receiveAuthnResponseFromIdp(any(SamlAuthnResponseContainerDto.class), eq(SESSION_ID))).thenReturn(responseActionDto);

        Response response = samlMessageReceiverApi.handleResponsePost(SAML_REQUEST_DTO);

        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(responseActionDto);
    }

    @Test
    public void handleRequestPost_shouldValidateSignatureOfMessage() throws Exception {
        AuthnRequest authnRequest = anAuthnRequest().withIssuer(anIssuer().withIssuerId(ISSUER_ID).build()).build();

        when(authnRequestTransformer.apply(SAML_REQUEST)).thenReturn(authnRequest);
        when(sessionProxy.createSession(any(SamlAuthnRequestContainerDto.class))).thenReturn(SESSION_ID);
        when(samlMessageSignatureValidator.validate(any(AuthnRequest.class), any(QName.class))).thenReturn(SamlValidationResponse.aValidResponse());
        when(stringSamlAuthnRequestTransformer.apply(SAML_REQUEST)).thenReturn(authnRequest);

        samlMessageReceiverApi.handleRequestPost(SAML_REQUEST_DTO);

        verify(samlMessageSignatureValidator).validate(any(AuthnRequest.class), any(QName.class));
    }

    @Test
    public void handleResponsePost_shouldReportPrincipalIpAddress() throws Exception {
        AuthnRequest authnRequest = anAuthnRequest().withIssuer(anIssuer().withIssuerId(ISSUER_ID).build()).build();

        when(stringSamlAuthnRequestTransformer.apply(SAML_REQUEST)).thenReturn(authnRequest);
        when(stringSamlResponseTransformer.apply(SAML_REQUEST)).thenReturn(aResponse().build());
        when(samlMessageSignatureValidator.validate(any(org.opensaml.saml.saml2.core.Response.class), any(QName.class))).thenReturn(SamlValidationResponse.aValidResponse());
        when(sessionProxy.createSession(any(SamlAuthnRequestContainerDto.class))).thenReturn(SESSION_ID);
        when(responseTransformer.apply(anyString())).thenReturn(aResponse().build());

        samlMessageReceiverApi.handleResponsePost(SAML_REQUEST_DTO);

        ArgumentCaptor<SamlAuthnResponseContainerDto> samlAuthnResponseContainerDtoArgumentCaptor = ArgumentCaptor.forClass(SamlAuthnResponseContainerDto.class);

        verify(sessionProxy).receiveAuthnResponseFromIdp(samlAuthnResponseContainerDtoArgumentCaptor.capture(), any(SessionId.class));
        assertThat(samlAuthnResponseContainerDtoArgumentCaptor.getValue().getPrincipalIPAddressAsSeenByHub()).isEqualTo(SAML_REQUEST_DTO.getPrincipalIpAsSeenByFrontend());
    }

    @Test
    public void handleRequestPost_shouldLogSamlRequestInCorrectFormat() throws Exception {
        AuthnRequest authnRequest = anAuthnRequest().withIssuer(anIssuer().withIssuerId(ISSUER_ID).build()).withDestination(DESTINATION).build();

        when(samlMessageSignatureValidator.validate(any(AuthnRequest.class), any(QName.class))).thenReturn(SamlValidationResponse.aValidResponse());
        when(stringSamlAuthnRequestTransformer.apply(SAML_REQUEST)).thenReturn(authnRequest);
        when(authnRequestTransformer.apply(SAML_REQUEST)).thenReturn(authnRequest);
        when(sessionProxy.createSession(any(SamlAuthnRequestContainerDto.class))).thenReturn(SESSION_ID);

        samlMessageReceiverApi.handleRequestPost(SAML_REQUEST_DTO);

        verify(protectiveMonitoringLogger).logAuthnRequest(authnRequest, Direction.INBOUND, SignatureStatus.VALID_SIGNATURE);
    }

    @Test
    public void handleResponsePost_shouldLogSamlResponseInCorrectFormat() throws Exception {
        when(stringSamlResponseTransformer.apply(SAML_REQUEST)).thenReturn(validSamlResponse);
        when(samlMessageSignatureValidator.validate(any(org.opensaml.saml.saml2.core.Response.class), any(QName.class))).thenReturn(SamlValidationResponse.aValidResponse());

        samlMessageReceiverApi.handleResponsePost(SAML_REQUEST_DTO);

        verify(protectiveMonitoringLogger).logAuthnResponse(
                validSamlResponse,
                Direction.INBOUND,
                SignatureStatus.VALID_SIGNATURE
        );
    }

    @Test
    public void handleResponsePost_shouldValidateSignatureOfIncomingSamlMessage() throws Exception {
        when(stringSamlResponseTransformer.apply(SAML_REQUEST)).thenReturn(validSamlResponse);
        when(samlMessageSignatureValidator.validate(eq(validSamlResponse), any(QName.class))).thenReturn(SamlValidationResponse.aValidResponse());

        samlMessageReceiverApi.handleResponsePost(SAML_REQUEST_DTO);

        verify(samlMessageSignatureValidator).validate(eq(validSamlResponse), any(QName.class));
    }

    @Test
    public void handleEidasResponsePost_shouldValidateSignatureOfIncomingSamlMessage() throws Exception {
        when(stringSamlResponseTransformer.apply(SAML_REQUEST)).thenReturn(validSamlResponse);
        ValidatedResponse validatedResponse = new ValidatedResponse(validSamlResponse);
        when(eidasValidatorFactory.getValidatedResponse(validSamlResponse)).thenReturn(validatedResponse);

        Response response = samlMessageReceiverApi.handleEidasResponsePost(SAML_REQUEST_DTO);

        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    }

    @Test
    public void handleEidasResponsePost_shouldReturnNotFoundIfValidatorAbsent() {
        samlMessageReceiverApi = new SamlMessageReceiverApi(
            relayStateValidator,
            stringSamlAuthnRequestTransformer,
            stringSamlResponseTransformer,
            samlMessageSignatureValidator,
            samlMessageSignatureValidator,
            Optional.empty(),
            protectiveMonitoringLogger,
            sessionProxy);

        Response response = samlMessageReceiverApi.handleEidasResponsePost(SAML_REQUEST_DTO);

        assertThat(response.getStatus()).isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

}
