package uk.gov.ida.hub.samlsoapproxy.builders;

import org.w3c.dom.Element;
import uk.gov.ida.hub.samlsoapproxy.rest.HealthCheckResponse;

public class HealthCheckResponseBuilder {

    private Element element;

    public static HealthCheckResponseBuilder aHealthCheckResponse(){
        return new HealthCheckResponseBuilder();
    }

    public HealthCheckResponse build(){
        return new HealthCheckResponse(element);
    }

    public HealthCheckResponseBuilder withElement(Element element) {
        this.element = element;
        return this;
    }
}
