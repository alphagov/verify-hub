package uk.gov.ida.hub.samlengine.security;

import org.junit.Test;
import java.io.FileDescriptor;

import static junit.framework.TestCase.fail;

public class EncryptionKeysFromFileDescriptorsTest {

    @Test
    public void sharedSecretsWrapperNoException() {
        try {
            SharedSecretsWrapper.setJavaIOFileDescriptorAccess(new FileDescriptor(), 4);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
