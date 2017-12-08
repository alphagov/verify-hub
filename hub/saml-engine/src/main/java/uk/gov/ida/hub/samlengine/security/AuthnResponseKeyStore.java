package uk.gov.ida.hub.samlengine.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.saml.metadata.IdpMetadataPublicKeyStore;
import uk.gov.ida.saml.security.SigningKeyStore;

import javax.inject.Inject;
import java.security.PublicKey;
import java.util.List;

public class AuthnResponseKeyStore implements SigningKeyStore {

    private static final Logger LOG = LoggerFactory.getLogger(AuthnResponseKeyStore.class);
    private final IdpMetadataPublicKeyStore idpMetadataPublicKeyStore;

    @Inject
    public AuthnResponseKeyStore(IdpMetadataPublicKeyStore idpMetadataPublicKeyStore) {
        this.idpMetadataPublicKeyStore = idpMetadataPublicKeyStore;
    }

    @Override
    public List<PublicKey> getVerifyingKeysForEntity(String entityId) {
        LOG.info("Requesting signature verifying key for {} in federation metadata", entityId);
        return idpMetadataPublicKeyStore.getVerifyingKeysForEntity(entityId);
    }
}
