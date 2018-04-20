package uk.gov.ida.saml.core;

import java.security.PublicKey;
import java.util.List;

public interface InternalPublicKeyStore {
    List<PublicKey> getVerifyingKeysForEntity();
}
