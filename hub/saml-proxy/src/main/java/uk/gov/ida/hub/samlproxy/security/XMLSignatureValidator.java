package uk.gov.ida.hub.samlproxy.security;

import com.google.common.collect.ImmutableList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import java.security.Key;
import java.security.Provider;
import java.security.SignatureException;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class XMLSignatureValidator {

    public static final ImmutableList<String> SUPPORTED_DIGEST_METHODS = ImmutableList.of(DigestMethod.SHA1, DigestMethod.SHA256);
    public static final List<Object> SUPPORT_TRANSFORM_METHODS = asList(CanonicalizationMethod.ENVELOPED, CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS, CanonicalizationMethod.EXCLUSIVE);
    private XMLSignatureFactory fac;
    public static final List<String> SUPPORTED_SIGNATURE_METHODS = asList(
            org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA512,
            org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1,
            org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256

    );
    public static final ImmutableList<String> SUPPORTED_CANONICALIZATION_METHODS = ImmutableList.of(CanonicalizationMethod.EXCLUSIVE, CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS);

    public XMLSignatureValidator() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        String providerName = System.getProperty(
                "jsr105Provider",
                "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
        fac = XMLSignatureFactory.getInstance("DOM",
                (Provider) Class.forName(providerName).newInstance());
    }


    public boolean validate(Element element, Key key) throws MarshalException, XMLSignatureException, SignatureException {
        NodeList signatureElements = element.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if(signatureElements.getLength() ==  0) {
            throw new SignatureException("No Signature found in Element: " + element.getNodeName());
        }
        if(signatureElements.getLength() > 1) {
            throw new SignatureException("More than one Signature found in Element: " + element.getNodeName());
        }
        Node signatureElement = signatureElements.item(0);

        DOMValidateContext valContext = new DOMValidateContext(KeySelector.singletonKeySelector(key), signatureElement);

        XMLSignature xmlSignature = fac.unmarshalXMLSignature(valContext);

        validateSignedInfo(element, xmlSignature);

        validateNoObjects(xmlSignature);

        return xmlSignature.validate(valContext);
    }

    private void validateNoObjects(XMLSignature xmlSignature) throws SignatureException {
       if(xmlSignature.getObjects().size() != 0) {
           throw new SignatureException("Signature element has one or more child Object elements, which are not permitted");
       }
    }

    private void validateSignedInfo(Element element, XMLSignature xmlSignature) throws SignatureException {
        SignedInfo signedInfo = xmlSignature.getSignedInfo();
        validateReference(element, signedInfo);
        validateCanonicalizationMethod(signedInfo);
        validateSignatureMethod(signedInfo);
    }

    private void validateReference(Element element, SignedInfo signedInfo) throws SignatureException {
        List<Reference> references = (List<Reference>) signedInfo.getReferences();
        if(references.size() ==  0) {
            throw new SignatureException("No Reference found in Signature");
        }
        if(references.size() > 1) {
            throw new SignatureException("More than one Reference found in Signature");
        }

        Reference reference = references.get(0);

        validateReferenceUri(element, reference);
        validateReferenceDigestMethod(reference);
        validateReferenceTransforms(reference);
    }

    private void validateSignatureMethod(SignedInfo signedInfo) throws SignatureException {
        String signatureMethod = signedInfo.getSignatureMethod().getAlgorithm();
        if(!SUPPORTED_SIGNATURE_METHODS.contains( signatureMethod)) {
           throw new SignatureException("Signature's Method "+ signatureMethod + " is not supported");
        }
    }

    private void validateCanonicalizationMethod(SignedInfo signedInfo) throws SignatureException {
        String canonicalizationMethod = signedInfo.getCanonicalizationMethod().getAlgorithm();
        if(!SUPPORTED_CANONICALIZATION_METHODS.contains(canonicalizationMethod)) {
            throw new SignatureException("Signature's CanonicalizationMethod "+ canonicalizationMethod + " is not supported");
        }
    }

    private void validateReferenceUri(Element element, Reference reference) throws SignatureException {
        String uri = reference.getURI();
        if(!uri.equals("#" + element.getAttribute("ID"))) {
            throw new SignatureException("Signature Reference URI doesn't equal enveloping element ID");
        }

        if(!element.getOwnerDocument().getElementById(uri.substring(1)).isSameNode(element)) {
            throw new SignatureException("Signature URI doesn't reference enveloping element in DOM");
        }
    }

    private void validateReferenceDigestMethod(Reference reference) throws SignatureException {
        String referenceDigestMethod = reference.getDigestMethod().getAlgorithm();
        if(!SUPPORTED_DIGEST_METHODS.contains(referenceDigestMethod)) {
            throw new SignatureException("DigestMethod '" + referenceDigestMethod  + "' is not supported");
        }
    }

    private void validateReferenceTransforms(Reference reference) throws SignatureException {
        List<Transform> transforms = (List<Transform>) reference.getTransforms();
        if(transforms.size() > 2) {
            throw new SignatureException("A Reference may have at most 2 Transforms");
        }
        List<String> referenceTransformMethods = transforms.stream().map((AlgorithmMethod::getAlgorithm)).collect(Collectors.toList());
        if(!referenceTransformMethods.contains(Transform.ENVELOPED)) {
            throw new SignatureException("The Reference is missing a '" + Transform.ENVELOPED + "' Transform element");
        }

        if(!SUPPORT_TRANSFORM_METHODS.containsAll(referenceTransformMethods)) {
            throw new SignatureException("The Reference has a Transform method that is not in the whitelist");
        }
    }
}
