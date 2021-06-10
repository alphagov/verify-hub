package uk.gov.ida.hub.policy.contracts;

import nl.jqno.equalsverifier.EqualsVerifier;
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
public class AttributeQueryRequestDtoTest {

    private static final AttributeQueryContainerDto ATTRIBUTE_QUERY_CONTAINER_DTO = anAttributeQueryContainerDto().build();
    private static final String AUTHN_STATEMENT_ASSERTION = "authnStatementAssertion";
    @Mock
    private SamlEngineProxy samlEngineProxy;

    private static final String REQUEST_ID = "requestId";
    private static final String ENCRYPTED_MATCHING_DATASET_ASSERTION = "encryptedIdentityAssertion";
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
    private AttributeQueryRequestDto attributeQueryRequestDto;

    @Before
    public void setUp() throws Exception {
        attributeQueryRequestDto = new AttributeQueryRequestDto(
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
            ENCRYPTED_MATCHING_DATASET_ASSERTION,
            AUTHN_STATEMENT_ASSERTION
        );
    }

    @Test
    public void getRequestId() throws Exception {
        assertThat(attributeQueryRequestDto.getRequestId()).isEqualTo(REQUEST_ID);
    }

    @Test
    public void getPersistentId() throws Exception {
        assertThat(attributeQueryRequestDto.getPersistentId()).isEqualTo(PERSISTENT_ID);
    }

    @Test
    public void getEncryptedMatchingDataSetAssertion() throws Exception {
        assertThat(attributeQueryRequestDto.getEncryptedMatchingDatasetAssertion()).isEqualTo(ENCRYPTED_MATCHING_DATASET_ASSERTION);
    }

    @Test
    public void getAuthnStatementAssertion() throws Exception {
        assertThat(attributeQueryRequestDto.getEncryptedAuthnAssertion()).isEqualTo(AUTHN_STATEMENT_ASSERTION);
    }

    @Test
    public void getAssertionConsumerServiceUri() throws Exception {
        assertThat(attributeQueryRequestDto.getAssertionConsumerServiceUri()).isEqualTo(ASSERTION_CONSUMER_SERVICE_URI);
    }

    @Test
    public void getAuthnRequestIssuerEntityId() throws Exception {
        assertThat(attributeQueryRequestDto.getAuthnRequestIssuerEntityId()).isEqualTo(AUTHN_REQUEST_ISSUER_ENTITY_ID);
    }

    @Test
    public void getLevelOfAssurance() throws Exception {
        assertThat(attributeQueryRequestDto.getLevelOfAssurance()).isEqualTo(LevelOfAssurance.LEVEL_2);
    }

    @Test
    public void getAttributeQueryUri() throws Exception {
        assertThat(attributeQueryRequestDto.getAttributeQueryUri()).isEqualTo(MATCHING_SERVICE_ADAPTER_URI);
    }

    @Test
    public void getMatchingServiceEntityId() throws Exception {
        assertThat(attributeQueryRequestDto.getMatchingServiceEntityId()).isEqualTo(MATCHING_SERVICE_ADAPTER_ENTITY_ID);
    }

    @Test
    public void getMatchingServiceRequestTimeOut() throws Exception {
        assertThat(attributeQueryRequestDto.getMatchingServiceRequestTimeOut()).isEqualTo(MATCHING_SERVICE_REQUEST_TIME_OUT);
    }

    @Test
    public void isOnboarding() throws Exception {
        assertThat(attributeQueryRequestDto.isOnboarding()).isTrue();
    }

    @Test
    public void getCycle3Dataset() throws Exception {
        assertThat(attributeQueryRequestDto.getCycle3Dataset()).isEqualTo(CYCLE_3_DATASET);
    }

    @Test
    public void getUserAccountCreationAttributes() throws Exception {
        assertThat(attributeQueryRequestDto.getUserAccountCreationAttributes()).isEqualTo(USER_ACCOUNT_CREATION_ATTRIBUTES);
    }

    @Test
    public void getAssertionExpiry() throws Exception {
        assertThat(attributeQueryRequestDto.getAssertionExpiry()).isEqualTo(ASSERTION_EXPIRY);
    }

