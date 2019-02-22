package uk.gov.ida.hub.policy.redis;

import com.fasterxml.jackson.annotation.JsonProperty;

/* The SessionId class is used for serialization both between microservices
* and into Redis. To allow it to be serialized in the correct way for Redis,
* this Mixin is included with the ObjectMapper Redis uses */
public abstract class SessionIdMixIn {
    SessionIdMixIn(@JsonProperty("sessionId") String sessionId) { }

    @JsonProperty("sessionId") abstract int getSessionId();
}
