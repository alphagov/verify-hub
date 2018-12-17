package uk.gov.ida.hub.samlengine;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import uk.gov.ida.saml.metadata.JerseyClientMetadataResolver;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;
import java.util.Timer;

public class EidasMetadataResolver extends JerseyClientMetadataResolver {
    private final URI metadataUri;

    public EidasMetadataResolver(Timer timer, Client client, URI metadataUri) {
        super(timer, client, metadataUri);
        this.metadataUri = metadataUri;
        initialiseResolver();
    }

    private void initialiseResolver() {
        try {
            BasicParserPool parserPool = new BasicParserPool();
            parserPool.initialize();
            this.setParserPool(parserPool);
            this.setId("dynamic-resolver!");
            this.initialize();
        } catch (ComponentInitializationException e) {
            throw new RuntimeException(e);
        }
    }

    protected byte[] fetchMetadata() {
        try {
            return new Scanner(new URL(metadataUri.toString()).openStream(), "UTF-8").useDelimiter("\\A").next().getBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
