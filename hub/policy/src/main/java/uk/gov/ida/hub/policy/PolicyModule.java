package uk.gov.ida.hub.policy;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import io.dropwizard.setup.Environment;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.masterslave.MasterSlave;
import io.lettuce.core.masterslave.StatefulRedisMasterSlaveConnection;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.shared.security.IdGenerator;
import uk.gov.ida.eventemitter.Configuration;
import uk.gov.ida.hub.shared.eventsink.EventSinkHttpProxy;
import uk.gov.ida.hub.shared.eventsink.EventSinkProxy;
import uk.gov.ida.hub.policy.annotations.Config;
import uk.gov.ida.hub.policy.annotations.SamlEngine;
import uk.gov.ida.hub.policy.annotations.SamlSoapProxy;
import uk.gov.ida.hub.policy.configuration.AssertionLifetimeConfiguration;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.configuration.RedisConfiguration;
import uk.gov.ida.hub.policy.controllogic.AuthnRequestFromTransactionHandler;
import uk.gov.ida.hub.policy.controllogic.ResponseFromIdpHandler;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.controller.StateControllerFactory;
import uk.gov.ida.hub.policy.factories.SamlAuthnResponseTranslatorDtoFactory;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;
import uk.gov.ida.hub.policy.proxy.SamlSoapProxyProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.redis.SessionStoreRedisCodec;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.services.AuthnResponseFromCountryService;
import uk.gov.ida.hub.policy.services.AuthnResponseFromIdpService;
import uk.gov.ida.hub.policy.services.CountriesService;
import uk.gov.ida.hub.policy.services.Cycle3Service;
import uk.gov.ida.hub.policy.services.MatchingServiceResponseService;
import uk.gov.ida.hub.policy.services.SessionService;
import uk.gov.ida.hub.policy.session.RedisSessionStore;
import uk.gov.ida.hub.policy.session.SessionStore;
import uk.gov.ida.jerseyclient.DefaultClientProvider;
import uk.gov.ida.jerseyclient.ErrorHandlingClient;
import uk.gov.ida.jerseyclient.JsonClient;
import uk.gov.ida.jerseyclient.JsonResponseProcessor;
import uk.gov.ida.restclient.ClientProvider;
import uk.gov.ida.restclient.RestfulClientConfiguration;
import uk.gov.ida.truststore.ClientTrustStoreConfiguration;
import uk.gov.ida.truststore.KeyStoreLoader;
import uk.gov.ida.truststore.KeyStoreProvider;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import java.net.URI;
import java.security.KeyStore;

import static java.util.Collections.singletonList;

