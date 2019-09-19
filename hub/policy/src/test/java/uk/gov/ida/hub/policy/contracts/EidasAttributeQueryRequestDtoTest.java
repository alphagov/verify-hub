package uk.gov.ida.hub.policy.contracts;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.domain.Cycle3Dataset;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto;

@RunWith(MockitoJUnitRunner.class)
public class EidasAttributeQueryRequestDtoTest {
    private static final AttributeQueryContainerDto ATTRIBUTE_QUERY_CONTAINER_DTO = anAttributeQueryContainerDto().build();
    @Mock
    private SamlEngineProxy samlEngineProxy;

    private static final String REQUEST_ID = "requestId";
    private static final String ENCRYPTED_IDENTITY_ASSERTION = "encryptedIdentityAssertion";
    private static final String NAME_ID = "nameId";
    private static final PersistentId PERSISTENT_ID = new PersistentId(NAME_ID);
    private static final URI ASSERTION_CONSUMER_SERVICE_URI = URI.create("assertionConsumerServiceUri");
    private static final String AUTHN_REQUEST_ISSUER_ENTITY_ID = "authnRequestIssuerEntityId";
    private static final URI MATCHING_SERVICE_ADAPTER_URI = URI.create("matchingServiceAdapterUri");
    private static final String MATCHING_SERVICE_ADAPTER_ENTITY_ID = "matchingServiceEntityId";
    private static final DateTime MATCHING_SERVICE_REQUEST_TIME_OUT = DateTime.now();
    private static final boolean ONBOARDING = true;
    private static final Optional<Cycle3Dataset> CYCLE_3_DATASET = Optional.of(Cycle3Dataset.createFromData("attributeName", "attributeValue"));
    private static final Optional<List<UserAccountCreationAttribute>> USER_ACCOUNT_CREATION_ATTRIBUTES = Optional.empty();
    private static final DateTime ASSERTION_EXPIRY = DateTime.now();
    private EidasAttributeQueryRequestDto eidasAttributeQueryRequestDto;

    @Before
    public void setUp() throws Exception {
        eidasAttributeQueryRequestDto = new EidasAttributeQueryRequestDto(
            REQUEST_ID,
            AUTHN_REQUEST_ISSUER_ENTITY_ID,
            ASSERTION_CONSUMER_SERVICE_URI,
            ASSERTION_EXPIRY,
            MATCHING_SERVICE_ADAPTER_ENTITY_ID,
            MATCHING_SERVICE_ADAPTER_URI,
            MATCHING_SERVICE_REQUEST_TIME_OUT,
            ONBOARDING,
            LevelOfAssurance.LEVEL_2,
            PERSISTENT_ID,
            CYCLE_3_DATASET,
            USER_ACCOUNT_CREATION_ATTRIBUTES,
            ENCRYPTED_IDENTITY_ASSERTION
        );
    }

    @Test
    public void getRequestId() throws Exception {
        assertThat(eidasAttributeQueryRequestDto.getRequestId()).isEqualTo(REQUEST_ID);
    }

    @Test
    public void getPersistentId() throws Exception {
        assertThat(eidasAttributeQueryRequestDto.getPersistentId()).isEqualTo(PERSISTENT_ID);
    }

    @Test
    public void getEncryptedIdentityAssertion() throws Exception {
        assertThat(eidasAttributeQueryRequestDto.getEncryptedIdentityAssertion()).isEqualTo(ENCRYPTED_IDENTITY_ASSERTION);
    }

    @Test
    public void getAssertionConsumerServiceUri() throws Exception {
        assertThat(eidasAttributeQueryRequestDto.getAssertionConsumerServiceUri()).isEqualTo(ASSERTION_CONSUMER_SERVICE_URI);
    }

    @Test
    public void getAuthnRequestIssuerEntityId() throws Exception {
        assertThat(eidasAttributeQueryRequestDto.getAuthnRequestIssuerEntityId()).isEqualTo(AUTHN_REQUEST_ISSUER_ENTITY_ID);
    }

