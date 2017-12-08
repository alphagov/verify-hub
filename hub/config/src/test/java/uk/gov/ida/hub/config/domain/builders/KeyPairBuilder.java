package uk.gov.ida.hub.config.domain.builders;

import com.google.common.base.Throwables;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class KeyPairBuilder {
    public KeyPair build() {
        KeyPair keyPair = null;

        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            keyPair = keyGen.generateKeyPair();
        } catch(NoSuchAlgorithmException e) {
            Throwables.propagate(e);
        }

        return keyPair;
    }
}
