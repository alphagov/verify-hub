package uk.gov.ida.integrationtest.hub.samlengine.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Throwables;
import httpstub.HttpStub;
import httpstub.HttpStubRule;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;

import static com.google.common.base.Throwables.propagate;

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
            throw Throwables.propagate(e);
        }

        countryMetadataUri = entityId;
        String countryMetadata = NodeMetadataFactory.createCountryMetadata(entityId);

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
