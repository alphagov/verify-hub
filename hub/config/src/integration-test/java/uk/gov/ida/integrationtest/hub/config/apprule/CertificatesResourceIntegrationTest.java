package uk.gov.ida.integrationtest.hub.config.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.dto.CertificateDto;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppRule;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigEntityDataBuilder.aTransactionConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigEntityDataBuilder.aMatchingServiceConfigEntityData;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;


public class CertificatesResourceIntegrationTest {
    public static Client client;
    private static final String RP_ENTITY_ID = "rp-entity-id";
    private static final String RP_MS_ENTITY_ID = "rp-ms-entity-id";

    @ClassRule
    public static ConfigAppRule configAppRule = new ConfigAppRule()
            .addTransaction(aTransactionConfigData()
                    .withEntityId(RP_ENTITY_ID)
                    .withMatchingServiceEntityId(RP_MS_ENTITY_ID)
                    .build())
            .addMatchingService(aMatchingServiceConfigEntityData()
                    .withEntityId(RP_MS_ENTITY_ID)
                    .build())
            .addIdp(anIdentityProviderConfigData()
                    .withEntityId("idp-entity-id")
                    .withOnboarding(asList(RP_ENTITY_ID))
                    .build());

    @BeforeClass
    public static void setUp() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(configAppRule.getEnvironment()).using(jerseyClientConfiguration).build(CertificatesResourceIntegrationTest.class.getSimpleName());
    }

    @Test
    public void getEncryptionCertificate_returnsOkAndEncryptionCertificate(){
        String entityId = RP_MS_ENTITY_ID;
        Response response = getForEntityIdAndPath(entityId, Urls.ConfigUrls.ENCRYPTION_CERTIFICATES_RESOURCE);
        assertForEntityId(entityId, response);
    }

    @Test
    public void getEncyptionCertificate_returnsOkAndEncryptionCertificateForMatchingService(){
        String entityId = RP_ENTITY_ID;
        Response response = getForEntityIdAndPath(entityId, Urls.ConfigUrls.ENCRYPTION_CERTIFICATES_RESOURCE);
        assertForEntityId(entityId, response);
    }

    @Test
    public void getEncryptionCertificate_returnsNotFoundForEntityThatDoesNotExist(){
        String entityId = "not-found";
        Response response = getForEntityIdAndPath(entityId, Urls.ConfigUrls.ENCRYPTION_CERTIFICATES_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getSignatureVerificationCertificates_returnsOkCollection(){
        String entityId = RP_ENTITY_ID;
        Response response = getForEntityIdAndPath(entityId, Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(Collection.class)).isNotEmpty();
    }

    @Test
    public void getSignatureVerificationCertificates_returnsOkCollectionForMatchingService(){
        String entityId = RP_MS_ENTITY_ID;
        Response response = getForEntityIdAndPath(entityId, Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(Collection.class)).isNotEmpty();
    }

    @Test
    public void getSignatureVerificationCertificates_returnsNotFoundForEntityThatDoesNotExist(){
        String entityId = "not-found";
        Response response = getForEntityIdAndPath(entityId, Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getHealthCheck_returnsOk(){
        URI uri = configAppRule.getUri(Urls.ConfigUrls.CERTIFICATES_HEALTH_CHECK_RESOURCE).build();
        Response response =  client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(Collection.class)).isNotEmpty();
    }

    @Ignore("This will be replaced by an acceptance test when certs and keys will be dynamically generated for testing")
    @Test
    public void invalidCertificatesCheck_returnsOk(){
        URI uri = configAppRule.getUri(Urls.ConfigUrls.INVALID_CERTIFICATES_CHECK_RESOURCE).build();
        Response response =  client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(Collection.class)).isEmpty();
    }

    private void assertForEntityId(String entityId, Response response){
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(CertificateDto.class).getIssuerId()).isEqualTo(entityId);
    }
    private Response getForEntityIdAndPath(String entityId, String path) {
        URI uri = configAppRule.getUri(path).buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        return client.target(uri).request().get();
    }
}
