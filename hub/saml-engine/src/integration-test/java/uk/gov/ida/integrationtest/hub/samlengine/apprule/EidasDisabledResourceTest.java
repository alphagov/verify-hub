package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.IdaAuthnRequestFromHubDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;
import uk.gov.ida.saml.core.domain.AuthnContext;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static java.util.Arrays.asList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_ONE;

public class EidasDisabledResourceTest {

    private static Client client;

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(false,
            ConfigOverride.config("configUri", configStub.baseUri().build().toASCIIString())
    );

    @BeforeClass
    public static void setUp() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration).build(EidasDisabledResourceTest.class.getSimpleName());
    }

    @Test
    public void generateAuthnRequestShouldReturnNotFound() throws Exception {
        Response clientResponse = generateCountryAuthnRequest();

        assertThat(clientResponse.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void translateAuthnResponseShouldReturnNotFound() {
        Response clientResponse = translateAuthnResponse();

        assertThat(clientResponse.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void generateMatchingServiceRequestShouldReturnNotFound() {
        Response clientResponse = generateMatchingServiceRequest();

        assertThat(clientResponse.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    private Response generateCountryAuthnRequest() {
        IdaAuthnRequestFromHubDto idaAuthnRequestFromHubDto = new IdaAuthnRequestFromHubDto(
                "1",
                asList(AuthnContext.LEVEL_2),
                Optional.of(false),
                new DateTime(),
                STUB_IDP_ONE,
                false);

        return client.target(samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_COUNTRY_AUTHN_REQUEST_RESOURCE))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(idaAuthnRequestFromHubDto, MediaType.APPLICATION_JSON_TYPE));
    }

    private Response generateMatchingServiceRequest() {
        return client.target(samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_COUNTRY_ATTRIBUTE_QUERY_RESOURCE))
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE));
    }

    private Response translateAuthnResponse() {
        return client.target(samlEngineAppRule.getUri(Urls.SamlEngineUrls.TRANSLATE_COUNTRY_AUTHN_RESPONSE_RESOURCE))
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE));
    }
}
