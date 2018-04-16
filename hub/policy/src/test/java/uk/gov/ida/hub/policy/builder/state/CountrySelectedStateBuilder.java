package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.CountrySelectedState;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Optional.absent;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

public class CountrySelectedStateBuilder {
    private boolean transactionSupportsEidas = false;
    private String requestId = UUID.randomUUID().toString();
    private String requestIssuerId = "requestIssuerId";
    private URI assertionConsumerServiceUri = URI.create("/default-service-index");
    private Optional<String> relayState = absent();
    private DateTime sessionExpiryTimestamp = DateTime.now(DateTimeZone.UTC).plusMinutes(10);
    private SessionId sessionId = aSessionId().build();
    private List<LevelOfAssurance> levelOfAssurance;

    private String countryCode;

    public static CountrySelectedStateBuilder aCountrySelectedState() {
        return new CountrySelectedStateBuilder();
    }

    public CountrySelectedStateBuilder withSelectedCountry(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public CountrySelectedState build() {
        return new CountrySelectedState(
            countryCode,
            relayState,
            requestId,
            requestIssuerId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            sessionId,
            transactionSupportsEidas,
            levelOfAssurance);
    }

    public CountrySelectedStateBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public CountrySelectedStateBuilder withSessionExpiryTimestamp(DateTime sessionExpiryTimestamp) {
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        return this;
    }

    public CountrySelectedStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public CountrySelectedStateBuilder withRequestIssuerEntityId(String requestIssuerEntityId) {
        this.requestIssuerId = requestIssuerEntityId;
        return this;
    }

    public CountrySelectedStateBuilder withTransactionSupportsEidas(boolean transactionSupportsEidas) {
        this.transactionSupportsEidas = transactionSupportsEidas;
        return this;
    }

    public CountrySelectedStateBuilder withLevelOfAssurance(List<LevelOfAssurance> levelOfAssurance) {
        this.levelOfAssurance = levelOfAssurance;
        return this;
    }
}
