package uk.gov.ida.hub.samlsoapproxy.builders;

import java.util.Optional;
import org.w3c.dom.Element;
import uk.gov.ida.hub.samlsoapproxy.rest.HealthCheckResponse;

public class HealthCheckResponseBuilder {

    private Element element;
    private Optional<String> versionNumber = Optional.empty();

    public static HealthCheckResponseBuilder aHealthCheckResponse(){
        return new HealthCheckResponseBuilder();
    }

    public HealthCheckResponse build(){
        return new HealthCheckResponse(element, versionNumber);
    }

    public HealthCheckResponseBuilder withElement(Element element) {
        this.element = element;
        return this;
    }

    public HealthCheckResponseBuilder withVersionNumber(String versionNumber) {
        this.versionNumber = Optional.ofNullable(versionNumber);
        return this;
    }
}
