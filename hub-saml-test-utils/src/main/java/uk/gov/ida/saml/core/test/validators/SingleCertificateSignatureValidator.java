package uk.gov.ida.saml.core.test.validators;

import net.shibboleth.utilities.java.support.resolver.Criterion;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialResolver;
import org.opensaml.security.credential.impl.StaticCredentialResolver;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.xmlsec.config.impl.DefaultSecurityConfigurationBootstrap;
import org.opensaml.xmlsec.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;
import uk.gov.ida.saml.security.SignatureValidator;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;

public class SingleCertificateSignatureValidator extends SignatureValidator {
    private final Credential credential;

    public SingleCertificateSignatureValidator(Credential credential) {
        this.credential = credential;
    }

    @Override
    protected TrustEngine<Signature> getTrustEngine(String entityId) {
        CredentialResolver credResolver = new StaticCredentialResolver(credential);
        KeyInfoCredentialResolver kiResolver = DefaultSecurityConfigurationBootstrap.buildBasicInlineKeyInfoCredentialResolver();
        return new ExplicitKeySignatureTrustEngine(credResolver, kiResolver);
    }

    @Override
    protected List<Criterion> getAdditionalCriteria(String entityId, QName role) {
        return Collections.emptyList();
    }
}
