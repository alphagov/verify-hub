package uk.gov.ida.saml.hub.transformers.inbound;

import com.google.common.collect.ImmutableMap;

public class MatchingServiceIdaStatusUnmarshaller extends IdaStatusUnmarshaller<MatchingServiceIdaStatus> {

    private static final ImmutableMap<IdaStatusMapperStatus, MatchingServiceIdaStatus> SAML_TO_REST_CODES =
            ImmutableMap.<IdaStatusMapperStatus, MatchingServiceIdaStatus>builder()
                    .put(IdaStatusMapperStatus.RequesterError, MatchingServiceIdaStatus.RequesterError)
                    .put(IdaStatusMapperStatus.NoMatchingServiceMatchFromMatchingService, MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService)
                    .put(IdaStatusMapperStatus.MatchingServiceMatch, MatchingServiceIdaStatus.MatchingServiceMatch)
                    .put(IdaStatusMapperStatus.Healthy, MatchingServiceIdaStatus.Healthy)
                    .put(IdaStatusMapperStatus.Created, MatchingServiceIdaStatus.UserAccountCreated)
                    .put(IdaStatusMapperStatus.CreateFailed, MatchingServiceIdaStatus.UserAccountCreationFailed)
                    .build();

    public MatchingServiceIdaStatusUnmarshaller() {
        super(SAML_TO_REST_CODES);
    }
}
