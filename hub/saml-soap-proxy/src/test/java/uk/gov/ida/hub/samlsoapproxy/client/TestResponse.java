package uk.gov.ida.hub.samlsoapproxy.client;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CharSequenceInputStream;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Mimicks the behaviour of a {@link org.glassfish.jersey.client.InboundJaxrsResponse}.
 */
public class TestResponse extends Response {
    private final int status;
    private final CharSequenceInputStream inputStream;

    public TestResponse(int status, String entity) {
        this.status = status;
        this.inputStream = new CharSequenceInputStream(entity, "UTF-8");
    }

    @Override
    public <T> T readEntity(Class<T> entityType) {
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(inputStream, writer, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return (T) writer.toString();
    }

    @Override
    public int getStatus() {
        return status;
    }
    
    @Override
    public boolean hasEntity() {
        try {
            if (inputStream.available() == 0) {
                throw new IllegalStateException("Entity input stream has already been closed");
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return true;
    }

    @Override
    public boolean bufferEntity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        try {
            IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public MediaType getMediaType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Locale getLanguage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public Set<String> getAllowedMethods() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityTag getEntityTag() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getDate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getLastModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public URI getLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Link> getLinks() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasLink(String relation) {
        return false;
    }

    @Override
    public Link getLink(String relation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StatusType getStatusInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getEntity() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public <T> T readEntity(GenericType<T> entityType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
        throw new UnsupportedOperationException();
    }

    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
        throw new UnsupportedOperationException();
    };

    public Link.Builder getLinkBuilder(String relation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHeaderString(String name) {
        throw new UnsupportedOperationException();
    }
}
