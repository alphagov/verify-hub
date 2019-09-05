package uk.gov.ida.saml.hub.transformers.outbound;

import com.google.inject.Inject;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.core.domain.MatchingServiceIdaStatus;
import uk.gov.ida.saml.core.transformers.outbound.IdaStatusMarshaller;

import java.util.Map;

public class MatchingServiceIdaStatusMarshaller extends IdaStatusMarshaller<MatchingServiceIdaStatus> {

    private static final Map<MatchingServiceIdaStatus, DetailedStatusCode> REST_TO_SAML_CODES = Map.of(
                    MatchingServiceIdaStatus.MatchingServiceMatch, DetailedStatusCode.MatchingServiceMatch,
                    MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService, DetailedStatusCode.NoMatchingServiceMatchFromMatchingService,
                    MatchingServiceIdaStatus.RequesterError, DetailedStatusCode.RequesterErrorFromIdp,
                    MatchingServiceIdaStatus.Healthy, DetailedStatusCode.Healthy);

    @Inject
    public MatchingServiceIdaStatusMarshaller(OpenSamlXmlObjectFactory samlObjectFactory) {
        super(samlObjectFactory);
    }

    @Override
    protected DetailedStatusCode getDetailedStatusCode(MatchingServiceIdaStatus originalStatus) {
        return REST_TO_SAML_CODES.get(originalStatus);
    }
}
