package uk.gov.ida.saml.metadata.transformers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.metadata.domain.AssertionConsumerServiceEndpointDto;
import uk.gov.ida.saml.metadata.domain.SamlEndpointDto;

import java.net.URI;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.builders.metadata.AssertionConsumerServiceBuilder.anAssertionConsumerService;

@RunWith(OpenSAMLMockitoRunner.class)
public class AssertionConsumerServicesMarshallerTest {

    private AssertionConsumerServicesMarshaller assertionConsumerServicesMarshaller;

    @Before
    public void setup() {
        assertionConsumerServicesMarshaller = new AssertionConsumerServicesMarshaller();
    }

    @Test
    public void transform_shouldTransformAssertionConsumerServices() throws Exception {

        String location1 = "/foo1";
        String location2 = "/foo2";
        AssertionConsumerService assertionConsumerServiceOne = anAssertionConsumerService()
                .withBinding(SAMLConstants.SAML2_POST_BINDING_URI)
                .withLocation(location1)
                .withIndex(1)
                .isDefault()
                .build();
        AssertionConsumerService assertionConsumerServiceTwo = anAssertionConsumerService()
                .withBinding(SAMLConstants.SAML2_POST_BINDING_URI)
                .withLocation(location2)
                .withIndex(2)
                .build();

        List<AssertionConsumerServiceEndpointDto> result = assertionConsumerServicesMarshaller.toDto(asList(assertionConsumerServiceOne, assertionConsumerServiceTwo));

        assertThat(result.size()).isEqualTo(2);

        assertThat(result.get(0).getLocation()).isEqualTo(URI.create(location1));
        assertThat(result.get(0).getBinding()).isEqualTo(SamlEndpointDto.Binding.POST);
        assertThat(result.get(0).getIsDefault()).isEqualTo(true);
        assertThat(result.get(0).getIndex()).isEqualTo(1);

        assertThat(result.get(1).getLocation()).isEqualTo(URI.create(location2));
        assertThat(result.get(1).getBinding()).isEqualTo(SamlEndpointDto.Binding.POST);
        assertThat(result.get(1).getIsDefault()).isEqualTo(false);
        assertThat(result.get(1).getIndex()).isEqualTo(2);
    }
}
