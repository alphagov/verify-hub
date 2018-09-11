package uk.gov.ida.hub.samlproxy.factories;

import com.google.inject.Inject;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.slf4j.event.Level;
import uk.gov.ida.hub.samlproxy.security.AuthnResponseKeyStore;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.metadata.EidasMetadataResolverRepository;
import uk.gov.ida.saml.metadata.IdpMetadataPublicKeyStore;
import uk.gov.ida.saml.security.CredentialFactorySignatureValidator;
import uk.gov.ida.saml.security.SamlMessageSignatureValidator;
import uk.gov.ida.saml.security.SigningCredentialFactory;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;

import static java.text.MessageFormat.format;

public class EidasValidatorFactory {

    private EidasMetadataResolverRepository metadataResolverRepository;

    @Inject
    public EidasValidatorFactory(EidasMetadataResolverRepository metadataResolverRepository) {
        this.metadataResolverRepository = metadataResolverRepository;
    }

    public ValidatedResponse getValidatedResponse(Response response) {
        String entityId = response.getIssuer().getValue();
        SamlResponseSignatureValidator samlResponseSignatureValidator = new SamlResponseSignatureValidator(getSamlMessageSignatureValidator(entityId));
        return samlResponseSignatureValidator.validate(response, IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
    }

    private SamlMessageSignatureValidator getSamlMessageSignatureValidator(String entityId) {
        MetadataResolver metadataResolver = metadataResolverRepository.getMetadataResolver(entityId)
                .orElseThrow(() -> new SamlTransformationErrorException(format("Unable to find metadata resolver for entity Id {0}", entityId), Level.ERROR));
        return new SamlMessageSignatureValidator(
                new CredentialFactorySignatureValidator(
                        new SigningCredentialFactory(
                                new AuthnResponseKeyStore(
                                        new IdpMetadataPublicKeyStore(metadataResolver)))));
    }
}
