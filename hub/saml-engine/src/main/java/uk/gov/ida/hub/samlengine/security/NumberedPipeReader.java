package uk.gov.ida.hub.samlengine.security;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import uk.gov.ida.common.shared.security.PrivateKeyFactory;
import uk.gov.ida.common.shared.security.exceptions.KeyLoadingException;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;

public class NumberedPipeReader {

    private final PrivateKeyFactory privateKeyFactory;

    public NumberedPipeReader(PrivateKeyFactory privateKeyFactory) {
        this.privateKeyFactory = privateKeyFactory;
    }

    //CLOSING THE FILE_STREAM AFTER READING FROM THE FILE DESCRIPTORS IS VERY IMPORTANT.
    //DO NOT REMOVE THE FINALLY BLOCK SINCE THIS MAKES SURE THE DESCRIPTORS GET CLOSED.

    public PrivateKey readKey(int fileDescriptorNumber) {
        FileDescriptor fileDescriptor = new FileDescriptor();
        SharedSecretsWrapper.setJavaIOFileDescriptorAccess(fileDescriptor, fileDescriptorNumber);
        InputStream fileInputStream = new FileInputStream(fileDescriptor);
        try {
            if (fileInputStream.available() == 0) {
                throw new KeyLoadingException("Key not loaded: No data found at file descriptor");
            }

            byte[] cert = ByteStreams.toByteArray(fileInputStream);

            return privateKeyFactory.createPrivateKey(cert);
        } catch (IOException e) {
            throw new KeyLoadingException(fileDescriptorNumber, e);
        } finally {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        }
    }
}
