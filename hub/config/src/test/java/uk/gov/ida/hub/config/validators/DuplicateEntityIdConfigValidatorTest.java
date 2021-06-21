package uk.gov.ida.hub.config.validators;

import org.junit.jupiter.api.Test;
import uk.gov.ida.hub.config.domain.EntityIdentifiable;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;

import java.util.ArrayList;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;

public class DuplicateEntityIdConfigValidatorTest {

    private DuplicateEntityIdConfigValidator validator = new DuplicateEntityIdConfigValidator();

    @Test
    public void validate_shouldThrowExceptionIfTwoEntitiesHaveSameEntityId() {
        String entityId = "transaction-entity-id";
        Collection<EntityIdentifiable> configs = new ArrayList<>();
        configs.add(aTransactionConfigData().withEntityId(entityId).build());
        configs.add(anIdentityProviderConfigData().withEntityId(entityId).build());

        try {
            this.validator.validate(configs);
            fail("fail");
        } catch (ConfigValidationException e) {
            assertThat(e.getMessage()).isEqualTo(ConfigValidationException.createDuplicateEntityIdException(entityId).getMessage());
        }
    }
}
