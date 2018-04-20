package uk.gov.ida.saml.msa.test.outbound;

import com.google.common.collect.ImmutableMap;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.core.transformers.outbound.IdaStatusMarshaller;
import uk.gov.ida.saml.msa.test.domain.UnknownUserCreationIdaStatus;

public class UnknownUserCreationIdaStatusMarshaller extends IdaStatusMarshaller<UnknownUserCreationIdaStatus> {

    private static final ImmutableMap<UnknownUserCreationIdaStatus, DetailedStatusCode> REST_TO_SAML_CODES =
            ImmutableMap.<UnknownUserCreationIdaStatus, DetailedStatusCode>builder()
                    .put(UnknownUserCreationIdaStatus.CreateFailure, DetailedStatusCode.UnknownUserCreateFailure)
                    .put(UnknownUserCreationIdaStatus.Success, DetailedStatusCode.UnknownUserCreateSuccess)
                    .put(UnknownUserCreationIdaStatus.NoAttributeFailure, DetailedStatusCode.UnknownUserNoAttributeFailure)
                    .build();

    public UnknownUserCreationIdaStatusMarshaller(OpenSamlXmlObjectFactory samlObjectFactory) {
        super(samlObjectFactory);
    }

    @Override
    protected DetailedStatusCode getDetailedStatusCode(UnknownUserCreationIdaStatus originalStatus) {
        return REST_TO_SAML_CODES.get(originalStatus);
    }
}
