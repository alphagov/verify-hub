package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.validation.ValidationMethod;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Optional;

public class AssertionConsumerService {

    protected AssertionConsumerService() {
    }

    @NotNull
    @Valid
    @JsonProperty
    protected URI uri;

    @Valid
    @JsonProperty
    protected Integer index;

    @NotNull
    @Valid
    @JsonProperty
    protected Boolean isDefault = false;

    public URI getUri() {
        return uri;
    }

    @SuppressWarnings("unused")
    @ValidationMethod(message = "Assertion Consumer Service url must be an absolute url.")
    public boolean isUriValid() {
        return uri.isAbsolute();
    }

    public Optional<Integer> getIndex() {
        return Optional.ofNullable(index);
    }

    @SuppressWarnings("unused")
    @ValidationMethod(message = "Index must be an unsigned integer.")
    public boolean isIndexAbsentOrUnsigned() {
        if (getIndex().isPresent()) {
            return getIndex().get() >= 0;
        }
        return true;
    }

    public Boolean getDefault() {
        return isDefault;
    }
}
