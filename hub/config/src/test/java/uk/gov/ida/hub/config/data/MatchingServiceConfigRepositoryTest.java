package uk.gov.ida.hub.config.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConnectedServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteMatchingServiceConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MatchingServiceConfigRepositoryTest {
    @Mock
    private S3ConfigSource s3ConfigSource;

    @Before
    public void setup() throws Exception {
        URL url = this.getClass().getResource("/remote-test-config.json");
        File initialFile = new File(url.getFile());
        InputStream inputStream = new FileInputStream(initialFile);
        ObjectMapper om = new ObjectMapper();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        om.setDateFormat(df);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(RemoteConfigCollection.class, new RemoteConfigCollectionDeserializer());
        om.registerModule(module);
        RemoteConfigCollection remoteConfigCollection = om.readValue(inputStream, RemoteConfigCollection.class);
        when(s3ConfigSource.getRemoteConfig()).thenReturn(remoteConfigCollection);
    }

    @Test
    public void getMatchingServiceWithMatchingEntityId() throws Exception {
        MatchingServiceConfigRepository testObject = new MatchingServiceConfigRepository(s3ConfigSource);
        String testEntityId = "https://msa.bananaregistry.service.gov.uk";
        RemoteMatchingServiceConfig result = testObject.get(testEntityId);
        assertThat(result.getEntityId()).isEqualTo(testEntityId);
    }

    @Test
    public void getMatchingServiceWithNonMatchingEntityId() throws Exception {
        MatchingServiceConfigRepository testObject = new MatchingServiceConfigRepository(s3ConfigSource);
        String testEntityId = "https://msa.missing.service.gov.uk";
        RemoteMatchingServiceConfig result = testObject.get(testEntityId);
        assertThat(result).isNull();
    }
}