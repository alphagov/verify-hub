package uk.gov.ida.saml.metadata.transformers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.metadata.domain.SamlEndpointDto;

import java.net.URI;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.SamlEndpointDtoBuilder.aSamlEndpointDto;
import static uk.gov.ida.saml.core.test.builders.metadata.EndpointBuilder.anEndpoint;

@RunWith(OpenSAMLMockitoRunner.class)
public class SingleSignOnServicesMarshallerTest {

    @Mock
    private EndpointMarshaller endpointMarshaller;

    private SingleSignOnServicesMarshaller marshaller;

    @Before
    public void setUp() throws Exception {
        marshaller = new SingleSignOnServicesMarshaller(endpointMarshaller);
    }

    @Test
    public void transform_shouldTransformAllServices() throws Exception {
        final String bindingOne = SAMLConstants.SAML2_POST_BINDING_URI;
        final URI locationTwo = URI.create("/foo");
        SingleSignOnService serviceOne = anEndpoint().withBinding(bindingOne).buildSingleSignOnService();
        SingleSignOnService serviceTwo = anEndpoint().withLocation(locationTwo.toString()).buildSingleSignOnService();
        when(endpointMarshaller.toDto(serviceOne)).thenReturn(aSamlEndpointDto().withBinding(SamlEndpointDto.Binding.POST).build());
        when(endpointMarshaller.toDto(serviceTwo)).thenReturn(aSamlEndpointDto().withLocation(locationTwo).build());

        final List<SamlEndpointDto> result = marshaller.toDto(asList(serviceOne, serviceTwo));

        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).getBinding()).isEqualTo(SamlEndpointDto.Binding.POST);
        assertThat(result.get(1).getLocation()).isEqualTo(locationTwo);
    }
}
