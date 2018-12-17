package uk.gov.ida.saml.hub.transformers.outbound.decorators;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.xmlsec.signature.Signature;
import uk.gov.ida.saml.security.SignatureFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SigningRequestAbstractTypeSignatureCreatorTest {

    private SigningRequestAbstractTypeSignatureCreator<AttributeQuery> signatureCreator;
    @Mock
    private SignatureFactory signatureFactory;
    private static final String id = "response-id";
    @Before
    public void setup() {
        signatureCreator = new SigningRequestAbstractTypeSignatureCreator<>(signatureFactory);
    }

    @Test
    public void decorate_shouldGetSignatureAndAssignIt() {
        AttributeQuery response = mock(AttributeQuery.class);
        when(response.getID()).thenReturn(id);

        signatureCreator.addUnsignedSignatureTo(response);

        verify(signatureFactory).createSignature(id);
    }

    @Test
    public void decorate_shouldAssignSignatureToResponse() {
        AttributeQuery response = mock(AttributeQuery.class);
        Signature signature = mock(Signature.class);
        when(response.getID()).thenReturn(id);
        when(signatureFactory.createSignature(id)).thenReturn(signature);

        signatureCreator.addUnsignedSignatureTo(response);

        verify(response).setSignature(signature);
    }
}
