package uk.gov.ida.hub.samlsoapproxy.client;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlsoapproxy.logging.ExternalCommunicationEventLogger;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ResponseProcessingException;
import java.net.URI;

import static java.util.Optional.ofNullable;
import static java.text.MessageFormat.format;

public class AttributeQueryRequestClient {

    private static final Logger LOG = LoggerFactory.getLogger(AttributeQueryRequestClient.class);

    public static class MatchingServiceException extends RuntimeException{
        public MatchingServiceException(String message){
            super(message);
        }
        public MatchingServiceException(String message, Exception cause){
            super(message, cause);
        }
    }

    private final SoapRequestClient soapRequestClient;
    private final ExternalCommunicationEventLogger externalCommunicationEventLogger;
    private final MetricRegistry metricsRegistry;

    @Inject
    public AttributeQueryRequestClient(
            SoapRequestClient soapRequestClient,
            ExternalCommunicationEventLogger externalCommunicationEventLogger, MetricRegistry metricsRegistry) {

        this.soapRequestClient = soapRequestClient;
        this.externalCommunicationEventLogger = externalCommunicationEventLogger;
        this.metricsRegistry = metricsRegistry;
    }

    public Element sendQuery(Element matchingServiceRequest, String messageId, SessionId sessionId, URI matchingServiceUri) {
        LOG.info("Sending attribute query to {}", matchingServiceUri);
        Optional<Element> response = sendSingleQuery(
                matchingServiceRequest,
                messageId,
                sessionId,
                matchingServiceUri
        );
        if (response.isPresent()) {
            return response.get();
        }
        throw new MatchingServiceException(format("Attribute query failed"));
    }

    private Optional<Element> sendSingleQuery(Element serialisedQuery, String messageId, SessionId sessionId, URI matchingServiceUri) {
        try {
            externalCommunicationEventLogger.logMatchingServiceRequest(messageId, sessionId, matchingServiceUri);

            // Use a custom timer so that we get separate metrics for each matching service
            final String scope = matchingServiceUri.toString().replace(':', '_').replace('/', '_');
            final Timer timer = metricsRegistry.timer(MetricRegistry.name(AttributeQueryRequestClient.class, "sendSingleQuery", scope));
            final Timer.Context context = timer.time();
            try {
                return ofNullable(soapRequestClient.makeSoapRequest(serialisedQuery, matchingServiceUri));
            } finally {
                context.stop();
            }
        } catch (SOAPRequestError e) {
            if(e.getEntity().isPresent()) {
                final String responseBody = e.getEntity().get();
                LOG.info(format("Error received from MSA (URI '{0}') following HTTP response {1}; response body:\n{2}", matchingServiceUri, e.getResponseStatus(), responseBody));
            }
            throw new MatchingServiceException(format("Matching Service response from {0} was status {1}",
                    matchingServiceUri, e.getResponseStatus()), e);
        } catch (ResponseProcessingException e) {
            LOG.error(format("Matching service attribute query to {0} failed during response processing ({1})",
                    matchingServiceUri, e
                    .getMessage()), e);
            throw new MatchingServiceException("Request to Matching Service Failed during response processing", e);
        } catch (ProcessingException e) {
            LOG.error(format("Matching service attribute query to {0} experienced a connection level failure ({1})", matchingServiceUri, e.getMessage()), e);
            throw new MatchingServiceException("Request to Matching Service Failed At Http Layer", e);
        } catch (Exception e) {
            throw new MatchingServiceException(format("The matching service attribute query to {0} failed for an unknown reason ({1}).", matchingServiceUri, e.getMessage()), e);
        }
    }
}
