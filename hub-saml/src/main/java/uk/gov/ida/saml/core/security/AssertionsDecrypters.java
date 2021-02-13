package uk.gov.ida.saml.core.security;

import io.prometheus.client.Counter;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import uk.gov.ida.saml.security.AssertionDecrypter;
import uk.gov.ida.saml.security.exception.SamlFailedToDecryptException;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import java.util.List;

public class AssertionsDecrypters { 
    private final List<AssertionDecrypter> assertionDecrypters;
    
    public AssertionsDecrypters(List<AssertionDecrypter> decrypters) {
        this.assertionDecrypters = decrypters;
    }
    
    public List<Assertion> decryptedAssertions(ValidatedResponse validatedResponse, Counter counter, Class responseValidatorClass) {
        
        for (AssertionDecrypter assertionDecrypter : assertionDecrypters) {
            try {
                return assertionDecrypter.decryptAssertions(validatedResponse);
            } catch (SamlFailedToDecryptException e) {
                counter.labels(validatedResponse.getIssuer().getValue()).inc();
                logFailedToDecryptWarning(validatedResponse, responseValidatorClass, e);
            }
        }
        
        throw samlFailedToDecryptException(validatedResponse, responseValidatorClass);
    }

    private void logFailedToDecryptWarning(ValidatedResponse validatedResponse, Class responseValidatorClass,  SamlFailedToDecryptException e) {
        LoggerFactory.getLogger(responseValidatorClass.getSimpleName()).warn(
                String.format(
                        "%s failed to decrypt assertions from %s with one of the decrypters",
                        responseValidatorClass.getSimpleName(),
                        validatedResponse.getIssuer().getValue()), 
                e);
    }

    private SamlFailedToDecryptException samlFailedToDecryptException(ValidatedResponse responseValidator, Class klass) {
        return new SamlFailedToDecryptException(
                String.format(
                        "%s could not decrypt assertions from %s with any of the decrypters",
                        klass.getSimpleName(),
                        responseValidator.getIssuer().getValue()
                ), Level.ERROR
        );
    }
}
