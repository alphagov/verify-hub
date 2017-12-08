package uk.gov.ida.hub.samlengine.builders;

import org.slf4j.event.Level;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;

public class SamlTransformationFailureExceptionBuilder {

    private String errorMessage = "message";
    private Exception cause = new RuntimeException("Boom!");

    public static SamlTransformationFailureExceptionBuilder aSamlTransformationFailureException(){
        return new SamlTransformationFailureExceptionBuilder();
    }

    public SamlTransformationErrorException build(){
        return new TestSamlTransformationErrorException(errorMessage, cause);
    }

    private class TestSamlTransformationErrorException extends SamlTransformationErrorException {
        protected TestSamlTransformationErrorException(String errorMessage, Exception cause) {
            super(errorMessage, cause, Level.ERROR);
        }
    }
}
