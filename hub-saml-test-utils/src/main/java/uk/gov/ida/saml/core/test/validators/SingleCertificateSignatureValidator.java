package uk.gov.ida.saml.core.test.validators;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.Criterion;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialResolver;
import org.opensaml.security.credential.impl.StaticCredentialResolver;
import org.opensaml.xmlsec.config.DefaultSecurityConfigurationBootstrap;
import org.opensaml.xmlsec.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;
import uk.gov.ida.saml.security.SignatureValidator;

import javax.xml.namespace.QName;
import java.util.List;

public class SingleCertificateSignatureValidator extends SignatureValidator {
    private final Credential credential;

    public SingleCertificateSignatureValidator(Credential credential) {
        this.credential = credential;
    }

    @Override
    protected boolean additionalValidations(SignableSAMLObject signableSAMLObject, String entityId, QName role) throws SecurityException {
        CredentialResolver credResolver = new StaticCredentialResolver(credential);
        KeyInfoCredentialResolver kiResolver = DefaultSecurityConfigurationBootstrap.buildBasicInlineKeyInfoCredentialResolver();
        ExplicitKeySignatureTrustEngine trustEngine = new ExplicitKeySignatureTrustEngine(credResolver, kiResolver);

        return trustEngine.validate(signableSAMLObject.getSignature(), new CriteriaSet(new Criterion() {}));
    }
}
