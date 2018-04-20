package uk.gov.ida.saml.msa.test.api;

import org.opensaml.saml.saml2.core.Response;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.w3c.dom.Element;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.security.EncrypterFactory;
import uk.gov.ida.saml.security.EncryptionCredentialFactory;
import uk.gov.ida.saml.security.EncryptionKeyStore;
import uk.gov.ida.saml.security.EntityToEncryptForLocator;
import uk.gov.ida.saml.security.IdaKeyStore;
import uk.gov.ida.saml.security.IdaKeyStoreCredentialRetriever;
import uk.gov.ida.saml.security.SignatureFactory;
import uk.gov.ida.saml.serializers.XmlObjectToElementTransformer;
import uk.gov.ida.saml.core.transformers.outbound.decorators.ResponseAssertionSigner;
import uk.gov.ida.saml.core.transformers.outbound.decorators.ResponseSignatureCreator;
import uk.gov.ida.saml.core.transformers.outbound.decorators.SamlResponseAssertionEncrypter;
import uk.gov.ida.saml.core.transformers.outbound.decorators.SamlSignatureSigner;
import uk.gov.ida.saml.hub.transformers.outbound.MatchingServiceIdaStatusMarshaller;
import uk.gov.ida.saml.msa.test.outbound.HealthCheckResponseFromMatchingService;
import uk.gov.ida.saml.msa.test.outbound.transformers.HealthCheckResponseFromMatchingServiceTransformer;
import uk.gov.ida.saml.msa.test.transformers.ResponseToElementTransformer;
import java.util.function.Function;

public class MsaTransformersFactory {

    public ResponseToElementTransformer getResponseToElementTransformer(
            EncryptionKeyStore encryptionKeyStore,
            IdaKeyStore keyStore,
            EntityToEncryptForLocator entityToEncryptForLocator,
            SignatureAlgorithm signatureAlgorithm,
            DigestAlgorithm digestAlgorithm
    ) {
        SignatureFactory signatureFactory = new SignatureFactory(new IdaKeyStoreCredentialRetriever(keyStore), signatureAlgorithm, digestAlgorithm);
        SamlResponseAssertionEncrypter assertionEncrypter = new SamlResponseAssertionEncrypter(
                new EncryptionCredentialFactory(encryptionKeyStore),
                new EncrypterFactory(),
                entityToEncryptForLocator);
        return new ResponseToElementTransformer(
                new XmlObjectToElementTransformer<Response>(),
                new SamlSignatureSigner<Response>(),
                assertionEncrypter,
                new ResponseAssertionSigner(signatureFactory),
                new ResponseSignatureCreator(signatureFactory)
        );
    }

    public HealthCheckResponseFromMatchingServiceTransformer getHealthCheckResponseFromMatchingServiceToResponseTransformer() {
        return new HealthCheckResponseFromMatchingServiceTransformer(
                new OpenSamlXmlObjectFactory(),
                new MatchingServiceIdaStatusMarshaller(new OpenSamlXmlObjectFactory())
        );
    }

    public Function<HealthCheckResponseFromMatchingService, Element> getHealthcheckResponseFromMatchingServiceToElementTransformer(
            final EncryptionKeyStore encryptionKeyStore,
            final IdaKeyStore keyStore,
            final EntityToEncryptForLocator entityToEncryptForLocator,
            final SignatureAlgorithm signatureAlgorithm,
            final DigestAlgorithm digestAlgorithm
    ){
        Function<Response, Element> responseToElementTransformer = getResponseToElementTransformer(encryptionKeyStore, keyStore, entityToEncryptForLocator, signatureAlgorithm, digestAlgorithm);

        return responseToElementTransformer.compose(getHealthCheckResponseFromMatchingServiceToResponseTransformer());
    }

}
