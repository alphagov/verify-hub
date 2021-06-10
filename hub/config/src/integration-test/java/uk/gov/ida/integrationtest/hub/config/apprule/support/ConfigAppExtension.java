package uk.gov.ida.integrationtest.hub.config.apprule.support;

import certificates.values.CACertificates;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import io.dropwizard.Application;
import io.prometheus.client.CollectorRegistry;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.domain.TranslationData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;
import static uk.gov.ida.hub.config.domain.builders.TranslationDataBuilder.aTranslationData;

public class ConfigAppExtension extends TestDropwizardAppExtension {

    public static final KeyStoreResource clientTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    public static final KeyStoreResource rpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rpCA", CACertificates.TEST_RP_CA).build();
    public static final File FED_CONFIG_ROOT = new File(System.getProperty("java.io.tmpdir"), "test-fed-config");
    public static final String TRANSLATIONS_RELATIVE_PATH = "../display-locales/transactions";
    private static final File TRANSLATIONS_ROOT = new File(FED_CONFIG_ROOT.getAbsolutePath() + "/" + TRANSLATIONS_RELATIVE_PATH);

    public static ConfigBuilder forApp(final Class<? extends Application> app) {
        return new ConfigBuilder(app);
    }

    public static class ConfigBuilder extends TestDropwizardAppExtension.Builder {
        private List<TransactionConfig> transactions = new ArrayList<>();
        private List<TranslationData> translations = new ArrayList<>();
        private List<MatchingServiceConfig> matchingServices = new ArrayList<>();
        private List<IdentityProviderConfig> idps = new ArrayList<>();
        private final ObjectMapper mapper = new ObjectMapper();

        public ConfigBuilder(Class<? extends Application> app) {
            super(app);
            mapper.registerModule(new Jdk8Module().configureAbsentsAsNulls(true));
            mapper.registerModule(new JodaModule());
        }

        public ConfigBuilder addTransaction(TransactionConfig transaction) {
            this.transactions.add(transaction);
            return this;
        }

        public ConfigBuilder addMatchingService(MatchingServiceConfig matchingService) {
            this.matchingServices.add(matchingService);
            return this;
        }

        public ConfigBuilder addIdp(IdentityProviderConfig idp) {
            this.idps.add(idp);
            return this;
        }

        public ConfigBuilder writeFederationConfig() {
            clientTrustStore.create();
            rpTrustStore.create();
            createFedConfig();
            createTranslations();
            return this;
        }

        public ConfigBuilder withDefaultConfigOverridesAnd(String... extraOverrides) {
            String[] defaultOverrides = {
                    "clientTrustStoreConfiguration.path: " +  clientTrustStore.getAbsolutePath(),
                    "clientTrustStoreConfiguration.password: " + clientTrustStore.getPassword(),
                    "rpTrustStoreConfiguration.path: " + rpTrustStore.getAbsolutePath(),
                    "rpTrustStoreConfiguration.password: " + rpTrustStore.getPassword(),
                    "rootDataDirectory: " + FED_CONFIG_ROOT.getAbsolutePath(),
                    "translationsDirectory: " + TRANSLATIONS_RELATIVE_PATH
            };

            this.configOverrides(ArrayUtils.addAll(defaultOverrides, extraOverrides));
            return this;
        }

        public ConfigBuilder withClearedCollectorRegistry() {
            CollectorRegistry.defaultRegistry.clear();
            return this;
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
            try {
                FileUtils.write(new File(folder.getAbsolutePath(), folder.getName() + index + ".yml"), mapper.writeValueAsString(content));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
