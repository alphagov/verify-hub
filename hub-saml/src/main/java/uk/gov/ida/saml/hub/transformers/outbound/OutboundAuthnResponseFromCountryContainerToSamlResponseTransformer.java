package uk.gov.ida.saml.hub.transformers.outbound;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.common.shared.security.IdGenerator;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.AuthnResponseFromCountryContainerDto;
import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.core.extensions.eidas.CountrySamlResponse;
import uk.gov.ida.saml.core.extensions.eidas.EncryptedAssertionKeys;
import uk.gov.ida.saml.core.extensions.eidas.impl.CountrySamlResponseBuilder;
import uk.gov.ida.saml.core.extensions.eidas.impl.EncryptedAssertionKeysBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class OutboundAuthnResponseFromCountryContainerToSamlResponseTransformer implements Function<AuthnResponseFromCountryContainerDto, Response> {

    private final OpenSamlXmlObjectFactory openSamlXmlObjectFactory;
    private final String hubEntityId;
    private final IdGenerator idGenerator = new IdGenerator();

    public OutboundAuthnResponseFromCountryContainerToSamlResponseTransformer(
            OpenSamlXmlObjectFactory openSamlXmlObjectFactory,
            String hubEntityId
    ) {
        this.openSamlXmlObjectFactory = openSamlXmlObjectFactory;
        this.hubEntityId = hubEntityId;
    }
    @Override
    public Response apply(AuthnResponseFromCountryContainerDto countryResponseDto) {
        Response transformedResponse = openSamlXmlObjectFactory.createResponse();

        transformedResponse.setIssuer(openSamlXmlObjectFactory.createIssuer(hubEntityId));
        transformedResponse.setID(countryResponseDto.getResponseId());
        transformedResponse.setInResponseTo(countryResponseDto.getInResponseTo());
        transformedResponse.setIssueInstant(DateTime.now());
        transformedResponse.setDestination(countryResponseDto.getPostEndpoint().toASCIIString());

        setSuccessStatusCode(transformedResponse);
        setAssertion(transformedResponse, countryResponseDto);

        return transformedResponse;
    }

    private void setSuccessStatusCode(Response transformedResponse) {
        Status status = openSamlXmlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = openSamlXmlObjectFactory.createStatusCode();
        DetailedStatusCode detailedStatusCode = DetailedStatusCode.Success;

        topLevelStatusCode.setValue(detailedStatusCode.getStatus());
        status.setStatusCode(topLevelStatusCode);
        transformedResponse.setStatus(status);
    }

    private void setAssertion(Response transformedResponse, AuthnResponseFromCountryContainerDto countryResponseDto) {
        XMLObjectBuilderFactory factory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        Assertion assertion = (Assertion) factory
                .getBuilder(Assertion.DEFAULT_ELEMENT_NAME)
                .buildObject(Assertion.DEFAULT_ELEMENT_NAME, Assertion.TYPE_NAME);
        assertion.setID(idGenerator.getId());

        List<Attribute> attributes = getAttribtuesList(countryResponseDto);

        AttributeStatement attributeStatement = openSamlXmlObjectFactory.createAttributeStatement();
        attributeStatement.getAttributes().addAll(attributes);

        assertion.getAttributeStatements().add(attributeStatement);
        transformedResponse.getAssertions().add(assertion);
    }

    private List<Attribute> getAttribtuesList(AuthnResponseFromCountryContainerDto countryResponseDto) {
        Attribute countrySamlResponseAttribute = createCountrySamlResponseAttribute(countryResponseDto);
        Attribute encryptedAssertionKeysAttribute = createEncryptedAssertionKeysAttribute(countryResponseDto);
        return Arrays.asList(countrySamlResponseAttribute, encryptedAssertionKeysAttribute);
    }

    private Attribute createCountrySamlResponseAttribute(AuthnResponseFromCountryContainerDto countryResponseDto) {
        CountrySamlResponse attributeValue = new CountrySamlResponseBuilder().buildObject();
        attributeValue.setCountrySamlResponse(countryResponseDto.getSamlResponse());

        Attribute attribute = (Attribute) XMLObjectSupport.buildXMLObject(Attribute.DEFAULT_ELEMENT_NAME);
        attribute.setName("countrySamlResponse");
        attribute.setFriendlyName("friendlyCountrySamlResponse");
        attribute.setNameFormat(Attribute.URI_REFERENCE);

        attribute.getAttributeValues().add(attributeValue);
        return attribute;
    }

    private Attribute createEncryptedAssertionKeysAttribute(AuthnResponseFromCountryContainerDto countryResponseDto) {
        EncryptedAssertionKeys attributeValue = new EncryptedAssertionKeysBuilder().buildObject();
        attributeValue.setEncryptedAssertionKeys(String.join(".", countryResponseDto.getEncryptedKeys()));

        Attribute attribute = (Attribute) XMLObjectSupport.buildXMLObject(Attribute.DEFAULT_ELEMENT_NAME);
        attribute.setName("encryptedAssertionKeys");
        attribute.setFriendlyName("friendlyEncryptedAssertionKeys");
        attribute.setNameFormat(Attribute.URI_REFERENCE);

        attribute.getAttributeValues().add(attributeValue);
        return attribute;
    }
}
