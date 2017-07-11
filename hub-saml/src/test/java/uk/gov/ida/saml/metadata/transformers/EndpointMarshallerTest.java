package uk.gov.ida.saml.metadata.transformers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.Endpoint;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;import uk.gov.ida.saml.metadata.domain.SamlEndpointDto;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.builders.metadata.EndpointBuilder.anEndpoint;

@RunWith(OpenSAMLMockitoRunner.class)
public class EndpointMarshallerTest {

    private EndpointMarshaller marshaller;

    @Before
    public void setUp() throws Exception {
        marshaller = new EndpointMarshaller();
    }

    @Test
    public void transform_shouldTransformLocation() throws Exception {
        final URI location = URI.create("/foo");
        Endpoint endpoint = anEndpoint().withLocation(location.toString()).buildSingleSignOnService();

        final SamlEndpointDto result = marshaller.toDto(endpoint);

        assertThat(result.getLocation()).isEqualTo(location);
    }

    @Test
    public void transform_shouldTransformPostBinding() throws Exception {
        final String binding = SAMLConstants.SAML2_POST_BINDING_URI;
        Endpoint endpoint = anEndpoint().withBinding(binding).buildSingleSignOnService();

        final SamlEndpointDto result = marshaller.toDto(endpoint);

        assertThat(result.getBinding()).isEqualTo(SamlEndpointDto.Binding.POST);
    }

    @Test
    public void transform_shouldNotSupportTransformRedirectBinding() throws Exception {
        final String binding = SAMLConstants.SAML2_REDIRECT_BINDING_URI;
        final Endpoint endpoint = anEndpoint().withBinding(binding).buildSingleSignOnService();

        SamlTransformationErrorManagerTestHelper.validateFail(
                () -> marshaller.toDto(endpoint),
                SamlTransformationErrorFactory.unrecognisedBinding(endpoint.getBinding())
        );

    }

    @Test
    public void transform_shouldTransformSoapBinding() throws Exception {
        final String binding = SAMLConstants.SAML2_SOAP11_BINDING_URI;
        Endpoint endpoint = anEndpoint().withBinding(binding).buildSingleSignOnService();

        final SamlEndpointDto result = marshaller.toDto(endpoint);

        assertThat(result.getBinding()).isEqualTo(SamlEndpointDto.Binding.SOAP);
    }
}
