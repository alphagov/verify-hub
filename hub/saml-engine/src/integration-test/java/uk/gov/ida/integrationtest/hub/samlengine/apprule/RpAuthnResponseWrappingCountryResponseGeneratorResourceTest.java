package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import net.shibboleth.utilities.java.support.codec.Base64Support;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;
import uk.gov.ida.saml.core.domain.AuthnResponseFromCountryContainerDto;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.deserializers.parser.SamlObjectParser;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;

public class RpAuthnResponseWrappingCountryResponseGeneratorResourceTest {

    private static Client client;
    private static final String BASE_64_SAML_RESPONSE = "base64SamlResponse";
    private static final String ENCRYPTED_KEY = "base64EncryptedKey";
    private static final String COUNTRY_ENTITY_ID = "countryEntityId";
    private static final URI POST_ENDPOINT = URI.create("https://post-endpoint");
    private static final String RELAY_STATE = "relayState";
    private static final String IN_RESPONSE_TO = "inResponseTo";
    private static final String RESPONSE_ID = "responseId";

    private CountrySignedResponseContainer countrySignedResponseContainer;
    private URI endpoint;

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @Rule
    public SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
            config("configUri", configStub.baseUri().build().toASCIIString())
    );

    @Before
    public void setUp() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        if (client == null ) {
            client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration).build(RpAuthnResponseGeneratorResourceTest.class.getSimpleName());
        }
        countrySignedResponseContainer = new CountrySignedResponseContainer(
                BASE_64_SAML_RESPONSE,
                List.of(ENCRYPTED_KEY),
                COUNTRY_ENTITY_ID
        );
        configStub.reset();
        endpoint = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_WRAPPING_COUNTRY_RESPONSE_RESOURCE);
    }

    @Test
    public void testShouldReturnAResponseWithOriginalCountryResponseAndEncryptedAssertionKeysAsAttributes() throws Exception {
        configStub.setupCertificatesForEntity(TestEntityIds.TEST_RP);
        AuthnResponseFromCountryContainerDto countryContainerDto = new AuthnResponseFromCountryContainerDto(
                countrySignedResponseContainer,
                POST_ENDPOINT,
                Optional.of(RELAY_STATE),
                IN_RESPONSE_TO,
                TestEntityIds.TEST_RP,
                RESPONSE_ID
        );

        Response httpResponse = client.target(endpoint).request().post(Entity.entity(countryContainerDto, MediaType.APPLICATION_JSON_TYPE));

        assertThat(httpResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        AuthnResponseFromHubContainerDto result = httpResponse.readEntity(AuthnResponseFromHubContainerDto.class);
        org.opensaml.saml.saml2.core.Response response = extractResponse(result);

        assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
        assertThat(response.getAssertions()).isEmpty();
        assertThat(response.getID()).isEqualTo(countryContainerDto.getResponseId());
        assertThat(response.getInResponseTo()).isEqualTo(countryContainerDto.getInResponseTo());
        assertThat(response.getIssuer().getValue()).isEqualTo(TestEntityIds.HUB_ENTITY_ID);
        assertThat(response.getDestination()).isEqualTo(countryContainerDto.getPostEndpoint().toString());
        assertThat(response.getSignature()).isNotNull();
        assertThat(response.getEncryptedAssertions()).isNotEmpty();

    }

    @Test
    public void shouldReturnAnErrorResponseGivenBadInput() {
        AuthnResponseFromCountryContainerDto countryContainerDto = new AuthnResponseFromCountryContainerDto(
                countrySignedResponseContainer,
                null,
                Optional.of(RELAY_STATE),
                IN_RESPONSE_TO,
                TestEntityIds.TEST_RP,
                RESPONSE_ID
        );

        Response httpResponse = client.target(endpoint).request().post(Entity.entity(countryContainerDto, MediaType.APPLICATION_JSON_TYPE));

        assertThat(httpResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = httpResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_INPUT);
    }

    private org.opensaml.saml.saml2.core.Response extractResponse(AuthnResponseFromHubContainerDto actualResult) throws Exception {
        return new SamlObjectParser().getSamlObject(new String(Base64Support.decode(actualResult.getSamlResponse())));
    }

}
