package uk.gov.ida.hub.samlproxy.handlers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.joda.time.DateTime;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.shared.security.Certificate;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.samlproxy.SamlProxyConfiguration;
import uk.gov.ida.saml.metadata.EntitiesDescriptorNameCriterion;
import uk.gov.ida.saml.metadata.domain.HubIdentityProviderMetadataDto;
import uk.gov.ida.saml.metadata.domain.OrganisationDto;
import uk.gov.ida.saml.metadata.domain.SamlEndpointDto;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Collections.singletonList;
import static uk.gov.ida.hub.samlproxy.Urls.FrontendUrls.SAML2_SSO_REQUEST_ENDPOINT;

public class HubAsIdpMetadataHandler {

    private static final Logger LOG = Logger.getLogger(HubAsIdpMetadataHandler.class.getName());

    private static final OrganisationDto organisationDto = new OrganisationDto("GOV.UK", "GOV.UK", "https://www.gov.uk");

    private final MetadataResolver metadataResolver;
    private final String hubEntityId;
    private final String hubFederationId;
    private final SamlProxyConfiguration samlProxyConfiguration;

    @Inject
    public HubAsIdpMetadataHandler(
            MetadataResolver metadataResolver,
            SamlProxyConfiguration samlProxyConfiguration,
            @Named("HubEntityId") String hubEntityId,
            @Named("HubFederationId") String hubFederationId) {

        this.samlProxyConfiguration = samlProxyConfiguration;
        this.metadataResolver = metadataResolver;
        this.hubEntityId = hubEntityId;
        this.hubFederationId = hubFederationId;
    }

    public HubIdentityProviderMetadataDto getMetadataAsAnIdentityProvider() {
        URI hubFrontend = samlProxyConfiguration.getFrontendExternalUri();

        SamlEndpointDto binding = new SamlEndpointDto(SamlEndpointDto.Binding.POST, URI.create(hubFrontend + SAML2_SSO_REQUEST_ENDPOINT));

        Iterable<EntityDescriptor> entityDescriptors;
        try {
            CriteriaSet criteria = new CriteriaSet(new EntitiesDescriptorNameCriterion(hubFederationId));
            entityDescriptors = metadataResolver.resolve(criteria);
            LOG.info("Retrieved metadata from " + samlProxyConfiguration.getMetadataConfiguration().getUri());
        } catch (ResolverException e) {
            throw ApplicationException.createUnauditedException(ExceptionType.METADATA_PROVIDER_EXCEPTION, e.getMessage(), e);
        }

        final Iterable<EntityDescriptor> idpEntityDescriptors = StreamSupport
                .stream(entityDescriptors.spliterator(), false)
                .filter(input -> input.getIDPSSODescriptor(SAMLConstants.SAML20P_NS) != null)
                .collect(Collectors.toList());

        final Iterable<EntityDescriptor> hubEntityDescriptors = StreamSupport
                .stream(entityDescriptors.spliterator(), false)
                .filter(input -> input.getEntityID().equals(hubEntityId))
                .collect(Collectors.toList());

        final Iterable<List<Certificate>> idpSigningCertificates = StreamSupport
                .stream(idpEntityDescriptors.spliterator(), false)
                .map(this::getIDPSigningCertificates)
                .collect(Collectors.toList());

        final Iterable<Certificate> hubEncryptionCertificate = StreamSupport
                .stream(hubEntityDescriptors.spliterator(), false)
                .map(this::getHubEncryptionCertificate)
                .collect(Collectors.toList());

        final Iterable<List<Certificate>> hubSigningCertificates = StreamSupport
                .stream(hubEntityDescriptors.spliterator(), false)
                .map(this::getHubSigningCertificates)
                .collect(Collectors.toList());

        return new HubIdentityProviderMetadataDto(
                singletonList(binding),
                hubEntityId,
                organisationDto,
                Collections.emptySet(),
                ImmutableList.copyOf(Iterables.concat(idpSigningCertificates)),
                DateTime.now().plus(samlProxyConfiguration.getMetadataValidDuration().toMilliseconds()),
                ImmutableList.copyOf(Iterables.concat(hubSigningCertificates)),
                hubEncryptionCertificate.iterator().next()
        );
    }

    private List<Certificate> getHubSigningCertificates(EntityDescriptor input) {
        return extractSigningCerts(input.getSPSSODescriptor(SAMLConstants.SAML20P_NS).getKeyDescriptors(), input.getEntityID());
    }

    private List<Certificate> getIDPSigningCertificates(EntityDescriptor input) {
        return extractSigningCerts(input.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getKeyDescriptors(), input.getEntityID());
    }

    private List<Certificate> extractSigningCerts(List<KeyDescriptor> keyDescriptors, String entityId) {
        return keyDescriptors
                .stream()
                .filter(keyDescriptor -> keyDescriptor.getUse() == UsageType.SIGNING)
                .map(keyDescriptor -> keyDescriptor.getKeyInfo().getX509Datas())
                .flatMap(List::stream)
                .map(X509Data::getX509Certificates)
                .flatMap(List::stream)
                .map(x509Certificate -> new Certificate(entityId, x509Certificate.getValue(), Certificate.KeyUse.Signing))
                .collect(Collectors.toList());
    }

    private Certificate getHubEncryptionCertificate(EntityDescriptor entityDescriptor) {
        KeyDescriptor hubEncryptionKey = entityDescriptor.getSPSSODescriptor(SAMLConstants.SAML20P_NS).getKeyDescriptors()
                .stream()
                .filter(input1 -> input1.getUse() == UsageType.ENCRYPTION) //there should only be one and only one hub encryption key
                .findFirst()
                .get();
        X509Certificate x509Certificate = hubEncryptionKey.getKeyInfo().getX509Datas().get(0).getX509Certificates().get(0);
        return new Certificate(entityDescriptor.getEntityID(), x509Certificate.getValue(), Certificate.KeyUse.Encryption);
    }
}
