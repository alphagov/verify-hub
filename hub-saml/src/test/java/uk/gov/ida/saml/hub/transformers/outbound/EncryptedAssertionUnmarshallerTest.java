package uk.gov.ida.saml.hub.transformers.outbound;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.impl.EncryptedAssertionBuilder;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EncryptedAssertionUnmarshallerTest {

    private static final String ENCRYPTED_ASSERTION_BLOB = "BLOB";

    @Mock
    public StringToOpenSamlObjectTransformer<EncryptedAssertion> stringToEncryptedAssertionTransformer;

    @Test
    public void shouldCreateAEncryptedAssertionObjectFromAGivenString() throws Exception {
        EncryptedAssertionUnmarshaller encryptedAssertionUnmarshaller = new EncryptedAssertionUnmarshaller(stringToEncryptedAssertionTransformer);
        final EncryptedAssertion expected = new EncryptedAssertionBuilder().buildObject();
        when(stringToEncryptedAssertionTransformer.apply(ENCRYPTED_ASSERTION_BLOB)).thenReturn(expected);
        final EncryptedAssertion encryptedAssertion = encryptedAssertionUnmarshaller.transform(ENCRYPTED_ASSERTION_BLOB);
        assertThat(encryptedAssertion).isEqualTo(expected);

    }
}
