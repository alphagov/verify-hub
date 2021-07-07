package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.IdaAuthnRequestFromHubDto;
import uk.gov.ida.hub.samlengine.domain.SamlRequestDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension.SamlEngineAppExtensionBuilder;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension.SamlEngineClient;
import uk.gov.ida.saml.core.domain.AuthnContext;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_ONE;

public class IdpAuthnRequestGeneratorResourceTest {

    @Order(0)
    @RegisterExtension
    public static ConfigStubExtension configStub = new ConfigStubExtension();

    @Order(1)
    @RegisterExtension
    public static SamlEngineAppExtension samlEngineApp = new SamlEngineAppExtensionBuilder()
            .withConfigOverrides(
                    config("configUri", () -> configStub.baseUri().build().toASCIIString())
            )
            .build();


    private SamlEngineClient client;

    @BeforeEach
    void setup() { client = samlEngineApp.getClient(); }

    @AfterAll
    public static void afterAll() {
        samlEngineApp.tearDown();
    }

    @Test
    public void sendAuthnRequest_shouldRespondWithSamlRequest() throws Exception {
        final String idpEntityId = STUB_IDP_ONE;
        final URI ssoUri = URI.create("http://foo.com/bar");

        IdaAuthnRequestFromHubDto idaAuthnRequestFromHubDto = new IdaAuthnRequestFromHubDto("1", singletonList(AuthnContext.LEVEL_2), Optional.of(false), new DateTime(), idpEntityId, false);

        Response clientResponse = client.postTargetMain(
                Urls.SamlEngineUrls.GENERATE_IDP_AUTHN_REQUEST_RESOURCE,
                idaAuthnRequestFromHubDto
        );

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        SamlRequestDto samlRequestDto = clientResponse.readEntity(SamlRequestDto.class);
        assertThat(samlRequestDto.getSamlRequest()).isNotNull();
        assertThat(samlRequestDto.getSsoUri()).isEqualTo(ssoUri);
    }

}
