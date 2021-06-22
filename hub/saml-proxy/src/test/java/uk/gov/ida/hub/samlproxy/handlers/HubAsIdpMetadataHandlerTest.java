package uk.gov.ida.hub.samlproxy.handlers;

import java.util.Optional;
import io.dropwizard.util.Duration;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
            throw new RuntimeException(e);
        }
    }

    private HubAsIdpMetadataHandler handler;

    @Before
    public void setUp() {
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
    public void shouldReturnSingleHubEncryptionCert() {
        HubIdentityProviderMetadataDto metadataAsAnIdentityProvider = handler.getMetadataAsAnIdentityProvider();

        final List<Certificate> encryptionCertificates = metadataAsAnIdentityProvider.getEncryptionCertificates();
        assertThat(encryptionCertificates).hasSize(1);
        final Optional<Certificate> hubEncryptionCertificate = encryptionCertificates.stream().filter(getPredicateByIssuerId(TestEntityIds.HUB_ENTITY_ID)).findFirst();
        assertThat(hubEncryptionCertificate.isPresent()).isTrue();
        assertThat(hubEncryptionCertificate.get().getKeyUse()).isEqualTo(Certificate.KeyUse.Encryption);
    }

    @Test
    public void shouldReturnHubSigningCerts() {
        HubIdentityProviderMetadataDto metadataAsAnIdentityProvider = handler.getMetadataAsAnIdentityProvider();

        final List<Certificate> signingCertificates = metadataAsAnIdentityProvider.getSigningCertificates();
        assertThat(signingCertificates).hasSize(2);
        assertThat(signingCertificates.get(0).getKeyUse()).isEqualTo(Certificate.KeyUse.Signing);
        assertThat(signingCertificates.get(1).getKeyUse()).isEqualTo(Certificate.KeyUse.Signing);
    }

    @Test
    public void shouldReturnListOfIDPSigningCerts() {
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
