package uk.gov.ida.saml.hub.transformers.outbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.MatchingServiceIdaStatus;
import uk.gov.ida.saml.core.domain.SamlStatusCode;
import uk.gov.ida.saml.core.test.OpenSAMLRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(OpenSAMLRunner.class)
public class MatchingServiceIdaStatusToSamlStatusMarshallerTest {

    private MatchingServiceIdaStatusMarshaller marshaller;

    @Before
    public void setUp() throws Exception {
        marshaller = new MatchingServiceIdaStatusMarshaller(new OpenSamlXmlObjectFactory());
    }

    @Test
    public void toSamlStatus_shouldTransformMatchingServiceMatch() throws Exception {
        Status transformedStatus = marshaller.toSamlStatus(MatchingServiceIdaStatus.MatchingServiceMatch);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
        assertThat(transformedStatus.getStatusCode().getStatusCode()).isNotNull();
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(SamlStatusCode.MATCH);
    }

    @Test
    public void toSamlStatus_shouldTransformNoMatchingServiceMatch() throws Exception {
        Status transformedStatus = marshaller.toSamlStatus(MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
        assertThat(transformedStatus.getStatusCode().getStatusCode()).isNotNull();
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(SamlStatusCode.NO_MATCH);
    }

    @Test
    public void toSamlStatus_shouldTransformRequesterError() throws Exception {
        Status transformedStatus = marshaller.toSamlStatus(MatchingServiceIdaStatus.RequesterError);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.REQUESTER);
    }
}
