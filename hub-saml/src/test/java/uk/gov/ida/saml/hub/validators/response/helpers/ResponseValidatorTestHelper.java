package uk.gov.ida.saml.hub.validators.response.helpers;

import org.assertj.core.util.Strings;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.test.builders.ResponseBuilder;
import uk.gov.ida.saml.core.test.builders.StatusCodeBuilder;

import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;
import static uk.gov.ida.saml.core.test.builders.StatusBuilder.aStatus;
import static uk.gov.ida.saml.core.test.builders.StatusCodeBuilder.aStatusCode;

public class ResponseValidatorTestHelper {
    public static StatusCode createSubStatusCode() {
        return createSubStatusCode(null);
    }

    public static StatusCode createSubStatusCode(String subStatusCodeValue) {
        StatusCodeBuilder subStatusCodeBuilder = aStatusCode();

        return Strings.isNullOrEmpty(subStatusCodeValue)
            ? subStatusCodeBuilder.build()
            : subStatusCodeBuilder.withValue(subStatusCodeValue).build();
    }

    public static Status createStatus(String statusCodeValue) {
        return createStatus(statusCodeValue, null);
    }

    public static Status createStatus(String statusCodeValue, StatusCode subStatusCode) {
        StatusCodeBuilder statusCodeBuilder = aStatusCode().withValue(statusCodeValue);

        StatusCode statusCode = subStatusCode == null
            ? statusCodeBuilder.build()
            : statusCodeBuilder.withSubStatusCode(subStatusCode).build();

        return aStatus().withStatusCode(statusCode).build();
    }

    public static ResponseBuilder getResponseBuilderWithTwoAssertions() {
        return aResponse()
            .addEncryptedAssertion(anAssertion().build())
            .addEncryptedAssertion(anAssertion().build());
    }
}
