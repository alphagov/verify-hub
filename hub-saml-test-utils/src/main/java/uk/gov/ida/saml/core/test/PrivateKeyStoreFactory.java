package uk.gov.ida.saml.core.test;

import org.apache.commons.codec.binary.Base64;
import uk.gov.ida.common.shared.security.PrivateKeyFactory;
import uk.gov.ida.common.shared.security.PrivateKeyStore;

import java.security.PrivateKey;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class PrivateKeyStoreFactory {
    public PrivateKeyStore create(String entityId) {
        PrivateKey privateSigningKey = new PrivateKeyFactory().createPrivateKey(Base64.decodeBase64(TestCertificateStrings.PRIVATE_SIGNING_KEYS.get(entityId)));
        List<String> encryptionKeyStrings = TestCertificateStrings.PRIVATE_ENCRYPTION_KEYS.get(entityId);
        List<PrivateKey> privateEncryptionKeys = encryptionKeyStrings.stream()
            .map(input -> new PrivateKeyFactory().createPrivateKey(Base64.decodeBase64(input)))
            .collect(toList());
        return new PrivateKeyStore(privateSigningKey, privateEncryptionKeys);
    }
}
