package uk.gov.ida.integrationtest.hub.samlproxy.apprule.support;

import certificates.values.CACertificates;
import httpstub.HttpStubRule;
import io.dropwizard.Application;
import io.prometheus.client.CollectorRegistry;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.opensaml.core.config.InitializationService;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.Constants;
import uk.gov.ida.saml.metadata.test.factories.metadata.MetadataFactory;

import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;

public class SamlProxyAppExtension extends TestDropwizardAppExtension {
    private static final String VERIFY_METADATA_PATH = "/uk/gov/ida/saml/metadata/federation";
    private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----\n";
    private static final String END_CERT = "\n-----END CERTIFICATE-----";

    private static final HttpStubRule verifyMetadataServer = new HttpStubRule();

    private static final KeyStoreResource metadataTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("metadataCA", CACertificates.TEST_METADATA_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).build();
    private static final KeyStoreResource hubTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("hubCA", CACertificates.TEST_CORE_CA).build();
    private static final KeyStoreResource idpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    private static final KeyStoreResource rpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rpCA", CACertificates.TEST_RP_CA).build();

    public static SamlProxyBuilder forApp(final Class<? extends Application> app) {
        return new SamlProxyBuilder(app);
    }

    public static class SamlProxyBuilder extends TestDropwizardAppExtension.Builder {
        public SamlProxyBuilder(Class<? extends Application> app) {
            super(app);
            metadataTrustStore.create();
            hubTrustStore.create();
            idpTrustStore.create();
            rpTrustStore.create();
            CollectorRegistry.defaultRegistry.clear();

            try {
                InitializationService.initialize();
                verifyMetadataServer.reset();
                verifyMetadataServer.register(
                        VERIFY_METADATA_PATH,
                        200,
                        Constants.APPLICATION_SAMLMETADATA_XML,
                        new MetadataFactory().defaultMetadata()
                );

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public SamlProxyBuilder withDefaultConfigOverridesAnd(String... extraOverrides) {
            String[] defaultOverrides = {
                    "saml.entityId: " + HUB_ENTITY_ID,
                    "server.applicationConnectors[0].port: 0",
                    "server.adminConnectors[0].port: 0",
                    "rpTrustStoreConfiguration.path: " + rpTrustStore.getAbsolutePath(),
                    "rpTrustStoreConfiguration.password: " + rpTrustStore.getPassword(),
                    "metadata.trustStore.path: " + metadataTrustStore.getAbsolutePath(),
                    "metadata.trustStore.password: " + metadataTrustStore.getPassword(),
                    "metadata.uri: " + "http://localhost:" + verifyMetadataServer.getPort() + VERIFY_METADATA_PATH,
                    "metadata.hubTrustStore.path: " + hubTrustStore.getAbsolutePath(),
                    "metadata.hubTrustStore.password: " + hubTrustStore.getPassword(),
                    "metadata.idpTrustStore.path: " + idpTrustStore.getAbsolutePath(),
                    "metadata.idpTrustStore.password: " + idpTrustStore.getPassword(),
                    "certificatesConfigCacheExpiry: 20s",
                    "eventEmitterConfiguration.enabled: false"
            };
            this.configOverrides(ArrayUtils.addAll(defaultOverrides, extraOverrides));
            return this;
        }
    }

    public static void tearDown() {
        metadataTrustStore.delete();
        hubTrustStore.delete();
        idpTrustStore.delete();
        rpTrustStore.delete();
    }
}
