package uk.gov.ida.hub.config.data;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.domain.TransactionConfig;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;

@RunWith(MockitoJUnitRunner.class)
public class ManagedEntityConfigRepositoryTest {

    private static final String REMOTE_ENABLED_ENTITY_ID = "https://appleregistry.test.com";
    private static final String REMOTE_DISABLED_ENTITY_ID = "https://cherryregistry.test.com";
    private static final String LOCAL_ONLY_ENTITY_ID = "https://local.test.com";
    private static final String BAD_ENTITY_ID = "http://none.existent.test.com";

    @Mock
    private LocalConfigRepository<TransactionConfig> localConfigRepository;

    private TransactionConfig localOnlyTransaction = aTransactionConfigData()
            .withEntityId(LOCAL_ONLY_ENTITY_ID)
            .withSelfService(false)
            .build();

    private TransactionConfig remoteEnabledTransaction = aTransactionConfigData()
            .withEntityId(REMOTE_ENABLED_ENTITY_ID)
            .withSelfService(true)
            .build();

    private TransactionConfig remoteDisabledTransaction = aTransactionConfigData()
            .withEntityId(REMOTE_DISABLED_ENTITY_ID)
            .withSelfService(false)
            .build();

    @Before
    public void setUp(){
        when(localConfigRepository.getData(LOCAL_ONLY_ENTITY_ID)).thenReturn(Optional.of(localOnlyTransaction));
        when(localConfigRepository.getData(BAD_ENTITY_ID)).thenReturn(Optional.empty());

        Set<TransactionConfig> allLocalConfig = Set.of(localOnlyTransaction, remoteEnabledTransaction, remoteDisabledTransaction);
        when(localConfigRepository.getAllData()).thenReturn(allLocalConfig);
    }

    @Test
    public void getReturnsOptionalEmptyIfNoLocalConfigFound() {
        ManagedEntityConfigRepository<TransactionConfig> configRepo = new ManagedEntityConfigRepository<>(localConfigRepository);
        Optional<TransactionConfig> result = configRepo.get(BAD_ENTITY_ID);
        assertThat(result.isPresent()).isFalse();
    }


    @Test
    public void getReturnsLocalConfigWhenAvailable() {
        ManagedEntityConfigRepository<TransactionConfig> configRepo = new ManagedEntityConfigRepository<>(localConfigRepository);
        Optional<TransactionConfig> result = configRepo.get(LOCAL_ONLY_ENTITY_ID);
        assertThat(result.isPresent()).isTrue();
    }

    @Test
    public void getAllReturnsAvailableConfig(){
        ManagedEntityConfigRepository<TransactionConfig> configRepo = new ManagedEntityConfigRepository<>(localConfigRepository);
        Set<TransactionConfig> result = configRepo.getAll();
        assertThat(result.size()).isEqualTo(3);
    }

}