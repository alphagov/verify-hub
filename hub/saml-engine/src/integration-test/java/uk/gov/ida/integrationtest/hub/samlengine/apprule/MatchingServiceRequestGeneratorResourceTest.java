package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.hub.samlengine.SamlEngineApplication;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlengine.domain.AttributeQueryRequestDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension;
import uk.gov.ida.integrationtest.hub.samlengine.builders.AttributeQueryRequestBuilder;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

public class MatchingServiceRequestGeneratorResourceTest {
    private static ClientSupport client;

    @Order(0)
    @RegisterExtension
    public static ConfigStubExtension configStub = new ConfigStubExtension();

    @Order(1)
    @RegisterExtension
    public static TestDropwizardAppExtension samlEngineApp = SamlEngineAppExtension.forApp(SamlEngineApplication.class)
            .withDefaultConfigOverridesAnd()
            .configOverride("configUri", () -> configStub.baseUri().build().toASCIIString())
            .config(ResourceHelpers.resourceFilePath("saml-engine.yml"))
            .randomPorts()
            .create();

    @BeforeAll
    public static void beforeClass(ClientSupport clientSupport) {
        client = clientSupport;
    }

    @AfterAll
    public static void afterAll() {
        SamlEngineAppExtension.tearDown();
    }

    @Test
    public void should_createAttributeQueryRequest() throws Exception {
        configStub.setupCertificatesForEntity(TEST_RP_MS);

        Response response = getAttributeQuery(new AttributeQueryRequestBuilder().build());
        AttributeQueryContainerDto entity = response.readEntity(AttributeQueryContainerDto.class);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(entity.getSamlRequest()).isNotNull();
    }

    private Response getAttributeQuery(AttributeQueryRequestDto dto) {
        return client.targetMain(Urls.SamlEngineUrls.GENERATE_ATTRIBUTE_QUERY_RESOURCE)
                .request()
                .post(Entity.json(dto), Response.class);
    }
}
