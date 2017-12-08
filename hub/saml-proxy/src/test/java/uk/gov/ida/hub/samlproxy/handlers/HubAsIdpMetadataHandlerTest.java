package uk.gov.ida.hub.samlproxy.handlers;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import io.dropwizard.util.Duration;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.saml.metadata.criteria.entity.impl.EntityDescriptorCriterionPredicateRegistry;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import uk.gov.ida.common.shared.security.Certificate;
import uk.gov.ida.hub.samlproxy.SamlProxyConfiguration;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.metadata.EntitiesDescriptorNameCriterion;
import uk.gov.ida.saml.metadata.EntitiesDescriptorNamePredicate;
import uk.gov.ida.saml.metadata.MetadataConfiguration;
import uk.gov.ida.saml.metadata.StringBackedMetadataResolver;
import uk.gov.ida.saml.metadata.domain.HubIdentityProviderMetadataDto;
import uk.gov.ida.saml.metadata.test.factories.metadata.MetadataFactory;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Throwables.propagate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HubAsIdpMetadataHandlerTest {

    private final String hubFederationId = "VERIFY-FEDERATION";

    @Mock
    private SamlProxyConfiguration samlProxyConfiguration;
    @Mock
    private MetadataConfiguration metadataConfiguration;

    private final MetadataResolver metadataResolver = initializeMetadata();

    private MetadataResolver initializeMetadata() {
        try {
            InitializationService.initialize();
            String content = new MetadataFactory().defaultMetadata();
            StringBackedMetadataResolver metadataResolver = new StringBackedMetadataResolver(content);
            BasicParserPool pool = new BasicParserPool();
            pool.initialize();
            metadataResolver.setParserPool(pool);
            metadataResolver.setId("testResolver");
            metadataResolver.setResolveViaPredicatesOnly(true);
            metadataResolver.setRequireValidMetadata(true);
            EntityDescriptorCriterionPredicateRegistry registry = new EntityDescriptorCriterionPredicateRegistry();
            registry.register(EntitiesDescriptorNameCriterion.class, EntitiesDescriptorNamePredicate.class);
            metadataResolver.setCriterionPredicateRegistry(registry);
            metadataResolver.initialize();
            return metadataResolver;
        } catch (ComponentInitializationException | InitializationException e) {
            throw propagate(e);
        }
    }

    private HubAsIdpMetadataHandler handler;

    @Before
    public void setUp() throws Exception {
        when(samlProxyConfiguration.getFrontendExternalUri()).thenReturn(URI.create("http://localhost"));
        when(samlProxyConfiguration.getMetadataValidDuration()).thenReturn(Duration.parse("5h"));
        when(samlProxyConfiguration.getMetadataConfiguration()).thenReturn(metadataConfiguration);

        handler = new HubAsIdpMetadataHandler(
                metadataResolver,
                samlProxyConfiguration,
                TestEntityIds.HUB_ENTITY_ID,
                hubFederationId);
    }

    @Test
    public void shouldReturnSingleHubEncryptionCert() throws Exception {
        HubIdentityProviderMetadataDto metadataAsAnIdentityProvider = handler.getMetadataAsAnIdentityProvider();

        final List<Certificate> encryptionCertificates = metadataAsAnIdentityProvider.getEncryptionCertificates();
        assertThat(encryptionCertificates).hasSize(1);
        final Optional<Certificate> hubEncryptionCertificate = Iterables.tryFind(encryptionCertificates, getPredicateByIssuerId(TestEntityIds.HUB_ENTITY_ID));
        assertThat(hubEncryptionCertificate.isPresent()).isTrue();
        assertThat(hubEncryptionCertificate.get().getKeyUse()).isEqualTo(Certificate.KeyUse.Encryption);
    }

    @Test
    public void shouldReturnHubSigningCerts() throws Exception {
        HubIdentityProviderMetadataDto metadataAsAnIdentityProvider = handler.getMetadataAsAnIdentityProvider();

        final List<Certificate> signingCertificates = metadataAsAnIdentityProvider.getSigningCertificates();
        assertThat(signingCertificates).hasSize(2);
        assertThat(signingCertificates.get(0).getKeyUse()).isEqualTo(Certificate.KeyUse.Signing);
        assertThat(signingCertificates.get(1).getKeyUse()).isEqualTo(Certificate.KeyUse.Signing);
    }

    @Test
    public void shouldReturnListOfIDPSigningCerts() throws Exception {
        HubIdentityProviderMetadataDto metadataAsAnIdentityProvider = handler.getMetadataAsAnIdentityProvider();

        List<String> idpSigningCertificates = metadataAsAnIdentityProvider
                .getIdpSigningCertificates()
                .stream()
                .map(Certificate::getIssuerId)
                .collect(Collectors.toList());
        assertThat(idpSigningCertificates).containsOnly(TestEntityIds.STUB_IDP_ONE, TestEntityIds.STUB_IDP_TWO, TestEntityIds.STUB_IDP_THREE, TestEntityIds.STUB_IDP_FOUR);
    }

    private Predicate<Certificate> getPredicateByIssuerId(final String issuerId) {
        return input -> input.getIssuerId().equals(issuerId);
    }
}
