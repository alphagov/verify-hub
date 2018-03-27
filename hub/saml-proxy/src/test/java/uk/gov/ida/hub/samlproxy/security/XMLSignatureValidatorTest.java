package uk.gov.ida.hub.samlproxy.security;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA384;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA1;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA384;
import org.opensaml.xmlsec.signature.support.Signer;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.gov.ida.saml.core.IdaSamlBootstrap;
import uk.gov.ida.saml.core.test.TestCertificateStrings;
import uk.gov.ida.saml.core.test.TestCredentialFactory;
import uk.gov.ida.saml.core.test.builders.AuthnRequestBuilder;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import java.security.PublicKey;
import java.security.SignatureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class XMLSignatureValidatorTest {

    private static XMLSignatureValidator xmlSignatureValidator;
    private final Credential signingCredential = new TestCredentialFactory(TestCertificateStrings.TEST_PUBLIC_CERT, TestCertificateStrings.TEST_PRIVATE_KEY).getSigningCredential();
    private final PublicKey publicKey = signingCredential.getPublicKey();

    @BeforeClass
    public static void before() throws Exception {
        IdaSamlBootstrap.bootstrap();
        xmlSignatureValidator = new XMLSignatureValidator();
    }

    @Test
    public void shouldReturnTrueIfTheSignatureIsGood() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().build();
        PublicKey publicKey = new TestCredentialFactory(TestCertificateStrings.TEST_PUBLIC_CERT, TestCertificateStrings.TEST_PRIVATE_KEY).getSigningCredential().getPublicKey();

        assertThat(xmlSignatureValidator.validate(authnRequest.getDOM(), publicKey)).isTrue();
    }

    @Test
    public void shouldThrowExceptionWhenSignatureIsntSigned() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().withoutSigning().build();
        Element element = XMLObjectSupport.marshall(authnRequest);

        assertThatThrownBy(() -> xmlSignatureValidator.validate(element, publicKey))
                .isInstanceOf(XMLSignatureException.class)
                .hasMessage("java.security.SignatureException: Signature length not correct: got 0 but was expecting 256");
    }

    @Test
    public void shouldThrowExceptionWhenSignatureMissing() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().withoutSignatureElement().build();
        Element element = XMLObjectSupport.marshall(authnRequest);

        assertThatThrownBy(() -> xmlSignatureValidator.validate(element, publicKey))
                .isInstanceOf(SignatureException.class)
                .hasMessage("No Signature found in Element: saml2p:AuthnRequest");
    }

    @Test
    public void shouldThrowExceptionWhenSignatureMethodIsSha1() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().withSignatureAlgorithm(new SignatureRSASHA1()).build();
        Element element = XMLObjectSupport.marshall(authnRequest);

        assertThat(xmlSignatureValidator.validate(element, publicKey)).isTrue();
    }

    @Test
    public void shouldThrowExceptionWhenSignatureMethodIsSha256() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().withSignatureAlgorithm(new SignatureRSASHA256()).build();
        Element element = XMLObjectSupport.marshall(authnRequest);

        assertThat(xmlSignatureValidator.validate(element, publicKey)).isTrue();
    }

    @Test
    public void shouldThrowExceptionWhenSignatureMethodIsNotSupport() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().withSignatureAlgorithm(new SignatureRSASHA384()).build();
        Element element = XMLObjectSupport.marshall(authnRequest);

        assertThatThrownBy(() -> xmlSignatureValidator.validate(element, publicKey))
                .isInstanceOf(SignatureException.class)
                .hasMessage("Signature's Method http://www.w3.org/2001/04/xmldsig-more#rsa-sha384 is not supported");
    }

    @Test
    public void shouldThrowExceptionWhenDigestMethodIsSha256() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().withDigestAlgorithm(new DigestSHA256()).build();
        Element element = XMLObjectSupport.marshall(authnRequest);

        assertThat(xmlSignatureValidator.validate(element, publicKey)).isTrue();
    }

    @Test
    public void shouldThrowExceptionWhenDigestMethodIsNotSupported() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().withDigestAlgorithm(new DigestSHA384()).build();
        Element element = XMLObjectSupport.marshall(authnRequest);

        assertThatThrownBy(() -> xmlSignatureValidator.validate(element, publicKey))
                .isInstanceOf(SignatureException.class)
                .hasMessage("DigestMethod 'http://www.w3.org/2001/04/xmldsig-more#sha384' is not supported");
    }

    @Test
    public void shouldThrowExceptionWhenCanonicalizationIsInclusive() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().withoutSigning().build();
        authnRequest.getSignature().setCanonicalizationAlgorithm(CanonicalizationMethod.INCLUSIVE);
        Element element = XMLObjectSupport.marshall(authnRequest);

        assertThatThrownBy(() -> xmlSignatureValidator.validate(element, publicKey))
                .isInstanceOf(SignatureException.class)
                .hasMessage("Signature's CanonicalizationMethod " + CanonicalizationMethod.INCLUSIVE + " is not supported");
    }

    @Test
    public void shouldThrowExceptionWhenCanonicalizationIsInclusiveWithComments() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().withoutSigning().build();
        authnRequest.getSignature().setCanonicalizationAlgorithm(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS);
        Element element = XMLObjectSupport.marshall(authnRequest);

        assertThatThrownBy(() -> xmlSignatureValidator.validate(element, publicKey))
                .isInstanceOf(SignatureException.class)
                .hasMessage("Signature's CanonicalizationMethod " + CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS + " is not supported");
    }

    @Test
    public void shouldThrowExceptionWhenCanonicalizationIsExclusive() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().withoutSigning().build();
        authnRequest.getSignature().setCanonicalizationAlgorithm(CanonicalizationMethod.EXCLUSIVE);
        Signer.signObject(authnRequest.getSignature());
        Element element = XMLObjectSupport.marshall(authnRequest);

        assertThat(xmlSignatureValidator.validate(element, publicKey)).isTrue();
    }

    @Test
    public void shouldThrowExceptionWhenCanonicalizationIsExclusiveWithComments() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().withoutSigning().build();
        authnRequest.getSignature().setCanonicalizationAlgorithm(CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS);
        XMLObjectSupport.marshall(authnRequest);
        Signer.signObject(authnRequest.getSignature());
        Element element = XMLObjectSupport.marshall(authnRequest);
        assertThat(xmlSignatureValidator.validate(element, publicKey)).isTrue();
    }

    private Element getElementByName(Element element, String signature) {
        return Element.class.cast(element.getElementsByTagNameNS(XMLSignature.XMLNS, signature).item(0));
    }

    @Test
    public void shouldThrowExceptionWhenReferenceUriIsWrong() throws Exception {

        AuthnRequest authnRequest = new AuthnRequestBuilder().build();
        String id = authnRequest.getID();
        authnRequest.setID("FOOBAR");
        authnRequest.getIssuer().getDOM().setAttribute("ID", id);

        Element element = XMLObjectSupport.marshall(authnRequest);
        assertThatThrownBy(() -> xmlSignatureValidator.validate(element, publicKey))
                .isInstanceOf(SignatureException.class)
                .hasMessage("Signature Reference URI doesn't equal enveloping element ID");
    }

    @Test
    public void shouldThrowExceptionWhenReferenceUriIsMalformed() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().build();
        Element element = XMLObjectSupport.marshall(authnRequest);
        Element signature = getElementByName(element, "Signature");
        Element signedInfo = getElementByName(signature, "SignedInfo");
        Element reference = getElementByName(signedInfo, "Reference");
        reference.setAttribute("URI", "FOOBAR");

        assertThatThrownBy(() -> xmlSignatureValidator.validate(element, publicKey))
                .isInstanceOf(SignatureException.class)
                .hasMessage("Signature Reference URI doesn't equal enveloping element ID");
    }

    @Test
    public void shouldThrowExceptionWhenReferenceWhenMoreThan1Reference() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().build();
        Element element = XMLObjectSupport.marshall(authnRequest);
        Element signature = getElementByName(element, "Signature");
        Element signedInfo = getElementByName(signature, "SignedInfo");
        Element reference = getElementByName(signedInfo, "Reference");
        Node referenceCopy = element.getOwnerDocument().importNode(reference, true);
        signedInfo.appendChild(referenceCopy);

        assertThatThrownBy(() -> xmlSignatureValidator.validate(element, publicKey))
                .isInstanceOf(SignatureException.class)
                .hasMessage("More than one Reference found in Signature");
    }

    private Element getTransformsElement(Element element) {
        Element signature = getElementByName(element, "Signature");
        Element signedInfo = getElementByName(signature, "SignedInfo");
        Element reference = getElementByName(signedInfo, "Reference");
        return getElementByName(reference, "Transforms");
    }

    @Test
    public void shouldThrowExceptionWhenReferenceWhenNoEnvelopTransform() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().build();
        Element element = XMLObjectSupport.marshall(authnRequest);
        Element transformsHolder = getTransformsElement(element);
        NodeList transforms = transformsHolder.getElementsByTagNameNS(XMLSignature.XMLNS, "Transform");
        Node item = transforms.item(0);
        if (Element.class.cast(item).getAttribute("Algorithm").equals(CanonicalizationMethod.ENVELOPED)) {
            transformsHolder.removeChild(item);
        }

        assertThatThrownBy(() -> xmlSignatureValidator.validate(element, publicKey))
                .isInstanceOf(SignatureException.class)
                .hasMessage("The Reference is missing a 'http://www.w3.org/2000/09/xmldsig#enveloped-signature' Transform element");
    }

    @Test
    public void shouldThrowExceptionWhenReferenceWhenSecondTransformMethodIsExclusive() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().build();
        Element element = XMLObjectSupport.marshall(authnRequest);
        Element transformsHolder = getTransformsElement(element);
        NodeList transforms = transformsHolder.getElementsByTagNameNS(XMLSignature.XMLNS, "Transform");
        Element.class.cast(transforms.item(1)).setAttribute("Algorithm", CanonicalizationMethod.EXCLUSIVE);

        assertThat(xmlSignatureValidator.validate(element, publicKey)).isTrue();
    }

    @Test
    public void shouldThrowExceptionWhenReferenceWhenSecondTransformMethodIsExclusiveWithComments() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().withoutSigning().build();
        Element element = XMLObjectSupport.marshall(authnRequest);
        Element transformsHolder = getTransformsElement(element);
        NodeList transforms = transformsHolder.getElementsByTagNameNS(XMLSignature.XMLNS, "Transform");
        //Set second transform as Exclusive with Comments
        Element.class.cast(transforms.item(1)).setAttribute("Algorithm", CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS);
        Signer.signObject(authnRequest.getSignature());

        assertThat(xmlSignatureValidator.validate(authnRequest.getDOM(), publicKey)).isTrue();
    }

    @Test
    public void shouldThrowExceptionWhenReferenceWhenHasUnsupportedTransformMethod() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().build();
        Element element = XMLObjectSupport.marshall(authnRequest);
        Element transformsHolder = getTransformsElement(element);
        NodeList transforms = transformsHolder.getElementsByTagNameNS(XMLSignature.XMLNS, "Transform");
        Element.class.cast(transforms.item(1)).setAttribute("Algorithm", CanonicalizationMethod.INCLUSIVE);

        assertThatThrownBy(() -> xmlSignatureValidator.validate(element, publicKey))
                .isInstanceOf(SignatureException.class)
                .hasMessage("The Reference has a Transform method that is not in the whitelist");
    }

    @Test
    public void shouldNotHaveAnObjectElement() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().build();
        Element element = XMLObjectSupport.marshall(authnRequest);
        Element signatureElement = Element.class.cast(getElementByName(element, "Signature"));
        signatureElement.appendChild(element.getOwnerDocument().createElementNS(XMLSignature.XMLNS, "Object"));

        assertThatThrownBy(() -> xmlSignatureValidator.validate(element, publicKey))
                .isInstanceOf(SignatureException.class)
                .hasMessage("Signature element has one or more child Object elements, which are not permitted");
    }
}