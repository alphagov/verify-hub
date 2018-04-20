package uk.gov.ida.saml.core.test.builders;

import com.google.common.base.Optional;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;

public class StatusBuilder {

    private static OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();
    private Optional<StatusCode> statusCode = Optional.fromNullable(StatusCodeBuilder.aStatusCode().build());
    private Optional<StatusMessage> message = Optional.absent();


    public static StatusBuilder aStatus() {
        return new StatusBuilder();
    }

    public Status build() {
        Status status = openSamlXmlObjectFactory.createStatus();

        if (statusCode.isPresent()) {
            status.setStatusCode(statusCode.get());
        }

        if (message.isPresent()) {
            status.setStatusMessage(message.get());
        }

        return status;
    }

    public StatusBuilder withStatusCode(StatusCode statusCode) {
        this.statusCode = Optional.fromNullable(statusCode);
        return this;
    }

    public StatusBuilder withMessage(StatusMessage message) {
        this.message = Optional.fromNullable(message);
        return this;
    }
}
