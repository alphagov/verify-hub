package uk.gov.ida.saml.hub.domain;

import uk.gov.ida.saml.core.domain.IdaStatus;

public interface AuthenticationStatusFactory<T extends Enum, U extends IdaStatus> {
    U create(T status, String message);
}
