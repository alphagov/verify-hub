package uk.gov.ida.hub.shared.guice;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.matcher.Matchers;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;

public class GuiceBundle<T extends Configuration> implements ConfiguredBundle<T> {

    private static final Logger LOG = LoggerFactory.getLogger(GuiceBundle.class);

    private final ModulesProvider modulesProvider;
    private final Class<T> configurationClass;
    private uk.gov.ida.hub.shared.guice.GuiceBridgeFeature component;

    public GuiceBundle(ModulesProvider modulesProvider, Class<T> configurationClass) {
        this.modulesProvider = modulesProvider;
        this.configurationClass = configurationClass;
    }

    public GuiceBundle(ModuleProvider moduleProvider, Class<T> configurationClass) {
        this.modulesProvider = () -> singletonList(moduleProvider.get());
        this.configurationClass = configurationClass;
    }

    public Optional<Injector> getInjector() {
        return Optional.ofNullable(component).map(uk.gov.ida.hub.shared.guice.GuiceBridgeFeature::getInjector);
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {}

    @Override
    public void run(T configuration, Environment environment) throws Exception {
        component = new uk.gov.ida.hub.shared.guice.GuiceBridgeFeature(combineModules(configuration, environment));
        environment.jersey().register(component);
        environment
                .servlets()
                .addFilter("guice filter", GuiceFilter.class)
                .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
    }

    public List<Module> combineModules(T configuration, Environment environment) {
        List<Module> combinedModules =
                ImmutableList.<Module>builder()
                        .addAll(modulesProvider.get())
                        .add(dropwizardModule(configuration, environment))
                        .add(new ServletModule())
                        .build();
        LOG.info("combineModules: " + combinedModules);
        return combinedModules;
    }

    private Module dropwizardModule(T configuration, Environment environment) {
        return binder -> {
            binder.bind(Environment.class).toInstance(environment);
            binder.bind(configurationClass).toInstance(configuration);
            binder.bindListener(Matchers.any(), new uk.gov.ida.hub.shared.guice.DropwizardServiceBinder(environment));

            binder.disableCircularProxies();
            binder.requireExplicitBindings();
            binder.requireExactBindingAnnotations();
            binder.requireAtInjectOnConstructors();
        };
    }

    public interface ModulesProvider {
        List<Module> get();
    }

    public interface ModuleProvider {
        Module get();
    }
}
