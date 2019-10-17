package uk.gov.ida.hub.samlengine.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.samlengine.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.samlengine.exceptions.UnableToGenerateSamlException;
import uk.gov.ida.hub.samlengine.factories.OutboundResponseFromHubToResponseTransformerFactory;
import uk.gov.ida.hub.samlengine.locators.AssignableEntityToEncryptForLocator;
import uk.gov.ida.saml.core.domain.AuthnResponseFromCountryContainerDto;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundAuthnResponseFromCountryContainerToStringFunction;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RpAuthnResponseGeneratorServiceTest {

    private static final String BASE_64_SAML_RESPONSE = "base64SamlResponse";
    private static final String ENCRYPTED_KEY = "base64EncryptedKey";
    private static final String COUNTRY_ENTITY_ID = "countryEntityId";
    private static final URI POST_ENDPOINT = URI.create("https://post-endpoint");
    private static final String RELAY_STATE = "relayState";
    private static final String IN_RESPONSE_TO = "inResponseTo";
    private static final String RESPONSE_ID = "responseId";
    private static final String HUB_ENTITY_ID = "hubEntityId";
    private static final String TRANSFORMED_SAML_RESPONSE = "transformedSamlResponse";
    private static final String SERVICE_ENTITY_ID = "serviceEntityId";

    private AuthnResponseFromCountryContainerDto countryContainer;
    private RpAuthnResponseGeneratorService generatorService;

    @Mock
    private OutboundResponseFromHubToResponseTransformerFactory outboundResponseFromHubToResponseTransformerFactory;

    @Before
    public void setUp() {
        countryContainer = new AuthnResponseFromCountryContainerDto(
                new CountrySignedResponseContainer(
                        BASE_64_SAML_RESPONSE,
                        List.of(ENCRYPTED_KEY),
                        COUNTRY_ENTITY_ID
                ),
                POST_ENDPOINT,
                Optional.of(RELAY_STATE),
                IN_RESPONSE_TO,
                SERVICE_ENTITY_ID,
                RESPONSE_ID
        );
        generatorService = new RpAuthnResponseGeneratorService(
                outboundResponseFromHubToResponseTransformerFactory,
                HUB_ENTITY_ID,
                new AssignableEntityToEncryptForLocator()
        );
    }

    @Test
    public void generateShouldReturnAnAuthnResponseFromHubContainerDtoFromAuthnResponseFromCountryContainerDto() {
        OutboundAuthnResponseFromCountryContainerToStringFunction transformerMock = mock(OutboundAuthnResponseFromCountryContainerToStringFunction.class);

        when(outboundResponseFromHubToResponseTransformerFactory.getCountryTransformer()).thenReturn(transformerMock);
        when(transformerMock.apply(countryContainer)).thenReturn(TRANSFORMED_SAML_RESPONSE);

        AuthnResponseFromHubContainerDto hubContainer = generatorService.generate(countryContainer);

        assertThat(hubContainer.getSamlResponse()).isEqualTo(TRANSFORMED_SAML_RESPONSE);
        assertThat(hubContainer.getPostEndpoint()).isEqualTo(POST_ENDPOINT);
        assertThat(hubContainer.getRelayState().get()).isEqualTo(RELAY_STATE);
        assertThat(hubContainer.getResponseId()).isEqualTo(RESPONSE_ID);
    }

    @Test(expected = UnableToGenerateSamlException.class)
    public void generateWithAuthnResponseFromCountryContainerDtoShouldThrowUnableToGenerateSamlExceptionIfExceptionIsCaught() {
        when(outboundResponseFromHubToResponseTransformerFactory.getCountryTransformer()).thenThrow(NullPointerException.class);
        generatorService.generate(countryContainer);
    }
}
