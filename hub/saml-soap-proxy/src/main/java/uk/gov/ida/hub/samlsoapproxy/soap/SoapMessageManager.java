package uk.gov.ida.hub.samlsoapproxy.soap;

import org.apache.ws.commons.util.NamespaceContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static java.text.MessageFormat.format;
import static uk.gov.ida.shared.utils.xml.XmlUtils.newDocumentBuilder;
import static uk.gov.ida.shared.utils.xml.XmlUtils.writeToString;

public class SoapMessageManager {
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final Logger LOG = LoggerFactory.getLogger(SoapMessageManager.class);

    public Document wrapWithSoapEnvelope(Element element) {
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            LOG.error("*** ALERT: Failed to create a document builder when trying to construct the the soap message. ***", e);
            throw new RuntimeException(e);
        }
        Document document = documentBuilder.newDocument();
        document.adoptNode(element);
        Element envelope = document.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "soapenv:Envelope");
        Element body = document.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "soapenv:Body");
        envelope.appendChild(body);
        body.appendChild(element);
        document.appendChild(envelope);

        return document;
    }

    public Element unwrapSoapMessage(Document document) {
        return unwrapSoapMessage(document.getDocumentElement());
    }

    public Element unwrapSoapMessage(Element soapElement) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NamespaceContextImpl context = new NamespaceContextImpl();
        context.startPrefixMapping("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");
        context.startPrefixMapping("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");
        context.startPrefixMapping("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
        context.startPrefixMapping("ds", "http://www.w3.org/2000/09/xmldsig#");
        xpath.setNamespaceContext(context);
        try {
            final String expression = "/soapenv:Envelope/soapenv:Body/samlp:Response";
            Element element = (Element) xpath.evaluate(expression, soapElement, XPathConstants.NODE);

            if (element == null) {
                String errorMessage = format("Document{0}{1}{0}does not have element {2} inside it.", NEW_LINE,
                        writeToString(soapElement), expression);
                LOG.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }

            return element;
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

}
