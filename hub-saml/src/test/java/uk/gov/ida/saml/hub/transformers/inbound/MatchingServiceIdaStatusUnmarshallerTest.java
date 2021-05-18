package uk.gov.ida.saml.hub.transformers.inbound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.SamlStatusCode;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OpenSAMLExtension.class)
public class MatchingServiceIdaStatusUnmarshallerTest {

    private static MatchingServiceIdaStatusUnmarshaller unmarshaller;

    @BeforeAll
    public static void setUp() throws Exception {
        unmarshaller = new MatchingServiceIdaStatusUnmarshaller();
    }

    @Test
    public void transform_shouldTransformMatchingServiceSuccessfulMatch() {
        OpenSamlXmlObjectFactory samlObjectFactory = new OpenSamlXmlObjectFactory();
        Status originalStatus = samlObjectFactory.createStatus();
        StatusCode successStatusCode = samlObjectFactory.createStatusCode();
        successStatusCode.setValue(StatusCode.SUCCESS);
        originalStatus.setStatusCode(successStatusCode);
        StatusCode matchStatusCode = samlObjectFactory.createStatusCode();
        matchStatusCode.setValue(SamlStatusCode.MATCH);
        successStatusCode.setStatusCode(matchStatusCode);

        MatchingServiceIdaStatus transformedStatus = unmarshaller.fromSaml(originalStatus);

        assertThat(transformedStatus).isEqualTo(MatchingServiceIdaStatus.MatchingServiceMatch);
    }

    @Test
    public void transform_shouldTransformNoMatchFromMatchingService() {
        OpenSamlXmlObjectFactory samlObjectFactory = new OpenSamlXmlObjectFactory();
        Status originalStatus = samlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = samlObjectFactory.createStatusCode();
        topLevelStatusCode.setValue(StatusCode.RESPONDER);
        StatusCode subStatusCode = samlObjectFactory.createStatusCode();
        subStatusCode.setValue(SamlStatusCode.NO_MATCH);
        topLevelStatusCode.setStatusCode(subStatusCode);
        originalStatus.setStatusCode(topLevelStatusCode);

        MatchingServiceIdaStatus transformedStatus = unmarshaller.fromSaml(originalStatus);

        assertThat(transformedStatus).isEqualTo(MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService);
    }

    @Test
    public void transform_shouldTransformRequesterErrorFromMatchingService() {
        OpenSamlXmlObjectFactory samlObjectFactory = new OpenSamlXmlObjectFactory();
        Status originalStatus = samlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = samlObjectFactory.createStatusCode();
        topLevelStatusCode.setValue(StatusCode.REQUESTER);
        originalStatus.setStatusCode(topLevelStatusCode);

        MatchingServiceIdaStatus transformedStatus = unmarshaller.fromSaml(originalStatus);

        assertThat(transformedStatus).isEqualTo(MatchingServiceIdaStatus.RequesterError);
    }

    @Test
    public void transform_shouldTransformHealthyStatusFromMatchingService() {
        OpenSamlXmlObjectFactory samlObjectFactory = new OpenSamlXmlObjectFactory();
        Status status = samlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = samlObjectFactory.createStatusCode();
        topLevelStatusCode.setValue(StatusCode.SUCCESS);
        status.setStatusCode(topLevelStatusCode);
        StatusCode subStatusCode = samlObjectFactory.createStatusCode();
        subStatusCode.setValue(SamlStatusCode.HEALTHY);
        topLevelStatusCode.setStatusCode(subStatusCode);
        MatchingServiceIdaStatus transformedStatus = unmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(MatchingServiceIdaStatus.Healthy);
    }

    @Test
    public void shouldTransformCreateFailureCaseFromMatchingService() {
        OpenSamlXmlObjectFactory samlObjectFactory = new OpenSamlXmlObjectFactory();
        Status status = samlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = samlObjectFactory.createStatusCode();
        topLevelStatusCode.setValue(StatusCode.RESPONDER);
        status.setStatusCode(topLevelStatusCode);
        StatusCode subStatusCode = samlObjectFactory.createStatusCode();
        subStatusCode.setValue(SamlStatusCode.CREATE_FAILURE);
        topLevelStatusCode.setStatusCode(subStatusCode);
        MatchingServiceIdaStatus transformedStatus = unmarshaller.fromSaml(status);

        assertThat(transformedStatus).isEqualTo(MatchingServiceIdaStatus.UserAccountCreationFailed);
    }
}
