package uk.gov.ida.hub.samlengine.security;

import uk.gov.ida.common.shared.security.exceptions.KeyLoadingException;

import java.io.FileDescriptor;

public class SharedSecretsWrapper {
    public static void setJavaIOFileDescriptorAccess(FileDescriptor fileDescriptor, int fileDescriptorNumber) {

        /**
         * Temporary code to dynamically load a moved class in a Java 10 JVM having been built with an 8 JDK
         * To be removed when built with a 10 JDK
         */
        try {
            String javaVersion = System.getProperty("java.specification.version");
            String sharedSecretsPackageName = javaVersion.startsWith("1.8") ? "sun.misc." : "jdk.internal.misc.";

            Object javaIOFileDescriptorAccess = Class.forName(sharedSecretsPackageName + "SharedSecrets").getMethod(
                    "getJavaIOFileDescriptorAccess").invoke(null);

            Class.forName(sharedSecretsPackageName + "JavaIOFileDescriptorAccess").getMethod(
                    "set", FileDescriptor.class, int.class).invoke(javaIOFileDescriptorAccess, fileDescriptor, fileDescriptorNumber);

        } catch (Exception e) {
            throw new KeyLoadingException(fileDescriptorNumber, e);
        }
    }
}