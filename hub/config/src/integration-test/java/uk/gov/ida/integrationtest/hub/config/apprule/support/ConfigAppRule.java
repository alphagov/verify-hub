package uk.gov.ida.integrationtest.hub.config.apprule.support;

import certificates.values.CACertificates;
import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.prometheus.client.CollectorRegistry;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.apache.commons.io.FileUtils;
import uk.gov.ida.hub.config.ConfigApplication;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.domain.CountryConfig;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.domain.TranslationData;

import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static uk.gov.ida.hub.config.domain.builders.CountryConfigBuilder.aCountryConfig;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;
import static uk.gov.ida.hub.config.domain.builders.TranslationDataBuilder.aTranslationData;

public class ConfigAppRule extends DropwizardAppRule<ConfigConfiguration> {

    private static final KeyStoreResource clientTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    private static final KeyStoreResource rpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rpCA", CACertificates.TEST_RP_CA).build();
    private static final File FED_CONFIG_ROOT = new File(System.getProperty("java.io.tmpdir"), "test-fed-config");
    private static final String TRANSLATIONS_RELATIVE_PATH = "../display-locales/transactions";
    private static final File TRANSLATIONS_ROOT = new File(FED_CONFIG_ROOT.getAbsolutePath() + "/" + TRANSLATIONS_RELATIVE_PATH);
    private final ObjectMapper mapper = new ObjectMapper();

    private List<TransactionConfig> transactions = new ArrayList<>();
    private List<TranslationData> translations = new ArrayList<>();
    private List<MatchingServiceConfig> matchingServices = new ArrayList<>();
    private List<IdentityProviderConfig> idps = new ArrayList<>();
    private List<CountryConfig> countries = new ArrayList<>();

    public ConfigAppRule(Supplier<AmazonS3> s3ClientSupplier, ConfigOverride... configOverrides) {
        super(ConfigIntegrationApplication.class,
                ResourceHelpers.resourceFilePath("config.yml"),
                withDefaultOverrides(configOverrides)
        );
        ConfigIntegrationApplication.setS3ClientSupplier(s3ClientSupplier);
    }

    public ConfigAppRule(ConfigOverride... configOverrides) {
        super(ConfigApplication.class,
                ResourceHelpers.resourceFilePath("config.yml"),
                withDefaultOverrides(configOverrides)
        );
    }

    private static ConfigOverride[] withDefaultOverrides(ConfigOverride ... configOverrides) {
        List<ConfigOverride> overrides = new ArrayList<>(List.of(
                config("clientTrustStoreConfiguration.path", clientTrustStore.getAbsolutePath()),
                config("clientTrustStoreConfiguration.password", clientTrustStore.getPassword()),
                config("rpTrustStoreConfiguration.path", rpTrustStore.getAbsolutePath()),
                config("rpTrustStoreConfiguration.password", rpTrustStore.getPassword()),
                config("rootDataDirectory", FED_CONFIG_ROOT.getAbsolutePath()),
                config("translationsDirectory", TRANSLATIONS_RELATIVE_PATH)));
        if (configOverrides != null) {
            overrides.addAll(asList(configOverrides));
        }
        return overrides.toArray(new ConfigOverride[0]);
    }

    public ConfigAppRule addTransaction(TransactionConfig transaction) {
        this.transactions.add(transaction);
        return this;
    }

    public ConfigAppRule addMatchingService(MatchingServiceConfig matchingService) {
        this.matchingServices.add(matchingService);
        return this;
    }

    public ConfigAppRule addIdp(IdentityProviderConfig idp) {
        this.idps.add(idp);
        return this;
    }

    public ConfigAppRule addCountry(CountryConfig country) {
        this.countries.add(country);
        return this;
    }

    @Override
    protected void before() throws Exception {
        mapper.registerModule(new Jdk8Module().configureAbsentsAsNulls(true));
        mapper.registerModule(new JodaModule());
        clientTrustStore.create();
        rpTrustStore.create();

        createFedConfig();
        createTranslations();
        CollectorRegistry.defaultRegistry.clear();

        super.before();
    }

    @Override
    protected void after() {
        rpTrustStore.delete();
        clientTrustStore.delete();

        try {
            FileUtils.deleteDirectory(FED_CONFIG_ROOT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.after();
    }

    public UriBuilder getUri(String path) {
        return UriBuilder.fromUri("http://localhost")
                .path(path)
                .port(getLocalPort());
    }

    private void createFedConfig() {
        FED_CONFIG_ROOT.mkdir();
        try {
            FileUtils.cleanDirectory(FED_CONFIG_ROOT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        File countryFolder = new File(FED_CONFIG_ROOT, "countries");
        File idpFolder = new File(FED_CONFIG_ROOT, "idps");
        File matchingServiceFolder = new File(FED_CONFIG_ROOT, "matching-services");
        File transactionFolder = new File(FED_CONFIG_ROOT, "transactions");

        countryFolder.mkdir();
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
        if (countries.isEmpty()) {
            countries.add(
                aCountryConfig()
                    .withEntityId("default-country-entity-id")
                    .withSimpleId("default-country-simple-id")
                    .build()
            );
        }

        IntStream.range(0, transactions.size()).forEach(i -> writeFile(transactionFolder, i, transactions.get(i)));
        IntStream.range(0, matchingServices.size()).forEach(i -> writeFile(matchingServiceFolder, i, matchingServices.get(i)));
        IntStream.range(0, idps.size()).forEach(i -> writeFile(idpFolder, i, idps.get(i)));
        IntStream.range(0, countries.size()).forEach(i -> writeFile(countryFolder, i, countries.get(i)));
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
