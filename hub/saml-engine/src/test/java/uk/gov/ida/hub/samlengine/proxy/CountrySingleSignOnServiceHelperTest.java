package uk.gov.ida.hub.samlengine.proxy;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.saml.saml2.metadata.impl.EntityDescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.IDPSSODescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.SingleSignOnServiceBuilder;
import uk.gov.ida.saml.metadata.EidasMetadataResolverRepository;

import java.net.URI;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CountrySingleSignOnServiceHelperTest {
    @Test
    public void getSingleSignOn() throws Exception {
        // Given
        SingleSignOnServiceBuilder singleSignOnServiceBuilder = new SingleSignOnServiceBuilder();
        SingleSignOnService singleSignOnService = singleSignOnServiceBuilder.buildObject();
        singleSignOnService.setLocation("http://the-sso-location");

        IDPSSODescriptorBuilder idpssoDescriptorBuilder = new IDPSSODescriptorBuilder();
        IDPSSODescriptor idpssoDescriptor = idpssoDescriptorBuilder.buildObject();
        idpssoDescriptor.getSingleSignOnServices().add(singleSignOnService);
        idpssoDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);

        EntityDescriptorBuilder entityDescriptorBuilder = new EntityDescriptorBuilder();
        EntityDescriptor entityDescriptor = entityDescriptorBuilder.buildObject();
        entityDescriptor.setEntityID("an-entity-id");
        entityDescriptor.getRoleDescriptors().add(idpssoDescriptor);

        EidasMetadataResolverRepository eidasMetadataResolverRepository = mock(EidasMetadataResolverRepository.class);

        MetadataResolver metadataResolver = mock(MetadataResolver.class);
        when(eidasMetadataResolverRepository.getMetadataResolver(any())).thenReturn(Optional.of(metadataResolver));

        CountrySingleSignOnServiceHelper service = new CountrySingleSignOnServiceHelper(eidasMetadataResolverRepository);

        // When
        when(metadataResolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(entityDescriptor.getEntityID())))).thenReturn(entityDescriptor);
        URI singleSignOnUri = service.getSingleSignOn(entityDescriptor.getEntityID());

        // Then
        assertThat(singleSignOnUri.toString(), equalTo(singleSignOnService.getLocation()));
        verify(metadataResolver).resolveSingle(any(CriteriaSet.class));
    }
}