    @Test
    public void sendToSamlEngine() throws Exception {
        when(samlEngineProxy.generateAttributeQuery(attributeQueryRequestDto)).thenReturn(ATTRIBUTE_QUERY_CONTAINER_DTO);

        AttributeQueryContainerDto actual = attributeQueryRequestDto.sendToSamlEngine(samlEngineProxy);

        assertThat(actual).isEqualTo(ATTRIBUTE_QUERY_CONTAINER_DTO);
    }

    @Test
    public void createCycle01MatchingServiceRequest() {
         AttributeQueryRequestDto expected = new AttributeQueryRequestDto(
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
            Optional.empty(),
            USER_ACCOUNT_CREATION_ATTRIBUTES,
            ENCRYPTED_MATCHING_DATASET_ASSERTION,
            AUTHN_STATEMENT_ASSERTION
        );

        AttributeQueryRequestDto actual = AttributeQueryRequestDto.createCycle01MatchingServiceRequest(
            REQUEST_ID,
            ENCRYPTED_MATCHING_DATASET_ASSERTION,
            AUTHN_STATEMENT_ASSERTION,
            AUTHN_REQUEST_ISSUER_ENTITY_ID,
            ASSERTION_CONSUMER_SERVICE_URI,
            MATCHING_SERVICE_ADAPTER_ENTITY_ID,
            MATCHING_SERVICE_REQUEST_TIME_OUT,
            LevelOfAssurance.LEVEL_2,
            PERSISTENT_ID,
            ASSERTION_EXPIRY,
            MATCHING_SERVICE_ADAPTER_URI,
            ONBOARDING
        );

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void createCycle3MatchingServiceRequest() {
        AttributeQueryRequestDto actual = AttributeQueryRequestDto.createCycle3MatchingServiceRequest(
            REQUEST_ID,
            ENCRYPTED_MATCHING_DATASET_ASSERTION,
            AUTHN_STATEMENT_ASSERTION,
            CYCLE_3_DATASET.get(),
            AUTHN_REQUEST_ISSUER_ENTITY_ID,
            ASSERTION_CONSUMER_SERVICE_URI,
            MATCHING_SERVICE_ADAPTER_ENTITY_ID,
            MATCHING_SERVICE_REQUEST_TIME_OUT,
            LevelOfAssurance.LEVEL_2,
            Optional.empty(),
            PERSISTENT_ID,
            ASSERTION_EXPIRY,
            MATCHING_SERVICE_ADAPTER_URI,
            ONBOARDING
        );

        assertThat(actual).isEqualTo(attributeQueryRequestDto);
    }

    @Test
    public void createUserAccountRequiredMatchingServiceRequest() {
        final List<UserAccountCreationAttribute> userAccountCreationAttributes = List.of(UserAccountCreationAttribute.CURRENT_ADDRESS);
        AttributeQueryRequestDto expected = new AttributeQueryRequestDto(
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
            Optional.of(userAccountCreationAttributes),
            ENCRYPTED_MATCHING_DATASET_ASSERTION,
            AUTHN_STATEMENT_ASSERTION
        );

        AttributeQueryRequestDto actual = AttributeQueryRequestDto.createUserAccountRequiredMatchingServiceRequest(
            REQUEST_ID,
            ENCRYPTED_MATCHING_DATASET_ASSERTION,
            AUTHN_STATEMENT_ASSERTION,
            CYCLE_3_DATASET,
            AUTHN_REQUEST_ISSUER_ENTITY_ID,
            ASSERTION_CONSUMER_SERVICE_URI,
            MATCHING_SERVICE_ADAPTER_ENTITY_ID,
            MATCHING_SERVICE_REQUEST_TIME_OUT,
            LevelOfAssurance.LEVEL_2,
            userAccountCreationAttributes,
            PERSISTENT_ID,
            ASSERTION_EXPIRY,
            MATCHING_SERVICE_ADAPTER_URI,
            ONBOARDING
        );

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(AttributeQueryRequestDto.class).verify();
    }
}
