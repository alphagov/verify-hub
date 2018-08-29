package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;

import java.net.URI;

public class EidasSuccessfulMatchStateBuilder {

    private String requestId = "request-id";
    private String countryEntityId = "country-entity-id";
    private String matchingServiceAssertion = "aPassthroughAssertion().buildMatchingServiceAssertion()";
    private URI assertionConsumerServiceUri = URI.create("http://assertionconsumeruri");
    private String relayState = "relay state";
    private String requestIssuerId = "request issuer id";
    private DateTime sessionExpiryTimestamp = DateTime.now(DateTimeZone.UTC).plusMinutes(10);
    private SessionId sessionId = SessionIdBuilder.aSessionId().build();
    private LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_2;

    public static EidasSuccessfulMatchStateBuilder anEidasSuccessfulMatchState() {
        return new EidasSuccessfulMatchStateBuilder();
    }

    public EidasSuccessfulMatchState build() {
        return new EidasSuccessfulMatchState(
                requestId,
                sessionExpiryTimestamp,
                countryEntityId,
                matchingServiceAssertion,
                relayState,
                requestIssuerId,
                assertionConsumerServiceUri,
                sessionId,
                levelOfAssurance,
                true);
    }

    public EidasSuccessfulMatchStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public EidasSuccessfulMatchStateBuilder withCountryEntityId(String identityProviderEntityId) {
        this.countryEntityId = identityProviderEntityId;
        return this;
    }

    public EidasSuccessfulMatchStateBuilder withMatchingServiceAssertion(String matchingServiceAssertion) {
        this.matchingServiceAssertion = matchingServiceAssertion;
        return this;
    }

    public EidasSuccessfulMatchStateBuilder withRelayState(String relayState) {
        this.relayState = relayState;
        return this;
    }

    public EidasSuccessfulMatchStateBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public EidasSuccessfulMatchStateBuilder withRequestIssuerId(String requestIssuerId) {
        this.requestIssuerId = requestIssuerId;
        return this;
    }

    public EidasSuccessfulMatchStateBuilder withSessionExpiryTimestamp(DateTime sessionExpiryTimestamp) {
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        return this;
    }

    public EidasSuccessfulMatchStateBuilder withAssertionConsumerServiceUri(URI assertionConsumerServiceUri ) {
        this.assertionConsumerServiceUri = assertionConsumerServiceUri;
        return this;
    }
}
