package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import org.junit.Test;

import static org.assertj.guava.api.Assertions.assertThat;

public class HealthCheckDataTest {

    @Test
    public void shouldReturnEmptyHealthCheckDataWhenExtractedFromNullString() {
        HealthCheckData testData = HealthCheckData.extractFrom("test-id");
        assertThat(testData.getVersion()).isAbsent();
        assertThat(testData.getEidasEnabled()).isAbsent();
        assertThat(testData.getShouldSignWithSha1()).isAbsent();
    }

    @Test
    public void shouldReturnEmptyHealthCheckDataWhenExtractedFromEmptyString() {
        HealthCheckData testData = HealthCheckData.extractFrom("");
        assertThat(testData.getVersion()).isAbsent();
        assertThat(testData.getEidasEnabled()).isAbsent();
        assertThat(testData.getShouldSignWithSha1()).isAbsent();
    }

    @Test
    public void shouldReturnEmptyHealthCheckDataWhenExtractedFromSimpleIdString() {
        HealthCheckData testData = HealthCheckData.extractFrom("test-id");
        assertThat(testData.getVersion()).isAbsent();
        assertThat(testData.getEidasEnabled()).isAbsent();
        assertThat(testData.getShouldSignWithSha1()).isAbsent();
    }

    @Test
    public void shouldReturnHealthCheckDataWithVersionWhenExtractedFromStringThatContainsVersionData() {
        String expectedVersion = "1234";
        HealthCheckData testData = HealthCheckData.extractFrom("test-id-version-" + expectedVersion);
        assertThat(testData.getVersion()).contains(expectedVersion);
        assertThat(testData.getEidasEnabled()).isAbsent();
        assertThat(testData.getShouldSignWithSha1()).isAbsent();
    }

    @Test
    public void shouldReturnHealthCheckDataWithVersionAndEidasWhenExtractedFromStringThatContainsVersionDataAndEidas() {
        String expectedVersion = "1234";
        String expectedEidasEnabled = "true";
        HealthCheckData testData = HealthCheckData.extractFrom("test-id-version-" + expectedVersion + "-eidasenabled-" + expectedEidasEnabled);
        assertThat(testData.getVersion()).contains(expectedVersion);
        assertThat(testData.getEidasEnabled()).contains(expectedEidasEnabled);
        assertThat(testData.getShouldSignWithSha1()).isAbsent();
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

        assertThat(testData.getVersion()).contains(expectedVersion);
        assertThat(testData.getEidasEnabled()).contains(expectedEidasEnabled);
        assertThat(testData.getShouldSignWithSha1()).contains(expectedShouldSignWithSha1);
    }

    @Test
    public void shouldReturnHealthCheckDataWithVersionWithShaNoiseWhenExtractedFromStringThatContainsVersionAndShouldSignWithSha1Data() {
        String expectedVersion = "1234";
        String expectedShouldSignWithSha1 = "true";

        HealthCheckData testData = HealthCheckData.extractFrom(
                "test-id-version-" + expectedVersion+
                "-shouldsignwithsha1-" + expectedShouldSignWithSha1);

        assertThat(testData.getVersion()).contains(expectedVersion+"-shouldsignwithsha1-" + expectedShouldSignWithSha1);
        assertThat(testData.getEidasEnabled()).isAbsent();
        assertThat(testData.getShouldSignWithSha1()).isAbsent();
    }
}
