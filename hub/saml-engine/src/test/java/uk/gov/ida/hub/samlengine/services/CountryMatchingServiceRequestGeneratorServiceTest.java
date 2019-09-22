package uk.gov.ida.hub.samlengine.services;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.ida.hub.samlengine.attributequery.AttributeQueryGenerator;
import uk.gov.ida.hub.samlengine.attributequery.HubEidasAttributeQueryRequestBuilder;
import uk.gov.ida.hub.samlengine.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlengine.domain.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.samlengine.domain.LevelOfAssurance;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;
import uk.gov.ida.saml.core.domain.PersistentId;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.hub.domain.HubEidasAttributeQueryRequest;

import java.net.URI;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.samlengine.builders.PersistentIdBuilder.aPersistentId;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;

@RunWith(OpenSAMLMockitoRunner.class)
public class CountryMatchingServiceRequestGeneratorServiceTest {

    @Mock
    AttributeQueryGenerator<HubEidasAttributeQueryRequest> eidasAttributeQueryGenerator;

    @Mock
    HubEidasAttributeQueryRequestBuilder eidasAttributeQueryRequestBuilder;

    private static CountryMatchingServiceRequestGeneratorService service;
    private static final String REQUEST_ID = "request-id";
    private static final PersistentId PERSISTENT_ID = aPersistentId().buildSamlCorePersistentId();
    private static final String ENCRYPTED_IDENTITY_ASSERTION = "encrypted-identity-assertion";
    private static final URI ASSERTION_CONSUMER_SERVICE_URI = URI.create("assertion-consumer-service-uri");
    private static final URI MSA_URI = URI.create("msa-uri");
    private static final String MATCHING_SERVICE_ENTITY_ID = "matching-service-entity-id";
    private static final DateTime MATCHING_SERVICE_REQUEST_TIMEOUT = DateTime.now();
    private static final boolean IS_ONBOARDING = true;
    private static final String HUB_EIDAS_ENTITY_ID = "hub-eidas-entity-id";
    private static final String SAML_REQUEST = "SAML_REQUEST";
    private static Optional<CountrySignedResponseContainer> COUNTRY_SIGNED_RESPONSE = Optional.empty();
    private static final EidasAttributeQueryRequestDto EIDAS_ATTRIBUTE_QUERY_REQUEST_DTO = new EidasAttributeQueryRequestDto(
        REQUEST_ID,
        TEST_RP,
        ASSERTION_CONSUMER_SERVICE_URI,
        DateTime.now(),
        MATCHING_SERVICE_ENTITY_ID,
        MSA_URI,
        MATCHING_SERVICE_REQUEST_TIMEOUT,
        IS_ONBOARDING,
        LevelOfAssurance.LEVEL_2,
        new uk.gov.ida.hub.samlengine.domain.PersistentId(PERSISTENT_ID.getNameId()),
        Optional.empty(),
        Optional.empty(),
        ENCRYPTED_IDENTITY_ASSERTION,
        COUNTRY_SIGNED_RESPONSE
    );
    private static final HubEidasAttributeQueryRequest HUB_EIDAS_ATTRIBUTE_QUERY_REQUEST = new HubEidasAttributeQueryRequest(
        REQUEST_ID,
        HUB_EIDAS_ENTITY_ID,
        DateTime.now(),
        PERSISTENT_ID,
        ASSERTION_CONSUMER_SERVICE_URI,
        ENCRYPTED_IDENTITY_ASSERTION,
        TEST_RP,
        AuthnContext.LEVEL_2,
        Optional.empty(),
        Optional.empty(),
        COUNTRY_SIGNED_RESPONSE);
    private static final AttributeQueryContainerDto ATTRIBUTE_QUERY_CONTAINER_DTO = new AttributeQueryContainerDto(
        REQUEST_ID,
        TEST_RP,
        SAML_REQUEST,
        MSA_URI,
        MATCHING_SERVICE_REQUEST_TIMEOUT,
        IS_ONBOARDING);

    @Before
    public void setUp() {
        service = new CountryMatchingServiceRequestGeneratorService(eidasAttributeQueryRequestBuilder, eidasAttributeQueryGenerator);
        when(eidasAttributeQueryRequestBuilder.createHubAttributeQueryRequest(EIDAS_ATTRIBUTE_QUERY_REQUEST_DTO)).thenReturn(HUB_EIDAS_ATTRIBUTE_QUERY_REQUEST);
        when(eidasAttributeQueryGenerator.newCreateAttributeQueryContainer(
            HUB_EIDAS_ATTRIBUTE_QUERY_REQUEST,
            MSA_URI,
            MATCHING_SERVICE_ENTITY_ID,
            MATCHING_SERVICE_REQUEST_TIMEOUT,
            IS_ONBOARDING)).thenReturn(ATTRIBUTE_QUERY_CONTAINER_DTO);
    }

    @Test
    public void shouldGenerateAttributeQueryContainerDto() {
        service.generate(EIDAS_ATTRIBUTE_QUERY_REQUEST_DTO);

        verify(eidasAttributeQueryRequestBuilder).createHubAttributeQueryRequest(EIDAS_ATTRIBUTE_QUERY_REQUEST_DTO);
        verify(eidasAttributeQueryGenerator).newCreateAttributeQueryContainer(
            HUB_EIDAS_ATTRIBUTE_QUERY_REQUEST,
            MSA_URI,
            MATCHING_SERVICE_ENTITY_ID,
            MATCHING_SERVICE_REQUEST_TIMEOUT,
            IS_ONBOARDING);
    }
}
