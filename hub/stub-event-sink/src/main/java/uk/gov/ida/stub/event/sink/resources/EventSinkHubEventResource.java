package uk.gov.ida.stub.event.sink.resources;

import uk.gov.ida.eventsink.EventSinkHubEvent;
import uk.gov.ida.stub.event.sink.Urls;
import uk.gov.ida.stub.event.sink.repositories.InMemoryEventSinkHubEventStore;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE)
public class EventSinkHubEventResource {

    private final InMemoryEventSinkHubEventStore inMemoryEventSinkHubEventStore;

    @Inject
    public EventSinkHubEventResource(InMemoryEventSinkHubEventStore inMemoryEventSinkHubEventStore) {
        this.inMemoryEventSinkHubEventStore = inMemoryEventSinkHubEventStore;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postHubEvent(EventSinkHubEvent event) {
        inMemoryEventSinkHubEventStore.add(event);
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
