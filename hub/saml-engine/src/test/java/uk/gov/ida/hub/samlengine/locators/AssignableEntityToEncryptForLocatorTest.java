package uk.gov.ida.hub.samlengine.locators;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AssignableEntityToEncryptForLocatorTest {

    @Test
    public void shouldStoreEntityIdInMapAgainstRequestId() {
        AssignableEntityToEncryptForLocator assignableEntityToEncryptForLocator = new AssignableEntityToEncryptForLocator();
        String requestId = "requestId";
        String entityId = "entityId";
        
        assignableEntityToEncryptForLocator.addEntityIdForRequestId(requestId, entityId);
        
        assertThat(assignableEntityToEncryptForLocator.fromRequestId(requestId)).isEqualTo(entityId);
    }

    @Test
    public void shouldRemoveEntityIdInMapAgainstRequestId() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            AssignableEntityToEncryptForLocator assignableEntityToEncryptForLocator = new AssignableEntityToEncryptForLocator();
            String requestId = "requestId";
            String entityId = "entityId";

            assignableEntityToEncryptForLocator.addEntityIdForRequestId(requestId, entityId);

            assertThat(assignableEntityToEncryptForLocator.fromRequestId(requestId)).isEqualTo(entityId);

            assignableEntityToEncryptForLocator.removeEntityIdForRequestId(requestId);

            assignableEntityToEncryptForLocator.fromRequestId(requestId);
        });
    }
}
