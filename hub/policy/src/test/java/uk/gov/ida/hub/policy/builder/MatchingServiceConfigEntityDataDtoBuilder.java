package uk.gov.ida.hub.policy.builder;

import uk.gov.ida.hub.policy.contracts.MatchingServiceConfigEntityDataDto;

import java.net.URI;

public class MatchingServiceConfigEntityDataDtoBuilder {
    public static final URI DEFAULT_MATCHING_SERVICE_URI = URI.create("/a-matching-service-uri");
    public static final URI DEFAULT_MATCHING_SERVICE_USER_ACCOUNT_CREATION_URI = URI.create("/a-matching-service-uri/user-account-creation");

    private String entityId = "some-entity-id";
    private URI uri = DEFAULT_MATCHING_SERVICE_URI;
    private String transactionEntityId = "transaction-entity-id";
    private boolean healthCheckEnabled = true;
    private boolean onboarding;
    private URI userAccountCreationUri = DEFAULT_MATCHING_SERVICE_USER_ACCOUNT_CREATION_URI;

    public static MatchingServiceConfigEntityDataDtoBuilder aMatchingServiceConfigEntityDataDto() {
        return new MatchingServiceConfigEntityDataDtoBuilder();
    }

    public MatchingServiceConfigEntityDataDto build() {
        return new MatchingServiceConfigEntityDataDto(this.entityId, uri, transactionEntityId, healthCheckEnabled, onboarding, false, userAccountCreationUri);
    }

    public MatchingServiceConfigEntityDataDtoBuilder withEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public MatchingServiceConfigEntityDataDtoBuilder withHealthCheckDisabled() {
        healthCheckEnabled = false;
        return this;
    }

    public MatchingServiceConfigEntityDataDtoBuilder withHealthCheckEnabled() {
        healthCheckEnabled = true;
        return this;
    }

    public MatchingServiceConfigEntityDataDtoBuilder withOnboarding(boolean onboarding) {
        this.onboarding = onboarding;
        return this;
    }

    public MatchingServiceConfigEntityDataDtoBuilder withUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public MatchingServiceConfigEntityDataDtoBuilder withTransactionEntityId(String transactionEntityId) {
        this.transactionEntityId = transactionEntityId;
        return this;
    }

    public MatchingServiceConfigEntityDataDtoBuilder withUri(String uri) {
        return withUri(URI.create(uri));
    }

    public MatchingServiceConfigEntityDataDtoBuilder withUserAccountCreationUri(URI uri){
        this.userAccountCreationUri = uri;
        return this;
    }
}
