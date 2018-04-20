package uk.gov.ida.saml.core.test.builders;

import org.opensaml.saml.saml2.core.Scoping;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;

public class ScopingBuilder {

    private OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();

    public static ScopingBuilder aScoping() {
        return new ScopingBuilder();
    }

    public Scoping build() {
        return openSamlXmlObjectFactory.createScoping();
    }
}
