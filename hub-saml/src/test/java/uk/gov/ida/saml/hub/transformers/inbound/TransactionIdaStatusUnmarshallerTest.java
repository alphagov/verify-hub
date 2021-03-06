package uk.gov.ida.saml.hub.transformers.inbound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.SamlStatusCode;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.builders.StatusBuilder.aStatus;
import static uk.gov.ida.saml.core.test.builders.StatusCodeBuilder.aStatusCode;

@ExtendWith(OpenSAMLExtension.class)
public class TransactionIdaStatusUnmarshallerTest {

    private static TransactionIdaStatusUnmarshaller unmarshaller;

    @BeforeAll
    public static void setUp() {
        unmarshaller = new TransactionIdaStatusUnmarshaller();
    }

    @Test
    public void transform_shouldTransformSuccessWithNoSubCode() {
        OpenSamlXmlObjectFactory samlObjectFactory = new OpenSamlXmlObjectFactory();
        Status originalStatus = samlObjectFactory.createStatus();
        StatusCode successStatusCode = samlObjectFactory.createStatusCode();
        successStatusCode.setValue(StatusCode.SUCCESS);
        originalStatus.setStatusCode(successStatusCode);

        TransactionIdaStatus transformedStatus = unmarshaller.fromSaml(originalStatus);

        assertThat(transformedStatus).isEqualTo(TransactionIdaStatus.Success);
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

        TransactionIdaStatus transformedStatus = unmarshaller.fromSaml(originalStatus);

        assertThat(transformedStatus).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void transform_shouldTransformNoMatchFromHub() {
        OpenSamlXmlObjectFactory samlObjectFactory = new OpenSamlXmlObjectFactory();
        Status originalStatus = samlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = samlObjectFactory.createStatusCode();
        topLevelStatusCode.setValue(StatusCode.SUCCESS);
        StatusCode subStatusCode = samlObjectFactory.createStatusCode();
        subStatusCode.setValue(SamlStatusCode.NO_MATCH);
        topLevelStatusCode.setStatusCode(subStatusCode);
        originalStatus.setStatusCode(topLevelStatusCode);

        TransactionIdaStatus transformedStatus = unmarshaller.fromSaml(originalStatus);

        assertThat(transformedStatus).isEqualTo(TransactionIdaStatus.NoMatchingServiceMatchFromHub);
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
        TransactionIdaStatus transformedStatus = unmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(TransactionIdaStatus.AuthenticationFailed);
    }

    @Test
    public void transform_shouldTransformRequesterErrorFromIdpAsSentByHub() {
        Status status =
            aStatus()
                .withStatusCode(
                    aStatusCode()
                        .withValue(StatusCode.RESPONDER)
                        .withSubStatusCode(
                            aStatusCode()
                            .withValue(StatusCode.REQUESTER)
                            .build()
                        )
                        .build())
                .build();


        TransactionIdaStatus transformedStatus = unmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(TransactionIdaStatus.RequesterError);
    }
}
