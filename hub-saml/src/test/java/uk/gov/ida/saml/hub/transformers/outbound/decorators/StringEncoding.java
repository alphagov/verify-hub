package uk.gov.ida.saml.hub.transformers.outbound.decorators;

import org.apache.commons.codec.binary.Base64;

import static org.apache.commons.codec.binary.StringUtils.newStringUtf8;

public abstract class StringEncoding {

    public static String toBase64Encoded(byte[] bytes) {
        return newStringUtf8(Base64.encodeBase64(bytes));
    }
}
