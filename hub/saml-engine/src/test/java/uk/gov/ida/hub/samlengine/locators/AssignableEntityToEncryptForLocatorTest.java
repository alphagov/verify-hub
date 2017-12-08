package uk.gov.ida.hub.samlengine.locators;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AssignableEntityToEncryptForLocatorTest {

    @Test
    public void shouldStoreEntityIdInMapAgainstRequestId() throws Exception {
        AssignableEntityToEncryptForLocator assignableEntityToEncryptForLocator = new AssignableEntityToEncryptForLocator();
        String requestId = "requestId";
        String entityId = "entityId";
        
        assignableEntityToEncryptForLocator.addEntityIdForRequestId(requestId, entityId);
        
        assertThat(assignableEntityToEncryptForLocator.fromRequestId(requestId)).isEqualTo(entityId);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRemoveEntityIdInMapAgainstRequestId() throws Exception {
        AssignableEntityToEncryptForLocator assignableEntityToEncryptForLocator = new AssignableEntityToEncryptForLocator();
        String requestId = "requestId";
        String entityId = "entityId";

        assignableEntityToEncryptForLocator.addEntityIdForRequestId(requestId, entityId);

        assertThat(assignableEntityToEncryptForLocator.fromRequestId(requestId)).isEqualTo(entityId);

        assignableEntityToEncryptForLocator.removeEntityIdForRequestId(requestId);

        assignableEntityToEncryptForLocator.fromRequestId(requestId);
    }
}
