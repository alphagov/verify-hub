package uk.gov.ida.hub.samlengine.logging;

import org.opensaml.saml.saml2.core.Response;
import org.slf4j.MDC;

public class MdcHelper {

    private MdcHelper() {}

    public static void addContextToMdc(final String id, final String issuer) {
        MDC.put("messageId", id);
        MDC.put("entityId", issuer);
        logPrefix("AuthnRequest", id, issuer);
    }

    public static void addContextToMdc(Response response) {
        MDC.put("messageId", response.getID());
        MDC.put("entityId", response.getIssuer().getValue());
        String messageType = "Response";
        logPrefix(messageType, response.getID(), response.getIssuer().getValue());
    }

    private static void logPrefix(String messageType, String messageId, String entityId) {
        MDC.put("logPrefix", "[" + messageType + " " + messageId + " from " + entityId + "] ");
    }
}
