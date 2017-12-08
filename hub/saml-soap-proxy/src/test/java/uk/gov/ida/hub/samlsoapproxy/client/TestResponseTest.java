package uk.gov.ida.hub.samlsoapproxy.client;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestResponseTest {

    private TestResponse response;
    private String errorMessage = "something bad happened";

    @Before
    public void setup() {
        response = new TestResponse(500, errorMessage);
    }

    @Test
    public void assertStatus() {
        assertThat(response.getStatus()).isEqualTo(500);
    }

    @Test(expected = IllegalStateException.class)
    public void assertHasEntityThrowsExceptionAfterStreamHasBeenClosed() {
        response.close();
        response.hasEntity();
    }

    @Test
    public void assertCanReadEntity() {
        assertThat(response.hasEntity()).isTrue();
        assertThat(response.readEntity(String.class)).isEqualTo(errorMessage);
    }
}