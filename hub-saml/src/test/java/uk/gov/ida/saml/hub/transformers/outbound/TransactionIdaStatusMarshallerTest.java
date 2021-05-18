package uk.gov.ida.saml.hub.transformers.outbound;

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

@ExtendWith(OpenSAMLExtension.class)
public class TransactionIdaStatusMarshallerTest {

    private static TransactionIdaStatusMarshaller marshaller;

    @BeforeAll
    public static void setUp() {
        marshaller = new TransactionIdaStatusMarshaller(new OpenSamlXmlObjectFactory());
    }

    @Test
    public void toSamlStatus_shouldTransformSuccess() {
        Status transformedStatus = marshaller.toSamlStatus(TransactionIdaStatus.Success);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
    }

    @Test
    public void toSamlStatus_shouldTransformNoAuthenticationContext() {
        Status transformedStatus = marshaller.toSamlStatus(TransactionIdaStatus.NoAuthenticationContext);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(StatusCode.NO_AUTHN_CONTEXT);
    }

    @Test
    public void toSamlStatus_shouldTransformAuthnFailedWithNoSubStatus() {
        Status transformedStatus = marshaller.toSamlStatus(TransactionIdaStatus.AuthenticationFailed);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(StatusCode.AUTHN_FAILED);
        assertThat(transformedStatus.getStatusCode().getStatusCode().getStatusCode()).isNull();
    }

    @Test
    public void toSamlStatus_shouldTransformRequesterError() {
        Status transformedStatus = marshaller.toSamlStatus(TransactionIdaStatus.RequesterError);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(StatusCode.REQUESTER);
    }

    @Test
    public void toSamlStatus_shouldTransformNoMatchingServiceMatchMayRetry() {
        Status transformedStatus = marshaller.toSamlStatus(TransactionIdaStatus.NoMatchingServiceMatchFromHub);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
        assertThat(transformedStatus.getStatusCode().getStatusCode()).isNotNull();
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(SamlStatusCode.NO_MATCH);
    }
}
