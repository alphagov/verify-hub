package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HealthCheckDataTest {

    @Test
    public void shouldReturnEmptyHealthCheckDataWhenExtractedFromNullString() {
        HealthCheckData testData = HealthCheckData.extractFrom("test-id");
        assertFalse(testData.getVersion().isPresent());
        assertFalse(testData.getEidasEnabled().isPresent());
        assertFalse(testData.getShouldSignWithSha1().isPresent());
    }

    @Test
    public void shouldReturnEmptyHealthCheckDataWhenExtractedFromEmptyString() {
        HealthCheckData testData = HealthCheckData.extractFrom("");
        assertFalse(testData.getVersion().isPresent());
        assertFalse(testData.getEidasEnabled().isPresent());
        assertFalse(testData.getShouldSignWithSha1().isPresent());
    }

    @Test
    public void shouldReturnEmptyHealthCheckDataWhenExtractedFromSimpleIdString() {
        HealthCheckData testData = HealthCheckData.extractFrom("test-id");
        assertThat(testData.getVersion());
        assertFalse(testData.getEidasEnabled().isPresent());
        assertFalse(testData.getShouldSignWithSha1().isPresent());
    }

    @Test
    public void shouldReturnHealthCheckDataWithVersionWhenExtractedFromStringThatContainsVersionData() {
        String expectedVersion = "1234";
        HealthCheckData testData = HealthCheckData.extractFrom("test-id-version-" + expectedVersion);
        assertFalse(testData.getEidasEnabled().isPresent());
        assertFalse(testData.getShouldSignWithSha1().isPresent());
        assertTrue(testData.getVersion().get().contains(expectedVersion));
    }

    @Test
    public void shouldReturnHealthCheckDataWithVersionAndEidasWhenExtractedFromStringThatContainsVersionDataAndEidas() {
        String expectedVersion = "1234";
        String expectedEidasEnabled = "true";
        HealthCheckData testData = HealthCheckData.extractFrom("test-id-version-" + expectedVersion + "-eidasenabled-" + expectedEidasEnabled);
        assertTrue(testData.getVersion().get().contains(expectedVersion));
        assertTrue(testData.getEidasEnabled().get().contains(expectedEidasEnabled));
        assertFalse(testData.getShouldSignWithSha1().isPresent());
    }

    @Test
    public void shouldReturnFullHealthCheckDataWhenExtractedFromStringContainingAllData() {
        String expectedVersion = "1234";
        String expectedEidasEnabled = "true";
        String expectedShouldSignWithSha1 = "true";

        HealthCheckData testData = HealthCheckData.extractFrom(
                "test-id-version-" + expectedVersion +
                "-eidasenabled-" + expectedEidasEnabled +
                "-shouldsignwithsha1-" + expectedShouldSignWithSha1
        );
        assertTrue(testData.getVersion().get().contains(expectedVersion));
        assertTrue(testData.getEidasEnabled().get().contains(expectedEidasEnabled));
        assertTrue(testData.getShouldSignWithSha1().get().contains(expectedShouldSignWithSha1));
    }

    @Test
    public void shouldReturnHealthCheckDataWithVersionWithShaNoiseWhenExtractedFromStringThatContainsVersionAndShouldSignWithSha1Data() {
        String expectedVersion = "1234";
        String expectedShouldSignWithSha1 = "true";

        HealthCheckData testData = HealthCheckData.extractFrom(
                "test-id-version-" + expectedVersion+
                "-shouldsignwithsha1-" + expectedShouldSignWithSha1);

        assertTrue(testData.getVersion().get().contains(expectedVersion+"-shouldsignwithsha1-" + expectedShouldSignWithSha1));
        assertFalse(testData.getEidasEnabled().isPresent());
        assertFalse(testData.getShouldSignWithSha1().isPresent());
    }
}
