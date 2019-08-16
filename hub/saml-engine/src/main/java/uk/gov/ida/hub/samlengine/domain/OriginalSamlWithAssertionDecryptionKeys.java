package uk.gov.ida.hub.samlengine.domain;

import org.opensaml.saml.saml2.core.Assertion;

import java.security.Key;
import java.util.List;
import java.util.Map;

public class OriginalSamlWithAssertionDecryptionKeys {
    private String originalSaml;
    private Map<Assertion, List<Key>> assertionDecryptionKeysMap;

    public OriginalSamlWithAssertionDecryptionKeys(String originalSaml, Map<Assertion, List<Key>> assertionDecryptionKeysMap) {
        this.originalSaml = originalSaml;
        this.assertionDecryptionKeysMap = assertionDecryptionKeysMap;
    }

    public String getOriginalSaml() {
        return this.originalSaml;
    }

    public Map<Assertion, List<Key>> getAssertionDecryptionKeysMap() {
        return assertionDecryptionKeysMap;
    }
}
