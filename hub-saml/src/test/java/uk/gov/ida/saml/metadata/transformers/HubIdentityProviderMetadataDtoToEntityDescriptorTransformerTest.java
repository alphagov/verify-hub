package uk.gov.ida.saml.metadata.transformers;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.KeyName;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import uk.gov.ida.common.shared.security.Certificate;
import uk.gov.ida.common.shared.security.IdGenerator;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;
import uk.gov.ida.saml.core.test.builders.metadata.IdentityProviderMetadataDtoBuilder;

import java.util.List;
import java.util.UUID;

import static uk.gov.ida.saml.core.test.builders.CertificateBuilder.aCertificate;

@ExtendWith(OpenSAMLExtension.class)
public class HubIdentityProviderMetadataDtoToEntityDescriptorTransformerTest {

    private static HubIdentityProviderMetadataDtoToEntityDescriptorTransformer transformer;

    @BeforeAll
    public static void setUp() {
        OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();
        transformer = new HubIdentityProviderMetadataDtoToEntityDescriptorTransformer(openSamlXmlObjectFactory, new KeyDescriptorsUnmarshaller(openSamlXmlObjectFactory), new IdGenerator());
    }

    @Test
    public void transform_shouldTransformIdpSigningCertificates() {
        String idpOneIssuerId = UUID.randomUUID().toString();
        String idpTwoIssuerId = UUID.randomUUID().toString();
        final Certificate idpCertOne = aCertificate().withIssuerId(idpOneIssuerId).build();
        final Certificate idpCertTwo = aCertificate().withIssuerId(idpTwoIssuerId).build();

        final EntityDescriptor result = transformer.apply(IdentityProviderMetadataDtoBuilder.anIdentityProviderMetadataDto().addIdpSigningCertificate(idpCertOne).addIdpSigningCertificate(idpCertTwo).build());

        final List<KeyDescriptor> keyDescriptors = result.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getKeyDescriptors();
        Assertions.assertThat(keyDescriptors.size()).isEqualTo(4);

        assertCertificateCorrect(keyDescriptors.get(1), idpOneIssuerId, idpCertOne);
        assertCertificateCorrect(keyDescriptors.get(2), idpTwoIssuerId, idpCertTwo);
    }

    @Test
    public void transform_shouldTransformHubEncryptionCertificate() {
        final Certificate encryptionCert = aCertificate().withKeyUse(Certificate.KeyUse.Encryption).build();

        final EntityDescriptor result = transformer.apply(IdentityProviderMetadataDtoBuilder.anIdentityProviderMetadataDto()
                .withHubEncryptionCertificate(encryptionCert)
                .build());

        final List<KeyDescriptor> keyDescriptors = result.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getKeyDescriptors();
        assertCertificateIsPresent(keyDescriptors, encryptionCert);
    }

    private void assertCertificateIsPresent(List<KeyDescriptor> keyDescriptors, Certificate encryptionCert) {
        for (KeyDescriptor keyDescriptor : keyDescriptors) {
            String keyEntityId = keyDescriptor.getKeyInfo().getKeyNames().get(0).getValue();
            String x509Value = keyDescriptor.getKeyInfo().getX509Datas().get(0).getX509Certificates().get(0).getValue();
            UsageType keyUse = keyDescriptor.getUse();

            if (keyEntityId.equals(encryptionCert.getIssuerId()) && x509Value.equals(encryptionCert.getCertificate()) && keyUse == UsageType.ENCRYPTION) {
                return;
            }
        }

        Assertions.fail("Certificate is not present.");
    }

    private void assertCertificateCorrect(KeyDescriptor keyDescriptor, String issuerId, Certificate certificateValue) {
        final KeyInfo keyInfo = keyDescriptor.getKeyInfo();
        final List<KeyName> keyNames = keyInfo.getKeyNames();
        Assertions.assertThat(keyNames.size()).isEqualTo(1);
        Assertions.assertThat(keyNames.get(0).getValue()).isEqualTo(issuerId);
        Assertions.assertThat(keyInfo.getX509Datas().size()).isEqualTo(1);

        final List<X509Data> x509Datas = keyInfo.getX509Datas();
        final List<X509Certificate> x509Certificates = x509Datas.get(0).getX509Certificates();
        Assertions.assertThat(x509Certificates.size()).isEqualTo(1);
        Assertions.assertThat(x509Certificates.get(0).getValue()).isEqualTo(certificateValue.getCertificate());
    }
}
