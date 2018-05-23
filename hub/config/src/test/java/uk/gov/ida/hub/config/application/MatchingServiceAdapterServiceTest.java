package uk.gov.ida.hub.config.application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.config.data.ConfigEntityDataRepository;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigEntityDataBuilder.aMatchingServiceConfigEntityData;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigEntityDataBuilder.aTransactionConfigData;

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
    private ConfigEntityDataRepository<TransactionConfigEntityData> transactionConfigEntityDataRepository;

    @Mock
    private ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceConfigEntityDataRepository;

    @Before
    public void initialise() {
        matchingServiceAdapterService = new MatchingServiceAdapterService(transactionConfigEntityDataRepository, matchingServiceConfigEntityDataRepository);
    }

    @Test
    public void matchingServiceFoundWhenMatchingServiceExistsForTransactionEntityId() {
        MatchingServiceConfigEntityData matchingServiceConfigEntity = aMatchingServiceConfigEntityData()
                .withEntityId("http://www.some-rp-ms.gov.uk")
                .build();
        when(matchingServiceConfigEntityDataRepository.getData(TRANSACTION_ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfigEntity));

        MatchingServiceAdapterService.MatchingServicePerTransaction expectedMatchingServicePerTransaction =
                matchingServiceAdapterService.new MatchingServicePerTransaction(TRANSACTION_ENTITY_ID, matchingServiceConfigEntity);
        assertThat(matchingServiceAdapterService.getMatchingService(TRANSACTION_ENTITY_ID)).isEqualTo(expectedMatchingServicePerTransaction);
    }

    @Test(expected = NoSuchElementException.class)
    public void exceptionThrownWhenMatchingServiceDoesNotExistsForTransactionEntityId() {
        when(matchingServiceConfigEntityDataRepository.getData(TRANSACTION_ENTITY_ID)).thenReturn(Optional.empty());

        matchingServiceAdapterService.getMatchingService(TRANSACTION_ENTITY_ID);
    }

    @Test
    public void emptyListReturnedWhenNoTransactionConfigEntitiesExist() {
        when(transactionConfigEntityDataRepository.getAllData()).thenReturn(new HashSet<>());

        Collection<MatchingServiceAdapterService.MatchingServicePerTransaction> matchingServices = matchingServiceAdapterService.getMatchingServices();
        assertThat(matchingServices.isEmpty()).isTrue();
    }

    @Test(expected = NoSuchElementException.class)
    public void exceptionThrownWhenNoMatchingServiceConfigEntitiesExist() {
        TransactionConfigEntityData transactionConfigEntityData = aTransactionConfigData().build();
        when(transactionConfigEntityDataRepository.getAllData())
                .thenReturn(aTransactionConfigEntityDataSetWith(transactionConfigEntityData));

        when(matchingServiceConfigEntityDataRepository.getData(transactionConfigEntityData.getMatchingServiceEntityId())).thenReturn(Optional.empty());

        matchingServiceAdapterService.getMatchingServices();
    }

    @Test
    public void singleMatchingServiceReturnedWhenOnlyOneTransactionExists() {
        TransactionConfigEntityData transactionConfigEntityData = aTransactionConfigData()
                .withEntityId(TRANSACTION_ENTITY_ID)
                .withMatchingServiceEntityId(MATCHING_SERVICE_ENTITY_ID)
                .build();
        when(transactionConfigEntityDataRepository.getAllData())
                .thenReturn(aTransactionConfigEntityDataSetWith(transactionConfigEntityData));

        MatchingServiceConfigEntityData matchingServiceConfigEntity = aMatchingServiceConfigEntityData()
                .withEntityId("http://www.some-rp-ms.gov.uk")
                .build();
        when(matchingServiceConfigEntityDataRepository.getData(MATCHING_SERVICE_ENTITY_ID))
                .thenReturn(Optional.of(matchingServiceConfigEntity));

        MatchingServiceAdapterService.MatchingServicePerTransaction expectedMatchingServicePerTransaction =
                matchingServiceAdapterService.new MatchingServicePerTransaction(TRANSACTION_ENTITY_ID, matchingServiceConfigEntity);
        assertThat(matchingServiceAdapterService.getMatchingServices())
                .hasSize(1)
                .contains(expectedMatchingServicePerTransaction, expectedMatchingServicePerTransaction);
    }

    @Test
    public void multipleMatchingServicesReturnedWhenMultipleTransactionsExists() {
        TransactionConfigEntityData transactionConfigEntityData = aTransactionConfigData()
                .withEntityId(TRANSACTION_ENTITY_ID)
                .withMatchingServiceEntityId(MATCHING_SERVICE_ENTITY_ID)
                .build();
        TransactionConfigEntityData transactionConfigEntityData2 = aTransactionConfigData()
                .withEntityId(TRANSACTION_ENTITY_ID_2)
                .withMatchingServiceEntityId(MATCHING_SERVICE_ENTITY_ID_2)
                .build();
        when(transactionConfigEntityDataRepository.getAllData())
                .thenReturn(aTransactionConfigEntityDataSetWith(transactionConfigEntityData, transactionConfigEntityData2));

        MatchingServiceConfigEntityData matchingServiceConfigEntity = aMatchingServiceConfigEntityData()
                .withEntityId(MATCHING_SERVICE_CONFIG_ENTITY_ID)
                .build();
        MatchingServiceConfigEntityData matchingServiceConfigEntity2 = aMatchingServiceConfigEntityData()
                .withEntityId(ANOTHER_MATCHING_SERVICE_CONFIG_ENTITY_ID)
                .build();
        when(matchingServiceConfigEntityDataRepository.getData(MATCHING_SERVICE_ENTITY_ID))
                .thenReturn(Optional.of(matchingServiceConfigEntity));
        when(matchingServiceConfigEntityDataRepository.getData(MATCHING_SERVICE_ENTITY_ID_2))
                .thenReturn(Optional.of(matchingServiceConfigEntity2));

        MatchingServiceAdapterService.MatchingServicePerTransaction expectedMatchingServicePerTransaction =
                matchingServiceAdapterService.new MatchingServicePerTransaction(TRANSACTION_ENTITY_ID, matchingServiceConfigEntity);
        MatchingServiceAdapterService.MatchingServicePerTransaction otherExpectedMatchingServicePerTransaction =
                matchingServiceAdapterService.new MatchingServicePerTransaction(TRANSACTION_ENTITY_ID_2, matchingServiceConfigEntity2);
        assertThat(matchingServiceAdapterService.getMatchingServices())
                .hasSize(2)
                .contains(expectedMatchingServicePerTransaction, otherExpectedMatchingServicePerTransaction);
    }

    private HashSet<TransactionConfigEntityData> aTransactionConfigEntityDataSetWith(TransactionConfigEntityData... transactionConfigEntityData) {
        HashSet<TransactionConfigEntityData> transactionConfigEntityDataSet = new HashSet<>();
        transactionConfigEntityDataSet.addAll(Arrays.asList(transactionConfigEntityData));
        return transactionConfigEntityDataSet;
    }
}
