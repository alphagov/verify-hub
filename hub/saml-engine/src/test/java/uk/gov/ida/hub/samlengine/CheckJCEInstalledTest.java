package uk.gov.ida.hub.samlengine;

import org.assertj.core.description.TextDescription;
import org.junit.Test;

import javax.crypto.Cipher;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckJCEInstalledTest {

    @Test
    public void testJCEInstalled() throws NoSuchAlgorithmException {
        assertThat(Cipher.getMaxAllowedKeyLength("AES"))
                .describedAs(new TextDescription("You need to have the unlimited JCE installed"))
                .isGreaterThan(128);
    }
}
