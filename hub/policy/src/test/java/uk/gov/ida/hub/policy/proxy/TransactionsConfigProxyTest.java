package uk.gov.ida.hub.policy.proxy;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.jerseyclient.JsonClient;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class TransactionsConfigProxyTest {

    private final String ENTITY_ID = "test-entity-id";
    private final URI CONFIG_BASE_URI = URI.create("http://not-a-real-server");
    private TransactionsConfigProxy configProxy;
    @Mock
    private JsonClient client;

    @BeforeClass
    public static void setUp() {
        initMocks(TransactionsConfigProxyTest.class);
    }

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

        assertTrue(configProxy.isUsingMatching(entityId));
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

        assertFalse(configProxy.isUsingMatching(entityId));
    }

    @Test(expected = RuntimeException.class)
    public void shouldPropagateExceptionWhenIsUsingMatchingResultsInARuntimeException() {

        configProxy = new TransactionsConfigProxy(
                client,
                CONFIG_BASE_URI
        );

        when(client.get(any(), eq(boolean.class))).thenThrow(new RuntimeException());

        configProxy.isUsingMatching(ENTITY_ID);
    }
}
