package uk.gov.ida.hub.policy.builder;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.Cycle3Dataset;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class EidasAttributeQueryRequestDtoBuilderTest {
    private static final DateTime NOW = DateTime.now(DateTimeZone.UTC);

    @Before
    public void setUp() {
        DateTimeUtils.setCurrentMillisFixed(NOW.getMillis());
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void anEidasAttributeQueryRequest() {
        assertThat(EidasAttributeQueryRequestDtoBuilder.anEidasAttributeQueryRequestDto()).isInstanceOf(EidasAttributeQueryRequestDtoBuilder.class);
    }

    @Test
    public void build() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("attribute", "attributeValue");

        EidasAttributeQueryRequestDto eidasAttributeQueryRequestDto = EidasAttributeQueryRequestDtoBuilder.anEidasAttributeQueryRequestDto().build();

        assertThat(eidasAttributeQueryRequestDto.getRequestId()).isEqualTo("requestId");
        assertThat(eidasAttributeQueryRequestDto.getPersistentId()).isEqualTo(new PersistentId("nameId"));
        assertThat(eidasAttributeQueryRequestDto.getEncryptedIdentityAssertion()).isEqualTo("encryptedIdentityAssertion");
        assertThat(eidasAttributeQueryRequestDto.getAssertionConsumerServiceUri()).isEqualTo(URI.create("assertionConsumerServiceUri"));
        assertThat(eidasAttributeQueryRequestDto.getAuthnRequestIssuerEntityId()).isEqualTo("authnRequestIssuesEntityId");
        assertThat(eidasAttributeQueryRequestDto.getLevelOfAssurance()).isEqualTo(LevelOfAssurance.LEVEL_2);
        assertThat(eidasAttributeQueryRequestDto.getAttributeQueryUri()).isEqualTo(URI.create("matchingServiceAdapterUri"));
        assertThat(eidasAttributeQueryRequestDto.getMatchingServiceEntityId()).isEqualTo("matchingServiceAdapterEntityId");
        assertThat(eidasAttributeQueryRequestDto.getMatchingServiceRequestTimeOut()).isEqualTo(DateTime.now().plusHours(1));
        assertThat(eidasAttributeQueryRequestDto.isOnboarding()).isTrue();
        assertThat(eidasAttributeQueryRequestDto.getCycle3Dataset()).isEqualTo(Optional.of(new Cycle3Dataset(map)));
        assertThat(eidasAttributeQueryRequestDto.getUserAccountCreationAttributes()).isEqualTo(Optional.empty());
        assertThat(eidasAttributeQueryRequestDto.getAssertionExpiry()).isEqualTo(DateTime.now().plusHours(2));
    }
}
