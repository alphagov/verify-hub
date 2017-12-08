package uk.gov.ida.hub.samlsoapproxy.security;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.samlsoapproxy.config.ConfigServiceKeyStore;
import uk.gov.ida.saml.metadata.exceptions.NoKeyConfiguredForEntityException;
import uk.gov.ida.saml.security.SigningKeyStore;

import javax.inject.Inject;
import java.security.PublicKey;
import java.util.List;

public class MatchingResponseSigningKeyStore implements SigningKeyStore {

    private static final Logger LOG = LoggerFactory.getLogger(MatchingResponseSigningKeyStore.class);
    private final ConfigServiceKeyStore configServiceKeyStore;

    @Inject
    public MatchingResponseSigningKeyStore(ConfigServiceKeyStore configServiceKeyStore) {
        this.configServiceKeyStore = configServiceKeyStore;
    }

    @Override
    public List<PublicKey> getVerifyingKeysForEntity(String entityId) {
        LOG.info("Requesting signature verifying key for {} from config", entityId);
        try {
            return configServiceKeyStore.getVerifyingKeysForEntity(entityId);
        } catch (ApplicationException e) {
            if (e.getExceptionType().equals(ExceptionType.CLIENT_ERROR)) {
                throw new NoKeyConfiguredForEntityException(entityId);
            }
            throw Throwables.propagate(e);
        }
    }
}
