package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HealthCheckDataTest {

    @Test
    public void shouldReturnEmptyHealthCheckDataWhenExtractedFromNullString() {
        HealthCheckData testData = HealthCheckData.extractFrom("test-id");
        assertThat(testData.getVersion()).isNotPresent();
        assertThat(testData.getEidasEnabled()).isNotPresent();
        assertThat(testData.getShouldSignWithSha1()).isNotPresent();
    }

    @Test
    public void shouldReturnEmptyHealthCheckDataWhenExtractedFromEmptyString() {
        HealthCheckData testData = HealthCheckData.extractFrom("");
        assertThat(testData.getVersion()).isNotPresent();
        assertThat(testData.getEidasEnabled()).isNotPresent();
        assertThat(testData.getShouldSignWithSha1()).isNotPresent();
    }

    @Test
    public void shouldReturnEmptyHealthCheckDataWhenExtractedFromSimpleIdString() {
        HealthCheckData testData = HealthCheckData.extractFrom("test-id");
        assertThat(testData.getVersion());
        assertThat(testData.getEidasEnabled()).isNotPresent();
        assertThat(testData.getShouldSignWithSha1()).isNotPresent();
    }

    @Test
    public void shouldReturnHealthCheckDataWithVersionWhenExtractedFromStringThatContainsVersionData() {
        String expectedVersion = "1234";
        HealthCheckData testData = HealthCheckData.extractFrom("test-id-version-" + expectedVersion);
        assertThat(testData.getEidasEnabled()).isNotPresent();
        assertThat(testData.getShouldSignWithSha1()).isNotPresent();
        assertThat(testData.getVersion().get()).contains(expectedVersion);
    }

    @Test
    public void shouldReturnHealthCheckDataWithVersionAndEidasWhenExtractedFromStringThatContainsVersionDataAndEidas() {
        String expectedVersion = "1234";
        String expectedEidasEnabled = "true";
        HealthCheckData testData = HealthCheckData.extractFrom("test-id-version-" + expectedVersion + "-eidasenabled-" + expectedEidasEnabled);
        assertThat(testData.getVersion().get()).contains(expectedVersion);
        assertThat(testData.getEidasEnabled().get()).contains(expectedEidasEnabled);
        assertThat(testData.getShouldSignWithSha1()).isNotPresent();
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
        assertThat(testData.getVersion().get()).contains(expectedVersion);
        assertThat(testData.getEidasEnabled().get()).contains(expectedEidasEnabled);
        assertThat(testData.getShouldSignWithSha1().get()).contains(expectedShouldSignWithSha1);
    }

    @Test
    public void shouldReturnHealthCheckDataWithVersionWithShaNoiseWhenExtractedFromStringThatContainsVersionAndShouldSignWithSha1Data() {
        String expectedVersion = "1234";
        String expectedShouldSignWithSha1 = "true";

        HealthCheckData testData = HealthCheckData.extractFrom(
                "test-id-version-" + expectedVersion +
                        "-shouldsignwithsha1-" + expectedShouldSignWithSha1);

        assertThat(testData.getVersion().get()).contains(expectedVersion + "-shouldsignwithsha1-" + expectedShouldSignWithSha1);
        assertThat(testData.getEidasEnabled()).isNotPresent();
        assertThat(testData.getShouldSignWithSha1()).isNotPresent();
    }
}
