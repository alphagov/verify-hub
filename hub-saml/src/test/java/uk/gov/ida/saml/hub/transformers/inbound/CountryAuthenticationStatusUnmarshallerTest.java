package uk.gov.ida.saml.hub.transformers.inbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.api.CoreTransformersFactory;
import uk.gov.ida.saml.core.test.OpenSAMLRunner;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.domain.CountryAuthenticationStatus;

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
public class CountryAuthenticationStatusUnmarshallerTest {

    private OpenSamlXmlObjectFactory samlObjectFactory;
    private StringToOpenSamlObjectTransformer<Response> stringToOpenSamlObjectTransformer;
    private CountryAuthenticationStatusUnmarshaller countryAuthenticationStatusUnmarshaller;

    @Before
    public void setUp() throws Exception {
        samlObjectFactory = new OpenSamlXmlObjectFactory();
        stringToOpenSamlObjectTransformer = new CoreTransformersFactory().getStringtoOpenSamlObjectTransformer(input -> {});
        countryAuthenticationStatusUnmarshaller = new CountryAuthenticationStatusUnmarshaller();
    }

    @Test
    public void shouldTransformSuccessWithNoSubCode() {
        Status originalStatus = samlObjectFactory.createStatus();
        StatusCode successStatusCode = samlObjectFactory.createStatusCode();
        successStatusCode.setValue(StatusCode.SUCCESS);
        originalStatus.setStatusCode(successStatusCode);

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(originalStatus);

        assertThat(transformedStatus).isEqualTo(CountryAuthenticationStatus.success());
    }

    @Test
    public void shouldTransformSuccessWithMessage() {
        Status originalStatus = samlObjectFactory.createStatus();
        StatusCode successStatusCode = samlObjectFactory.createStatusCode();
        successStatusCode.setValue(StatusCode.SUCCESS);
        StatusMessage statusMessage = samlObjectFactory.createStatusMessage();
        statusMessage.setMessage(StatusCode.SUCCESS);
        originalStatus.setStatusCode(successStatusCode);
        originalStatus.setStatusMessage(statusMessage);

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(originalStatus);

        assertThat(transformedStatus).isEqualTo(CountryAuthenticationStatus.success());
        assertThat(transformedStatus.getMessage().isPresent()).isEqualTo(false);
    }

    @Test
    public void shouldTransformNoAuthenticationContext() {
        Status originalStatus = samlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = samlObjectFactory.createStatusCode();
        topLevelStatusCode.setValue(StatusCode.RESPONDER);
        StatusCode subStatusCode = samlObjectFactory.createStatusCode();
        subStatusCode.setValue(StatusCode.NO_AUTHN_CONTEXT);
        topLevelStatusCode.setStatusCode(subStatusCode);
        originalStatus.setStatusCode(topLevelStatusCode);

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(originalStatus);

        assertThat(transformedStatus).isEqualTo(CountryAuthenticationStatus.failure());
    }

    @Test
    public void shouldTransformAuthnFailed() {
        Status status = samlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = samlObjectFactory.createStatusCode();
        topLevelStatusCode.setValue(StatusCode.RESPONDER);
        status.setStatusCode(topLevelStatusCode);
        StatusCode subStatusCode = samlObjectFactory.createStatusCode();
        subStatusCode.setValue(StatusCode.AUTHN_FAILED);
        topLevelStatusCode.setStatusCode(subStatusCode);

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(CountryAuthenticationStatus.failure());
    }

    @Test
    public void shouldTransformAuthnFailedWithoutSubstatus() {
        String message = "error detail";
        StatusCode topLevelStatusCode = aStatusCode().withValue(StatusCode.AUTHN_FAILED).build();
        Status status = aStatus()
                .withStatusCode(topLevelStatusCode)
                .withMessage(aStatusMessage().withMessage(message).build())
                .build();

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(CountryAuthenticationStatus.failure());
        assertThat(transformedStatus.getMessage().isPresent()).isEqualTo(true);
        assertThat(transformedStatus.getMessage().get()).isEqualTo(message);
    }

    @Test
    public void shouldTransformRequestDeniedWithoutSubstatus() {
        String message = "error detail";
        StatusCode topLevelStatusCode = aStatusCode().withValue(StatusCode.REQUEST_DENIED).build();
        Status status = aStatus()
                .withStatusCode(topLevelStatusCode)
                .withMessage(aStatusMessage().withMessage(message).build())
                .build();

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(CountryAuthenticationStatus.failure());
        assertThat(transformedStatus.getMessage().isPresent()).isEqualTo(true);
        assertThat(transformedStatus.getMessage().get()).isEqualTo(message);
    }

    @Test
    public void shouldTransformRequesterErrorWithoutMessage() {
        Status status = samlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = samlObjectFactory.createStatusCode();
        topLevelStatusCode.setValue(StatusCode.REQUESTER);
        status.setStatusCode(topLevelStatusCode);

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(CountryAuthenticationStatus.failure());
        assertThat(transformedStatus.getMessage().isPresent()).isEqualTo(false);
    }

