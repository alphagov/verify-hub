package uk.gov.ida.integrationtest.hub.samlproxy.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Throwables;
import httpstub.HttpStubRule;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;

import static com.google.common.base.Throwables.propagate;

public class CountryMetadataRule extends HttpStubRule {

    private String countryMetadataUri;

    public CountryMetadataRule(String entityId) {
        try {
            InitializationService.initialize();
        } catch (InitializationException e) {
            throw Throwables.propagate(e);
        }

        countryMetadataUri = baseUri().path(entityId).build().toString();
        String countryMetadata = NodeMetadataFactory.createCountryMetadata(countryMetadataUri);

        try {
            this.register(entityId, 200, "application/samlmetadata+xml", countryMetadata);
        } catch (JsonProcessingException e) {
            propagate(e);
        }
    }

    public String getCountryMetadataUri() {
        return countryMetadataUri;
    }
}
