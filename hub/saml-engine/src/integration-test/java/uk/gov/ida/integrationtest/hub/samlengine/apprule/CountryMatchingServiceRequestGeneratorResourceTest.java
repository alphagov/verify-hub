package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.builders.EidasAttributeQueryRequestDtoBuilder;
import uk.gov.ida.hub.samlengine.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlengine.domain.EidasAttributeQueryRequestDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

public class CountryMatchingServiceRequestGeneratorResourceTest {

    private static Client client;

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
        config("configUri", configStub.baseUri().build().toASCIIString())
    );

    @BeforeClass
    public static void setupClass() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder
            .aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration)
            .build(CountryMatchingServiceRequestGeneratorResourceTest.class.getSimpleName());
    }

    @Before
    public void beforeEach() throws Exception {
        configStub.setupCertificatesForEntity(TEST_RP_MS, TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PUBLIC_ENCRYPTION_CERT);
        configStub.setupCertificatesForEntity(TEST_RP, TEST_RP_PUBLIC_SIGNING_CERT, TEST_RP_PUBLIC_ENCRYPTION_CERT);
    }

    @Test
    public void shouldCreateAttributeQueryRequest() throws Exception {

        EidasAttributeQueryRequestDto eidasAttributeQueryRequestDto = new EidasAttributeQueryRequestDtoBuilder().withAnEncryptedIdentityAssertion().build();
        Response response = generateEidasAttributeQueryRequest(eidasAttributeQueryRequestDto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AttributeQueryContainerDto attributeQueryContainerDto = response.readEntity(AttributeQueryContainerDto.class);
        assertThat(attributeQueryContainerDto.getId()).isEqualTo(eidasAttributeQueryRequestDto.getRequestId());
        assertThat(attributeQueryContainerDto.getIssuer()).isEqualTo(HUB_ENTITY_ID);
        assertThat(attributeQueryContainerDto.getMatchingServiceUri()).isEqualTo(eidasAttributeQueryRequestDto.getAttributeQueryUri());
        assertThat(attributeQueryContainerDto.getAttributeQueryClientTimeOut()).isEqualTo(eidasAttributeQueryRequestDto.getMatchingServiceRequestTimeOut());
        assertThat(attributeQueryContainerDto.isOnboarding()).isEqualTo(eidasAttributeQueryRequestDto.isOnboarding());
        assertThat(attributeQueryContainerDto.getSamlRequest()).contains("saml2p:AttributeQuery");
    }

    @Test
    public void shouldCreateAttributeQueryRequestUnsigned() {
        EidasAttributeQueryRequestDto eidasAttributeQueryRequestDto = new EidasAttributeQueryRequestDtoBuilder()
                .withAnEncryptedIdentityAssertion()
                .withACountrySignedResponseWithIssuer("a country entity id")
                .build();
        Response response = generateEidasAttributeQueryRequest(eidasAttributeQueryRequestDto);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    private Response generateEidasAttributeQueryRequest(EidasAttributeQueryRequestDto dto) {
        final URI uri = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_COUNTRY_ATTRIBUTE_QUERY_RESOURCE);
        return client.target(uri)
            .request()
            .post(Entity.json(dto), Response.class);
    }
}
