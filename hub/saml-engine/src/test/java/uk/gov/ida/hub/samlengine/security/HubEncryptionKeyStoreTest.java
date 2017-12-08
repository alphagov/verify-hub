package uk.gov.ida.hub.samlengine.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.samlengine.config.ConfigServiceKeyStore;
import uk.gov.ida.saml.metadata.exceptions.NoKeyConfiguredForEntityException;

import java.security.PublicKey;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HubEncryptionKeyStoreTest {

    HubEncryptionKeyStore keyStore;

    @Mock
    private ConfigServiceKeyStore configServiceKeyStore;
    @Mock
    private PublicKey publicKey;

    @Before
    public void setUp() throws Exception {
        keyStore = new HubEncryptionKeyStore(configServiceKeyStore);
    }

    @Test
    public void shouldGetPublicKeyForAnEntityThatExists() throws Exception {
        String entityId = "entityId";
        when(configServiceKeyStore.getEncryptionKeyForEntity(entityId)).thenReturn(publicKey);

        final PublicKey key = keyStore.getEncryptionKeyForEntity(entityId);
        assertThat(key).isEqualTo(publicKey);
    }

    @Test(expected = NoKeyConfiguredForEntityException.class)
    public void shouldThrowExceptionIfNoPublicKeyForEntityId() throws Exception {
        String entityId = "non-existent-entity";
        when(configServiceKeyStore.getEncryptionKeyForEntity(entityId)).thenThrow(ApplicationException.createUnauditedException(ExceptionType.CLIENT_ERROR, UUID.randomUUID()));

        keyStore.getEncryptionKeyForEntity(entityId);
    }
}