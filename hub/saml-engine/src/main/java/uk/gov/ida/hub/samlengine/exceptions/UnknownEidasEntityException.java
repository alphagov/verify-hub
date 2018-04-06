package uk.gov.ida.hub.samlengine.exceptions;

import java.text.MessageFormat;

public class UnknownEidasEntityException extends RuntimeException {
    private static String PATTERN = "Unable to locate metadata for eIDAS entity '{0}'";

    public UnknownEidasEntityException(String entityId) {
        super(MessageFormat.format(PATTERN, entityId));
    }
}
