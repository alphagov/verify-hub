package uk.gov.ida.saml.core.test.builders;

import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;

import static uk.gov.ida.saml.core.test.builders.AuthnContextClassRefBuilder.anAuthnContextClassRef;

public class AuthnContextBuilder {


    private static OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();
    private AuthnContextClassRef authnContextClassRef = anAuthnContextClassRef().build();

    public static AuthnContextBuilder anAuthnContext() {
        return new AuthnContextBuilder();
    }

    public AuthnContext build() {
        AuthnContext authnContext = openSamlXmlObjectFactory.createAuthnContext();
        authnContext.setAuthnContextClassRef(authnContextClassRef);
        return authnContext;
    }

    public AuthnContextBuilder withAuthnContextClassRef(AuthnContextClassRef authnContextClassRef) {
        this.authnContextClassRef = authnContextClassRef;
        return this;
    }
}
