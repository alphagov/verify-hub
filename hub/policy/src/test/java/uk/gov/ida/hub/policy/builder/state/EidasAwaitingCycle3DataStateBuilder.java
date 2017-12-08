package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState;

import java.net.URI;

public class EidasAwaitingCycle3DataStateBuilder {

    public static EidasAwaitingCycle3DataStateBuilder anEidasAwaitingCycle3DataState() {
        return new EidasAwaitingCycle3DataStateBuilder();
    }

    public EidasAwaitingCycle3DataState build() {
        return new EidasAwaitingCycle3DataState(
            "requestId",
            "requestIssuerId",
            DateTime.now().plusMinutes(10),
            URI.create("assertionConsumerServiceUri"),
            new SessionId("sessionId"),
            true,
            "identityProviderEntityId",
            "matchingServiceAdapterEntityId",
            Optional.of("relayState"),
            new PersistentId("nameId"),
            LevelOfAssurance.LEVEL_2,
            "encryptedIdentityAssertion"
        );
    }
}
