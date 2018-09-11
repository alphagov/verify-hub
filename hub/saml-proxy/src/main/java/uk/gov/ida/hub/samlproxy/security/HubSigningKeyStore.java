package uk.gov.ida.hub.samlproxy.security;

import uk.gov.ida.saml.core.InternalPublicKeyStore;
import uk.gov.ida.saml.metadata.exceptions.NoKeyConfiguredForEntityException;
import uk.gov.ida.saml.security.SigningKeyStore;

import java.security.PublicKey;
import java.util.List;

public class HubSigningKeyStore implements SigningKeyStore {

    private final InternalPublicKeyStore internalPublicKeyStore;

    public HubSigningKeyStore(InternalPublicKeyStore internalPublicKeyStore) {
        this.internalPublicKeyStore = internalPublicKeyStore;
    }

    @Override
    public List<PublicKey> getVerifyingKeysForEntity(String entityId) {
        final List<PublicKey> verifyingKeysForEntity = internalPublicKeyStore.getVerifyingKeysForEntity();
        if (!verifyingKeysForEntity.isEmpty()) {
            return verifyingKeysForEntity;
        }
        throw new NoKeyConfiguredForEntityException(entityId);
    }
}
