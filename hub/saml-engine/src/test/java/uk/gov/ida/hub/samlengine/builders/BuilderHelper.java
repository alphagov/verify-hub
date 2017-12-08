package uk.gov.ida.hub.samlengine.builders;

import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.XMLUtils;
import org.apache.xml.security.utils.resolver.implementations.ResolverXPointer;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

/**
 * BuilderHelper class provides reusable methods for building XML object for
 * testing purpose only.
 */
public final class BuilderHelper {
    public static XMLSignature createXMLSignature(final SignatureAlgorithm signatureAlgorithm, final DigestAlgorithm digestAlgorithm) {
        DocumentBuilder documentBuilder = null;
        XMLSignature xmlSignature = null;
        try {
            documentBuilder = XMLUtils.createDocumentBuilder(false);
        }
        catch (ParserConfigurationException e) { }
        Document doc = documentBuilder.newDocument();
        Element rootElement = doc.createElementNS("https://www.verify.gov.uk/", "root");
        rootElement.appendChild(doc.createTextNode("Welcome to Verify GOV.UK!"));
        doc.appendChild(rootElement);
        try {
            xmlSignature = new XMLSignature(doc, "", signatureAlgorithm.getURI());
            Element root = doc.getDocumentElement();
            root.appendChild(xmlSignature.getElement());
            xmlSignature.getSignedInfo().addResourceResolver(new ResolverXPointer());
            Transforms transforms = new Transforms(doc);
            transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
            transforms.addTransform(Transforms.TRANSFORM_C14N_WITH_COMMENTS);
            xmlSignature.addDocument("", transforms, digestAlgorithm.getURI());
        }
        catch (org.apache.xml.security.exceptions.XMLSecurityException e) {}
        return xmlSignature;
    }
}
