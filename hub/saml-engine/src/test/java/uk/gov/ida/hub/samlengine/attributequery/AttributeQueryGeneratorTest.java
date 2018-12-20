package uk.gov.ida.hub.samlengine.attributequery;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Element;
import uk.gov.ida.hub.samlengine.domain.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlengine.exceptions.UnableToGenerateSamlException;
import uk.gov.ida.hub.samlengine.locators.AssignableEntityToEncryptForLocator;
import uk.gov.ida.saml.core.domain.HubAssertion;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.hub.domain.HubAttributeQueryRequest;
import uk.gov.ida.saml.hub.domain.UserAccountCreationAttribute;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.assertj.jodatime.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AttributeQueryGeneratorTest {

    @Mock
    private Function<HubAttributeQueryRequest, Element> transformer;

    @Mock
    private AssignableEntityToEncryptForLocator entityToEncryptForLocator;

    @Captor
    private ArgumentCaptor<HubAttributeQueryRequest> hubAttributeQueryRequestCaptor = null;

    private AttributeQueryGenerator<HubAttributeQueryRequest> attributeQueryGenerator;
    private static final String MATCHING_SERVICE_ENTITY_ID = "matching-service-entity-id";

    @Before
    public void setUp() throws Exception {
        attributeQueryGenerator = new AttributeQueryGenerator<>(transformer, entityToEncryptForLocator);
    }

    @Test
    public void handle_shouldSetEntityToEncryptFor() throws Exception {
        HubAttributeQueryRequest hubAttributeQueryRequest = aHubAttributeQueryRequest();
        when(transformer.apply(any(HubAttributeQueryRequest.class))).thenThrow(new RuntimeException("Avoid NullPointerException later."));

        try {
            attributeQueryGenerator.createAttributeQueryContainer(hubAttributeQueryRequest, URI.create("/dont_care"), MATCHING_SERVICE_ENTITY_ID, null, false);
            fail("Should have thrown exception.");
        } catch (RuntimeException e) {
            assertThat(e).hasMessage("Avoid NullPointerException later.");
        }

        verify(entityToEncryptForLocator).addEntityIdForRequestId(hubAttributeQueryRequest.getId(), MATCHING_SERVICE_ENTITY_ID);
        verify(entityToEncryptForLocator).removeEntityIdForRequestId(hubAttributeQueryRequest.getId());
    }

    @Test
    public void handle_shouldDelegateToTransformerWithCorrectData() throws Exception {
        DateTimeFreezer.freezeTime();
        HubAttributeQueryRequest hubAttributeQueryRequest = aHubAttributeQueryRequest();

        attributeQueryGenerator.createAttributeQueryContainer(hubAttributeQueryRequest, URI.create("/dont_care"), MATCHING_SERVICE_ENTITY_ID, null, false);

        verify(transformer).apply(hubAttributeQueryRequestCaptor.capture());

        HubAttributeQueryRequest request = hubAttributeQueryRequestCaptor.getValue();

        assertThat(request.getId()).isEqualTo(hubAttributeQueryRequest.getId());
        assertThat(request.getEncryptedAuthnAssertion()).isEqualTo(hubAttributeQueryRequest.getEncryptedAuthnAssertion());
        assertThat(request.getCycle3AttributeAssertion()).isAbsent();
        assertThat(request.getIssueInstant()).isEqualTo(DateTime.now());
        assertThat(request.getAssertionConsumerServiceUrl()).isEqualTo(hubAttributeQueryRequest.getAssertionConsumerServiceUrl());
        assertThat(request.getAuthnRequestIssuerEntityId()).isEqualTo(hubAttributeQueryRequest.getAuthnRequestIssuerEntityId());
    }

    //This test should move to the request builder?
    @Test
    public void handle_shouldReturnDtoWithSamlMatchingServiceRequest() throws Exception {
        HubAttributeQueryRequest hubAttributeQueryRequest = aHubAttributeQueryRequest();
        Element transformedRequest = mock(Element.class);
        when(transformer.apply(any(HubAttributeQueryRequest.class))).thenReturn(transformedRequest);

        AttributeQueryContainerDto attributeQueryDto = attributeQueryGenerator.createAttributeQueryContainer(hubAttributeQueryRequest, URI.create("/dont_care"), MATCHING_SERVICE_ENTITY_ID, null, false);

        assertThat(attributeQueryDto.getSamlRequest()).isEqualTo(XmlUtils.writeToString(transformedRequest));
    }

    @Test(expected = UnableToGenerateSamlException.class)
    public void handle_shouldReturnErrorDtoWhenTransformFails() throws Exception {
        HubAttributeQueryRequest hubAttributeQueryRequest = aHubAttributeQueryRequest();
        when(transformer.apply(hubAttributeQueryRequest)).thenThrow(new RuntimeException("failed to create attribute query request"));

        attributeQueryGenerator.createAttributeQueryContainer(hubAttributeQueryRequest, MATCHING_SERVICE_ENTITY_ID);
    }

    @Test
    public void handle_shouldReturnDtoWithMatchingServiceEndpointReturnedFromConfig() throws Exception {
        HubAttributeQueryRequest hubAttributeQueryRequest = aHubAttributeQueryRequest();

        URI attributeQueryUri = URI.create("/attribute-query-uri");
        AttributeQueryContainerDto attributeQueryDto = attributeQueryGenerator.createAttributeQueryContainer(hubAttributeQueryRequest, attributeQueryUri, MATCHING_SERVICE_ENTITY_ID, null, false);

        assertThat(attributeQueryDto.getMatchingServiceUri()).isEqualTo(attributeQueryUri);
    }

    private HubAttributeQueryRequest aHubAttributeQueryRequest() {
        return new HubAttributeQueryRequest(
                "",
                null,
                "",
                "",
                Optional.<HubAssertion>absent(),
                Optional.<List<UserAccountCreationAttribute>>absent(),
                DateTime.now(),
                null,
                "",
                null,
                TestEntityIds.HUB_ENTITY_ID);
    }
}
