package uk.gov.ida.hub.policy.contracts;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.domain.Cycle3Dataset;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;

import java.net.URI;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto;

@RunWith(MockitoJUnitRunner.class)
public class AttributeQueryRequestDtoTest {

    private static final AttributeQueryContainerDto ATTRIBUTE_QUERY_CONTAINER_DTO = anAttributeQueryContainerDto().build();
    @Mock
    private SamlEngineProxy samlEngineProxy;

    private static final String REQUEST_ID = "requestId";
    private static final String ENCRYPTED_MATCHING_DATASET_ASSERTION = "encryptedIdentityAssertion";
    private static final String ENCRYPTED_AUTHN_ASSERTION = "encryptedAuthnAssertion";
    private static final String NAME_ID = "nameId";
    private static final PersistentId PERSISTENT_ID = new PersistentId(NAME_ID);
    private static final URI ASSERTION_CONSUMER_SERVICE_URI = URI.create("assertionConsumerServiceUri");
    private static final String AUTHN_REQUEST_ISSUER_ENTITY_ID = "authnRequestIssuerEntityId";
    private static final URI MATCHING_SERVICE_ADAPTER_URI = URI.create("matchingServiceAdapterUri");
    private static final String MATCHING_SERVICE_ADAPTER_ENTITY_ID = "matchingServiceEntityId";
    private static final DateTime MATCHING_SERVICE_REQUEST_TIME_OUT = DateTime.now();
    private static final boolean ONBOARDING = true;
    private static final Optional<Cycle3Dataset> CYCLE_3_DATASET = Optional.of(Cycle3Dataset.createFromData("attributeName", "attributeValue"));
    private static final Optional<List<UserAccountCreationAttribute>> USER_ACCOUNT_CREATION_ATTRIBUTES = Optional.absent();
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
            asList(ENCRYPTED_MATCHING_DATASET_ASSERTION, ENCRYPTED_AUTHN_ASSERTION)
        );
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
            Optional.absent(),
            USER_ACCOUNT_CREATION_ATTRIBUTES,
            asList(ENCRYPTED_MATCHING_DATASET_ASSERTION, ENCRYPTED_AUTHN_ASSERTION)
        );

        AttributeQueryRequestDto actual = AttributeQueryRequestDto.createCycle01MatchingServiceRequest(
            REQUEST_ID,
            ENCRYPTED_MATCHING_DATASET_ASSERTION,
                ENCRYPTED_AUTHN_ASSERTION,
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
                ENCRYPTED_AUTHN_ASSERTION,
            CYCLE_3_DATASET.get(),
            AUTHN_REQUEST_ISSUER_ENTITY_ID,
            ASSERTION_CONSUMER_SERVICE_URI,
            MATCHING_SERVICE_ADAPTER_ENTITY_ID,
            MATCHING_SERVICE_REQUEST_TIME_OUT,
            LevelOfAssurance.LEVEL_2,
            Optional.absent(),
            PERSISTENT_ID,
            ASSERTION_EXPIRY,
            MATCHING_SERVICE_ADAPTER_URI,
            ONBOARDING
        );

        assertThat(actual).isEqualTo(attributeQueryRequestDto);
    }

    @Test
    public void createUserAccountRequiredMatchingServiceRequest() {
        final List<UserAccountCreationAttribute> userAccountCreationAttributes = ImmutableList.of(UserAccountCreationAttribute.CURRENT_ADDRESS);
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
            asList(ENCRYPTED_MATCHING_DATASET_ASSERTION, ENCRYPTED_AUTHN_ASSERTION)
        );

        AttributeQueryRequestDto actual = AttributeQueryRequestDto.createUserAccountRequiredMatchingServiceRequest(
            REQUEST_ID,
            ENCRYPTED_MATCHING_DATASET_ASSERTION,
                ENCRYPTED_AUTHN_ASSERTION,
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
