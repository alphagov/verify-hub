package uk.gov.ida.hub.policy.builder.state;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class IdpSelectedStateBuilder {
    private String requestId = UUID.randomUUID().toString();
    private String idpEntityId = "idp-entity-id";
    private String matchingServiceEntityId = "matching-service-entity-id";
    private List<LevelOfAssurance> levelsOfAssurance = Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2);
    private Boolean useExactComparisonType = false;
    private Boolean forceAuthentication = null;
    private URI assertionConsumerServiceUri = URI.create("/default-service-uri");
    private String requestIssuerId = "transaction-entity-id";
    private String relayState = null;
    private DateTime sessionExpiryTimestamp = DateTime.now(DateTimeZone.UTC).plusDays(5);
    private boolean isRegistration = false;
    private LevelOfAssurance requestedLoa = LevelOfAssurance.LEVEL_2;
    private SessionId sessionId = SessionIdBuilder.aSessionId().build();
    private List<String> availableIdentityProviders = ImmutableList.of("idp-a", "idp-b", "idp-c");
    private boolean transactionSupportsEidas = false;

    public static IdpSelectedStateBuilder anIdpSelectedState() {
        return new IdpSelectedStateBuilder();
    }

    public IdpSelectedState build() {
        return new IdpSelectedState(
                requestId,
                idpEntityId,
                matchingServiceEntityId,
                levelsOfAssurance,
                useExactComparisonType,
                forceAuthentication,
                assertionConsumerServiceUri,
                requestIssuerId,
                relayState,
                sessionExpiryTimestamp,
                isRegistration,
                requestedLoa,
                sessionId,
                availableIdentityProviders,
                transactionSupportsEidas);
    }

    public IdpSelectedStateBuilder withIdpEntityId(String idpEntityId) {
        this.idpEntityId = idpEntityId;
        return this;
    }

    public IdpSelectedStateBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public IdpSelectedStateBuilder withLevelsOfAssurance(List<LevelOfAssurance> levelsOfAssurance) {
        this.levelsOfAssurance = levelsOfAssurance;
        return this;
    }

    public IdpSelectedStateBuilder withUseExactComparisonType(Boolean useExactComparisonType) {
        this.useExactComparisonType = useExactComparisonType;
        return this;
    }

    public IdpSelectedStateBuilder withSessionExpiryTimestamp(DateTime sessionExpiryTimestamp) {
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        return this;
    }

    public IdpSelectedStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public IdpSelectedStateBuilder withRegistration(boolean registering) {
        this.isRegistration = registering;
        return this;
    }

    public IdpSelectedStateBuilder withRequestedLoa(LevelOfAssurance requestedLoa) {
        this.requestedLoa = requestedLoa;
        return this;
    }

    public IdpSelectedStateBuilder withRequestIssuerEntityId(String requestIssuerEntityId) {
        this.requestIssuerId = requestIssuerEntityId;
        return this;
    }

    public IdpSelectedStateBuilder withMatchingServiceEntityId(String matchingServiceEntityId) {
        this.matchingServiceEntityId = matchingServiceEntityId;
        return this;
    }

    public IdpSelectedStateBuilder withAvailableIdentityProviders(List<String> availableIdentityProviders) {
        this.availableIdentityProviders = availableIdentityProviders;
        return this;
    }

    public IdpSelectedStateBuilder withRelayState(String relayState){
        this.relayState = relayState;
        return this;
    }

    public IdpSelectedStateBuilder withTransactionSupportsEidas(boolean transactionSupportsEidas) {
        this.transactionSupportsEidas = transactionSupportsEidas;
        return this;
    }
}
