package uk.gov.ida.hub.samlproxy.factories;

import com.google.inject.Inject;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import uk.gov.ida.hub.samlproxy.security.AuthnResponseKeyStore;
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
    private MetadataResolver metadataResolver;

    @Inject
    public EidasValidatorFactory(MetadataResolver metadataResolver) {
        this.metadataResolver = metadataResolver;
    }

    public ValidatedResponse getValidatedResponse(Response response) {
        samlResponseSignatureValidator = new SamlResponseSignatureValidator(getSamlMessageSignatureValidator());
        return samlResponseSignatureValidator.validate(response, IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
    }

    private SamlMessageSignatureValidator getSamlMessageSignatureValidator() {
        return new SamlMessageSignatureValidator(
                new CredentialFactorySignatureValidator(
                        new SigningCredentialFactory(
                                new AuthnResponseKeyStore(
                                        new IdpMetadataPublicKeyStore(metadataResolver)))));
    }
}
