package uk.gov.ida.integrationtest.hub.config.apprule.support;

import certificates.values.CACertificates;
import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.prometheus.client.CollectorRegistry;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import uk.gov.ida.hub.config.ConfigApplication;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.domain.TranslationData;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.util.Collections.singletonList;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;
import static uk.gov.ida.hub.config.domain.builders.TranslationDataBuilder.aTranslationData;

public class ConfigAppExtension extends DropwizardAppExtension {

    public static final KeyStoreResource clientTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    public static final KeyStoreResource rpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rpCA", CACertificates.TEST_RP_CA).build();
    public static final File FED_CONFIG_ROOT = new File(System.getProperty("java.io.tmpdir"), "test-fed-config");
    public static final String TRANSLATIONS_RELATIVE_PATH = "../display-locales/transactions";
    private static final File TRANSLATIONS_ROOT = new File(FED_CONFIG_ROOT.getAbsolutePath() + "/" + TRANSLATIONS_RELATIVE_PATH);

    public ConfigAppExtension(Class<ConfigApplication> applicationClass, String resourceFilePath, ConfigOverride[] defaultConfigOverrides) {
        super(applicationClass, resourceFilePath, defaultConfigOverrides);
    }

    public ConfigClient getClient() {
        return new ConfigClient();
    }

    public void tearDown() {
        rpTrustStore.delete();
        clientTrustStore.delete();

        try {
            FileUtils.deleteDirectory(FED_CONFIG_ROOT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ConfigAppExtensionBuilder {
        private Class appClass;
        private List<TransactionConfig> transactions = new ArrayList<>();
        private List<TranslationData> translations = new ArrayList<>();
        private List<MatchingServiceConfig> matchingServices = new ArrayList<>();
        private List<IdentityProviderConfig> idps = new ArrayList<>();
        private ConfigOverride[] configOverrides = new ConfigOverride[]{};
        private final ObjectMapper mapper = new ObjectMapper();

        public static ConfigAppExtensionBuilder forApp(Class appClass) {
            ConfigAppExtensionBuilder builder = new ConfigAppExtensionBuilder();
            builder.appClass = appClass;
            return builder;
        }

        public ConfigAppExtensionBuilder withS3ClientSupplier(Supplier<AmazonS3> s3ClientSupplier)  {
            try {
                appClass.getDeclaredMethod("setS3ClientSupplier", Supplier.class).invoke(ConfigIntegrationApplication.class, s3ClientSupplier);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            return this;
        }

        public ConfigAppExtensionBuilder addTransaction(TransactionConfig transaction) {
            transactions.add(transaction);
            return this;
        }

        public ConfigAppExtensionBuilder addMatchingService(MatchingServiceConfig matchingService) {
            matchingServices.add(matchingService);
            return this;
        }

        public ConfigAppExtensionBuilder addIdp(IdentityProviderConfig idp) {
            idps.add(idp);
            return this;
        }

        public ConfigAppExtensionBuilder withConfigOverrides(ConfigOverride... overrides) {
            configOverrides = overrides;
            return this;
        }

        public ConfigAppExtension build() {
            writeFederationConfig();
            CollectorRegistry.defaultRegistry.clear();

            return new ConfigAppExtension(
                    appClass,
                    ResourceHelpers.resourceFilePath("config.yml"),
                    ArrayUtils.addAll(configOverrides, defaultConfigOverrides())
            );
        }

        private static ConfigOverride[] defaultConfigOverrides() {
            return new ConfigOverride[]{
                    config("clientTrustStoreConfiguration.path", clientTrustStore.getAbsolutePath()),
                    config("clientTrustStoreConfiguration.password", clientTrustStore.getPassword()),
                    config("rpTrustStoreConfiguration.path", rpTrustStore.getAbsolutePath()),
                    config("rpTrustStoreConfiguration.password", rpTrustStore.getPassword()),
                    config("rootDataDirectory", FED_CONFIG_ROOT.getAbsolutePath()),
                    config("translationsDirectory", TRANSLATIONS_RELATIVE_PATH)
            };
        }

        private void writeFederationConfig() {
            clientTrustStore.create();
            rpTrustStore.create();
            createFedConfig();
            createTranslations();
        }

        private void createFedConfig() {
            FED_CONFIG_ROOT.mkdir();
            try {
                FileUtils.cleanDirectory(FED_CONFIG_ROOT);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            File idpFolder = new File(FED_CONFIG_ROOT, "idps");
            File matchingServiceFolder = new File(FED_CONFIG_ROOT, "matching-services");
            File transactionFolder = new File(FED_CONFIG_ROOT, "transactions");

            idpFolder.mkdir();
            matchingServiceFolder.mkdir();
            transactionFolder.mkdir();

            if (transactions.isEmpty()) {
                transactions.add(
                        aTransactionConfigData()
                                .withEntityId("default-rp-entity-id")
                                .withMatchingServiceEntityId("default-rp-msa-entity-id")
                                .build()
                );
            }
            if (matchingServices.isEmpty()) {
                matchingServices.add(
                        aMatchingServiceConfig().withEntityId("default-rp-msa-entity-id").build()
                );
            }
            if (idps.isEmpty()) {
                idps.add(
                        anIdentityProviderConfigData()
                                .withEntityId("default-idp-entity-id")
                                .withOnboarding(singletonList("default-rp-entity-id"))
                                .build()
                );
            }

            IntStream.range(0, transactions.size()).forEach(i -> writeFile(transactionFolder, i, transactions.get(i)));
            IntStream.range(0, matchingServices.size()).forEach(i -> writeFile(matchingServiceFolder, i, matchingServices.get(i)));
            IntStream.range(0, idps.size()).forEach(i -> writeFile(idpFolder, i, idps.get(i)));
        }

        private void createTranslations() {
            TRANSLATIONS_ROOT.mkdirs();
            try {
                FileUtils.cleanDirectory(TRANSLATIONS_ROOT);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (translations.isEmpty()) {
                translations.add(aTranslationData().build());
            }

            IntStream.range(0, translations.size()).forEach(i -> writeFile(TRANSLATIONS_ROOT, i, translations.get(i)));
        }

        private void writeFile(File folder, int index, Object content) {
            mapper.registerModule(new Jdk8Module().configureAbsentsAsNulls(true));
            mapper.registerModule(new JodaModule());
            try {
                FileUtils.write(new File(folder.getAbsolutePath(), folder.getName() + index + ".yml"), mapper.writeValueAsString(content));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class ConfigClient {
        private Client client;

        public ConfigClient() { client = client(); }

        public Response targetMain(URI uri) { return target(uri, getLocalPort()); };

        public Response targetMain(String path) { return targetMain(UriBuilder.fromPath(path).build()); };

        public Response targetAdmin(String path) { return target(UriBuilder.fromPath(path).build(), getAdminPort()); }

        public Response target(URI uri, int port) {
            UriBuilder uriBuilder = UriBuilder.fromUri("http://localhost").port(port).path(uri.getRawPath());
            if (uri.getQuery() != null) {
                uriBuilder.replaceQuery(uri.getQuery());
            }

            return client.target(uriBuilder.build()).request().get();
        }
    }

}
