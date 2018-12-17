package uk.gov.ida.hub.samlengine.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.samlengine.config.ConfigServiceKeyStore;
import uk.gov.ida.saml.metadata.exceptions.NoKeyConfiguredForEntityException;
import uk.gov.ida.saml.security.EncryptionKeyStore;

import javax.inject.Inject;
import java.security.PublicKey;

public class HubEncryptionKeyStore implements EncryptionKeyStore {

    private static final Logger LOG = LoggerFactory.getLogger(HubEncryptionKeyStore.class);
    private final ConfigServiceKeyStore configServiceKeyStore;

    @Inject
    public HubEncryptionKeyStore(ConfigServiceKeyStore configServiceKeyStore) {
        this.configServiceKeyStore = configServiceKeyStore;
    }
    @Override
    public PublicKey getEncryptionKeyForEntity(String entityId) {
        LOG.info("Retrieving encryption key for {} from config", entityId);
        try {
            return configServiceKeyStore.getEncryptionKeyForEntity(entityId);
        } catch (ApplicationException e) {
            if (e.getExceptionType().equals(ExceptionType.CLIENT_ERROR)) {
                throw new NoKeyConfiguredForEntityException(entityId);
            }
            throw new RuntimeException(e);
        }
    }
}
