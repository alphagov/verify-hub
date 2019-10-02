package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.NonMatchingJourneySuccessState;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

public class NonMatchingJourneySuccessStateBuilder {
    private String requestId = UUID.randomUUID().toString();
    private String requestIssuerEntityId = "requestIssuerEntityId";
    private DateTime sessionExpiryTimestamp = DateTime.now(DateTimeZone.UTC).plusMinutes(10);
    private URI assertionConsumerServiceUri = URI.create("https://assertion-consumer-service-uri");
    private SessionId sessionId = aSessionId().build();
    private boolean transactionSupportsEidas = true;
    private String relayState = "relayState";
    private Set<String> encryptedAssertions = Set.of("encryptedAssertion");
    private CountrySignedResponseContainer countrySignedResponseContainer = new CountrySignedResponseContainer(
            "base64SamlResponse",
            List.of("base64EncryptedKey"),
            "countryEntityId"
    );

    public static NonMatchingJourneySuccessStateBuilder aNonMatchingJourneySuccessStateBuilder() {
        return new NonMatchingJourneySuccessStateBuilder();
    }

    public NonMatchingJourneySuccessState build() {
        return new NonMatchingJourneySuccessState(
                requestId,
                requestIssuerEntityId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                transactionSupportsEidas,
                relayState,
                encryptedAssertions,
                countrySignedResponseContainer
        );
    }

    public NonMatchingJourneySuccessStateBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public NonMatchingJourneySuccessStateBuilder withRequestIssuerEntityId(String requestIssuerEntityId) {
        this.requestIssuerEntityId = requestIssuerEntityId;
        return this;
    }

    public NonMatchingJourneySuccessStateBuilder withSessionExpiryTimestamp(DateTime sessionExpiryTimestamp) {
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        return this;
    }

    public NonMatchingJourneySuccessStateBuilder withAssertionConsumerServiceUri(URI assertionConsumerServiceUri) {
        this.assertionConsumerServiceUri = assertionConsumerServiceUri;
        return this;
    }

    public NonMatchingJourneySuccessStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public NonMatchingJourneySuccessStateBuilder withTransactionSupportsEidas(boolean transactionSupportsEidas) {
        this.transactionSupportsEidas = transactionSupportsEidas;
        return this;
    }

    public NonMatchingJourneySuccessStateBuilder withRelayState(String relayState) {
        this.relayState = relayState;
        return this;
    }

    public NonMatchingJourneySuccessStateBuilder withEncryptedAssertions(Set<String> encryptedAssertions) {
        this.encryptedAssertions = encryptedAssertions;
        return this;
    }

    public NonMatchingJourneySuccessStateBuilder withCountrySignedResponseContainer(CountrySignedResponseContainer countrySignedResponseContainer) {
        this.countrySignedResponseContainer = countrySignedResponseContainer;
        return this;
    }
}
