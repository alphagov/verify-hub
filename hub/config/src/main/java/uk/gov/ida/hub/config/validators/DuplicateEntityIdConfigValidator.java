package uk.gov.ida.hub.config.validators;

import uk.gov.ida.hub.config.domain.EntityIdentifiable;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class DuplicateEntityIdConfigValidator {

    public void validate(Collection<? extends EntityIdentifiable> configDataCollection) {
        Set<String> knownEntityIds = new HashSet<>(configDataCollection.size());

        for (EntityIdentifiable datum : configDataCollection) {
            String entityId = datum.getEntityId();
            if (knownEntityIds.contains(entityId)) {
                throw ConfigValidationException.createDuplicateEntityIdException(entityId);
            }
            knownEntityIds.add(entityId);
        }

    }

}
