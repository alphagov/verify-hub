package uk.gov.ida.stub.event.sink;

import com.google.inject.AbstractModule;
import uk.gov.ida.stub.event.sink.repositories.InMemoryEventSinkHubEventStore;

public class StubEventSinkModule extends AbstractModule {

    public StubEventSinkModule() {
    }

    @Override
    protected void configure() {
        bind(InMemoryEventSinkHubEventStore.class).asEagerSingleton();
    }
}
