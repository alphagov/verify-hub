package uk.gov.ida.hub.samlsoapproxy.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.samlsoapproxy.rest.HealthCheckResponse;
import uk.gov.ida.hub.samlsoapproxy.soap.SoapMessageManager;
import uk.gov.ida.hub.samlsoapproxy.soap.SoapResponse;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import java.net.URI;
import java.text.MessageFormat;
import java.util.UUID;

public class HealthCheckSoapRequestClient extends SoapRequestClient {
    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckSoapRequestClient.class);

    @Inject
    public HealthCheckSoapRequestClient(SoapMessageManager soapMessageManager, @Named("HealthCheckClient") Client client) {
        super(soapMessageManager, client);
    }

    public HealthCheckResponse makeSoapRequestForHealthCheck(Element requestElement, URI uri) {
        LOG.info(MessageFormat.format("Making SOAP request to: {0}", uri));

        try {
            SoapResponse response = makePost(uri, requestElement);
            return new HealthCheckResponse(response.getBody());
        } catch(ProcessingException e) {
            throw ApplicationException.createUnauditedException(ExceptionType.NETWORK_ERROR, UUID.randomUUID(), e);
        } catch (SOAPRequestError e) {
            throw ApplicationException.createUnauditedException(ExceptionType.REMOTE_SERVER_ERROR, UUID.randomUUID(), e);
        }
    }

}
