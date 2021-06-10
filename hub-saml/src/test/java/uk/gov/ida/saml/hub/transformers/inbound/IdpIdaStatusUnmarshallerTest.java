package uk.gov.ida.saml.hub.transformers.inbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.api.CoreTransformersFactory;
import uk.gov.ida.saml.core.test.OpenSAMLRunner;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.builders.StatusBuilder.aStatus;
import static uk.gov.ida.saml.core.test.builders.StatusCodeBuilder.aStatusCode;
import static uk.gov.ida.saml.core.test.builders.StatusMessageBuilder.aStatusMessage;

@RunWith(OpenSAMLRunner.class)
public class IdpIdaStatusUnmarshallerTest {

    private IdpIdaStatusUnmarshaller unmarshaller;
    private StringToOpenSamlObjectTransformer<Response> stringToOpenSamlObjectTransformer;

    @Before
    public void setUp() throws Exception {
        unmarshaller = new IdpIdaStatusUnmarshaller();
        stringToOpenSamlObjectTransformer = new CoreTransformersFactory().getStringtoOpenSamlObjectTransformer(input -> {});
    }

    @Test
    public void transform_shouldTransformSuccessWithNoSubCode() {
        OpenSamlXmlObjectFactory samlObjectFactory = new OpenSamlXmlObjectFactory();
        Status originalStatus = samlObjectFactory.createStatus();
        StatusCode successStatusCode = samlObjectFactory.createStatusCode();
        successStatusCode.setValue(StatusCode.SUCCESS);
        originalStatus.setStatusCode(successStatusCode);

        IdpIdaStatus transformedStatus = unmarshaller.fromSaml(originalStatus);

        assertThat(transformedStatus).isEqualTo(IdpIdaStatus.success());
    }

    @Test
    public void transform_shouldTransformNoAuthenticationContext() {
        OpenSamlXmlObjectFactory samlObjectFactory = new OpenSamlXmlObjectFactory();
        Status originalStatus = samlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = samlObjectFactory.createStatusCode();
        topLevelStatusCode.setValue(StatusCode.RESPONDER);
        StatusCode subStatusCode = samlObjectFactory.createStatusCode();
        subStatusCode.setValue(StatusCode.NO_AUTHN_CONTEXT);
        topLevelStatusCode.setStatusCode(subStatusCode);
        originalStatus.setStatusCode(topLevelStatusCode);

        IdpIdaStatus transformedStatus = unmarshaller.fromSaml(originalStatus);

        assertThat(transformedStatus).isEqualTo(IdpIdaStatus.noAuthenticationContext());
    }

    @Test
    public void transform_shouldTransformAuthnFailed() {
        OpenSamlXmlObjectFactory samlObjectFactory = new OpenSamlXmlObjectFactory();
        Status status = samlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = samlObjectFactory.createStatusCode();
        topLevelStatusCode.setValue(StatusCode.RESPONDER);
        status.setStatusCode(topLevelStatusCode);
        StatusCode subStatusCode = samlObjectFactory.createStatusCode();
        subStatusCode.setValue(StatusCode.AUTHN_FAILED);
        topLevelStatusCode.setStatusCode(subStatusCode);
        IdpIdaStatus transformedStatus = unmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(IdpIdaStatus.authenticationFailed());
    }

    @Test
    public void transform_shouldTransformAuthnFailedWithDetail() throws Exception {
        String authnFailedWithDetailXml = readXmlFile("status-authnfailed-with-detail.xml");
        Response response = stringToOpenSamlObjectTransformer.apply(authnFailedWithDetailXml);

        IdpIdaStatus transformedStatus = unmarshaller.fromSaml(response.getStatus());

        assertThat(transformedStatus).isEqualTo(IdpIdaStatus.authenticationFailed());
    }

    @Test
    public void transform_shouldTransformRequesterErrorWithoutMessage() {
        OpenSamlXmlObjectFactory samlObjectFactory = new OpenSamlXmlObjectFactory();

        Status status = samlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = samlObjectFactory.createStatusCode();
        topLevelStatusCode.setValue(StatusCode.REQUESTER);
        status.setStatusCode(topLevelStatusCode);

        IdpIdaStatus transformedStatus = unmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(IdpIdaStatus.requesterError());
    }

