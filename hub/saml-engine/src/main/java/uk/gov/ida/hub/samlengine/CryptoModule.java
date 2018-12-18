package uk.gov.ida.hub.samlengine;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA1;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.PKIXParametersProvider;
import uk.gov.ida.hub.samlengine.config.ConfigProxy;
import uk.gov.ida.hub.samlengine.config.TrustStoreForCertificateProvider;
import uk.gov.ida.hub.samlengine.security.AuthnRequestKeyStore;
import uk.gov.ida.hub.samlengine.security.HubEncryptionKeyStore;
import uk.gov.ida.hub.samlengine.security.SamlResponseFromMatchingServiceKeyStore;
import uk.gov.ida.saml.core.transformers.outbound.decorators.AssertionBlobEncrypter;
import uk.gov.ida.saml.core.transformers.outbound.decorators.SamlResponseAssertionEncrypter;
import uk.gov.ida.saml.security.EncrypterFactory;
import uk.gov.ida.saml.security.EncryptionKeyStore;
import uk.gov.ida.saml.security.EntityToEncryptForLocator;
import uk.gov.ida.saml.security.IdaKeyStore;
import uk.gov.ida.saml.security.IdaKeyStoreCredentialRetriever;
import uk.gov.ida.saml.security.KeyStoreBackedEncryptionCredentialResolver;
import uk.gov.ida.saml.security.SignatureFactory;
import uk.gov.ida.saml.security.SigningKeyStore;
import uk.gov.ida.truststore.KeyStoreCache;
import uk.gov.ida.truststore.KeyStoreLoader;

public class CryptoModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(EncryptionKeyStore.class).to(HubEncryptionKeyStore.class).asEagerSingleton();
        bind(SigningKeyStore.class).annotatedWith(Names.named("authnRequestKeyStore")).to(AuthnRequestKeyStore.class).asEagerSingleton();
        bind(SigningKeyStore.class).annotatedWith(Names.named("samlResponseFromMatchingServiceKeyStore")).to(SamlResponseFromMatchingServiceKeyStore.class).asEagerSingleton();
        bind(X509CertificateFactory.class).toInstance(new X509CertificateFactory());
        bind(CertificateChainValidator.class);
        bind(PKIXParametersProvider.class).toInstance(new PKIXParametersProvider());
        bind(ConfigProxy.class);
        bind(TrustStoreForCertificateProvider.class);
        bind(KeyStoreCache.class);
        bind(KeyStoreLoader.class).toInstance(new KeyStoreLoader());
        bind(AssertionBlobEncrypter.class);
        bind(EncrypterFactory.class).toInstance(new EncrypterFactory());
        bind(SignatureAlgorithm.class).toInstance(new SignatureRSASHA1());
        bind(DigestAlgorithm.class).toInstance(new DigestSHA256());
    }

    @Provides
    @Singleton
    public KeyStoreBackedEncryptionCredentialResolver getKeyStoreBackedEncryptionCredentialResolver(EncryptionKeyStore keyStore){
        return new KeyStoreBackedEncryptionCredentialResolver(keyStore);
    }

    @Provides
    @Singleton
    public SamlResponseAssertionEncrypter getSamlResponseAssertionEncrypter(KeyStoreBackedEncryptionCredentialResolver credentialResolver,
                                                                            EncrypterFactory encrypterFactory,
                                                                            EntityToEncryptForLocator entityToEncryptForLocator){
        return new SamlResponseAssertionEncrypter(credentialResolver,encrypterFactory, entityToEncryptForLocator);
    }

    @Provides
    @Singleton
    public IdaKeyStoreCredentialRetriever getKeyStoreCredentialRetriever(IdaKeyStore keyStore) {
        return new IdaKeyStoreCredentialRetriever(keyStore);
    }

    @Provides
    @Singleton
    public SignatureFactory getSignatureFactory(IdaKeyStoreCredentialRetriever keyStoreCredentialRetriever, SignatureAlgorithm signatureAlgorithm, DigestAlgorithm digestAlgorithm) {
        return new SignatureFactory(keyStoreCredentialRetriever, signatureAlgorithm, digestAlgorithm);
    }
}
