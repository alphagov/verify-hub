package uk.gov.ida.hub.samlengine.attributequery;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.samlengine.domain.Cycle3Dataset;
import uk.gov.ida.hub.samlengine.domain.EidasAttributeQueryRequestDto;
import uk.gov.ida.saml.core.domain.AssertionRestrictions;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.domain.HubAssertion;
import uk.gov.ida.saml.hub.domain.HubEidasAttributeQueryRequest;
import uk.gov.ida.saml.hub.domain.UserAccountCreationAttribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.samlengine.builders.EidasAttributeQueryRequestDtoBuilder.anEidasAttributeQueryRequestDto;

public class HubEidasAttributeQueryRequestBuilderTest {

    private static final String HUB_EIDAS_ENTITY_ID = "Eidas";
    private static final Optional<List<UserAccountCreationAttribute>> USER_ACCOUNT_CREATION_ATTRIBUTES = Optional.empty();
    private static final DateTime NOW = DateTime.now();

    private static HubEidasAttributeQueryRequestBuilder builder;

    @Before
    public void setup() {
        DateTimeUtils.setCurrentMillisFixed(NOW.getMillis());
        builder = new HubEidasAttributeQueryRequestBuilder(HUB_EIDAS_ENTITY_ID);
    }

    @After
    public void teardown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldCreateHubAttributeQueryRequest() {
        final EidasAttributeQueryRequestDto eidasAttributeQueryRequestDto = anEidasAttributeQueryRequestDto().build();
        final Optional<HubAssertion> cycle3AttributeAssertion = Optional.empty();

        HubEidasAttributeQueryRequest expectedResult = new HubEidasAttributeQueryRequest(
            eidasAttributeQueryRequestDto.getRequestId(),
            HUB_EIDAS_ENTITY_ID,
            NOW,
            new uk.gov.ida.saml.core.domain.PersistentId(eidasAttributeQueryRequestDto.getPersistentId().getNameId()),
            eidasAttributeQueryRequestDto.getAssertionConsumerServiceUri(),
            eidasAttributeQueryRequestDto.getAuthnRequestIssuerEntityId(),
            eidasAttributeQueryRequestDto.getEncryptedIdentityAssertion(),
            AuthnContext.LEVEL_2,
            cycle3AttributeAssertion,
            USER_ACCOUNT_CREATION_ATTRIBUTES);

        HubEidasAttributeQueryRequest hubEidasAttributeQueryRequest = builder.createHubAttributeQueryRequest(eidasAttributeQueryRequestDto);

        assertThat(hubEidasAttributeQueryRequest.getId()).isEqualTo(expectedResult.getId());
        assertThat(hubEidasAttributeQueryRequest.getPersistentId().getNameId()).isEqualTo(expectedResult.getPersistentId().getNameId());
        assertThat(hubEidasAttributeQueryRequest.getEncryptedIdentityAssertion()).isEqualTo(expectedResult.getEncryptedIdentityAssertion());
        assertThat(hubEidasAttributeQueryRequest.getAssertionConsumerServiceUrl()).isEqualTo(expectedResult.getAssertionConsumerServiceUrl());
        assertThat(hubEidasAttributeQueryRequest.getAuthnRequestIssuerEntityId()).isEqualTo(expectedResult.getAuthnRequestIssuerEntityId());
        assertThat(hubEidasAttributeQueryRequest.getAuthnContext()).isEqualTo(expectedResult.getAuthnContext());
        assertThat(hubEidasAttributeQueryRequest.getIssuer()).isEqualTo(expectedResult.getIssuer());
        assertThat(hubEidasAttributeQueryRequest.getIssueInstant()).isEqualTo(expectedResult.getIssueInstant());
        assertThat(hubEidasAttributeQueryRequest.getCycle3AttributeAssertion()).isEqualTo(cycle3AttributeAssertion);
        assertThat(hubEidasAttributeQueryRequest.getUserAccountCreationAttributes()).isEqualTo(USER_ACCOUNT_CREATION_ATTRIBUTES);
    }

