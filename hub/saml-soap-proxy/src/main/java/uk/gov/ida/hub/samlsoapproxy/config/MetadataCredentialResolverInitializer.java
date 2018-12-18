package uk.gov.ida.hub.samlsoapproxy.config;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.PredicateRoleDescriptorResolver;
import org.opensaml.saml.security.impl.MetadataCredentialResolver;
import org.opensaml.xmlsec.config.impl.DefaultSecurityConfigurationBootstrap;
import org.opensaml.xmlsec.keyinfo.KeyInfoCredentialResolver;

public class MetadataCredentialResolverInitializer {
    private MetadataCredentialResolver metadataCredentialResolver;
    private PredicateRoleDescriptorResolver predicateRoleDescriptorResolver;
    private KeyInfoCredentialResolver keyInfoCredentialResolver;

    public MetadataCredentialResolverInitializer(MetadataResolver metadataResolver) {
        metadataCredentialResolver = new MetadataCredentialResolver();
        predicateRoleDescriptorResolver = new PredicateRoleDescriptorResolver(metadataResolver);
        keyInfoCredentialResolver = DefaultSecurityConfigurationBootstrap.buildBasicInlineKeyInfoCredentialResolver();
    }

    public MetadataCredentialResolver initialize() throws ComponentInitializationException {
        predicateRoleDescriptorResolver.initialize();
        metadataCredentialResolver.setRoleDescriptorResolver(predicateRoleDescriptorResolver);
        metadataCredentialResolver.setKeyInfoCredentialResolver(keyInfoCredentialResolver);
        metadataCredentialResolver.initialize();
        return metadataCredentialResolver;
    }
}
