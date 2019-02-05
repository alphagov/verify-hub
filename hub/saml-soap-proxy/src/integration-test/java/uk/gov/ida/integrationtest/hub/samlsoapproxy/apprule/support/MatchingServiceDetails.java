package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support;

import java.net.URI;
import java.util.Objects;

public final class MatchingServiceDetails {
    private final URI msaUri;
    private final String msaEntityId;
    private final String rpEntityId;

    public MatchingServiceDetails(final URI msaUri,
                                  final String msaEntityId,
                                  final String rpEntityId) {
        this.msaUri = msaUri;
        this.msaEntityId = msaEntityId;
        this.rpEntityId = rpEntityId;
    }

    public URI getMsaUri() {
        return msaUri;
    }

    public String getMsaEntityId() {
        return msaEntityId;
    }

    public String getRpEntityId() {
        return rpEntityId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MatchingServiceDetails that = (MatchingServiceDetails) o;
        return Objects.equals(msaUri, that.msaUri) && Objects.equals(msaEntityId, that.msaEntityId) && Objects.equals(rpEntityId, that.rpEntityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(msaUri, msaEntityId, rpEntityId);
    }
}
