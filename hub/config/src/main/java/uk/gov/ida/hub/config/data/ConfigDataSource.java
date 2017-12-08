package uk.gov.ida.hub.config.data;

import java.util.Collection;

public interface ConfigDataSource<T> {
    Collection<T> loadConfig();
}
