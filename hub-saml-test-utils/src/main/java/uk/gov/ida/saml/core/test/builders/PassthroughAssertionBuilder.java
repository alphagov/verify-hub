package uk.gov.ida.saml.core.test.builders;

import java.util.Optional;

import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.domain.FraudDetectedDetails;
import uk.gov.ida.saml.core.domain.PassthroughAssertion;
import uk.gov.ida.saml.core.domain.PersistentId;

public class PassthroughAssertionBuilder {

    private PersistentId persistentId = new PersistentId("default-name-id");
    private Optional<AuthnContext> authnContext = Optional.ofNullable(AuthnContext.LEVEL_1);
    private String underlyingAssertion = "blob";
    private Optional<FraudDetectedDetails> fraudDetectedDetails = Optional.empty();
    private Optional<String> principalIpAddress = Optional.ofNullable("principal-ip-address");

    public static PassthroughAssertionBuilder aPassthroughAssertion() {
        return new PassthroughAssertionBuilder();
    }

    public PassthroughAssertion buildMatchingServiceAssertion() {
        return new PassthroughAssertion(
                persistentId,
                authnContext,
                underlyingAssertion,
                fraudDetectedDetails,
                Optional.empty());
    }

    public PassthroughAssertion buildAuthnStatementAssertion() {
        return new PassthroughAssertion(
                persistentId,
                authnContext,
                underlyingAssertion,
                fraudDetectedDetails,
                principalIpAddress);
    }

    public PassthroughAssertion buildMatchingDatasetAssertion() {
        return new PassthroughAssertion(
                persistentId,
                Optional.empty(),
                underlyingAssertion,
                fraudDetectedDetails,
                Optional.empty()
        );
    }

    public PassthroughAssertionBuilder withPersistentId(PersistentId persistentId) {
        this.persistentId = persistentId;
        return this;
    }

    public PassthroughAssertionBuilder withUnderlyingAssertion(String underlyingAssertion) {
        this.underlyingAssertion = underlyingAssertion;
        return this;
    }

    public PassthroughAssertionBuilder withAuthnContext(AuthnContext authnContext) {
        this.authnContext = Optional.ofNullable(authnContext);
        return this;
    }
    public String buildMatchingDatasetAssertionAsString() {
        return underlyingAssertion; //this is wrong (obviously) but it is sufficient to make tests that use this method pass for now
    }

    public String buildAuthnStatementAssertionAsString() {
        return underlyingAssertion; //this is wrong (obviously) but it is sufficient to make tests that use this method pass for now
    }

    public PassthroughAssertionBuilder withFraudDetectedDetails(FraudDetectedDetails fraudDetectedDetails) {
        this.fraudDetectedDetails = Optional.ofNullable(fraudDetectedDetails);
        return this;
    }

    public PassthroughAssertionBuilder withPrincipalIpAddressSeenByIdp(String principalIpAddress) {
        this.principalIpAddress = Optional.ofNullable(principalIpAddress);
        return this;
    }
}
