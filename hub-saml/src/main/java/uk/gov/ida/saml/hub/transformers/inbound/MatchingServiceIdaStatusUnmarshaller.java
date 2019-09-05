package uk.gov.ida.saml.hub.transformers.inbound;

import java.util.Map;

public class MatchingServiceIdaStatusUnmarshaller extends IdaStatusUnmarshaller<MatchingServiceIdaStatus> {

    private static final Map<IdaStatusMapperStatus, MatchingServiceIdaStatus> SAML_TO_REST_CODES = Map.of(
                    IdaStatusMapperStatus.RequesterError, MatchingServiceIdaStatus.RequesterError,
                    IdaStatusMapperStatus.NoMatchingServiceMatchFromMatchingService, MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService,
                    IdaStatusMapperStatus.MatchingServiceMatch, MatchingServiceIdaStatus.MatchingServiceMatch,
                    IdaStatusMapperStatus.Healthy, MatchingServiceIdaStatus.Healthy,
                    IdaStatusMapperStatus.Created, MatchingServiceIdaStatus.UserAccountCreated,
                    IdaStatusMapperStatus.CreateFailed, MatchingServiceIdaStatus.UserAccountCreationFailed);

    public MatchingServiceIdaStatusUnmarshaller() {
        super(SAML_TO_REST_CODES);
    }
}
