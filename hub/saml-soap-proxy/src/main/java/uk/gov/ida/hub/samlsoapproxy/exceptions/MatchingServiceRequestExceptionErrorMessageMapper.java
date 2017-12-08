package uk.gov.ida.hub.samlsoapproxy.exceptions;

import java.util.HashMap;
import java.util.Map;

public final class MatchingServiceRequestExceptionErrorMessageMapper {
    private static Map<String, String> matchingServiceRequestErrorMessages;

    private MatchingServiceRequestExceptionErrorMessageMapper() {
    }

    static {
        matchingServiceRequestErrorMessages = new HashMap<>();
        matchingServiceRequestErrorMessages.put("InvalidSamlRequestInAttributeQueryException", "Incorrect message provided by caller: \n");
        matchingServiceRequestErrorMessages.put("AttributeQueryRequestClient.MatchingServiceException", "Error sending matching service request: ");
        matchingServiceRequestErrorMessages.put("SamlTransformationErrorException", "Response from matching service failed validation: ");
        matchingServiceRequestErrorMessages.put("CertificateChainValidationException", "Problem with the matching service's signing certificate: ");
    }

    public static String getErrorMessageForException(Exception e) {
        String errorMessage = matchingServiceRequestErrorMessages.get(e.getClass().getSimpleName());
        if(errorMessage == null){
            errorMessage = String.format("A %s occurred. Exception message:\n %s", e.getClass().getName(), e.getMessage());
        }
        return errorMessage;
    }
}
