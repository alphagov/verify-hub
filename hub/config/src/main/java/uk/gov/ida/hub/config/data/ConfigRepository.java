package uk.gov.ida.hub.config.data;

import java.util.Collection;
import java.util.Optional;

public interface ConfigRepository<T> {

    Optional<T> get(String id);
    Collection<T> getAll();
    boolean has(String id);

}