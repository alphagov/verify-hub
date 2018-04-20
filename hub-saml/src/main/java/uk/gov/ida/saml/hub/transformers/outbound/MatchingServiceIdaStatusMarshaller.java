package uk.gov.ida.saml.hub.transformers.outbound;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.core.domain.MatchingServiceIdaStatus;
import uk.gov.ida.saml.core.transformers.outbound.IdaStatusMarshaller;

public class MatchingServiceIdaStatusMarshaller extends IdaStatusMarshaller<MatchingServiceIdaStatus> {

    private static final ImmutableMap<MatchingServiceIdaStatus, DetailedStatusCode> REST_TO_SAML_CODES =
            ImmutableMap.<MatchingServiceIdaStatus, DetailedStatusCode>builder()
                    .put(MatchingServiceIdaStatus.MatchingServiceMatch, DetailedStatusCode.MatchingServiceMatch)
                    .put(MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService, DetailedStatusCode.NoMatchingServiceMatchFromMatchingService)
                    .put(MatchingServiceIdaStatus.RequesterError, DetailedStatusCode.RequesterErrorFromIdp)
                    .put(MatchingServiceIdaStatus.Healthy, DetailedStatusCode.Healthy)
                    .build();

    @Inject
    public MatchingServiceIdaStatusMarshaller(OpenSamlXmlObjectFactory samlObjectFactory) {
        super(samlObjectFactory);
    }

    @Override
    protected DetailedStatusCode getDetailedStatusCode(MatchingServiceIdaStatus originalStatus) {
        return REST_TO_SAML_CODES.get(originalStatus);
    }
}
