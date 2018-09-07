package uk.gov.ida.saml.hub.api;

import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA256;
import uk.gov.ida.common.shared.security.PrivateKeyFactory;
import uk.gov.ida.common.shared.security.PublicKeyFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.saml.core.IdaSamlBootstrap;
import uk.gov.ida.saml.core.api.CoreTransformersFactory;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.test.TestCertificateStrings;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.domain.EidasAuthnRequestFromHub;
import uk.gov.ida.saml.hub.domain.IdaAuthnRequestFromHub;
import uk.gov.ida.saml.hub.test.builders.EidasAuthnRequestBuilder;
import uk.gov.ida.saml.hub.test.builders.IdaAuthnRequestBuilder;
import uk.gov.ida.saml.security.IdaKeyStore;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class HubTransformersFactoryTest {
    private StringToOpenSamlObjectTransformer<AuthnRequest> stringtoOpenSamlObjectTransformer;

    private final SignatureAlgorithm signatureAlgorithm = new SignatureRSASHA256();
    private final DigestAlgorithm digestAlgorithm = new DigestSHA256();

    @Before
    public void setUp() {
        IdaSamlBootstrap.bootstrap();
        CoreTransformersFactory coreTransformersFactory = new CoreTransformersFactory();
        stringtoOpenSamlObjectTransformer = coreTransformersFactory.
            getStringtoOpenSamlObjectTransformer(input -> {});
    }

    @Test
    public void getIdaAuthnRequestFromHubToStringTransformer_testDoesNotContainKeyInfo() throws Exception {
        Function<IdaAuthnRequestFromHub, String> eidasTransformer = new HubTransformersFactory().getIdaAuthnRequestFromHubToStringTransformer(
            getKeyStore(true),
            signatureAlgorithm,
            digestAlgorithm
        );
        IdaAuthnRequestFromHub idaAuthnRequestFromHub = IdaAuthnRequestBuilder.anIdaAuthnRequest()
            .withLevelsOfAssurance(Collections.singletonList(AuthnContext.LEVEL_3))
            .buildFromHub();

        String apply = eidasTransformer.apply(idaAuthnRequestFromHub);

        Assert.assertNotNull(apply);

        AuthnRequest authnReq = stringtoOpenSamlObjectTransformer.apply(apply);
        Assert.assertNotNull(authnReq);
        Assert.assertNull("The Authn Request does not contain a KeyInfo section for Verify UK", authnReq.getSignature().getKeyInfo());
    }

    @Test
    public void getEidasAuthnRequestFromHubToStringTransformer_testContainsKeyInfo() throws Exception {
        Function<EidasAuthnRequestFromHub, String> eidasTransformer = new HubTransformersFactory().getEidasAuthnRequestFromHubToStringTransformer(
            getKeyStore(true),
            signatureAlgorithm,
            digestAlgorithm
        );
        EidasAuthnRequestFromHub eidasAuthnRequestFromHub = EidasAuthnRequestBuilder.anEidasAuthnRequest()
            .withLevelsOfAssurance(Collections.singletonList(AuthnContext.LEVEL_2))
            .buildFromHub();

        String apply = eidasTransformer.apply(eidasAuthnRequestFromHub);

        Assert.assertNotNull(apply);

        AuthnRequest authnReq = stringtoOpenSamlObjectTransformer.apply(apply);
        Assert.assertNotNull(authnReq);
        Assert.assertNotNull("The Authn Request contains a KeyInfo section for eIDAS", authnReq.getSignature().getKeyInfo());
    }

    @Test (expected = SamlTransformationErrorException.class)
    public void shouldThrowExceptionWhenKeyInfoIsRequiredButSigningCertIsNotPresent() throws Base64DecodingException {
        Function<EidasAuthnRequestFromHub, String> eidasTransformer = new HubTransformersFactory().getEidasAuthnRequestFromHubToStringTransformer(
                getKeyStore(false),
                signatureAlgorithm,
                digestAlgorithm
        );
        EidasAuthnRequestFromHub eidasAuthnRequestFromHub = EidasAuthnRequestBuilder.anEidasAuthnRequest()
                .withLevelsOfAssurance(Collections.singletonList(AuthnContext.LEVEL_2))
                .buildFromHub();

        eidasTransformer.apply(eidasAuthnRequestFromHub);
    }

    private static IdaKeyStore getKeyStore(boolean includeSigningCert) throws Base64DecodingException {
        List<KeyPair> encryptionKeyPairs = new ArrayList<>();
        PublicKeyFactory publicKeyFactory = new PublicKeyFactory(new X509CertificateFactory());
        PrivateKeyFactory privateKeyFactory = new PrivateKeyFactory();
        PublicKey encryptionPublicKey = publicKeyFactory.createPublicKey(TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT);
        PrivateKey encryptionPrivateKey = privateKeyFactory.createPrivateKey(Base64.decode(TestCertificateStrings.HUB_TEST_PRIVATE_ENCRYPTION_KEY.getBytes()));
        encryptionKeyPairs.add(new KeyPair(encryptionPublicKey, encryptionPrivateKey));
        PublicKey publicSigningKey = publicKeyFactory.createPublicKey(TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT);
        PrivateKey privateSigningKey = privateKeyFactory.createPrivateKey(Base64.decode(TestCertificateStrings.HUB_TEST_PRIVATE_SIGNING_KEY.getBytes()));
        KeyPair signingKeyPair = new KeyPair(publicSigningKey, privateSigningKey);

        if (includeSigningCert == true){
            return new IdaKeyStore(new X509CertificateFactory().createCertificate(TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT), signingKeyPair, encryptionKeyPairs);
        } else {
            return new IdaKeyStore(signingKeyPair, encryptionKeyPairs);
        }
    }
}
