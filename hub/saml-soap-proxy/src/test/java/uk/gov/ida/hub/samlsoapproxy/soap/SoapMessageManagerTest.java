package uk.gov.ida.hub.samlsoapproxy.soap;

import org.apache.ws.commons.util.NamespaceContextImpl;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.shared.utils.xml.XmlUtils.newDocumentBuilder;

public class SoapMessageManagerTest {
    @Test
    public void wrapWithSoapEnvelope_shouldWrapElementInsideSoapMessageBody() throws Exception {
        Element element = getTestElement();

        SoapMessageManager manager = new SoapMessageManager();
        Document soapMessage = manager.wrapWithSoapEnvelope(element);

        assertThat(getAttributeQuery(soapMessage)).isNotNull();
    }

    @Test
    public void unwrapSoapMessage_shouldUnwrapElementInsideSoapMessageBody() throws Exception {
        Element element = getTestElement();

        SoapMessageManager manager = new SoapMessageManager();
        Document soapMessage = manager.wrapWithSoapEnvelope(element);

        Element unwrappedElement = manager.unwrapSoapMessage(soapMessage);

        assertThat(unwrappedElement).isNotNull();
        assertThat(unwrappedElement.getTagName()).isEqualTo("samlp:Response");
    }

    private Element getTestElement() throws ParserConfigurationException {
        Document document = newDocumentBuilder().newDocument();
        return document.createElementNS("urn:oasis:names:tc:SAML:2.0:protocol", "samlp:Response");
    }

    private Element getAttributeQuery(Document document) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NamespaceContextImpl context = new NamespaceContextImpl();
        context.startPrefixMapping("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");
        context.startPrefixMapping("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");
        xpath.setNamespaceContext(context);

        return (Element) xpath.evaluate("//samlp:Response", document, XPathConstants.NODE);
    }
}
