
package uk.gov.ida.hub.config.data;

import uk.gov.ida.hub.config.domain.CertificateConfigurable;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ManagedEntityConfigRepository <T extends CertificateConfigurable> implements ConfigRepository<T>{

    private LocalConfigRepository<T> localConfigRepository;

    @Inject
    public ManagedEntityConfigRepository(LocalConfigRepository<T> localConfigRepository) {
        this.localConfigRepository = localConfigRepository;
    }

    public Set<T> getAll() {
        return new HashSet<>(localConfigRepository.getAllData());
    }

    public Optional<T> get(String entityId) {
        return localConfigRepository.getData(entityId);
    }

    public boolean has(String entityId){
        return localConfigRepository.containsKey(entityId);
    }
}