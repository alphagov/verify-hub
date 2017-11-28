package uk.gov.ida.saml.hub.transformers.outbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.IdaSamlBootstrap;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;
import uk.gov.ida.saml.core.extensions.StatusValue;
import uk.gov.ida.saml.core.test.OpenSAMLRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(OpenSAMLRunner.class)
public class IdpIdaStatusMarshallerTest {

    private final IdpIdaStatusMarshaller marshaller = new IdpIdaStatusMarshaller(new OpenSamlXmlObjectFactory());


    @Before
    public void setUp() throws Exception {
        IdaSamlBootstrap.bootstrap();
    }

    @Test
    public void transform_shouldTransformSuccess() throws Exception {
        Status transformedStatus = marshaller.toSamlStatus(IdpIdaStatus.success());

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
    }

    @Test
    public void transform_shouldTransformNoAuthenticationContext() throws Exception {
        Status transformedStatus = marshaller.toSamlStatus(IdpIdaStatus.noAuthenticationContext());

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(StatusCode.NO_AUTHN_CONTEXT);
    }

    @Test
    public void transform_shouldTransformAuthenticationPending() throws Exception {
        Status transformedStatus = marshaller.toSamlStatus(IdpIdaStatus.authenticationPending());
        StatusValue actual = (StatusValue) transformedStatus.getStatusDetail().getOrderedChildren().get(0);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(StatusCode.NO_AUTHN_CONTEXT);
        assertThat(actual.getValue()).isEqualTo(StatusValue.PENDING);
    }

    @Test
    public void transform_shouldTransformAuthnFailedWithNoSubStatus() throws Exception {
        Status transformedStatus = marshaller.toSamlStatus(IdpIdaStatus.authenticationFailed());

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(StatusCode.AUTHN_FAILED);
        assertThat(transformedStatus.getStatusCode().getStatusCode().getStatusCode()).isNull();
    }

    @Test
    public void transform_shouldTransformRequesterError() throws Exception {
        Status transformedStatus = marshaller.toSamlStatus(IdpIdaStatus.requesterError());

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.REQUESTER);
    }

    @Test
    public void transform_shouldTransformRequesterErrorWithMessage() throws Exception {
        String message = "Oh dear";
        Status transformedStatus = marshaller.toSamlStatus(IdpIdaStatus.requesterError(Optional.of(message)));

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.REQUESTER);
        assertThat(transformedStatus.getStatusMessage().getMessage()).isEqualTo(message);
    }

    @Test
    public void shouldMarshallStatusDetailElementWhenInCancelStatus() {
        Status status = marshaller.toSamlStatus(IdpIdaStatus.authenticationCancelled());

        assertThat(status.getStatusDetail()).isNotNull();
    }

    @Test
    public void shouldMarshallStatusDetailWithStatusValueContainingAuthnCancelInCaseOfAuthenticationCancelled() {
        Status status = marshaller.toSamlStatus(IdpIdaStatus.authenticationCancelled());

        StatusValue actual = (StatusValue) status.getStatusDetail().getOrderedChildren().get(0);

        assertThat(actual.getNamespaces()).isEmpty();
        assertThat(actual.getValue()).isEqualTo(StatusValue.CANCEL);
    }

    @Test
    public void shouldMarshallStatusDetailWithStatusValueContainingUpliftFailed() {
        Status status = marshaller.toSamlStatus(IdpIdaStatus.upliftFailed());

        StatusValue actual = (StatusValue) status.getStatusDetail().getOrderedChildren().get(0);

        assertThat(actual.getNamespaces()).isEmpty();
        assertThat(actual.getValue()).isEqualTo(StatusValue.UPLIFT_FAILED);
    }

    @Test
    public void shouldMarshallStatusDetailWithASingleStatusValueElementInCaseOfAuthenticationCancelled() {
        Status status = marshaller.toSamlStatus(IdpIdaStatus.authenticationCancelled());

        assertThat(status.getStatusDetail().getOrderedChildren()).hasSize(1);
    }
}
