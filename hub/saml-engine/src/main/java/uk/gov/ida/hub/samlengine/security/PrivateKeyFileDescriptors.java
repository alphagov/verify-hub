package uk.gov.ida.hub.samlengine.security;

import uk.gov.ida.common.shared.security.PrivateKeyFactory;

import java.security.PrivateKey;

public enum PrivateKeyFileDescriptors {
    PRIMARY_ENCRYPTION_KEY(4),
    SECONDARY_ENCRYPTION_KEY(5),
    SIGNING_KEY(6);

    private int fileDescriptor;
    private NumberedPipeReader numberedPipeReader = new NumberedPipeReader(new PrivateKeyFactory());

    PrivateKeyFileDescriptors(int fileDescriptor) {
        this.fileDescriptor = fileDescriptor;
    }

    public PrivateKey loadKey() {
        return numberedPipeReader.readKey(fileDescriptor);
    }
}
