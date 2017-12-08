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
import uk.gov.ida.hub.samlengine.domain.SamlRequestDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;
import uk.gov.ida.saml.core.domain.AuthnContext;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_ONE;

public class IdpAuthnRequestGeneratorResourceTest {

    private static Client client;

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
            ConfigOverride.config("configUri", configStub.baseUri().build().toASCIIString())
    );

    @BeforeClass
    public static void setUp() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration).build(IdpAuthnRequestGeneratorResourceTest.class.getSimpleName());
    }

    @Test
    public void sendAuthnRequest_shouldRespondWithSamlRequest() throws Exception {
        final String idpEntityId = STUB_IDP_ONE;
        final URI ssoUri = URI.create("http://foo.com/bar");

        IdaAuthnRequestFromHubDto idaAuthnRequestFromHubDto = new IdaAuthnRequestFromHubDto("1", asList(AuthnContext.LEVEL_2), Optional.of(false), new DateTime(), idpEntityId, false);

        final URI uri = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_IDP_AUTHN_REQUEST_RESOURCE);
        Response clientResponse = client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(idaAuthnRequestFromHubDto, MediaType.APPLICATION_JSON_TYPE));

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        SamlRequestDto samlRequestDto = clientResponse.readEntity(SamlRequestDto.class);
        assertThat(samlRequestDto.getSamlRequest()).isNotNull();
        assertThat(samlRequestDto.getSsoUri()).isEqualTo(ssoUri);
    }

}
