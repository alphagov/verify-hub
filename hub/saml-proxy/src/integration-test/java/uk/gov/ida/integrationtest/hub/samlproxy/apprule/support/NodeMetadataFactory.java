package uk.gov.ida.integrationtest.hub.samlproxy.apprule.support;

import com.google.common.base.Throwables;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.w3c.dom.Element;
import uk.gov.ida.saml.core.test.TestCredentialFactory;
import uk.gov.ida.saml.core.test.builders.SignatureBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.EntityDescriptorBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.IdpSsoDescriptorBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.KeyDescriptorBuilder;
import uk.gov.ida.saml.serializers.XmlObjectToElementTransformer;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import static uk.gov.ida.saml.core.test.TestCertificateStrings.METADATA_SIGNING_A_PRIVATE_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.METADATA_SIGNING_A_PUBLIC_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_PUBLIC_CERT;

public class NodeMetadataFactory {

    private static XmlObjectToElementTransformer<EntityDescriptor>
            entityDescriptorXmlObjectToElementTransformer = new XmlObjectToElementTransformer<>();

    public static String createCountryMetadata(String entityID) {
        return createMetadata(createCountryEntityDescriptor(entityID));
    }

    public static EntityDescriptor createCountryEntityDescriptor(String entityID) {
        Signature entityDescriptorSignature = createSignature();
        KeyDescriptor keyDescriptor = KeyDescriptorBuilder.aKeyDescriptor().withX509ForSigning(TEST_PUBLIC_CERT).build();
        IDPSSODescriptor idpssoDescriptor = IdpSsoDescriptorBuilder
                .anIdpSsoDescriptor()
                .addKeyDescriptor(keyDescriptor)
                .build();
        try {
            return getEntityDescriptor(entityID, idpssoDescriptor, entityDescriptorSignature);
        } catch (MarshallingException | SignatureException e) {
            throw Throwables.propagate(e);
        }
    }

    private static EntityDescriptor getEntityDescriptor(String entityID, IDPSSODescriptor idpssoDescriptor, Signature entityDescriptorSignature) throws MarshallingException, SignatureException {
        return EntityDescriptorBuilder
                .anEntityDescriptor()
                .withEntityId(entityID)
                .withIdpSsoDescriptor(idpssoDescriptor)
                .withSignature(entityDescriptorSignature)
                .build();
    }

    public static String createMetadata(EntityDescriptor entityDescriptor) {
        Element element = entityDescriptorXmlObjectToElementTransformer.apply(entityDescriptor);
        return XmlUtils.writeToString(element);
    }

    private static Signature createSignature() {
        String metadataSigningCert = METADATA_SIGNING_A_PUBLIC_CERT;
        String metadataSigningKey = METADATA_SIGNING_A_PRIVATE_KEY;
        TestCredentialFactory testCredentialFactory = new TestCredentialFactory(metadataSigningCert, metadataSigningKey);
        Credential credential = testCredentialFactory.getSigningCredential();
        return SignatureBuilder
                .aSignature()
                .withSigningCredential(credential)
                .withX509Data(metadataSigningCert)
                .build();
    }
}
