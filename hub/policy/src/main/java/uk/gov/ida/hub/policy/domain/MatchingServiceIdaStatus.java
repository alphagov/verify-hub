package uk.gov.ida.hub.policy.domain;

public enum MatchingServiceIdaStatus {
    NoMatchingServiceMatchFromMatchingService,
    RequesterError,
    MatchingServiceMatch,
    UserAccountCreated,
    UserAccountCreationFailed,
    Healthy
}