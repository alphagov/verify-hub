package uk.gov.ida.hub.samlsoapproxy.security;

import uk.gov.ida.saml.configuration.SamlConfiguration;
import uk.gov.ida.saml.core.InternalPublicKeyStore;
import uk.gov.ida.saml.metadata.exceptions.NoKeyConfiguredForEntityException;
import uk.gov.ida.saml.security.SigningKeyStore;

import javax.inject.Inject;
import java.security.PublicKey;
import java.util.List;

public class MatchingRequestSigningKeyStore implements SigningKeyStore {
    private final String myEntityId;
    private final InternalPublicKeyStore internalPublicKeyStore;

    @Inject
    public MatchingRequestSigningKeyStore(InternalPublicKeyStore internalPublicKeyStore, SamlConfiguration samlConfiguration) {
        this.internalPublicKeyStore = internalPublicKeyStore;
        this.myEntityId = samlConfiguration.getEntityId();
    }

    @Override
    public List<PublicKey> getVerifyingKeysForEntity(String entityId) {
        if (entityId.equals(myEntityId)) {
            final List<PublicKey> verifyingKeysForEntity = internalPublicKeyStore.getVerifyingKeysForEntity();
            if (!verifyingKeysForEntity.isEmpty()) {
                return verifyingKeysForEntity;
            }
        }
        throw new NoKeyConfiguredForEntityException(entityId);
    }
}
