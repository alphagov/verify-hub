package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlengine.domain.AttributeQueryRequestDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;
import uk.gov.ida.integrationtest.hub.samlengine.builders.AttributeQueryRequestBuilder;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

public class MatchingServiceRequestGeneratorResourceTest {
    private static Client client;

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
            config("configUri", configStub.baseUri().build().toASCIIString())
    );

    @BeforeClass
    public static void setup() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder
                .aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration)
                .build(MatchingServiceRequestGeneratorResourceTest.class.getSimpleName());
    }

    @Test
    public void should_createAttributeQueryRequest() throws Exception {
        configStub.setupCertificatesForEntity(TEST_RP_MS);
        configStub.setUpStubForMatchingServiceDetails(TEST_RP_MS);
        configStub.setUpStubForRPMetadataEnabled(TEST_RP_MS);

        Response response = getAttributeQuery(new AttributeQueryRequestBuilder().build());
        AttributeQueryContainerDto entity = response.readEntity(AttributeQueryContainerDto.class);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(entity.getSamlRequest()).isNotNull();
    }

    private Response getAttributeQuery(AttributeQueryRequestDto dto) {
        final URI uri = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_ATTRIBUTE_QUERY_RESOURCE);
        return client.target(uri)
                .request()
                .post(Entity.json(dto), Response.class);

    }
}
