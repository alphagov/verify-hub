package uk.gov.ida.hub.policy.domain;

public enum TransactionIdaStatus {
    Success,
    RequesterError,
    NoAuthenticationContext,
    NoMatchingServiceMatchFromHub,
    AuthenticationFailed
}