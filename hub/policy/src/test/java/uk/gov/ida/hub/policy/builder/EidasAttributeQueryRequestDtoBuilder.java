package uk.gov.ida.hub.policy.builder;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.Cycle3Dataset;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EidasAttributeQueryRequestDtoBuilder {
    public static EidasAttributeQueryRequestDtoBuilder anEidasAttributeQueryRequestDto() {
        return new EidasAttributeQueryRequestDtoBuilder();
    }

    public EidasAttributeQueryRequestDto build() {
        return new EidasAttributeQueryRequestDto(
            "requestId",
            "authnRequestIssuesEntityId",
            URI.create("assertionConsumerServiceUri"),
            DateTime.now().plusHours(2),
            "matchingServiceAdapterEntityId",
            URI.create("matchingServiceAdapterUri"),
            DateTime.now().plusHours(1),
            true,
            LevelOfAssurance.LEVEL_2,
            new PersistentId("nameId"),
            Optional.of(aCycle3Dataset()),
            Optional.empty(),
            "encryptedIdentityAssertion"
        );
    }

    private Cycle3Dataset aCycle3Dataset() {
        Map<String, String> map = new HashMap<>();
        map.put("attribute", "attributeValue");
        return new Cycle3Dataset(map);
    }
}

