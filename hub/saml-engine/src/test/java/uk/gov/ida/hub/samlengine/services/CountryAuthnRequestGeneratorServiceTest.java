package uk.gov.ida.hub.samlengine.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.ida.hub.samlengine.contracts.IdaAuthnRequestFromHubDto;
import uk.gov.ida.hub.samlengine.domain.SamlRequestDto;
import uk.gov.ida.hub.samlengine.proxy.CountrySingleSignOnServiceHelper;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.hub.domain.EidasAuthnRequestFromHub;
import uk.gov.ida.saml.hub.test.builders.EidasAuthnRequestBuilder;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(OpenSAMLMockitoRunner.class)
public class CountryAuthnRequestGeneratorServiceTest {

    private static final String HUB_ENTITY_ID = "HUB_ENTITY_ID";
    private CountryAuthnRequestGeneratorService service;

    @Mock
    private Function<EidasAuthnRequestFromHub, String> eidasAuthnRequestFromHubStringTransformer;

    @Mock
    private CountrySingleSignOnServiceHelper countrySingleSignOnServiceHelper;

    @Mock
    private EidasAuthnRequestTranslator eidasAuthnRequestTranslator;

    @Before
    public void setUp() {
        service = new CountryAuthnRequestGeneratorService(countrySingleSignOnServiceHelper, eidasAuthnRequestFromHubStringTransformer, eidasAuthnRequestTranslator, HUB_ENTITY_ID);
    }

    @Test
    public void generateSamlRequest() {
        // Given
        String theCountryEntityId = "theCountryEntityId";
        IdaAuthnRequestFromHubDto dto = new IdaAuthnRequestFromHubDto("1", null, Optional.of(false), null, theCountryEntityId, false);

        URI ssoUri = UriBuilder.fromPath("/the-sso-uri").build();
        String samlRequest = "samlRequest";
        EidasAuthnRequestFromHub eidasAuthnRequestFromHub = new EidasAuthnRequestBuilder().buildFromHub();

        when(eidasAuthnRequestFromHubStringTransformer.apply(any())).thenReturn(samlRequest);
        when(countrySingleSignOnServiceHelper.getSingleSignOn(theCountryEntityId)).thenReturn(ssoUri);
        when(eidasAuthnRequestTranslator.getEidasAuthnRequestFromHub(dto, ssoUri, HUB_ENTITY_ID)).thenReturn(eidasAuthnRequestFromHub);

        // When
        final SamlRequestDto output = service.generateSaml(dto);

        // Then
        assertThat(output.getSamlRequest()).isEqualTo(samlRequest);
        assertThat(output.getSsoUri()).isEqualTo(ssoUri);

        verify(countrySingleSignOnServiceHelper).getSingleSignOn(theCountryEntityId);
        verify(eidasAuthnRequestFromHubStringTransformer).apply(any(EidasAuthnRequestFromHub.class));
        verifyNoMoreInteractions(countrySingleSignOnServiceHelper, eidasAuthnRequestFromHubStringTransformer);
    }

    @Test
    public void generateSamlRequestWithOverriddenSsoUri() {
        // Given
        String theCountryEntityId = "theCountryEntityId";
        URI overriddenSsoURI = URI.create("http://overridden.foo.bar");
        IdaAuthnRequestFromHubDto dto = new IdaAuthnRequestFromHubDto("1", null, Optional.of(false), null, theCountryEntityId, false, overriddenSsoURI);

        String samlRequest = "samlRequest";

        when(eidasAuthnRequestFromHubStringTransformer.apply(any())).thenReturn(samlRequest);

        // When
        final SamlRequestDto output = service.generateSaml(dto);

        // Then
        assertThat(output.getSamlRequest()).isEqualTo(samlRequest);
        assertThat(output.getSsoUri()).isEqualTo(overriddenSsoURI);

        verifyNoMoreInteractions(countrySingleSignOnServiceHelper);
    }
}
