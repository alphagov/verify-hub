package uk.gov.ida.hub.shared.guice;


import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.InjectionManagerProvider;
import org.glassfish.jersey.inject.hk2.DelayedHk2InjectionManager;
import org.glassfish.jersey.inject.hk2.ImmediateHk2InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import java.util.List;

public class GuiceBridgeFeature implements Feature, Provider<ServiceLocator> {

    private static final Logger LOG = LoggerFactory.getLogger(GuiceBridgeFeature.class);

    private ServiceLocator serviceLocator;

    public Injector getInjector() {
        return injector;
    }

    private Injector injector;

    @Inject
    public GuiceBridgeFeature(List<Module> modules) {
        injector = Guice.createInjector(modules);
    }

    @Override
    public boolean configure(FeatureContext context) {
        InjectionManager injectionManager = InjectionManagerProvider.getInjectionManager(context);
        if (injectionManager instanceof ImmediateHk2InjectionManager) {
            serviceLocator =
                    ((ImmediateHk2InjectionManager)
                            InjectionManagerProvider.getInjectionManager(context))
                            .getServiceLocator();
        } else if (injectionManager instanceof DelayedHk2InjectionManager) {
            serviceLocator =
                    ((DelayedHk2InjectionManager)
                            InjectionManagerProvider.getInjectionManager(context))
                            .getServiceLocator();
        }
        if (serviceLocator != null) {
            GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
            serviceLocator.getService(GuiceIntoHK2Bridge.class).bridgeGuiceInjector(injector);
            return true;
        } else {
            LOG.error("Unable to get ServiceLocator from the InjectionManager.");
            return false;
        }
    }

    @Override
    public ServiceLocator get() {
        return serviceLocator;
    }
}

