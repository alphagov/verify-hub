package uk.gov.ida.hub.samlsoapproxy.rest;

import com.google.common.base.Optional;
import org.w3c.dom.Element;

public class HealthCheckResponse {
    private final Element responseElement;
    private final Optional<String> versionNumber;

    public HealthCheckResponse(Element responseElement, Optional<String> versionNumber) {
        this.responseElement = responseElement;
        this.versionNumber = versionNumber;
    }

    public Element getResponseElement() {
        return responseElement;
    }

    public Optional<String> getVersionNumber() {
        return versionNumber;
    }
}
