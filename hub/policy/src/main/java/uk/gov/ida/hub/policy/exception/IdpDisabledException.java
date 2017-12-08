package uk.gov.ida.hub.policy.exception;

import java.text.MessageFormat;

public class IdpDisabledException extends RuntimeException {

    private final String entityId;

    public IdpDisabledException(String entityId) {
        super(getErrorMessage(entityId));
        this.entityId = entityId;
    }

    public String getEntityId() {
        return entityId;
    }

    public static String getErrorMessage(String entityId){
        return MessageFormat.format("{0} - Identity Provider is disabled.", entityId);
    }
}