    @Test
    public void shouldCreateHubAttributeQueryRequestWithCycle3Assertion() {
        final Map<String, String> dataset = new HashMap<>();
        dataset.put("DrivingLicenceNumber", "MORGA657054SM9IJ");
        final Cycle3Dataset cycle3Dataset = new Cycle3Dataset(dataset);
        final EidasAttributeQueryRequestDto eidasAttributeQueryRequestDto = anEidasAttributeQueryRequestDto().withCycle3Dataset(cycle3Dataset).build();
        final uk.gov.ida.saml.core.domain.PersistentId persistentId = new uk.gov.ida.saml.core.domain.PersistentId(eidasAttributeQueryRequestDto.getPersistentId().getNameId());
        final uk.gov.ida.saml.core.domain.Cycle3Dataset oldCycle3Dataset = uk.gov.ida.saml.core.domain.Cycle3Dataset.createFromData(eidasAttributeQueryRequestDto.getCycle3Dataset().get().getAttributes());
        final AssertionRestrictions assertionRestrictions = new AssertionRestrictions(
            eidasAttributeQueryRequestDto.getAssertionExpiry(),
            eidasAttributeQueryRequestDto.getRequestId(),
            eidasAttributeQueryRequestDto.getAuthnRequestIssuerEntityId());
        final HubAssertion hubAssertion = new HubAssertion(
            UUID.randomUUID().toString(),
            HUB_EIDAS_ENTITY_ID,
            NOW,
            persistentId,
            assertionRestrictions,
            Optional.of(oldCycle3Dataset));
        final Optional<HubAssertion> cycle3Assertion = Optional.of(hubAssertion);
        final HubEidasAttributeQueryRequest expectedResult = new HubEidasAttributeQueryRequest(
            eidasAttributeQueryRequestDto.getRequestId(),
            HUB_EIDAS_ENTITY_ID,
            NOW,
            persistentId,
            eidasAttributeQueryRequestDto.getAssertionConsumerServiceUri(),
            eidasAttributeQueryRequestDto.getAuthnRequestIssuerEntityId(),
            eidasAttributeQueryRequestDto.getEncryptedIdentityAssertion(),
            AuthnContext.LEVEL_2,
            cycle3Assertion,
            USER_ACCOUNT_CREATION_ATTRIBUTES);

        HubEidasAttributeQueryRequest hubEidasAttributeQueryRequest = builder.createHubAttributeQueryRequest(eidasAttributeQueryRequestDto);

        assertThat(hubEidasAttributeQueryRequest.getId()).isEqualTo(expectedResult.getId());
        assertThat(hubEidasAttributeQueryRequest.getPersistentId().getNameId()).isEqualTo(expectedResult.getPersistentId().getNameId());
        assertThat(hubEidasAttributeQueryRequest.getEncryptedIdentityAssertion()).isEqualTo(expectedResult.getEncryptedIdentityAssertion());
        assertThat(hubEidasAttributeQueryRequest.getAssertionConsumerServiceUrl()).isEqualTo(expectedResult.getAssertionConsumerServiceUrl());
        assertThat(hubEidasAttributeQueryRequest.getAuthnRequestIssuerEntityId()).isEqualTo(expectedResult.getAuthnRequestIssuerEntityId());
        assertThat(hubEidasAttributeQueryRequest.getAuthnContext()).isEqualTo(expectedResult.getAuthnContext());
        assertThat(hubEidasAttributeQueryRequest.getIssuer()).isEqualTo(expectedResult.getIssuer());
        assertThat(hubEidasAttributeQueryRequest.getIssueInstant()).isEqualTo(expectedResult.getIssueInstant());
        assertThat(hubEidasAttributeQueryRequest.getCycle3AttributeAssertion().isPresent()).isTrue();
        assertThat(hubEidasAttributeQueryRequest.getCycle3AttributeAssertion().get().getIssuerId()).isEqualTo(hubAssertion.getIssuerId());
        assertThat(hubEidasAttributeQueryRequest.getCycle3AttributeAssertion().get().getAssertionRestrictions().getInResponseTo()).isEqualTo(hubAssertion.getAssertionRestrictions().getInResponseTo());
        assertThat(hubEidasAttributeQueryRequest.getCycle3AttributeAssertion().get().getAssertionRestrictions().getNotOnOrAfter()).isEqualTo(hubAssertion.getAssertionRestrictions().getNotOnOrAfter());
        assertThat(hubEidasAttributeQueryRequest.getCycle3AttributeAssertion().get().getAssertionRestrictions().getRecipient()).isEqualTo(hubAssertion.getAssertionRestrictions().getRecipient());
        assertThat(hubEidasAttributeQueryRequest.getCycle3AttributeAssertion().get().getPersistentId().getNameId()).isEqualTo(hubAssertion.getPersistentId().getNameId());
        assertThat(hubEidasAttributeQueryRequest.getCycle3AttributeAssertion().get().getIssueInstant()).isEqualTo(hubAssertion.getIssueInstant());
        assertThat(hubEidasAttributeQueryRequest.getCycle3AttributeAssertion().get().getCycle3Data().isPresent()).isTrue();
        assertThat(hubEidasAttributeQueryRequest.getCycle3AttributeAssertion().get().getCycle3Data().get().getAttributes()).isEqualTo(hubAssertion.getCycle3Data().get().getAttributes());
        assertThat(hubEidasAttributeQueryRequest.getUserAccountCreationAttributes()).isEqualTo(USER_ACCOUNT_CREATION_ATTRIBUTES);
    }
}
