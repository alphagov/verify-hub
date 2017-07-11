package uk.gov.ida.saml.msa.test.transformers;

import com.google.inject.Inject;
import org.opensaml.saml.saml2.core.Response;
import org.w3c.dom.Element;
import uk.gov.ida.saml.serializers.XmlObjectToElementTransformer;
import uk.gov.ida.saml.core.transformers.outbound.decorators.ResponseAssertionSigner;
import uk.gov.ida.saml.core.transformers.outbound.decorators.ResponseSignatureCreator;
import uk.gov.ida.saml.core.transformers.outbound.decorators.SamlResponseAssertionEncrypter;
import uk.gov.ida.saml.core.transformers.outbound.decorators.SamlSignatureSigner;
import java.util.function.Function;

public class ResponseToElementTransformer implements Function<Response, Element> {

    private final XmlObjectToElementTransformer<Response> xmlObjectToElementTransformer;
    private final SamlSignatureSigner<Response> samlSignatureSigner;
    private final SamlResponseAssertionEncrypter samlResponseAssertionEncrypter;
    private final ResponseAssertionSigner responseAssertionSigner;
    private final ResponseSignatureCreator responseSignatureCreator;

    @Inject
    public ResponseToElementTransformer(
            final XmlObjectToElementTransformer<Response> xmlObjectToElementTransformer, SamlSignatureSigner<Response> samlSignatureSigner,
            final SamlResponseAssertionEncrypter samlResponseAssertionEncrypter,
            final ResponseAssertionSigner responseAssertionSigner,
            final ResponseSignatureCreator responseSignatureCreator) {

        this.xmlObjectToElementTransformer = xmlObjectToElementTransformer;
        this.samlSignatureSigner = samlSignatureSigner;
        this.samlResponseAssertionEncrypter = samlResponseAssertionEncrypter;
        this.responseAssertionSigner = responseAssertionSigner;
        this.responseSignatureCreator = responseSignatureCreator;
    }

    @Override
    public Element apply(final Response response) {
        final Response responseWithSignature = responseSignatureCreator.addUnsignedSignatureTo(response);
        final Response responseWithSignedAssertions = responseAssertionSigner.signAssertions(responseWithSignature);
        final Response responseWithEncryptedAssertions = samlResponseAssertionEncrypter.encryptAssertions(responseWithSignedAssertions);
        final Response signedResponse = samlSignatureSigner.sign(responseWithEncryptedAssertions);
        return xmlObjectToElementTransformer.apply(signedResponse);
    }

}
