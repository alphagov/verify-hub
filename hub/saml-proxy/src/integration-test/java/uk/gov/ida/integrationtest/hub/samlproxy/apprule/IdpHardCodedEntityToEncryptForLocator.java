package uk.gov.ida.integrationtest.hub.samlproxy.apprule;

import uk.gov.ida.saml.security.EntityToEncryptForLocator;

import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;

public class IdpHardCodedEntityToEncryptForLocator implements EntityToEncryptForLocator {

    @Override
    public String fromRequestId(String requestId) {
        return HUB_ENTITY_ID;
    }
}