    @Test
    public void getLevelOfAssurance() throws Exception {
        assertThat(eidasAttributeQueryRequestDto.getLevelOfAssurance()).isEqualTo(LevelOfAssurance.LEVEL_2);
    }

    @Test
    public void getAttributeQueryUri() throws Exception {
        assertThat(eidasAttributeQueryRequestDto.getAttributeQueryUri()).isEqualTo(MATCHING_SERVICE_ADAPTER_URI);
    }

    @Test
    public void getMatchingServiceEntityId() throws Exception {
        assertThat(eidasAttributeQueryRequestDto.getMatchingServiceEntityId()).isEqualTo(MATCHING_SERVICE_ADAPTER_ENTITY_ID);
    }

    @Test
    public void getMatchingServiceRequestTimeOut() throws Exception {
        assertThat(eidasAttributeQueryRequestDto.getMatchingServiceRequestTimeOut()).isEqualTo(MATCHING_SERVICE_REQUEST_TIME_OUT);
    }

    @Test
    public void isOnboarding() throws Exception {
        assertThat(eidasAttributeQueryRequestDto.isOnboarding()).isTrue();
    }

    @Test
    public void getCycle3Dataset() throws Exception {
        assertThat(eidasAttributeQueryRequestDto.getCycle3Dataset()).isEqualTo(CYCLE_3_DATASET);
    }

    @Test
    public void getUserAccountCreationAttributes() throws Exception {
        assertThat(eidasAttributeQueryRequestDto.getUserAccountCreationAttributes()).isEqualTo(USER_ACCOUNT_CREATION_ATTRIBUTES);
    }

    @Test
    public void getAssertionExpiry() throws Exception {
        assertThat(eidasAttributeQueryRequestDto.getAssertionExpiry()).isEqualTo(ASSERTION_EXPIRY);
    }

    @Test
    public void sendToSamlEngine() throws Exception {
        when(samlEngineProxy.generateEidasAttributeQuery(eidasAttributeQueryRequestDto)).thenReturn(ATTRIBUTE_QUERY_CONTAINER_DTO);

        AttributeQueryContainerDto actual = eidasAttributeQueryRequestDto.sendToSamlEngine(samlEngineProxy);

        assertThat(actual).isEqualTo(ATTRIBUTE_QUERY_CONTAINER_DTO);
    }

    @Test
    public void testToString() {
        final StringBuilder sb = new StringBuilder("EidasAttributeQueryRequestDto{");
        sb.append("requestId='").append(REQUEST_ID).append('\'');
        sb.append(",authnRequestIssuerEntityId='").append(AUTHN_REQUEST_ISSUER_ENTITY_ID).append('\'');
        sb.append(",assertionConsumerServiceUri=").append(ASSERTION_CONSUMER_SERVICE_URI);
        sb.append(",assertionExpiry=").append(ASSERTION_EXPIRY);
        sb.append(",matchingServiceEntityId='").append(MATCHING_SERVICE_ADAPTER_ENTITY_ID).append('\'');
        sb.append(",attributeQueryUri=").append(MATCHING_SERVICE_ADAPTER_URI);
        sb.append(",matchingServiceRequestTimeOut=").append(MATCHING_SERVICE_REQUEST_TIME_OUT);
        sb.append(",onboarding=").append(ONBOARDING);
        sb.append(",levelOfAssurance=").append(LevelOfAssurance.LEVEL_2);
        sb.append(",persistentId=").append(PERSISTENT_ID);
        sb.append(",cycle3Dataset=").append(CYCLE_3_DATASET);
        sb.append(",userAccountCreationAttributes=").append(USER_ACCOUNT_CREATION_ATTRIBUTES);
        sb.append(",encryptedIdentityAssertion='").append(ENCRYPTED_IDENTITY_ASSERTION).append('\'');
        sb.append('}');

        assertThat(eidasAttributeQueryRequestDto.toString()).isEqualTo(sb.toString());
    }

    @Test
    public void equalsContract() {

        EqualsVerifier.forClass(EidasAttributeQueryRequestDto.class).suppress(Warning.NONFINAL_FIELDS).verify();
    }
}
