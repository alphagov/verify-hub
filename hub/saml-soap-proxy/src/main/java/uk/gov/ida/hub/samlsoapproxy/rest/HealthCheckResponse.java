package uk.gov.ida.hub.samlsoapproxy.rest;

import org.w3c.dom.Element;

public class HealthCheckResponse {
    private final Element responseElement;

    public HealthCheckResponse(Element responseElement) {
        this.responseElement = responseElement;
    }

    public Element getResponseElement() {
        return responseElement;
    }

}
