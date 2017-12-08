package uk.gov.ida.integrationtest.hub.samlengine.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import httpstub.HttpStubRule;

import static com.google.common.base.Throwables.propagate;

public class MetadataRule extends HttpStubRule {

    public static final String SAML2_METADATA_FEDERATION = "/SAML2/metadata/federation";

    public MetadataRule withMetadata(String content) {
        try {
            this.register(SAML2_METADATA_FEDERATION, 200, "application/samlmetadata+xml", content);
        } catch (JsonProcessingException e) {
            propagate(e);
        }
        return this;
    }

    public String getMetadataUri() {
        return baseUri().path(SAML2_METADATA_FEDERATION).build().toString();
    }
}
