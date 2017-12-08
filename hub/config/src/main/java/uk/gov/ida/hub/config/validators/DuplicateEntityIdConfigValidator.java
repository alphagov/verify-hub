package uk.gov.ida.hub.config.validators;

import uk.gov.ida.hub.config.ConfigEntityData;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class DuplicateEntityIdConfigValidator {

    public void validate(Collection<? extends ConfigEntityData> configDataCollection) {
        Set<String> knownEntityIds = new HashSet<>(configDataCollection.size());

        for (ConfigEntityData datum : configDataCollection) {
            String entityId = datum.getEntityId();
            if (knownEntityIds.contains(entityId)) {
                throw ConfigValidationException.createDuplicateEntityIdException(entityId);
            }
            knownEntityIds.add(entityId);
        }

    }

}
