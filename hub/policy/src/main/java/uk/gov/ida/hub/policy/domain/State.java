package uk.gov.ida.hub.policy.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

import java.net.URI;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property="@class")
public interface State {
    String getRequestId();

    SessionId getSessionId();

    @SuppressWarnings("unused") // marker method
    void doNotDirectlyImplementThisInterface();

    String getRequestIssuerEntityId();

    DateTime getSessionExpiryTimestamp();

    URI getAssertionConsumerServiceUri();

    boolean getTransactionSupportsEidas();

    Optional<Boolean> getForceAuthentication();
}
