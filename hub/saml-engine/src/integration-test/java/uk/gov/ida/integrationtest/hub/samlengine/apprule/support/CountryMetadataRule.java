package uk.gov.ida.integrationtest.hub.samlengine.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import httpstub.HttpStub;
import httpstub.HttpStubRule;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;

public class CountryMetadataRule extends HttpStubRule {

    private String countryMetadataUri;

    public CountryMetadataRule(String entityId) {
        initialise(entityId);
    }

    public CountryMetadataRule(String entityId, int portNumber) {
        super(new HttpStub(portNumber));
        initialise(entityId);
    }

    public void initialise(String entityId) {
        try {
            InitializationService.initialize();
        } catch (InitializationException e) {
            throw new RuntimeException(e);
        }

        countryMetadataUri = entityId;
        String countryMetadata = NodeMetadataFactory.createCountryMetadata(entityId);

        try {
            this.register(entityId, 200, "application/samlmetadata+xml", countryMetadata);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getCountryMetadataUri() {
        return countryMetadataUri;
    }
}
