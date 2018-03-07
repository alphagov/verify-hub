package uk.gov.ida.saml.core.domain;

public final class MatchingServiceAuthnStatement {

    private AuthnContext authnContext;

    private MatchingServiceAuthnStatement() {
    }

    private MatchingServiceAuthnStatement(AuthnContext authnContext) {
        this.authnContext = authnContext;
    }

    public AuthnContext getAuthnContext() {
        return authnContext;
    }

    public static MatchingServiceAuthnStatement createIdaAuthnStatement(
            AuthnContext authnContext) {

        return new MatchingServiceAuthnStatement(authnContext);
    }
}
