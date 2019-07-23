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
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;


public class CertificatesResourceIntegrationTest {
    public static Client client;
    private static final String RP_ENTITY_ID = "rp-entity-id";
    private static final String RP_ENTITY_ID_BAD_SIGNATURE_CERT = "rp-entity-id-bad-cert";
    private static final String RP_ENTITY_ID_BAD_ENCRYPTION_CERT = "rp-entity-id-bad-encryption-cert";
    private static final String RP_MS_ENTITY_ID = "rp-ms-entity-id";
    private static final String BAD_CERTIFICATE_VALUE = "MIIEZzCCA0+gAwIBAgIQX/UeEoUFa9978uQ8FbLFyDANBgkqhkiG9w0BAQsFADBZMQswCQYDVQQGEwJHQjEXMBUGA1UEChMOQ2FiaW5ldCBPZmZpY2UxDDAKBgNVBAsTA0dEUzEjMCEGA1UEAxMaSURBUCBSZWx5aW5nIFBhcnR5IFRlc3QgQ0EwHhcNMTUwODI3MDAwMDAwWhcNMTcwODI2MjM1OTU5WjCBgzELMAkGA1UEBhMCR0IxDzANBgNVBAgTBkxvbmRvbjEPMA0GA1UEBxMGTG9uZG9uMRcwFQYDVQQKFA5DYWJpbmV0IE9mZmljZTEMMAoGA1UECxQDR0RTMSswKQYDVQQDEyJTYW1wbGUgUlAgU2lnbmluZyAoMjAxNTA4MjYxNjMzMDcpMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuIdy6fiwdlLpMOsOiZC8DXcAU1eKDKz0w04TRAdUMR4rdv36IcyTfUortDHQ60pmX4I/s5iksey4UHCqTNZKpw6coCboyFGtGy1M6tTFhrxKc/pZmjEqV0kqgfjUnVWqiOnjpuWOJsCRfScjGfJ4Gio0omnrfX6KOTrnieaSM7aZJ7WkWUe4KRGOyxBywRIyFFbUeNgIbD/IfV7GFZCLUa9XwKjnaidTTmEhihC0TiBcnl3NCeqSwNK0TsIYSh/k5i7U/QeIvc6w34lacHOsqL5woRMPBnmS91brY/hy/vdePx7Nk8Hiwx7VpLsn5b0BVJnEZcLs5gwDid0Vra+6kQIDAQABo4H/MIH8MAwGA1UdEwEB/wQCMAAwYQYDVR0fBFowWDBWoFSgUoZQaHR0cDovL29uc2l0ZWNybC50cnVzdHdpc2UuY29tL0NhYmluZXRPZmZpY2VJREFQUmVseWluZ1BhcnR5VGVzdENBL0xhdGVzdENSTC5jcmwwDgYDVR0PAQH/BAQDAgeAMB0GA1UdDgQWBBSsYjo5j/oZAQ/h35orm1VR+n5hVTAfBgNVHSMEGDAWgBTd5PVdGgoPOtFIIh5OwPhuNvbFJTA5BggrBgEFBQcBAQQtMCswKQYIKwYBBQUHMAGGHWh0dHA6Ly9zdGQtb2NzcC50cnVzdHdpc2UuY29tMA0GCSqGSIb3DQEBCwUAA4IBAQBHYp/kWufCENWW8xI/rwVRJrOjvYxbhyEM61QoMZzTqfSQVuaBCv1qwXTMU8D+iPVtSVStFdU+vxWrU0z8ZQcd9107wZtnIJWwoJJ4WJlrmXTzBNvlqc8Q57G4Y/x9SZZdyVn4JrQRK8Vm5NzZqYZeXqgMk5xeQEObY8EQFmdryZeh/B2j0WFm3ywXOYcz77a1e1WCxBgOULPh1sQD793KjbJlEUfyeq5w/cIPovI8u4xXa78ionzq+L9t3oRh/wuTNjG/qezgArncr53sV2RZzb45RtT9+PxdQ1YFbQM7lL526kxVij0+FS6+b+EBx2CBVLWalmOugi0vA9vYpZJL";
    private static final String BAD_SIGNATURE_CERTIFICATE = BAD_CERTIFICATE_VALUE;
    private static final String BAD_ENCRYPTION_CERTIFICATE = BAD_CERTIFICATE_VALUE;


    @ClassRule
    public static ConfigAppRule configAppRule = new ConfigAppRule()
            .addTransaction(aTransactionConfigData()
                    .withEntityId(RP_ENTITY_ID)
                    .withMatchingServiceEntityId(RP_MS_ENTITY_ID)
                    .build())
            .addTransaction(aTransactionConfigData()
                    .withEntityId(RP_ENTITY_ID_BAD_ENCRYPTION_CERT)
                    .withMatchingServiceEntityId(RP_MS_ENTITY_ID)
                    .withEncryptionCertificate(BAD_ENCRYPTION_CERTIFICATE)
                    .build())
            .addTransaction(aTransactionConfigData()
                    .withEntityId(RP_ENTITY_ID_BAD_SIGNATURE_CERT)
                    .withMatchingServiceEntityId(RP_MS_ENTITY_ID)
                    .addSignatureVerificationCertificate(BAD_SIGNATURE_CERTIFICATE)
                    .build())
            .addMatchingService(aMatchingServiceConfig()
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
    public void getEncryptionCertificate_returnsNotFoundForEntityThatDoesExistButContainsInvalidCertificate(){
        String entityId = RP_ENTITY_ID_BAD_ENCRYPTION_CERT;
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
    public void getSignatureVerificationCertificates_returnsNotFoundForEntityThatDoesExistButContainsInvalidCertificate(){
        String entityId = RP_ENTITY_ID_BAD_SIGNATURE_CERT;
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
