package uk.gov.ida.hub.policy.proxy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.jerseyclient.JsonClient;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionsConfigProxyTest {

    private final String ENTITY_ID = "test-entity-id";
    private final URI CONFIG_BASE_URI = URI.create("http://not-a-real-server");
    private TransactionsConfigProxy configProxy;
    @Mock
    private JsonClient client;

    @Test
    public void whenUrlForIsUsingMatchingIsValidReturnTrue() {

        String entityId = ENTITY_ID;

        configProxy = new TransactionsConfigProxy(
                client,
                CONFIG_BASE_URI
        );

        URI isUsingMatchingUri = UriBuilder
                .fromUri(CONFIG_BASE_URI)
                .path(Urls.ConfigUrls.MATCHING_ENABLED_FOR_TRANSACTION_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(entityId)
                        .replace("+", "%20"));

        when(client.get(eq(isUsingMatchingUri), eq(boolean.class))).thenReturn(true);

        assertThat(configProxy.isUsingMatching(entityId)).isTrue();
    }

    @Test
    public void whenUrlForIsUsingMatchingIsValidReturnFalse() {

        String entityId = ENTITY_ID;

        configProxy = new TransactionsConfigProxy(
                client,
                CONFIG_BASE_URI
        );

        URI isUsingMatchingUri = UriBuilder
                .fromUri(CONFIG_BASE_URI)
                .path(Urls.ConfigUrls.MATCHING_ENABLED_FOR_TRANSACTION_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(entityId)
                        .replace("+", "%20"));

        when(client.get(eq(isUsingMatchingUri), eq(boolean.class))).thenReturn(false);

        assertThat(configProxy.isUsingMatching(entityId)).isFalse();
    }

    @Test
    public void shouldPropagateExceptionWhenIsUsingMatchingResultsInARuntimeException() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            configProxy = new TransactionsConfigProxy(
                    client,
                    CONFIG_BASE_URI
            );

            when(client.get(any(), eq(boolean.class))).thenThrow(new RuntimeException());

            configProxy.isUsingMatching(ENTITY_ID);
        });
    }
}
