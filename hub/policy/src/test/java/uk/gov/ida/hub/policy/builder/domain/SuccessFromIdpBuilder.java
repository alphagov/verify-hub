package uk.gov.ida.hub.policy.builder.domain;

import com.google.common.base.Optional;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SuccessFromIdp;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

public class SuccessFromIdpBuilder {

    private String issuer = "issuer";
    private String authnStatementAssertion = "aPassthroughAssertion().buildAuthnStatementAssertion()";
    private String encryptedMatchingDatasetAssertion = "encrypted-mds-assertion";
    private PersistentId persistentIdDto = PersistentIdBuilder.aPersistentId().build();
    private LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_3;
    private String principalIpAsSeenByHub = "principal ip address as seen by hub";
    private Optional<String> principalIpAddressAsSeenByIdp = absent();

    public static SuccessFromIdpBuilder aSuccessFromIdp() {
        return new SuccessFromIdpBuilder();
    }

    public SuccessFromIdp build() {
        return new SuccessFromIdp(issuer, encryptedMatchingDatasetAssertion, authnStatementAssertion, persistentIdDto, levelOfAssurance, principalIpAsSeenByHub, principalIpAddressAsSeenByIdp);
    }

    public SuccessFromIdpBuilder withIssuerId(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public SuccessFromIdpBuilder withAuthnStatementAssertion(String authnStatementAssertion) {
        this.authnStatementAssertion = authnStatementAssertion;
        return this;
    }

    public SuccessFromIdpBuilder withPersistentId(PersistentId persistentIdDto) {
        this.persistentIdDto = persistentIdDto;
        return this;
    }

    public SuccessFromIdpBuilder withLevelOfAssurance(LevelOfAssurance levelOfAssurance) {
        this.levelOfAssurance = levelOfAssurance;
        return this;
    }

    public SuccessFromIdpBuilder withPrincipalIpAddressAsSeenByHub(String principalIpAddressAsSeenByHub) {
        this.principalIpAsSeenByHub = principalIpAddressAsSeenByHub;
        return this;
    }

    public SuccessFromIdpBuilder withPrincipalIpAddressSeenByIdp(String principalIpAddressAsSeenByIdp) {
        this.principalIpAddressAsSeenByIdp = fromNullable(principalIpAddressAsSeenByIdp);
        return this;
    }

    public SuccessFromIdpBuilder withEncryptedMatchingDatasetAssertion(String encryptedMatchingDatasetAssertion) {
        this.encryptedMatchingDatasetAssertion = encryptedMatchingDatasetAssertion;
        return this;
    }
}
