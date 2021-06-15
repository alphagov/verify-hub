package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import io.dropwizard.testing.ResourceHelpers;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.hub.samlengine.SamlEngineApplication;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.IdaAuthnRequestFromHubDto;
import uk.gov.ida.hub.samlengine.domain.SamlRequestDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension;
import uk.gov.ida.saml.core.domain.AuthnContext;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_ONE;

public class IdpAuthnRequestGeneratorResourceTest {

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
    public void sendAuthnRequest_shouldRespondWithSamlRequest() throws Exception {
        final String idpEntityId = STUB_IDP_ONE;
        final URI ssoUri = URI.create("http://foo.com/bar");

        IdaAuthnRequestFromHubDto idaAuthnRequestFromHubDto = new IdaAuthnRequestFromHubDto("1", singletonList(AuthnContext.LEVEL_2), Optional.of(false), new DateTime(), idpEntityId, false);

        Response clientResponse = client.targetMain(Urls.SamlEngineUrls.GENERATE_IDP_AUTHN_REQUEST_RESOURCE)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(idaAuthnRequestFromHubDto, MediaType.APPLICATION_JSON_TYPE));

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        SamlRequestDto samlRequestDto = clientResponse.readEntity(SamlRequestDto.class);
        assertThat(samlRequestDto.getSamlRequest()).isNotNull();
        assertThat(samlRequestDto.getSsoUri()).isEqualTo(ssoUri);
    }

}
