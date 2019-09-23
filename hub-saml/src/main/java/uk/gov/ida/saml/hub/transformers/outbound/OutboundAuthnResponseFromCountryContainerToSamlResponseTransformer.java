package uk.gov.ida.saml.hub.transformers.outbound;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.AuthnResponseFromCountryContainerDto;
import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.core.extensions.eidas.CountrySamlResponse;
import uk.gov.ida.saml.core.extensions.eidas.EncryptedAssertionKeys;
import uk.gov.ida.saml.core.extensions.eidas.impl.CountrySamlResponseBuilder;
import uk.gov.ida.saml.core.extensions.eidas.impl.EncryptedAssertionKeysBuilder;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class OutboundAuthnResponseFromCountryContainerToSamlResponseTransformer implements Function<AuthnResponseFromCountryContainerDto, Response> {

    private final OpenSamlXmlObjectFactory openSamlXmlObjectFactory;
    private final String HUB_ENTITY_ID = "https://signing.service.gov.uk";

    @Inject
    public OutboundAuthnResponseFromCountryContainerToSamlResponseTransformer(OpenSamlXmlObjectFactory openSamlXmlObjectFactory) {
        this.openSamlXmlObjectFactory = openSamlXmlObjectFactory;
    }
    @Override
    public Response apply(AuthnResponseFromCountryContainerDto countryResponseDto) {
        Response transformedResponse = openSamlXmlObjectFactory.createResponse();

        Issuer issuer = openSamlXmlObjectFactory.createIssuer(HUB_ENTITY_ID);

        transformedResponse.setID(countryResponseDto.getResponseId());
        transformedResponse.setIssuer(issuer);
        transformedResponse.setInResponseTo(countryResponseDto.getInResponseTo());
        transformedResponse.setIssueInstant(DateTime.now());

        Status status = openSamlXmlObjectFactory.createStatus();
        StatusCode topLevelStatusCode = openSamlXmlObjectFactory.createStatusCode();
        DetailedStatusCode detailedStatusCode = DetailedStatusCode.Success;
        topLevelStatusCode.setValue(detailedStatusCode.getStatus());
        status.setStatusCode(topLevelStatusCode);
        transformedResponse.setStatus(status);
//        transformedResponse.setDestination();

        XMLObjectBuilderFactory factory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        Assertion assertion = (Assertion) factory
                .getBuilder(Assertion.DEFAULT_ELEMENT_NAME)
                .buildObject(Assertion.DEFAULT_ELEMENT_NAME, Assertion.TYPE_NAME);

        assertion.setID("somecoolId");


//        XMLObjectBuilder<? extends CountrySamlResponse> typeBuilder = (XMLObjectBuilder<? extends CountrySamlResponse>) XMLObjectSupport.getBuilder(CountrySamlResponse.TYPE_NAME);
        CountrySamlResponse countrySamlResponse = new CountrySamlResponseBuilder().buildObject();
        countrySamlResponse.setCountrySamlResponse(countryResponseDto.getSamlResponse());

        EncryptedAssertionKeys encryptedAssertionKeys = new EncryptedAssertionKeysBuilder().buildObject();
        encryptedAssertionKeys.setEncryptedAssertionKeys(String.join(".", countryResponseDto.getEncryptedKeys()));

        List<AttributeValue> attributeValues = Arrays.asList(countrySamlResponse, encryptedAssertionKeys);

        Attribute attribute = (Attribute) XMLObjectSupport.buildXMLObject(Attribute.DEFAULT_ELEMENT_NAME);
        attribute.setName("countrySamlResponse");
        attribute.setFriendlyName("friendlyCountrySamlResponse");
        attribute.setNameFormat(Attribute.URI_REFERENCE);

//        Attribute attribute = openSamlXmlObjectFactory.createAttribute();
//        attribute.setName("countrySamlResponse");

        attribute.getAttributeValues().addAll(attributeValues);

        AttributeStatement attributeStatement = openSamlXmlObjectFactory.createAttributeStatement();
        attributeStatement.getAttributes().add(attribute);

        assertion.getAttributeStatements().add(attributeStatement);

        transformedResponse.getAssertions().add(assertion);
        return transformedResponse;

    }
}