    @Test
    public void transform_shouldTransformRequesterErrorWithRequestDeniedSubstatus() {
        OpenSamlXmlObjectFactory samlObjectFactory = new OpenSamlXmlObjectFactory();

        Status status = samlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = samlObjectFactory.createStatusCode();
        topLevelStatusCode.setValue(StatusCode.REQUESTER);
        StatusCode subStatusCode = samlObjectFactory.createStatusCode();
        subStatusCode.setValue(StatusCode.REQUEST_DENIED);

        status.setStatusCode(topLevelStatusCode);

        IdpIdaStatus transformedStatus = unmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(IdpIdaStatus.requesterError());
    }

    @Test
    public void transform_shouldTransformRequesterErrorWithMessage() {
        String message = "some message";

        StatusCode topLevelStatusCode = aStatusCode().withValue(StatusCode.REQUESTER).build();
        Status status = aStatus()
                .withStatusCode(topLevelStatusCode)
                .withMessage(aStatusMessage().withMessage(message).build())
                .build();

        IdpIdaStatus transformedStatus = unmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(IdpIdaStatus.requesterError());
        assertThat(transformedStatus.getMessage().isPresent()).isEqualTo(true);
        assertThat(transformedStatus.getMessage().get()).isEqualTo(message);
    }

    @Test
    public void shouldMapSamlStatusDetailOfAuthnCancelToAuthenticationCancelled() throws Exception {
        String cancelXml = readXmlFile("status-cancel.xml");
        Response cancelResponse = stringToOpenSamlObjectTransformer.apply(cancelXml);

        IdpIdaStatus idpIdaStatus = getStatusFrom(cancelResponse);

        assertThat(idpIdaStatus.getStatusCode()).isEqualTo(IdpIdaStatus.Status.AuthenticationCancelled);
    }

    @Test
    public void shouldMapSamlStatusDetailOfLoaPendingToAuthenticationPending() throws Exception {
        String pendingXml = readXmlFile("status-pending.xml");
        Response pendingResponse = stringToOpenSamlObjectTransformer.apply(pendingXml);

        IdpIdaStatus idpIdaStatus = getStatusFrom(pendingResponse);

        assertThat(idpIdaStatus.getStatusCode()).isEqualTo(IdpIdaStatus.Status.AuthenticationPending);
    }

    @Test
    public void shouldRemainSuccessEvenIfStatusDetailCancelReturned() throws Exception {
        String successWithCancelXml = readXmlFile("status-success-with-cancel.xml");
        Response cancelResponse = stringToOpenSamlObjectTransformer.apply(successWithCancelXml);

        IdpIdaStatus idpIdaStatus = getStatusFrom(cancelResponse);

        assertThat(idpIdaStatus.getStatusCode()).isEqualTo(IdpIdaStatus.Status.Success);
    }

    @Test
    public void shouldRemainNoAuthnContextIfStatusDetailAbsent() throws Exception {
        String successWithCancelXml = readXmlFile("status-noauthncontext.xml");
        Response cancelResponse = stringToOpenSamlObjectTransformer.apply(successWithCancelXml);

        IdpIdaStatus idpIdaStatus = getStatusFrom(cancelResponse);

        assertThat(idpIdaStatus.getStatusCode()).isEqualTo(IdpIdaStatus.Status.NoAuthenticationContext);
    }

    @Test
    public void shouldRemainNoAuthnContextIfStatusDetailPresentButUnknown() throws Exception {
        String xml = readXmlFile("status-noauthncontext-withotherdetail.xml");
        Response response = stringToOpenSamlObjectTransformer.apply(xml);

        IdpIdaStatus idpIdaStatus = getStatusFrom(response);

        assertThat(idpIdaStatus.getStatusCode()).isEqualTo(IdpIdaStatus.Status.NoAuthenticationContext);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowWhenNoMatch() throws Exception {
        String xml = readXmlFile("status-unknown.xml");
        Response response = stringToOpenSamlObjectTransformer.apply(xml);
        getStatusFrom(response);
    }


    private String readXmlFile(String xmlFile) throws IOException, URISyntaxException {
        Base64.Encoder encoder = Base64.getEncoder();
        URL resource = getClass().getClassLoader().getResource(xmlFile);
        return new String(encoder.encode(Files.readAllBytes(Paths.get(resource.toURI()))));
    }

    private IdpIdaStatus getStatusFrom(Response pendingResponse) {
        IdpIdaStatusUnmarshaller idpIdaStatusUnmarshaller = new IdpIdaStatusUnmarshaller();
        return idpIdaStatusUnmarshaller.fromSaml(pendingResponse.getStatus());
    }
}