public class PolicyModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(RestfulClientConfiguration.class).to(PolicyConfiguration.class).in(Scopes.SINGLETON);
        bind(AssertionLifetimeConfiguration.class).to(PolicyConfiguration.class).in(Scopes.SINGLETON);
        bind(Client.class).toProvider(DefaultClientProvider.class).in(Scopes.SINGLETON);
        bind(KeyStore.class).toProvider(KeyStoreProvider.class).in(Scopes.SINGLETON);
        bind(KeyStoreLoader.class).toInstance(new KeyStoreLoader());
        bind(SessionStoreStartupTasks.class).asEagerSingleton();
        bind(JsonResponseProcessor.class);
        bind(HubEventLogger.class);
        bind(SessionService.class);
        bind(CountriesService.class);
        bind(AuthnRequestFromTransactionHandler.class);
        bind(SessionRepository.class);
        bind(StateControllerFactory.class);
        bind(SamlEngineProxy.class);
        bind(TransactionsConfigProxy.class);
        bind(IdentityProvidersConfigProxy.class);
        bind(AuthnResponseFromIdpService.class);
        bind(AuthnResponseFromCountryService.class);
        bind(SamlAuthnResponseTranslatorDtoFactory.class).toInstance(new SamlAuthnResponseTranslatorDtoFactory());
        bind(IdGenerator.class).toInstance(new IdGenerator());
        bind(AttributeQueryService.class);
        bind(SamlSoapProxyProxy.class);
        bind(ResponseFromHubFactory.class);
        bind(AssertionRestrictionsFactory.class);
        bind(MatchingServiceConfigProxy.class);
        bind(Cycle3Service.class);
        bind(MatchingServiceResponseService.class);
        bind(ResponseFromIdpHandler.class);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    private ObjectMapper getObjectMapper(Environment environment) {
        return environment.getObjectMapper();
    }

    @Provides
    private Configuration getEventEmitterConfiguration(final PolicyConfiguration configuration) {
        return configuration.getEventEmitterConfiguration();
    }

    @Provides
    @Singleton
    public JsonClient jsonClient(JsonResponseProcessor jsonResponseProcessor, Environment environment, PolicyConfiguration configuration) {
        Client client = new ClientProvider(
                environment,
                configuration.getJerseyClientConfiguration(),
                configuration.getEnableRetryTimeOutConnections(),
                "policyClient").get();
        ErrorHandlingClient errorHandlingClient = new ErrorHandlingClient(client);
        return new JsonClient(errorHandlingClient, jsonResponseProcessor);
    }

    @Provides
    @Singleton
    @Named("samlSoapProxyClient")
    public JsonClient forSamlSoapProxy(JsonResponseProcessor responseProcessor, PolicyConfiguration configuration, Environment environment) {
        Client client = new ClientProvider(
                environment,
                configuration.getSamlSoapProxyClient(),
                configuration.getEnableRetryTimeOutConnections(),
                "SamlSoapProxyClient").get();
        ErrorHandlingClient errorHandlingClient = new ErrorHandlingClient(client);
        return new JsonClient(errorHandlingClient, responseProcessor);
    }

    @Provides
    @Singleton
    public SessionStore getSessionStore(PolicyConfiguration configuration) {
        return configuration.getSessionStoreConfiguration().getRedisConfiguration()
                .<SessionStore>map(this::getRedisSessionStore)
                .orElseThrow();

    }

    private RedisSessionStore getRedisSessionStore(RedisConfiguration config) {
        RedisClient redisClient = RedisClient.create();
        StatefulRedisMasterSlaveConnection<SessionId, State> redisConnection = MasterSlave.connect(
                redisClient,
                new SessionStoreRedisCodec(getRedisObjectMapper()),
                singletonList(config.getUri())
        );
        RedisCommands<SessionId, State> redisCommands = redisConnection.sync();
        return new RedisSessionStore(redisCommands, config.getRecordTTL());
    }

    public static ObjectMapper getRedisObjectMapper() {
        return new ObjectMapper()
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModule(new JodaModule())
                .registerModule(new Jdk8Module())
                .registerModule(new GuavaModule());
    }

    @Provides
    @Singleton
    public ServiceInfoConfiguration serviceInfo(PolicyConfiguration policyConfiguration) {
        return policyConfiguration.getServiceInfo();
    }

    @Provides
    @Singleton
    public ClientTrustStoreConfiguration clientTrustStoreConfiguration(PolicyConfiguration policyConfiguration) {
        return policyConfiguration.getClientTrustStoreConfiguration();
    }

    @Provides
    @Config
    @Singleton
    public URI configUri(PolicyConfiguration policyConfiguration) {
       return policyConfiguration.getConfigUri();
    }

    @Provides
    @Singleton
    public EventSinkProxy eventSinkProxy(JsonClient jsonClient, PolicyConfiguration policyConfiguration, Environment environment) {
        URI eventSinkUri = policyConfiguration.getEventSinkUri();
        if (eventSinkUri != null) {
            return new EventSinkHttpProxy(jsonClient, eventSinkUri, environment);
        }
        return event -> {};
    }


    @Provides
    @SamlEngine
    @Singleton
    public URI samlEngineUri(PolicyConfiguration policyConfiguration) {
        return policyConfiguration.getSamlEngineUri();
    }

    @Provides
    @SamlSoapProxy
    @Singleton
    public URI samlSoapProxyUri(PolicyConfiguration policyConfiguration) {
        return policyConfiguration.getSamlSoapProxyUri();
    }
}
