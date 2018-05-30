package uk.gov.ida.hub.samlproxy.logging;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.xmlsec.signature.support.SignatureException;
import uk.gov.ida.hub.samlproxy.repositories.Direction;
import uk.gov.ida.hub.samlproxy.repositories.SignatureStatus;
import uk.gov.ida.saml.core.api.CoreTransformersFactory;
import uk.gov.ida.saml.core.test.OpenSAMLRunner;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.builders.AuthnRequestBuilder.anAuthnRequest;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;

@RunWith(OpenSAMLRunner.class)
public class ProtectiveMonitoringLogFormatterTest {
    private StringToOpenSamlObjectTransformer<Response> stringtoOpenSamlObjectTransformer;

    @Before
    public void setUp() throws Exception {
        CoreTransformersFactory coreTransformersFactory = new CoreTransformersFactory();
        stringtoOpenSamlObjectTransformer = coreTransformersFactory.
                getStringtoOpenSamlObjectTransformer(input -> {});
    }

    @Test
    public void shouldFormatAuthnCancelResponse() throws IOException, URISyntaxException {
        String cancelXml = readXmlFile("status-cancel.xml");
        Response cancelResponse = stringtoOpenSamlObjectTransformer.apply(cancelXml);

        String logString = new ProtectiveMonitoringLogFormatter().formatAuthnResponse(cancelResponse, Direction.INBOUND, SignatureStatus.VALID_SIGNATURE);

        String expectedLogMessage = "Protective Monitoring – Authn Response Event – " +
                "{responseId: _ccb1eabc4827928c9cbb3db34fdbe9df186dfcb8, " +
                "inResponseTo: _7081cbd6-a811-440a-949a-12a9521ed7cc, " +
                "direction: INBOUND, " +
                "destination: https://www.signin.service.gov.uk:443/SAML2/SSO/Response/POST, " +
                "issuerId: http://stub-idp, " +
                "validSignature: true, " +
                "status: urn:oasis:names:tc:SAML:2.0:status:Responder, " +
                "subStatus: urn:oasis:names:tc:SAML:2.0:status:NoAuthnContext, " +
                "statusDetails: [authn-cancel]}";
        assertThat(logString).isEqualTo(expectedLogMessage);
    }

    @Test
    public void shouldFormatAuthnSuccessResponse() throws IOException, URISyntaxException, MarshallingException, SignatureException {
        Response response = aResponse().build();

        String logString = new ProtectiveMonitoringLogFormatter().formatAuthnResponse(response, Direction.INBOUND, SignatureStatus.VALID_SIGNATURE);

        String expectedLogMessage = "Protective Monitoring – Authn Response Event – " +
                "{responseId: default-response-id, " +
                "inResponseTo: default-request-id, " +
                "direction: INBOUND, " +
                "destination: http://destination.com, " +
                "issuerId: a-test-entity, " +
                "validSignature: true, " +
                "status: urn:oasis:names:tc:SAML:2.0:status:Success, " +
                "subStatus: , " +
                "statusDetails: []}";
        assertThat(logString).isEqualTo(expectedLogMessage);
    }

    @Test
    public void shouldFormatResponseWithNoIssuer() throws IOException, URISyntaxException, MarshallingException, SignatureException {
        Response response = aResponse().withIssuer(null).build();

        String logString = new ProtectiveMonitoringLogFormatter().formatAuthnResponse(response, Direction.INBOUND, SignatureStatus.VALID_SIGNATURE);

        assertThat(logString).contains("issuerId: ,");
    }

    @Test
    public void shouldFormatAuthnRequest() throws IOException, URISyntaxException {
        AuthnRequest authnRequest = anAuthnRequest().withId("test-id").withDestination("veganistan").build();

        String logString = new ProtectiveMonitoringLogFormatter().formatAuthnRequest(authnRequest, Direction.INBOUND, SignatureStatus.VALID_SIGNATURE);

        String expectedLogMessage = "Protective Monitoring – Authn Request Event – {" +
                "requestId: test-id, " +
                "direction: INBOUND, " +
                "destination: veganistan, " +
                "issuerId: a-test-entity, " +
                "validSignature: true}";
        assertThat(logString).isEqualTo(expectedLogMessage);
    }

    @Test
    public void shouldFormatAuthnRequestWithoutIssuer() throws IOException, URISyntaxException {
        AuthnRequest authnRequest = anAuthnRequest().withId("test-id").withDestination("veganistan").withIssuer(null).build();

        String logString = new ProtectiveMonitoringLogFormatter().formatAuthnRequest(authnRequest, Direction.INBOUND, SignatureStatus.VALID_SIGNATURE);
        assertThat(logString).contains("issuerId: ,");
    }


    private String readXmlFile(String xmlFile) throws IOException, URISyntaxException {
        Base64.Encoder encoder = Base64.getEncoder();
        URL resource = getClass().getClassLoader().getResource(xmlFile);
        return new String(encoder.encode(Files.readAllBytes(Paths.get(resource.toURI()))));
    }

}