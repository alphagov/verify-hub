package uk.gov.ida.hub.samlengine.logging;

import java.util.Objects;

/**
 * MethodAlgorithm class holds a pair of signature method algorithm and
 * digest method algorithm for signed SAML message.
 */
public final class MethodAlgorithm {

    private final String signatureMethodAlgorithm;
    private final String digestMethodAlgorithm;

    public MethodAlgorithm(final String signatureMethodAlgorithm, final String digestMethodAlgorithm) {
        this.signatureMethodAlgorithm = signatureMethodAlgorithm;
        this.digestMethodAlgorithm = digestMethodAlgorithm;
    }

    public String getSignatureMethodAlgorithm() {
        return this.signatureMethodAlgorithm;
    }

    public String getDigestMethodAlgorithm() {
        return this.digestMethodAlgorithm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MethodAlgorithm methodAlgorithm = (MethodAlgorithm) o;

        if (!Objects.equals(signatureMethodAlgorithm, methodAlgorithm.signatureMethodAlgorithm)) {
            return false;
        }
        else if (!Objects.equals(digestMethodAlgorithm, methodAlgorithm.digestMethodAlgorithm))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(signatureMethodAlgorithm, digestMethodAlgorithm);
    }

    @Override
    public String toString() {
        return "MethodAlgorithm{" +
                "signatureMethodAlgorithm=" + signatureMethodAlgorithm +
                ", digestMethodAlgorithm=" + digestMethodAlgorithm +
                '}';
    }
}
