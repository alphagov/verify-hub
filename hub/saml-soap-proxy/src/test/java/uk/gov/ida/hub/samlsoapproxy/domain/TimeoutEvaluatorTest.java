package uk.gov.ida.hub.samlsoapproxy.domain;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.ida.hub.samlsoapproxy.exceptions.AttributeQueryTimeoutException;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import static uk.gov.ida.hub.samlsoapproxy.builders.AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto;
import static uk.gov.ida.saml.core.test.builders.AttributeQueryBuilder.anAttributeQuery;

@RunWith(OpenSAMLMockitoRunner.class)
public class TimeoutEvaluatorTest {

    @Test(expected = AttributeQueryTimeoutException.class)
    public void hasAttributeQueryTimedOut_shouldThrowExceptionIfRequestIsTimedOut() {

        DateTimeFreezer.freezeTime();
        AttributeQueryContainerDto queryWithTimeoutInPast =
                anAttributeQueryContainerDto(anAttributeQuery().build())
                    .withAttributeQueryClientTimeout(DateTime.now().minusSeconds(20))
                    .build();

        TimeoutEvaluator timeoutEvaluator = new TimeoutEvaluator();

        timeoutEvaluator.hasAttributeQueryTimedOut(queryWithTimeoutInPast);
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
