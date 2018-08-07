package uk.gov.ida.integrationtest.hub.samlengine.support;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.codec.binary.Base64;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
import org.opensaml.xmlsec.encryption.support.EncryptionConstants;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import uk.gov.ida.common.shared.security.PrivateKeyFactory;
import uk.gov.ida.common.shared.security.PublicKeyFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.saml.security.DecrypterFactory;
import uk.gov.ida.saml.security.IdaKeyStore;
import uk.gov.ida.saml.security.IdaKeyStoreCredentialRetriever;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import uk.gov.ida.saml.security.validators.encryptedelementtype.EncryptionAlgorithmValidator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;

public class AssertionDecrypter {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;


    public AssertionDecrypter(String privateKey, String publicKey) {
        this.privateKey = new PrivateKeyFactory().createPrivateKey(Base64.decodeBase64(privateKey));
        this.publicKey = new PublicKeyFactory(new X509CertificateFactory()).createPublicKey(publicKey);
    }

    public List<Assertion> decryptAssertions(Response response) {
        KeyPair encryptionKeyPair = new KeyPair(publicKey, privateKey);
        KeyPair signingKeyPair = new KeyPair(publicKey, privateKey);
        IdaKeyStore keyStore = new IdaKeyStore(signingKeyPair, Collections.singletonList(encryptionKeyPair));
        IdaKeyStoreCredentialRetriever idaKeyStoreCredentialRetriever = new IdaKeyStoreCredentialRetriever(keyStore);
        Decrypter decrypter = new DecrypterFactory().createDecrypter(idaKeyStoreCredentialRetriever.getDecryptingCredentials());
        ImmutableSet<String> contentEncryptionAlgorithms = ImmutableSet.of(EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256, EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256_GCM);
        ImmutableSet<String> keyTransportAlgorithms = ImmutableSet.of(EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP, EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP11);

        uk.gov.ida.saml.security.AssertionDecrypter assertionDecrypter = new uk.gov.ida.saml.security.AssertionDecrypter(
            new EncryptionAlgorithmValidator(contentEncryptionAlgorithms, keyTransportAlgorithms),
            decrypter
        );
        return assertionDecrypter.decryptAssertions(new ValidatedResponse(response));
    }

    public Assertion decryptAssertion(String base64EncodedBlob) {
        Element assertionElement = parseXml(new String(Base64.decodeBase64(base64EncodedBlob)));
        EncryptedAssertion encryptedAssertion = unmarshall(assertionElement);
        return decrypt(encryptedAssertion);
    }

    private Element parseXml(String xml) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private EncryptedAssertion unmarshall(Element element) {
        UnmarshallerFactory unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
        try {
            return (EncryptedAssertion) unmarshaller.unmarshall(element);
        } catch (UnmarshallingException e) {
            throw new RuntimeException(e);
        }
    }

    private Assertion decrypt(EncryptedAssertion encryptedAssertion) {
        Decrypter decrypter = new DecrypterFactory().createDecrypter(ImmutableList.of(new BasicCredential(publicKey, privateKey)));
        decrypter.setRootInNewDocument(true);
        try {
            return decrypter.decrypt(encryptedAssertion);
        } catch (DecryptionException e) {
            throw new RuntimeException(e);
        }
    }
}
