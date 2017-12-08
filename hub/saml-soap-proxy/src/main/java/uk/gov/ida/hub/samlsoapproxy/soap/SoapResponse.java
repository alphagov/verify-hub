package uk.gov.ida.hub.samlsoapproxy.soap;

import org.w3c.dom.Element;

import javax.ws.rs.core.MultivaluedMap;

public class SoapResponse {
    private final Element body;
    private final MultivaluedMap<String, String> headers;

    public SoapResponse(Element body, MultivaluedMap<String, String> headers) {
        this.body = body;
        this.headers = headers;
    }

    public Element getBody() {
        return body;
    }

    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }
}
