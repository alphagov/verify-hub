package uk.gov.ida.hub.samlengine.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.NameIDType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.samlengine.domain.LevelOfAssurance;
import uk.gov.ida.hub.samlengine.logging.data.AttributeStatementLogData;
import uk.gov.ida.hub.samlengine.logging.data.VerifiedAttributeLogData;
import uk.gov.ida.saml.core.extensions.BaseMdsSamlObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.toList;

/**
 * Verified Attributes Logger is used to capture the properties of the attributes that are returned from the IDP
 * <p>
 * NB: No personally identifiable data is extracted from the assertion attributes
 */
public final class VerifiedAttributesLogger {

    private static final Logger LOG = LoggerFactory.getLogger(VerifiedAttributesLogger.class);

    public static void probeAssertionForVerifiedAttributes(
        Assertion assertion,
        LevelOfAssurance levelOfAssurance
    ) {
        assertion.getAttributeStatements()
            .forEach(attributeStatement -> {
                String issuerValue = Optional.of(assertion.getIssuer()).map(NameIDType::getValue).orElse("");
                try {
                    LOG.info(formatAttributes(issuerValue, levelOfAssurance, attributeStatement.getAttributes()));
                } catch (JsonProcessingException e) {
                    LOG.error(format("Could not generate json object: {0}", e));
                }
            });
    }

    public static String formatAttributes(
        String issuer,
        LevelOfAssurance levelOfAssurance,
        List<Attribute> attributes
    ) throws JsonProcessingException {
        AttributeStatementLogData attributeStatementLogDto = new AttributeStatementLogData(
            issuer,
            levelOfAssurance,
            getAttributesData(attributes)
        );
        return new ObjectMapper().writeValueAsString(attributeStatementLogDto);
    }

    private static Map<String, List<VerifiedAttributeLogData>> getAttributesData(List<Attribute> attributes) {
        Map<String, List<VerifiedAttributeLogData>> result = new HashMap<>();
        for(Attribute attribute : attributes) {
            try {
                result.put(attribute.getName(), getAttributeValuesData(attribute));
            } catch (IllegalStateException ex) {
                LOG.error("Caught an illegal state exception. Tried to add "  + attribute.getName() + " to " + result.keySet().toString());
                throw ex;
            }
        }
        return result;
    }

    private static List<VerifiedAttributeLogData> getAttributeValuesData(Attribute attribute) {
        return attribute.getAttributeValues().stream()
            .map(VerifiedAttributesLogger::getAttributeValueAttributesData)
            .filter(Objects::nonNull)
            .collect(toList());
    }

    private static VerifiedAttributeLogData getAttributeValueAttributesData(XMLObject xmlObject) {
        if (!(xmlObject instanceof BaseMdsSamlObject)) {
            return null;
        }

        BaseMdsSamlObject mdsAttributeValue = (BaseMdsSamlObject) xmlObject;
        return new VerifiedAttributeLogData(
            mdsAttributeValue.getVerified(),
            Optional.ofNullable(mdsAttributeValue.getTo())
                .map(VerifiedAttributesLogger::generateToString)
                .orElse(null)
        );
    }

    private static String generateToString(DateTime toAttributeValue) {
        int days = Days.daysBetween(toAttributeValue.toLocalDate(), DateTime.now().toLocalDate()).getDays();

        if (days > 405) return "more than 405 days";
        if (days < 180) return "less than 180 days";

        return "more than 180 days";
    }
}
