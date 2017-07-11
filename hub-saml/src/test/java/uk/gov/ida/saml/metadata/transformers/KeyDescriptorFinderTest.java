package uk.gov.ida.saml.metadata.transformers;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper;
import uk.gov.ida.saml.core.test.builders.metadata.KeyDescriptorBuilder;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static uk.gov.ida.saml.core.test.builders.metadata.KeyInfoBuilder.aKeyInfo;

@RunWith(OpenSAMLMockitoRunner.class)
public class KeyDescriptorFinderTest {

    private KeyDescriptorFinder finder;

    @Before
    public void setup() {
        finder = new KeyDescriptorFinder();
    }

    @Test
    public void find_shouldFindKeyDescriptorWithMatchingUsageAndEntityId() throws Exception {
        final String entityId = UUID.randomUUID().toString();
        final KeyDescriptor desiredKeyDescriptor = KeyDescriptorBuilder.aKeyDescriptor().withKeyInfo(aKeyInfo().withKeyName(entityId).build()).withUse(UsageType.SIGNING.toString()).build();

        final KeyDescriptor result =
                finder.find(asList(KeyDescriptorBuilder.aKeyDescriptor().build(), desiredKeyDescriptor), UsageType.SIGNING, entityId);

        Assertions.assertThat(result).isEqualTo(desiredKeyDescriptor);
    }

    @Test
    public void find_shouldFindKeyDescriptorWithMatchingUsageWhenItHasNoKeyName() throws Exception {
        final String entityId = UUID.randomUUID().toString();
        final KeyDescriptor desiredKeyDescriptor = KeyDescriptorBuilder.aKeyDescriptor().withKeyInfo(aKeyInfo().withKeyName(null).build()).withUse(UsageType.SIGNING.toString()).build();

        final KeyDescriptor result =
                finder.find(asList(KeyDescriptorBuilder.aKeyDescriptor().build(), desiredKeyDescriptor), UsageType.SIGNING, entityId);

        Assertions.assertThat(result).isEqualTo(desiredKeyDescriptor);
    }

    @Test
    public void find_shouldFindKeyDescriptorWithMatchingUsageWhenKeyNameIsPresentAndExpectedEntityIdIsNull() throws Exception {
        final KeyDescriptor desiredKeyDescriptor = KeyDescriptorBuilder.aKeyDescriptor().withKeyInfo(aKeyInfo().withKeyName("foo").build()).withUse(UsageType.SIGNING.toString()).build();

        final KeyDescriptor result =
                finder.find(asList(KeyDescriptorBuilder.aKeyDescriptor().withUse(UsageType.ENCRYPTION.toString()).build(), desiredKeyDescriptor), UsageType.SIGNING, null);

        Assertions.assertThat(result).isEqualTo(desiredKeyDescriptor);
    }

    @Test
    public void find_shouldThrowExceptionWhenSigningCertificateIsNotPresent() throws Exception {
        final KeyDescriptor keyDescriptor = KeyDescriptorBuilder.aKeyDescriptor().withUse(UsageType.ENCRYPTION.toString()).build();

        SamlTransformationErrorManagerTestHelper.validateFail(
                () -> finder.find(singletonList(keyDescriptor), UsageType.SIGNING, keyDescriptor.getKeyInfo().getKeyNames().get(0).getValue()),
                SamlTransformationErrorFactory.missingKey(UsageType.SIGNING.toString(), "default-key-name"));
    }

    @Test
    public void find_shouldThrowExceptionWhenEncryptionCertificateIsNotPresent() throws Exception {
        final KeyDescriptor keyDescriptor = KeyDescriptorBuilder.aKeyDescriptor().withUse(UsageType.SIGNING.toString()).build();

        SamlTransformationErrorManagerTestHelper.validateFail(
                () -> finder.find(singletonList(keyDescriptor), UsageType.ENCRYPTION, keyDescriptor.getKeyInfo().getKeyNames().get(0).getValue()),
                SamlTransformationErrorFactory.missingKey(UsageType.ENCRYPTION.toString(), "default-key-name"));

    }

    @Test
    public void find_shouldThrowExceptionWhenKeyNameIsPresentButDoesNotMatchExpectedEntityId() throws Exception {
        final KeyDescriptor keyDescriptor = KeyDescriptorBuilder.aKeyDescriptor().withUse(UsageType.SIGNING.toString()).build();

        SamlTransformationErrorManagerTestHelper.validateFail(
                () -> finder.find(singletonList(keyDescriptor), UsageType.SIGNING, "wrong-value"),
                SamlTransformationErrorFactory.missingKey(UsageType.SIGNING.toString(), "wrong-value"));

    }
}
