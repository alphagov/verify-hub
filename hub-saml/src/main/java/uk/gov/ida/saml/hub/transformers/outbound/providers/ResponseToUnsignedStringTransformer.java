package uk.gov.ida.saml.hub.transformers.outbound.providers;

import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.saml.core.transformers.outbound.decorators.ResponseAssertionSigner;
import uk.gov.ida.saml.core.transformers.outbound.decorators.SamlResponseAssertionEncrypter;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;
import javax.inject.Inject;
import java.util.function.Function;

public class ResponseToUnsignedStringTransformer implements Function<Response, String> {

    protected final XmlObjectToBase64EncodedStringTransformer<Response> xmlObjectToBase64EncodedStringTransformer;
    protected final ResponseAssertionSigner responseAssertionSigner;
    protected final SamlResponseAssertionEncrypter assertionEncrypter;

    @Inject
    public ResponseToUnsignedStringTransformer(
            XmlObjectToBase64EncodedStringTransformer<Response> xmlObjectToBase64EncodedStringTransformer,
            ResponseAssertionSigner responseAssertionSigner,
            SamlResponseAssertionEncrypter assertionEncrypter) {

        this.xmlObjectToBase64EncodedStringTransformer = xmlObjectToBase64EncodedStringTransformer;
        this.responseAssertionSigner = responseAssertionSigner;
        this.assertionEncrypter = assertionEncrypter;
    }

    @Override
    public String apply(Response response) {
        Response signedResponse = responseAssertionSigner.signAssertions(response);
        Response signedEncryptedResponse = assertionEncrypter.encryptAssertions(signedResponse);
        return xmlObjectToBase64EncodedStringTransformer.apply(signedEncryptedResponse);
    }

}
