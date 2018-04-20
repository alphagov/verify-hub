package uk.gov.ida.saml.core.test.builders;

import org.opensaml.saml.saml2.core.StatusMessage;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;

public class StatusMessageBuilder {

    private static OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();

    private String message = "default message";

    public static StatusMessageBuilder aStatusMessage() {
        return new StatusMessageBuilder();
    }

    public StatusMessage build() {
        StatusMessage statusCode = openSamlXmlObjectFactory.createStatusMessage();

        statusCode.setMessage(message);

       return statusCode;
    }

    public StatusMessageBuilder withMessage(String message) {
        this.message = message;
        return this;
    }
}
