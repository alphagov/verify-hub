package uk.gov.ida.saml.hub.transformers.inbound;

import com.google.inject.Inject;
import uk.gov.ida.saml.core.domain.InboundResponseFromIdpData;
import uk.gov.ida.saml.core.domain.PassthroughAssertion;
import uk.gov.ida.saml.core.transformers.outbound.decorators.AssertionBlobEncrypter;
import uk.gov.ida.saml.hub.domain.InboundResponseFromIdp;

import java.util.Optional;

import static java.util.Optional.empty;

public class InboundResponseFromIdpDataGenerator {

    private AssertionBlobEncrypter assertionBlobEncrypter;

    @Inject
    public InboundResponseFromIdpDataGenerator(AssertionBlobEncrypter assertionBlobEncrypter) {
        this.assertionBlobEncrypter = assertionBlobEncrypter;
    }

    public InboundResponseFromIdpData generate(InboundResponseFromIdp idaResponseFromIdp, String matchingServiceEntityId) {
        Optional<String> principalIpAddressFromIdp = empty();
        Optional<String> persistentId = empty();
        Optional<String> idpFraudEventId = empty();
        Optional<String> fraudIndicator = empty();
        String levelOfAssurance = null;

        if (idaResponseFromIdp.getAuthnStatementAssertion().isPresent()) {
            final PassthroughAssertion authnStatementAssertion = idaResponseFromIdp.getAuthnStatementAssertion().get();
            principalIpAddressFromIdp = Optional.ofNullable(authnStatementAssertion.getPrincipalIpAddressAsSeenByIdp().orElse(null));
            persistentId = Optional.ofNullable(authnStatementAssertion.getPersistentId().getNameId());
            if (authnStatementAssertion.getAuthnContext().isPresent()) {
                levelOfAssurance = authnStatementAssertion.getAuthnContext().get().name();
            }
            if (authnStatementAssertion.getFraudDetectedDetails().isPresent()) {
                idpFraudEventId = Optional.of(authnStatementAssertion.getFraudDetectedDetails().get().getIdpFraudEventId());
                fraudIndicator = Optional.of(authnStatementAssertion.getFraudDetectedDetails().get().getFraudIndicator());
            }
        }

        Optional<String> encryptedMatchingDatasetAssertion = idaResponseFromIdp.getMatchingDatasetAssertion()
                .map(PassthroughAssertion::getUnderlyingAssertionBlob)
                .map(blob -> assertionBlobEncrypter.encryptAssertionBlob(matchingServiceEntityId, blob));

        Optional<String> encryptedAuthnAssertion = idaResponseFromIdp.getAuthnStatementAssertion()
                .map(PassthroughAssertion::getUnderlyingAssertionBlob)
                .map(blob -> assertionBlobEncrypter.encryptAssertionBlob(matchingServiceEntityId, blob));

        return new InboundResponseFromIdpData(
                idaResponseFromIdp.getStatus().getStatusCode(),
                idaResponseFromIdp.getStatus().getMessage(),
                idaResponseFromIdp.getIssuer(),
                encryptedAuthnAssertion,
                encryptedMatchingDatasetAssertion,
                persistentId,
                principalIpAddressFromIdp,
                levelOfAssurance,
                idpFraudEventId,
                fraudIndicator,
                idaResponseFromIdp.getNotOnOrAfter());
    }
}
