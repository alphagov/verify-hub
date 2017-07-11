package uk.gov.ida.saml.metadata.transformers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.xmlsec.signature.KeyInfo;
import uk.gov.ida.common.shared.security.Certificate;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper;
import uk.gov.ida.saml.core.test.builders.metadata.KeyDescriptorBuilder;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.builders.metadata.KeyDescriptorBuilder.aKeyDescriptor;
import static uk.gov.ida.saml.core.test.builders.metadata.KeyInfoBuilder.aKeyInfo;
import static uk.gov.ida.saml.core.test.builders.metadata.X509CertificateBuilder.aX509Certificate;
import static uk.gov.ida.saml.core.test.builders.metadata.X509DataBuilder.aX509Data;

@RunWith(OpenSAMLMockitoRunner.class)
public class KeyDescriptorMarshallerTest {
    private KeyDescriptorMarshaller marshaller;

    @Before
    public void setup() {
        marshaller = new KeyDescriptorMarshaller();
    }


    @Test
    public void transform_shouldTransformSigningCertificate() throws Exception {
        String certificateValue = "some-cert-value";

        final Certificate result = marshaller.toCertificate(aKeyDescriptor().withX509ForSigning(certificateValue).build());

        assertThat(result.getCertificate()).isEqualTo(certificateValue);
    }

    @Test
    public void transform_shouldTransformEncryptionCertificate() throws Exception {
        String certificateValue = "some-cert-value";

        final Certificate result = marshaller.toCertificate(aKeyDescriptor().withX509ForEncryption(certificateValue).build());

        assertThat(result.getCertificate()).isEqualTo(certificateValue);
    }

    @Test
    public void transform_shouldThrowWhenKeyUsageIsUnspecified() throws Exception {
        final String entityId = "entityId";
        final String keyUse = "UNSPECIFIED";
        String certificateValue = "some-cert-value";

        SamlTransformationErrorManagerTestHelper.validateFail(
                () -> {
                    KeyDescriptorBuilder keyDescriptorBuilder = aKeyDescriptor()
                            .withUse(keyUse)
                            .withKeyInfo(aKeyInfo()
                                    .withKeyName(entityId)
                            .withX509Datas(Arrays.asList(aX509Data()
                                    .withX509Certificate(aX509Certificate().withCert(certificateValue).build())
                                    .build()
                    )).build());
                    marshaller.toCertificate(keyDescriptorBuilder.build());
                },
                SamlTransformationErrorFactory.unsupportedKey(keyUse));
    }

    @Test
    public void transform_shouldTransformToCertificateForKeyDescriptorWithNoKeyNames() throws Exception {
        String certificateValue = "some-cert-value";
        KeyInfo keyInfoWithNoKeyNames = aKeyInfo()
                        .withKeyName(null)
                        .withX509Data(aX509Data()
                                        .withX509Certificate(aX509Certificate().withCert(certificateValue).build())
                                        .build()
                        )
                        .build();

        KeyDescriptor keyDescriptor = aKeyDescriptor().withKeyInfo(keyInfoWithNoKeyNames).build();

        final Certificate result = marshaller.toCertificate(keyDescriptor);

        assertThat(result.getCertificate()).isEqualTo(certificateValue);
        assertThat(result.getIssuerId()).isNull();
    }
}
