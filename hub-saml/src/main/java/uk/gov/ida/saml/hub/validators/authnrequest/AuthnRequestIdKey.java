package uk.gov.ida.saml.hub.validators.authnrequest;

import java.io.Serializable;

public class AuthnRequestIdKey implements Serializable {
    private final String requestId;

    public AuthnRequestIdKey(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuthnRequestIdKey that = (AuthnRequestIdKey) o;

        if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return requestId != null ? requestId.hashCode() : 0;
    }
}
