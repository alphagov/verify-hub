package uk.gov.ida.saml.hub.domain;

import org.joda.time.DateTime;
import org.opensaml.xmlsec.signature.Signature;

import java.net.URI;
import java.util.Optional;

public class AuthnRequestFromRelyingParty extends VerifySamlMessage {

    private Optional<Boolean> forceAuthentication;
    private Optional<URI> assertionConsumerServiceUrl;
    private Optional<Integer> assertionConsumerServiceIndex;
    private Optional<Signature> signature;
    private Optional<String> verifyServiceProviderVersion;

    protected AuthnRequestFromRelyingParty() {
    }

    public AuthnRequestFromRelyingParty(
        String id,
        String issuer,
        DateTime issueInstant,
        URI destination,
        Optional<Boolean> forceAuthentication,
        Optional<URI> assertionConsumerServiceUrl,
        Optional<Integer> assertionConsumerServiceIndex,
        Optional<Signature> signature,
        Optional<String> verifyServiceProviderVersion
    ) {
        super(id, issuer, issueInstant, destination);
        this.forceAuthentication = forceAuthentication;
        this.assertionConsumerServiceUrl = assertionConsumerServiceUrl;
        this.assertionConsumerServiceIndex = assertionConsumerServiceIndex;
        this.signature = signature;
        this.verifyServiceProviderVersion = verifyServiceProviderVersion;
    }

    public Optional<Boolean> getForceAuthentication() {
        return forceAuthentication;
    }

    public Optional<Integer> getAssertionConsumerServiceIndex() {
        return assertionConsumerServiceIndex;
    }

    public Optional<Signature> getSignature() {
        return signature;
    }

    public Optional<URI> getAssertionConsumerServiceUrl() {
        return assertionConsumerServiceUrl;
    }

    public Optional<String> getVerifyServiceProviderVersion() {
        return verifyServiceProviderVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthnRequestFromRelyingParty)) return false;

        AuthnRequestFromRelyingParty that = (AuthnRequestFromRelyingParty) o;

        if (forceAuthentication != null ? !forceAuthentication.equals(that.forceAuthentication) : that.forceAuthentication != null)
            return false;
        if (assertionConsumerServiceUrl != null ? !assertionConsumerServiceUrl.equals(that.assertionConsumerServiceUrl) : that.assertionConsumerServiceUrl != null)
            return false;
        if (assertionConsumerServiceIndex != null ? !assertionConsumerServiceIndex.equals(that.assertionConsumerServiceIndex) : that.assertionConsumerServiceIndex != null)
            return false;
        if (signature != null ? !signature.equals(that.signature) : that.signature != null) return false;
        return verifyServiceProviderVersion != null ? verifyServiceProviderVersion.equals(that.verifyServiceProviderVersion) : that.verifyServiceProviderVersion == null;
    }

    @Override
    public int hashCode() {
        int result = forceAuthentication != null ? forceAuthentication.hashCode() : 0;
        result = 31 * result + (assertionConsumerServiceUrl != null ? assertionConsumerServiceUrl.hashCode() : 0);
        result = 31 * result + (assertionConsumerServiceIndex != null ? assertionConsumerServiceIndex.hashCode() : 0);
        result = 31 * result + (signature != null ? signature.hashCode() : 0);
        result = 31 * result + (verifyServiceProviderVersion != null ? verifyServiceProviderVersion.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AuthnRequestFromRelyingParty{" +
            "forceAuthentication=" + forceAuthentication +
            ", assertionConsumerServiceUrl=" + assertionConsumerServiceUrl +
            ", assertionConsumerServiceIndex=" + assertionConsumerServiceIndex +
            ", signature=" + signature +
            ", verifyServiceProviderVersion=" + verifyServiceProviderVersion +
            '}';
    }
}
