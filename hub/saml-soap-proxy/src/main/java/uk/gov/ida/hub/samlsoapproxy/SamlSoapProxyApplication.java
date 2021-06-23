package uk.gov.ida.hub.samlsoapproxy;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;
import uk.gov.ida.bundles.LoggingBundle;
import uk.gov.ida.bundles.MonitoringBundle;
import uk.gov.ida.bundles.ServiceStatusBundle;
import uk.gov.ida.common.shared.security.TrustStoreMetrics;
import uk.gov.ida.eventemitter.EventEmitterModule;
import uk.gov.ida.hub.samlsoapproxy.filters.SessionIdQueryParamLoggingFilter;
import uk.gov.ida.hub.samlsoapproxy.resources.AttributeQueryRequestSenderResource;
import uk.gov.ida.hub.samlsoapproxy.resources.MatchingServiceHealthCheckResource;
import uk.gov.ida.hub.samlsoapproxy.resources.MatchingServiceVersionCheckResource;
import uk.gov.ida.hub.shared.guice.GuiceBundle;
import uk.gov.ida.metrics.bundle.PrometheusBundle;
import uk.gov.ida.saml.core.IdaSamlBootstrap;
import uk.gov.ida.saml.metadata.MetadataResolverConfiguration;
import uk.gov.ida.saml.metadata.bundle.MetadataResolverBundle;
import uk.gov.ida.truststore.ClientTrustStoreConfiguration;
import uk.gov.ida.truststore.KeyStoreLoader;

import javax.servlet.DispatcherType;
import java.security.KeyStore;
import java.util.EnumSet;

import static java.util.Arrays.asList;

public class SamlSoapProxyApplication extends Application<SamlSoapProxyConfiguration> {

    private final MetadataResolverBundle<SamlSoapProxyConfiguration> verifyMetadataBundle = new MetadataResolverBundle<>((SamlSoapProxyConfiguration::getMetadataConfiguration));

    public static void main(String[] args) throws Exception {
        new SamlSoapProxyApplication().run(args);
    }

    @Override
    public String getName() {
        return "SamlSoapProxy Service";
    }


    @Override
    public final void initialize(Bootstrap<SamlSoapProxyConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );


        bootstrap.addBundle(verifyMetadataBundle);
        GuiceBundle<SamlSoapProxyConfiguration> guiceBundle = new GuiceBundle<>(
                () -> asList(new SamlSoapProxyModule(), new EventEmitterModule(), bindVerifyMetadata()),
                SamlSoapProxyConfiguration.class
        );
        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new ServiceStatusBundle());
        bootstrap.addBundle(new MonitoringBundle());
        bootstrap.addBundle(new LoggingBundle());
        bootstrap.addBundle(new PrometheusBundle());
    }

    private AbstractModule bindVerifyMetadata() {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(MetadataResolver.class)
                        .toProvider(verifyMetadataBundle.getMetadataResolverProvider());
                bind(ExplicitKeySignatureTrustEngine.class)
                        .annotatedWith(Names.named(SamlSoapProxyModule.VERIFY_METADATA_TRUST_ENGINE))
                        .toProvider(verifyMetadataBundle.getSignatureTrustEngineProvider());
            }
        };
    }

    @Override
    public void run(SamlSoapProxyConfiguration configuration, Environment environment) {
        IdaSamlBootstrap.bootstrap();
        environment.getObjectMapper().setDateFormat(new StdDateFormat());
        registerResources(environment);

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

    private void registerResources(Environment environment) {
        environment.jersey().register(AttributeQueryRequestSenderResource.class);
        environment.jersey().register(MatchingServiceHealthCheckResource.class);
        environment.jersey().register(MatchingServiceVersionCheckResource.class);
    }
}
