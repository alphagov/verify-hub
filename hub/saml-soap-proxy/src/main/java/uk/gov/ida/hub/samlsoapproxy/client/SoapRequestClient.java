package uk.gov.ida.hub.samlsoapproxy.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.gov.ida.hub.samlsoapproxy.soap.SoapMessageManager;
import uk.gov.ida.hub.samlsoapproxy.soap.SoapResponse;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static java.text.MessageFormat.format;

public class SoapRequestClient {
    private final Client client;
    private final SoapMessageManager soapMessageManager;

    private static final Logger LOG = LoggerFactory.getLogger(SoapRequestClient.class);

    @Inject
    public SoapRequestClient(SoapMessageManager soapMessageManager, @Named("SoapClient") Client client) {
        this.soapMessageManager = soapMessageManager;
        this.client = client;
    }

    public Element makeSoapRequest(Element requestElement, URI uri) throws SOAPRequestError {
        return makePost(uri, requestElement).getBody();
    }

    /**
     * Wrap the supplied element in a SOAP wrapper and post to the specified end-point
     *
     * @return Response from the remote server
     * @throws SOAPRequestError            if the remote server returns a non-200 status code
     * @throws ResponseProcessingException in case processing of a received HTTP response fails (e.g. in a filter
     *                                     or during conversion of the response entity data to an instance
     *                                     of a particular Java type).
     * @throws ProcessingException         in case the request processing or subsequent I/O operation fails.
     */
    protected SoapResponse makePost(URI uri, Element requestElement) throws SOAPRequestError {
        LOG.info(format("Making SOAP request to: {0}", uri));

        Document requestDocument = soapMessageManager.wrapWithSoapEnvelope(requestElement);
        WebTarget target = client.target(uri);
        final Invocation.Builder request = target.request();
        final Response response = request.post(Entity.entity(requestDocument, MediaType.TEXT_XML_TYPE));

        try {
            if (response.getStatus() != 200) {
                LOG.warn(format("Unexpected status code ({0}) when contacting ({1}))", response.getStatus(), uri));
                // let the calling code handle this issue appropriately
                throw new SOAPRequestError(response);
            } else {
                try {
                    return giveMeMySoap(response);
                } catch(BadRequestException e) {
                    LOG.warn(format("Couldn't parse SOAP response when contacting ({0}))", uri), e);
                    throw new SOAPRequestError(response, e);
                }
            }
        } finally {
            // Ensure that response's input stream has been closed.  This may not happen automatically in some cases
            // (e.g. the response body is never read from).
            try {
                response.close();
            } catch (ProcessingException f) {
                LOG.warn("Problem closing Jersey connection.", f);
            }
        }
    }

    private SoapResponse giveMeMySoap(Response response) {
        Document document = response.readEntity(Document.class);
        Element unwrappedSoap = soapMessageManager.unwrapSoapMessage(document);
        return new SoapResponse(unwrappedSoap, response.getStringHeaders());
    }
}
