package uk.gov.ida.hub.samlsoapproxy.domain;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.hub.samlsoapproxy.exceptions.AttributeQueryTimeoutException;
import uk.gov.ida.saml.core.IdaSamlBootstrap;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import static uk.gov.ida.hub.samlsoapproxy.builders.AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto;
import static uk.gov.ida.saml.core.test.builders.AttributeQueryBuilder.anAttributeQuery;

@ExtendWith(OpenSAMLExtension.class)
@ExtendWith(MockitoExtension.class)
public class TimeoutEvaluatorTest {
    @Test
    public void hasAttributeQueryTimedOut_shouldThrowExceptionIfRequestIsTimedOut() {
        Assertions.assertThrows(AttributeQueryTimeoutException.class, () -> {
            DateTimeFreezer.freezeTime();
            AttributeQueryContainerDto queryWithTimeoutInPast =
                    anAttributeQueryContainerDto(anAttributeQuery().build())
                            .withAttributeQueryClientTimeout(DateTime.now().minusSeconds(20))
                            .build();

            TimeoutEvaluator timeoutEvaluator = new TimeoutEvaluator();

            timeoutEvaluator.hasAttributeQueryTimedOut(queryWithTimeoutInPast);
        });
    }

    @Test
    public void hasAttributeQueryTimedOut_shouldDoNothingIfRequestIsNotTimedOut() {

        DateTimeFreezer.freezeTime();
        AttributeQueryContainerDto queryWithTimeoutInFuture =
                anAttributeQueryContainerDto(anAttributeQuery().build())
                        .withAttributeQueryClientTimeout(DateTime.now().plusSeconds(20))
                        .build();

        TimeoutEvaluator timeoutEvaluator = new TimeoutEvaluator();

        timeoutEvaluator.hasAttributeQueryTimedOut(queryWithTimeoutInFuture);
    }
}
