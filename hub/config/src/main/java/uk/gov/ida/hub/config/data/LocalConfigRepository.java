package uk.gov.ida.hub.config.data;

import uk.gov.ida.hub.config.domain.EntityIdentifiable;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class LocalConfigRepository<T extends EntityIdentifiable> {

    private Map<String, T> dataMap = new HashMap<>();

    @Inject
    public LocalConfigRepository() {
    }

    public boolean containsKey(String entityId){
        return dataMap.containsKey(entityId);
    }

    public Optional<T> getData(String entityId) {
        return Optional.ofNullable(dataMap.get(entityId));
    }

    public Set<T> getAllData() {
        return new HashSet<>(dataMap.values());
    }

    public void addData(T datum) {
        dataMap.put(datum.getEntityId(), datum);
    }

    public void addData(Collection<T> data) {
        for(T datum : data) {
            addData(datum);
        }
    }

    public Stream<T> stream() {
        return dataMap.values().stream();
    }
}
