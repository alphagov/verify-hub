package uk.gov.ida.hub.samlsoapproxy.client;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import java.util.Optional;

public class SOAPRequestError extends Exception {
    private final Optional<String> entity;
    private final int status;

    public SOAPRequestError(Response response) {
        this(response, null);
    }

    public SOAPRequestError(Response response, BadRequestException e) {
        super(e);
        status = response.getStatus();
        if (response.hasEntity()) {
            entity = Optional.of(response.readEntity(String.class));
        } else {
            entity = Optional.empty();
        }
    }

    public int getResponseStatus() {
        return status;
    }

    public Optional<String> getEntity() {
        return entity;
    }
}