    @Test
    public void shouldTransformRequesterErrorWithMessage() {
        String message = "some message";

        StatusCode topLevelStatusCode = aStatusCode().withValue(StatusCode.REQUESTER).build();
        Status status = aStatus()
                .withStatusCode(topLevelStatusCode)
                .withMessage(aStatusMessage().withMessage(message).build())
                .build();

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(CountryAuthenticationStatus.failure());
        assertThat(transformedStatus.getMessage().isPresent()).isEqualTo(true);
        assertThat(transformedStatus.getMessage().get()).isEqualTo(message);
    }

    @Test
    public void shouldTransformRequesterErrorWithRequestDeniedSubstatus() {
        Status status = samlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = samlObjectFactory.createStatusCode();
        topLevelStatusCode.setValue(StatusCode.REQUESTER);
        StatusCode subStatusCode = samlObjectFactory.createStatusCode();
        subStatusCode.setValue(StatusCode.REQUEST_DENIED);
        status.setStatusCode(topLevelStatusCode);

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(CountryAuthenticationStatus.failure());
    }

    @Test
    public void shouldTransformResponderErrorWithoutMessage() {
        Status status = samlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = samlObjectFactory.createStatusCode();
        topLevelStatusCode.setValue(StatusCode.RESPONDER);
        status.setStatusCode(topLevelStatusCode);

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(CountryAuthenticationStatus.failure());
        assertThat(transformedStatus.getMessage().isPresent()).isEqualTo(false);
    }

    @Test
    public void shouldTransformResponderErrorWithMessage() {
        String message = "some message";

        StatusCode topLevelStatusCode = aStatusCode().withValue(StatusCode.RESPONDER).build();
        Status status = aStatus()
                .withStatusCode(topLevelStatusCode)
                .withMessage(aStatusMessage().withMessage(message).build())
                .build();

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(CountryAuthenticationStatus.failure());
        assertThat(transformedStatus.getMessage().isPresent()).isEqualTo(true);
        assertThat(transformedStatus.getMessage().get()).isEqualTo(message);
    }

    @Test
    public void shouldTransformResponderErrorWithCanceledSubstatus() {
        Status status = samlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = samlObjectFactory.createStatusCode();
        topLevelStatusCode.setValue(StatusCode.RESPONDER);
        StatusCode subStatusCode = samlObjectFactory.createStatusCode();
        subStatusCode.setValue("Canceled");
        status.setStatusCode(topLevelStatusCode);

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(CountryAuthenticationStatus.failure());
    }

    @Test
    public void shouldMapSamlStatusDetailOfAuthnCancelToAuthenticationCancelled() throws Exception {
        String cancelXml = readXmlFile("status-cancel.xml");
        Response cancelResponse = stringToOpenSamlObjectTransformer.apply(cancelXml);

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(cancelResponse.getStatus());

        assertThat(transformedStatus.getStatusCode()).isEqualTo(CountryAuthenticationStatus.Status.Failure);
    }

    @Test
    public void shouldMapSamlStatusDetailOfLoaPendingToAuthenticationPending() throws Exception {
        String pendingXml = readXmlFile("status-pending.xml");
        Response pendingResponse = stringToOpenSamlObjectTransformer.apply(pendingXml);

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(pendingResponse.getStatus());

        assertThat(transformedStatus.getStatusCode()).isEqualTo(CountryAuthenticationStatus.Status.Failure);
    }

    @Test
    public void shouldRemainSuccessEvenIfStatusDetailCancelReturned() throws Exception {
        String successWithCancelXml = readXmlFile("status-success-with-cancel.xml");
        Response cancelResponse = stringToOpenSamlObjectTransformer.apply(successWithCancelXml);

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(cancelResponse.getStatus());

        assertThat(transformedStatus.getStatusCode()).isEqualTo(CountryAuthenticationStatus.Status.Success);
    }

    @Test
    public void shouldRemainNoAuthnContextIfStatusDetailAbsent() throws Exception {
        String successWithCancelXml = readXmlFile("status-noauthncontext.xml");
        Response cancelResponse = stringToOpenSamlObjectTransformer.apply(successWithCancelXml);

        final CountryAuthenticationStatus transformedStatus = countryAuthenticationStatusUnmarshaller.fromSaml(cancelResponse.getStatus());

        assertThat(transformedStatus.getStatusCode()).isEqualTo(CountryAuthenticationStatus.Status.Failure);
    }

    private String readXmlFile(String xmlFile) throws IOException, URISyntaxException {
        Base64.Encoder encoder = Base64.getEncoder();
        URL resource = getClass().getClassLoader().getResource(xmlFile);
        return new String(encoder.encode(Files.readAllBytes(Paths.get(resource.toURI()))));
    }
}
