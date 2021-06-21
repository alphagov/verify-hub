package uk.gov.ida.saml.hub.transformers.outbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.SamlStatusCode;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.core.test.OpenSAMLRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(OpenSAMLRunner.class)
public class SimpleProfileTransactionIdaStatusMarshallerTest {

    private SimpleProfileTransactionIdaStatusMarshaller marshaller;

    @Before
    public void setUp() throws Exception {
        marshaller = new SimpleProfileTransactionIdaStatusMarshaller(new OpenSamlXmlObjectFactory());
    }

    @Test
    public void toSamlStatus_shouldTransformSuccess() throws Exception {
        Status transformedStatus = marshaller.toSamlStatus(TransactionIdaStatus.Success);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
    }

    @Test
    public void toSamlStatus_shouldTransformNoAuthenticationContext() throws Exception {
        Status transformedStatus = marshaller.toSamlStatus(TransactionIdaStatus.NoAuthenticationContext);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(StatusCode.NO_AUTHN_CONTEXT);
    }

    @Test
    public void toSamlStatus_shouldTransformAuthnFailedWithNoSubStatus() throws Exception {
        Status transformedStatus = marshaller.toSamlStatus(TransactionIdaStatus.AuthenticationFailed);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(StatusCode.AUTHN_FAILED);
        assertThat(transformedStatus.getStatusCode().getStatusCode().getStatusCode()).isNull();
    }

    @Test
    public void toSamlStatus_shouldTransformRequesterError() throws Exception {
        Status transformedStatus = marshaller.toSamlStatus(TransactionIdaStatus.RequesterError);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(StatusCode.REQUESTER);
    }

    @Test
    public void toSamlStatus_shouldTransformNoMatchingServiceMatchMayRetry() throws Exception {
        Status transformedStatus = marshaller.toSamlStatus(TransactionIdaStatus.NoMatchingServiceMatchFromHub);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
        assertThat(transformedStatus.getStatusCode().getStatusCode()).isNotNull();
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(SamlStatusCode.NO_MATCH);
    }
}
