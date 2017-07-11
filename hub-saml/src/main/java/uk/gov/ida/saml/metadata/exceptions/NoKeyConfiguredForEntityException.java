package uk.gov.ida.saml.metadata.exceptions;

import static java.text.MessageFormat.format;

public class NoKeyConfiguredForEntityException extends RuntimeException {
    public NoKeyConfiguredForEntityException(String entityId) {
        super(format("KeyStore contains no keys for Entity: {0}", entityId));
    }
}
