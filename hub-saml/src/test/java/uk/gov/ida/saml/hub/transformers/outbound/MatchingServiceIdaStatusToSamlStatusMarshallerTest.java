package uk.gov.ida.saml.hub.transformers.outbound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.MatchingServiceIdaStatus;
import uk.gov.ida.saml.core.domain.SamlStatusCode;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OpenSAMLExtension.class)
public class MatchingServiceIdaStatusToSamlStatusMarshallerTest {

    private static MatchingServiceIdaStatusMarshaller marshaller;

    @BeforeAll
    public static void setUp() {
        marshaller = new MatchingServiceIdaStatusMarshaller(new OpenSamlXmlObjectFactory());
    }

    @Test
    public void toSamlStatus_shouldTransformMatchingServiceMatch() {
        Status transformedStatus = marshaller.toSamlStatus(MatchingServiceIdaStatus.MatchingServiceMatch);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
        assertThat(transformedStatus.getStatusCode().getStatusCode()).isNotNull();
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(SamlStatusCode.MATCH);
    }

    @Test
    public void toSamlStatus_shouldTransformNoMatchingServiceMatch() {
        Status transformedStatus = marshaller.toSamlStatus(MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
        assertThat(transformedStatus.getStatusCode().getStatusCode()).isNotNull();
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(SamlStatusCode.NO_MATCH);
    }

    @Test
    public void toSamlStatus_shouldTransformRequesterError() {
        Status transformedStatus = marshaller.toSamlStatus(MatchingServiceIdaStatus.RequesterError);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.REQUESTER);
    }
}
