package uk.gov.ida.hub.samlengine.locators;

import uk.gov.ida.saml.security.EntityToEncryptForLocator;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// NOTE: this class is not necessarily ideal - we might be able to use an in memory locator instead
public class AssignableEntityToEncryptForLocator implements EntityToEncryptForLocator {

    private final ConcurrentMap<String, String> requestIdToEntityId = new ConcurrentHashMap<>();

    @Inject
    public AssignableEntityToEncryptForLocator() {
    }

    @Override
    public String fromRequestId(String requestId) {
        if (!requestIdToEntityId.containsKey(requestId)){
            throw new IllegalStateException(MessageFormat.format("Unable to locate request with id {0}", requestId));
        }
        return requestIdToEntityId.get(requestId);
    }

    public void addEntityIdForRequestId(String requestId, String entityIdForEncryption) {
        requestIdToEntityId.put(requestId, entityIdForEncryption);
    }

    public void removeEntityIdForRequestId(String requestId) {
        requestIdToEntityId.remove(requestId);
    }
}
