package uk.gov.ida.hub.samlengine.factories;

import com.google.inject.Inject;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import uk.gov.ida.hub.samlengine.security.AuthnResponseKeyStore;
import uk.gov.ida.saml.metadata.EidasMetadataResolverRepository;
import uk.gov.ida.saml.metadata.IdpMetadataPublicKeyStore;
import uk.gov.ida.saml.security.CredentialFactorySignatureValidator;
import uk.gov.ida.saml.security.SamlAssertionsSignatureValidator;
import uk.gov.ida.saml.security.SamlMessageSignatureValidator;
import uk.gov.ida.saml.security.SigningCredentialFactory;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;

import java.util.List;

public class EidasValidatorFactory {

    private SamlResponseSignatureValidator samlResponseSignatureValidator;
    private SamlAssertionsSignatureValidator samlAssertionsSignatureValidator;
    private EidasMetadataResolverRepository eidasMetadataResolverRepository;

    @Inject
    public EidasValidatorFactory(EidasMetadataResolverRepository eidasMetadataResolverRepository) {
        this.eidasMetadataResolverRepository = eidasMetadataResolverRepository;
    }

    public ValidatedResponse getValidatedResponse(Response response) {
        samlResponseSignatureValidator = new SamlResponseSignatureValidator(getSamlMessageSignatureValidator(response.getIssuer().getValue()));
        return samlResponseSignatureValidator.validate(response, IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
    }

    public void getValidatedAssertion(ValidatedResponse validatedResponse, List<Assertion> decryptedAssertions) {
        samlAssertionsSignatureValidator = new SamlAssertionsSignatureValidator(getSamlMessageSignatureValidator(validatedResponse.getIssuer().getValue()));
        samlAssertionsSignatureValidator.validate(decryptedAssertions, IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
    }

    private SamlMessageSignatureValidator getSamlMessageSignatureValidator(String entityId) {
        MetadataResolver metadataResolver = eidasMetadataResolverRepository.getMetadataResolver(entityId);
        return new SamlMessageSignatureValidator(
                new CredentialFactorySignatureValidator(
                        new SigningCredentialFactory(
                                new AuthnResponseKeyStore(
                                        new IdpMetadataPublicKeyStore(metadataResolver)))));
    }
}
