package uk.gov.ida.hub.samlengine.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.samlengine.config.ConfigServiceKeyStore;
import uk.gov.ida.saml.metadata.exceptions.NoKeyConfiguredForEntityException;

import java.security.PublicKey;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HubEncryptionKeyStoreTest {

    HubEncryptionKeyStore keyStore;

    @Mock
    private ConfigServiceKeyStore configServiceKeyStore;
    @Mock
    private PublicKey publicKey;

    @BeforeEach
    public void setUp() throws Exception {
        keyStore = new HubEncryptionKeyStore(configServiceKeyStore);
    }

    @Test
    public void shouldGetPublicKeyForAnEntityThatExists() {
        String entityId = "entityId";
        when(configServiceKeyStore.getEncryptionKeyForEntity(entityId)).thenReturn(publicKey);

        final PublicKey key = keyStore.getEncryptionKeyForEntity(entityId);
        assertThat(key).isEqualTo(publicKey);
    }

    @Test
    public void shouldThrowExceptionIfNoPublicKeyForEntityId() {
        Assertions.assertThrows(NoKeyConfiguredForEntityException.class, () -> {
            String entityId = "non-existent-entity";
            when(configServiceKeyStore.getEncryptionKeyForEntity(entityId)).thenThrow(ApplicationException.createUnauditedException(ExceptionType.CLIENT_ERROR, UUID.randomUUID()));

            keyStore.getEncryptionKeyForEntity(entityId);
        });
    }
}
