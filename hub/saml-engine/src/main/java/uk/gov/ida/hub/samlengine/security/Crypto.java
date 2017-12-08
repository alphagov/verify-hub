package uk.gov.ida.hub.samlengine.security;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;

public class Crypto {
    public static KeyPair keyPairFromPrivateKey(PrivateKey privateKey) {
        try {
            RSAPrivateCrtKey rsaPrivateKey = (RSAPrivateCrtKey) privateKey;
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent());
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);
            return new KeyPair(publicKey, privateKey);
        } catch (ClassCastException ex) {
            throw new RuntimeException("Private key must be RSA format");
        } catch (Exception ex) {
            throw new RuntimeException("Could not get public key from private key");
        }
    }
}