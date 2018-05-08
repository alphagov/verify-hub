package uk.gov.ida.hub.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.util.Duration;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.configuration.ServiceNameConfiguration;
import uk.gov.ida.truststore.ClientTrustStoreConfiguration;
import uk.gov.ida.truststore.TrustStoreConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigConfiguration extends Configuration implements TrustStoreConfiguration, ServiceNameConfiguration {

    @Valid
    @NotNull
    @JsonProperty
    protected Boolean enableRetryTimeOutConnections = false;

    @JsonProperty
    @NotNull
    @Valid
    protected ServiceInfoConfiguration serviceInfo;

    @Valid
    @NotNull
    @JsonProperty
    protected ClientTrustStoreConfiguration clientTrustStoreConfiguration;

    @Valid
    @NotNull
    @JsonProperty
    protected ClientTrustStoreConfiguration rpTrustStoreConfiguration;

    @Valid
    @NotNull
    @JsonProperty
    protected String rootDataDirectory;

    @Valid
    @NotNull
    @JsonProperty
    protected Duration certificateWarningPeriod = Duration.days(30);

    protected ConfigConfiguration() {}

    public String getDataDirectory() {
        return rootDataDirectory;
    }

    public org.joda.time.Duration getCertificateWarningPeriod() {
        return new org.joda.time.Duration(certificateWarningPeriod.toMilliseconds());
    }

    public ServiceInfoConfiguration getServiceInfo() {
        return serviceInfo;
    }

    @Override
    public String getServiceName() {
        return serviceInfo.getName();
    }

    public ClientTrustStoreConfiguration getClientTrustStoreConfiguration() {
        return this.clientTrustStoreConfiguration;
    }

    @Override
    public ClientTrustStoreConfiguration getRpTrustStoreConfiguration() {
        return this.rpTrustStoreConfiguration;
    }

    public boolean getEnableRetryTimeOutConnections() { return enableRetryTimeOutConnections; }

}
