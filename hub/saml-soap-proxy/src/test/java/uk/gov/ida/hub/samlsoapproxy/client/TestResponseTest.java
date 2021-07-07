package uk.gov.ida.hub.samlsoapproxy.client;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestResponseTest {

    private TestResponse response;
    private String errorMessage = "something bad happened";

    @BeforeEach
    public void setup() {
        response = new TestResponse(500, errorMessage);
    }

    @Test
    public void assertStatus() {
        assertThat(response.getStatus()).isEqualTo(500);
    }

    @Test
    public void assertHasEntityThrowsExceptionAfterStreamHasBeenClosed() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            response.close();
            response.hasEntity();
        });
    }

    @Test
    public void assertCanReadEntity() {
        assertThat(response.hasEntity()).isTrue();
        assertThat(response.readEntity(String.class)).isEqualTo(errorMessage);
    }
}