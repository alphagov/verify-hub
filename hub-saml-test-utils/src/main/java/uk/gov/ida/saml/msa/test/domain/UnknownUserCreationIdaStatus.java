package uk.gov.ida.saml.msa.test.domain;

import uk.gov.ida.saml.core.domain.IdaStatus;

public enum UnknownUserCreationIdaStatus implements IdaStatus {
    Success,
    CreateFailure,
    NoAttributeFailure,
}
