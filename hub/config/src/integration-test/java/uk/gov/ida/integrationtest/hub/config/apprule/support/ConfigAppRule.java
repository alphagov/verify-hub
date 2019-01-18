package uk.gov.ida.integrationtest.hub.config.apprule.support;

import certificates.values.CACertificates;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.ImmutableList;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.prometheus.client.CollectorRegistry;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.apache.commons.io.FileUtils;
import uk.gov.ida.hub.config.ConfigApplication;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.domain.CountriesConfigEntityData;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.domain.TranslationData;

import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.util.Arrays.asList;
import static uk.gov.ida.hub.config.domain.builders.CountriesConfigEntityDataBuilder.aCountriesConfigEntityData;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigEntityDataBuilder.aMatchingServiceConfigEntityData;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigEntityDataBuilder.aTransactionConfigData;
import static uk.gov.ida.hub.config.domain.builders.TranslationDataBuilder.aTranslationData;

public class ConfigAppRule extends DropwizardAppRule<ConfigConfiguration> {

    private static final KeyStoreResource clientTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    private static final KeyStoreResource rpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rpCA", CACertificates.TEST_RP_CA).build();
    private static final File FED_CONFIG_ROOT = new File(System.getProperty("java.io.tmpdir"), "test-fed-config");
    private static final String TRANSLATIONS_RELATIVE_PATH = "../display-locales/transactions";
    private static final File TRANSLATIONS_ROOT = new File(FED_CONFIG_ROOT.getAbsolutePath() + "/" + TRANSLATIONS_RELATIVE_PATH);
    private final ObjectMapper mapper = new ObjectMapper();

    private List<TransactionConfigEntityData> transactions = new ArrayList<>();
    private List<TranslationData> translations = new ArrayList<>();
    private List<MatchingServiceConfigEntityData> matchingServices = new ArrayList<>();
    private List<IdentityProviderConfigEntityData> idps = new ArrayList<>();
    private List<CountriesConfigEntityData> countries = new ArrayList<>();

    public ConfigAppRule(ConfigOverride... configOverrides) {
        super(ConfigApplication.class,
                ResourceHelpers.resourceFilePath("config.yml"),
                withDefaultOverrides(configOverrides)
        );
    }

    public static ConfigOverride[] withDefaultOverrides(ConfigOverride ... configOverrides) {
        ImmutableList<ConfigOverride> overrides = ImmutableList.<ConfigOverride>builder()
                .add(config("clientTrustStoreConfiguration.path", clientTrustStore.getAbsolutePath()))
                .add(config("clientTrustStoreConfiguration.password", clientTrustStore.getPassword()))
                .add(config("rpTrustStoreConfiguration.path", rpTrustStore.getAbsolutePath()))
                .add(config("rpTrustStoreConfiguration.password", rpTrustStore.getPassword()))
                .add(config("rootDataDirectory", FED_CONFIG_ROOT.getAbsolutePath()))
                .add(config("translationsDirectory", TRANSLATIONS_RELATIVE_PATH))
                .add(configOverrides)
                .build();
        return overrides.toArray(new ConfigOverride[overrides.size()]);
    }

    public ConfigAppRule addTransaction(TransactionConfigEntityData transaction) {
        this.transactions.add(transaction);
        return this;
    }

    public ConfigAppRule addMatchingService(MatchingServiceConfigEntityData matchingService) {
        this.matchingServices.add(matchingService);
        return this;
    }

    public ConfigAppRule addIdp(IdentityProviderConfigEntityData idp) {
        this.idps.add(idp);
        return this;
    }

    public ConfigAppRule addCountry(CountriesConfigEntityData country) {
        this.countries.add(country);
        return this;
    }

    @Override
    protected void before() {
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
                aMatchingServiceConfigEntityData().withEntityId("default-rp-msa-entity-id").build()
            );
        }
        if (idps.isEmpty()) {
            idps.add(
                anIdentityProviderConfigData()
                    .withEntityId("default-idp-entity-id")
                    .withOnboarding(asList("default-rp-entity-id"))
                    .build()
            );
        }
        if (countries.isEmpty()) {
            countries.add(
                aCountriesConfigEntityData()
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
            FileUtils.write(new File(folder.getAbsolutePath(), folder.getName() + Integer.toString(index) + ".yml"), mapper.writeValueAsString(content));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
