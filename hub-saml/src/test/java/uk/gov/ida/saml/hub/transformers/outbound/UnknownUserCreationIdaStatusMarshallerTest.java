package uk.gov.ida.saml.hub.transformers.outbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.SamlStatusCode;
import uk.gov.ida.saml.msa.test.domain.UnknownUserCreationIdaStatus;
import uk.gov.ida.saml.core.test.OpenSAMLRunner;
import uk.gov.ida.saml.msa.test.outbound.UnknownUserCreationIdaStatusMarshaller;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(OpenSAMLRunner.class)
public class UnknownUserCreationIdaStatusMarshallerTest {

    private UnknownUserCreationIdaStatusMarshaller unknownUserCreationIdaStatusToSamlStatusMarshaller;

    @Before
    public void setUp() throws Exception {
        unknownUserCreationIdaStatusToSamlStatusMarshaller = new UnknownUserCreationIdaStatusMarshaller(new OpenSamlXmlObjectFactory());
    }

    @Test
    public void transform_shouldTransformUnknownUserCreationSuccess() throws Exception {

        Status transformedStatus = unknownUserCreationIdaStatusToSamlStatusMarshaller.toSamlStatus(UnknownUserCreationIdaStatus.Success);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(SamlStatusCode.CREATED);
    }

    @Test
    public void transform_shouldTransformUnknownUserCreationFailure() throws Exception {

        Status transformedStatus = unknownUserCreationIdaStatusToSamlStatusMarshaller.toSamlStatus(UnknownUserCreationIdaStatus.CreateFailure);

        assertThat(transformedStatus.getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
        assertThat(transformedStatus.getStatusCode().getStatusCode()).isNotNull();
        assertThat(transformedStatus.getStatusCode().getStatusCode().getValue()).isEqualTo(SamlStatusCode.CREATE_FAILURE);
    }
}
