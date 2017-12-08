package uk.gov.ida.stub.event.sink.resources;

import uk.gov.ida.eventsink.EventSinkHubEvent;
import uk.gov.ida.stub.event.sink.StubEventSinkUrls;
import uk.gov.ida.stub.event.sink.repositories.InMemoryEventSinkHubEventStore;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path(StubEventSinkUrls.HUB_SUPPORT_EVENT_SINK_TEST_ROOT)
public class EventSinkHubEventTestResource {

    private final InMemoryEventSinkHubEventStore inMemoryEventSinkHubEventStore;

    @Inject
    public EventSinkHubEventTestResource(InMemoryEventSinkHubEventStore inMemoryEventSinkHubEventStore) {
        this.inMemoryEventSinkHubEventStore = inMemoryEventSinkHubEventStore;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(StubEventSinkUrls.HUB_SUPPORT_EVENT_SINK_TEST_ID_PATH)
    public EventSinkHubEvent getHubEvent(@PathParam(StubEventSinkUrls.HUB_SUPPORT_EVENT_SINK_TEST_ID_PARAM) String id) {
        return inMemoryEventSinkHubEventStore.getEventById(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<EventSinkHubEvent> getHubEvents() {
        return inMemoryEventSinkHubEventStore.getAllEvents();
    }

    @DELETE
    public void deleteHubEvents() {
        inMemoryEventSinkHubEventStore.deleteAllEvents();
    }
}
