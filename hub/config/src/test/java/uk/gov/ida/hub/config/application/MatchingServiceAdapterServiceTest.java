package uk.gov.ida.hub.config.application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.data.ConfigRepository;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;

@RunWith(MockitoJUnitRunner.class)
public class MatchingServiceAdapterServiceTest {
    public static final String MATCHING_SERVICE_CONFIG_ENTITY_ID = "http://www.some-rp-ms.gov.uk";
    public static final String ANOTHER_MATCHING_SERVICE_CONFIG_ENTITY_ID = "http://www.some-rp-ms2.gov.uk";
    private static final String TRANSACTION_ENTITY_ID = "http://www.transaction.gov.uk/SAML2/MD";
    private static final String TRANSACTION_ENTITY_ID_2 = "http://www.transaction2.gov.uk/SAML2/MD";
    private static final String MATCHING_SERVICE_ENTITY_ID= "a-matching-service-entity-id";
    private static final String MATCHING_SERVICE_ENTITY_ID_2= "another-matching-service-entity-id";

    private MatchingServiceAdapterService matchingServiceAdapterService;

    @Mock
    private ConfigRepository<TransactionConfig> transactionConfigRepository;

    @Mock
    private ConfigRepository<MatchingServiceConfig> matchingServiceConfigRepository;

    @Before
    public void initialise() {
        matchingServiceAdapterService = new MatchingServiceAdapterService(transactionConfigRepository, matchingServiceConfigRepository);
    }

    @Test
    public void matchingServiceFoundWhenMatchingServiceExistsForTransactionEntityId() {
        MatchingServiceConfig matchingServiceConfigEntity = aMatchingServiceConfig()
                .withEntityId("http://www.some-rp-ms.gov.uk")
                .build();
        when(matchingServiceConfigRepository.getData(TRANSACTION_ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfigEntity));

        MatchingServiceAdapterService.MatchingServicePerTransaction expectedMatchingServicePerTransaction =
                matchingServiceAdapterService.new MatchingServicePerTransaction(TRANSACTION_ENTITY_ID, matchingServiceConfigEntity);
        assertThat(matchingServiceAdapterService.getMatchingService(TRANSACTION_ENTITY_ID)).isEqualTo(expectedMatchingServicePerTransaction);
    }

    @Test(expected = NoSuchElementException.class)
    public void exceptionThrownWhenMatchingServiceDoesNotExistsForTransactionEntityId() {
        when(matchingServiceConfigRepository.getData(TRANSACTION_ENTITY_ID)).thenReturn(Optional.empty());

        matchingServiceAdapterService.getMatchingService(TRANSACTION_ENTITY_ID);
    }

    @Test
    public void emptyListReturnedWhenNoTransactionConfigEntitiesExist() {
        when(transactionConfigRepository.getAllData()).thenReturn(new HashSet<>());

        Collection<MatchingServiceAdapterService.MatchingServicePerTransaction> matchingServices = matchingServiceAdapterService.getMatchingServices();
        assertThat(matchingServices.isEmpty()).isTrue();
    }

    @Test(expected = NoSuchElementException.class)
    public void exceptionThrownWhenNoMatchingServiceConfigEntitiesExist() {
        TransactionConfig transactionConfig = aTransactionConfigData().build();
        when(transactionConfigRepository.getAllData())
                .thenReturn(aTransactionConfigEntityDataSetWith(transactionConfig));

        when(matchingServiceConfigRepository.getData(transactionConfig.getMatchingServiceEntityId())).thenReturn(Optional.empty());

        matchingServiceAdapterService.getMatchingServices();
    }

    @Test
    public void singleMatchingServiceReturnedWhenOnlyOneTransactionExists() {
        TransactionConfig transactionConfig = aTransactionConfigData()
                .withEntityId(TRANSACTION_ENTITY_ID)
                .withMatchingServiceEntityId(MATCHING_SERVICE_ENTITY_ID)
                .build();
        when(transactionConfigRepository.getAllData())
                .thenReturn(aTransactionConfigEntityDataSetWith(transactionConfig));

        MatchingServiceConfig matchingServiceConfigEntity = aMatchingServiceConfig()
                .withEntityId("http://www.some-rp-ms.gov.uk")
                .build();
        when(matchingServiceConfigRepository.getData(MATCHING_SERVICE_ENTITY_ID))
                .thenReturn(Optional.of(matchingServiceConfigEntity));

        MatchingServiceAdapterService.MatchingServicePerTransaction expectedMatchingServicePerTransaction =
                matchingServiceAdapterService.new MatchingServicePerTransaction(TRANSACTION_ENTITY_ID, matchingServiceConfigEntity);
        assertThat(matchingServiceAdapterService.getMatchingServices())
                .hasSize(1)
                .contains(expectedMatchingServicePerTransaction, expectedMatchingServicePerTransaction);
    }

    @Test
    public void multipleMatchingServicesReturnedWhenMultipleTransactionsExists() {
        TransactionConfig transactionConfig = aTransactionConfigData()
                .withEntityId(TRANSACTION_ENTITY_ID)
                .withMatchingServiceEntityId(MATCHING_SERVICE_ENTITY_ID)
                .build();
        TransactionConfig transactionConfig2 = aTransactionConfigData()
                .withEntityId(TRANSACTION_ENTITY_ID_2)
                .withMatchingServiceEntityId(MATCHING_SERVICE_ENTITY_ID_2)
                .build();
        when(transactionConfigRepository.getAllData())
                .thenReturn(aTransactionConfigEntityDataSetWith(transactionConfig, transactionConfig2));

        MatchingServiceConfig matchingServiceConfigEntity = aMatchingServiceConfig()
                .withEntityId(MATCHING_SERVICE_CONFIG_ENTITY_ID)
                .build();
        MatchingServiceConfig matchingServiceConfigEntity2 = aMatchingServiceConfig()
                .withEntityId(ANOTHER_MATCHING_SERVICE_CONFIG_ENTITY_ID)
                .build();
        when(matchingServiceConfigRepository.getData(MATCHING_SERVICE_ENTITY_ID))
                .thenReturn(Optional.of(matchingServiceConfigEntity));
        when(matchingServiceConfigRepository.getData(MATCHING_SERVICE_ENTITY_ID_2))
                .thenReturn(Optional.of(matchingServiceConfigEntity2));

        MatchingServiceAdapterService.MatchingServicePerTransaction expectedMatchingServicePerTransaction =
                matchingServiceAdapterService.new MatchingServicePerTransaction(TRANSACTION_ENTITY_ID, matchingServiceConfigEntity);
        MatchingServiceAdapterService.MatchingServicePerTransaction otherExpectedMatchingServicePerTransaction =
                matchingServiceAdapterService.new MatchingServicePerTransaction(TRANSACTION_ENTITY_ID_2, matchingServiceConfigEntity2);
        assertThat(matchingServiceAdapterService.getMatchingServices())
                .hasSize(2)
                .contains(expectedMatchingServicePerTransaction, otherExpectedMatchingServicePerTransaction);
    }

    private HashSet<TransactionConfig> aTransactionConfigEntityDataSetWith(TransactionConfig... transactionConfigEntityData) {
        HashSet<TransactionConfig> transactionConfigSet = new HashSet<>();
        transactionConfigSet.addAll(Arrays.asList(transactionConfigEntityData));
        return transactionConfigSet;
    }
}
