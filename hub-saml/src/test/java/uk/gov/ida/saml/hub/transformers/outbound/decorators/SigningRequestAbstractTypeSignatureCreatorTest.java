package uk.gov.ida.saml.hub.transformers.outbound.decorators;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.Issuer;
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
        String id = "response-id";
        signatureCreator = new SigningRequestAbstractTypeSignatureCreator<>(signatureFactory);
    }

    @Test
    public void decorate_shouldGetSignatureAndAssignIt() {
        AttributeQuery response = mock(AttributeQuery.class);
        Issuer issuer = mock(Issuer.class);
        String issuerId = "some-issuer-id";
        when(issuer.getValue()).thenReturn(issuerId);
        when(response.getIssuer()).thenReturn(issuer);
        when(response.getID()).thenReturn(id);

        signatureCreator.addUnsignedSignatureTo(response);

        verify(signatureFactory).createSignature(id);
    }

    @Test
    public void decorate_shouldAssignSignatureToResponse() {
        AttributeQuery response = mock(AttributeQuery.class);
        Signature signature = mock(Signature.class);
        when(response.getIssuer()).thenReturn(mock(Issuer.class));
        when(response.getID()).thenReturn(id);
        when(signatureFactory.createSignature(id)).thenReturn(signature);

        signatureCreator.addUnsignedSignatureTo(response);

        verify(response).setSignature(signature);
    }
}
