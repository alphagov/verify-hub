package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectedState;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Optional.absent;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

public class EidasCountrySelectedStateBuilder {
    private boolean transactionSupportsEidas = false;
    private String requestId = UUID.randomUUID().toString();
    private String requestIssuerId = "requestIssuerId";
    private URI assertionConsumerServiceUri = URI.create("/default-service-index");
    private Optional<String> relayState = absent();
    private DateTime sessionExpiryTimestamp = DateTime.now(DateTimeZone.UTC).plusMinutes(10);
    private SessionId sessionId = aSessionId().build();
    private List<LevelOfAssurance> levelOfAssurance;
    private Boolean forceAuthentication;

    private String countryCode;

    public static EidasCountrySelectedStateBuilder anEidasCountrySelectedState() {
        return new EidasCountrySelectedStateBuilder();
    }

    public EidasCountrySelectedStateBuilder withSelectedCountry(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public EidasCountrySelectedState build() {
        return new EidasCountrySelectedState(
            countryCode,
            relayState,
            requestId,
            requestIssuerId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            sessionId,
            transactionSupportsEidas,
            levelOfAssurance,
            forceAuthentication);
    }

    public EidasCountrySelectedStateBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public EidasCountrySelectedStateBuilder withSessionExpiryTimestamp(DateTime sessionExpiryTimestamp) {
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        return this;
    }

    public EidasCountrySelectedStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public EidasCountrySelectedStateBuilder withRequestIssuerEntityId(String requestIssuerEntityId) {
        this.requestIssuerId = requestIssuerEntityId;
        return this;
    }

    public EidasCountrySelectedStateBuilder withTransactionSupportsEidas(boolean transactionSupportsEidas) {
        this.transactionSupportsEidas = transactionSupportsEidas;
        return this;
    }

    public EidasCountrySelectedStateBuilder withLevelOfAssurance(List<LevelOfAssurance> levelOfAssurance) {
        this.levelOfAssurance = levelOfAssurance;
        return this;
    }

    public EidasCountrySelectedStateBuilder withForceAuthentication(Boolean forceAuthentication) {
        this.forceAuthentication = forceAuthentication;
        return this;
    }
}
