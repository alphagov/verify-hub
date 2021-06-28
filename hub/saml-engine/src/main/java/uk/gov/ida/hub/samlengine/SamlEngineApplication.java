package uk.gov.ida.hub.samlengine;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;
import org.slf4j.MDC;
import ru.vyarus.dropwizard.guice.GuiceBundle;
import uk.gov.ida.bundles.LoggingBundle;
import uk.gov.ida.bundles.MonitoringBundle;
import uk.gov.ida.bundles.ServiceStatusBundle;
import uk.gov.ida.common.shared.security.TrustStoreMetrics;
import uk.gov.ida.hub.samlengine.filters.SessionIdQueryParamLoggingFilter;
import uk.gov.ida.hub.samlengine.resources.translators.IdpAuthnRequestGeneratorResource;
import uk.gov.ida.hub.samlengine.resources.translators.IdpAuthnResponseTranslatorResource;
import uk.gov.ida.hub.samlengine.resources.translators.MatchingServiceHealthcheckRequestGeneratorResource;
import uk.gov.ida.hub.samlengine.resources.translators.MatchingServiceHealthcheckResponseTranslatorResource;
import uk.gov.ida.hub.samlengine.resources.translators.MatchingServiceRequestGeneratorResource;
import uk.gov.ida.hub.samlengine.resources.translators.MatchingServiceResponseTranslatorResource;
import uk.gov.ida.hub.samlengine.resources.translators.RpAuthnRequestTranslatorResource;
import uk.gov.ida.hub.samlengine.resources.translators.RpAuthnResponseGeneratorResource;
import uk.gov.ida.hub.samlengine.resources.translators.RpErrorResponseGeneratorResource;
import uk.gov.ida.metrics.bundle.PrometheusBundle;
import uk.gov.ida.saml.core.IdaSamlBootstrap;
import uk.gov.ida.saml.metadata.MetadataResolverConfiguration;
import uk.gov.ida.saml.metadata.bundle.MetadataResolverBundle;
import uk.gov.ida.truststore.ClientTrustStoreConfiguration;
import uk.gov.ida.truststore.KeyStoreLoader;

import javax.servlet.DispatcherType;
import java.security.KeyStore;
import java.util.EnumSet;

public class SamlEngineApplication extends Application<SamlEngineConfiguration> {

    private final MetadataResolverBundle<SamlEngineConfiguration> verifyMetadataBundle;

    public SamlEngineApplication() {
        verifyMetadataBundle = new MetadataResolverBundle<>(SamlEngineConfiguration::getMetadataConfiguration);
    }

    @Override
    public String getName() {
        return "Saml Engine Service";
    }

    @Override
    public final void initialize(Bootstrap<SamlEngineConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );

        MDC.clear();
        bootstrap.addBundle(new ServiceStatusBundle());
        bootstrap.addBundle(new MonitoringBundle());
        bootstrap.addBundle(new LoggingBundle());
        bootstrap.addBundle(verifyMetadataBundle);
        bootstrap.addBundle(
                GuiceBundle.builder().enableAutoConfig(getClass().getPackage().getName())
                .modules(
                        new SamlEngineModule(),
                        new CryptoModule(),
                        bindMetadata()
                )
                .build()
        );

        bootstrap.addBundle(new PrometheusBundle());
    }

    private Module bindMetadata() {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(MetadataResolver.class)
                        .annotatedWith(Names.named(SamlEngineModule.VERIFY_METADATA_RESOLVER))
                        .toProvider(verifyMetadataBundle.getMetadataResolverProvider());

                bind(ExplicitKeySignatureTrustEngine.class)
                        .annotatedWith(Names.named(SamlEngineModule.VERIFY_METADATA_SIGNATURE_TRUST_ENGINE))
                        .toProvider(verifyMetadataBundle.getSignatureTrustEngineProvider());
            }
        };
    }

    // this can be overridden in integration tests
    protected void registerResources(Environment environment, SamlEngineConfiguration configuration) {
        environment.jersey().register(IdpAuthnRequestGeneratorResource.class);
        environment.jersey().register(IdpAuthnResponseTranslatorResource.class);
        environment.jersey().register(RpAuthnRequestTranslatorResource.class);
        environment.jersey().register(RpAuthnResponseGeneratorResource.class);
        environment.jersey().register(RpErrorResponseGeneratorResource.class);
        environment.jersey().register(MatchingServiceRequestGeneratorResource.class);
        environment.jersey().register(MatchingServiceResponseTranslatorResource.class);
        environment.jersey().register(MatchingServiceHealthcheckRequestGeneratorResource.class);
        environment.jersey().register(MatchingServiceHealthcheckResponseTranslatorResource.class);
    }

    @Override
    public final void run(SamlEngineConfiguration configuration, Environment environment) {
        IdaSamlBootstrap.bootstrap();

        environment.getObjectMapper().registerModule(new GuavaModule());
        environment.getObjectMapper().setDateFormat(new StdDateFormat());

        // register resources
        registerResources(environment, configuration);

        // calling .get() here is safe because the Optional is never empty
        MetadataResolverConfiguration metadataConfiguration = configuration.getMetadataConfiguration().get();
        ClientTrustStoreConfiguration rpTrustStoreConfiguration = configuration.getRpTrustStoreConfiguration();
        KeyStore rpTrustStore = new KeyStoreLoader().load(rpTrustStoreConfiguration.getPath(), rpTrustStoreConfiguration.getPassword());
        TrustStoreMetrics trustStoreMetrics = new TrustStoreMetrics();
        metadataConfiguration.getHubTrustStore().ifPresent(hubTrustStore -> trustStoreMetrics.registerTrustStore("hub", hubTrustStore));
        metadataConfiguration.getIdpTrustStore().ifPresent(idpTrustStore -> trustStoreMetrics.registerTrustStore("idp", idpTrustStore));
        trustStoreMetrics.registerTrustStore("rp", rpTrustStore);

        environment.servlets().addFilter("Logging SessionId registration Filter", SessionIdQueryParamLoggingFilter.class).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }

    public static void main(String[] args) throws Exception {
        new SamlEngineApplication().run(args);
    }

}
