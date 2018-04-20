package uk.gov.ida.saml.hub.transformers.inbound;

import uk.gov.ida.saml.core.domain.IdaStatus;

public enum MatchingServiceIdaStatus implements IdaStatus {
    NoMatchingServiceMatchFromMatchingService,
    RequesterError,
    MatchingServiceMatch,
    UserAccountCreated,
    UserAccountCreationFailed,
    Healthy
    }
