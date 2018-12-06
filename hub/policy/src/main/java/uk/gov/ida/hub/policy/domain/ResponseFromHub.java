package uk.gov.ida.hub.policy.domain;

import com.google.common.base.Optional;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.StandardToStringStyle;

import javax.annotation.concurrent.Immutable;
import java.net.URI;
import java.util.List;
import java.util.Objects;

@Immutable
public final class ResponseFromHub {
    private String authnRequestIssuerEntityId;
    private String responseId;
    private String inResponseTo;
    private TransactionIdaStatus status;
    private List<String> encryptedAssertions;
    private Optional<String> relayState;
    private URI assertionConsumerServiceUri;

    @SuppressWarnings("unused")//Needed by JAXB
    private ResponseFromHub() {
    }

    public ResponseFromHub(
            String responseId,
            String inResponseTo,
            String authnRequestIssuerEntityId,
            List<String> encryptedAssertions,
            Optional<String> relayState,
            URI assertionConsumerServiceUri,
            TransactionIdaStatus status) {

        this.authnRequestIssuerEntityId = authnRequestIssuerEntityId;
        this.responseId = responseId;
        this.inResponseTo = inResponseTo;
        this.encryptedAssertions = encryptedAssertions;
        this.relayState = relayState;
        this.assertionConsumerServiceUri = assertionConsumerServiceUri;
        this.status = status;
    }

    public String getAuthnRequestIssuerEntityId() {
        return authnRequestIssuerEntityId;
    }

    public String getResponseId() {
        return responseId;
    }

    public String getInResponseTo() {
        return inResponseTo;
    }

    public List<String> getEncryptedAssertions() {
        return encryptedAssertions;
    }

    public Optional<String> getRelayState() {
        return relayState;
    }

    public URI getAssertionConsumerServiceUri() {
        return assertionConsumerServiceUri;
    }

    public TransactionIdaStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        final StandardToStringStyle style = new StandardToStringStyle();
        style.setUseIdentityHashCode(false);
        return ReflectionToStringBuilder.toString(this, style);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResponseFromHub that = (ResponseFromHub) o;

        return Objects.equals(authnRequestIssuerEntityId, that.authnRequestIssuerEntityId) &&
            Objects.equals(responseId, that.responseId) &&
            Objects.equals(inResponseTo, that.inResponseTo) &&
            status == that.status &&
            Objects.equals(encryptedAssertions, that.encryptedAssertions) &&
            Objects.equals(relayState, that.relayState) &&
            Objects.equals(assertionConsumerServiceUri, that.assertionConsumerServiceUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authnRequestIssuerEntityId, responseId, inResponseTo, status, encryptedAssertions, relayState, assertionConsumerServiceUri);
    }
}
