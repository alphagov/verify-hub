package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class MatchingServiceConfigDeserializationTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp(){
        objectMapper = new ObjectMapper(new YAMLFactory());
    }

    @Test
    public void matchingServiceConfigIsDeserialized() throws IOException {
        File initialFile = new File(ResourceHelpers.resourceFilePath("test-rp-ms.yml"));
        InputStream inputStream = new FileInputStream(initialFile);
        MatchingServiceConfig msConfig = objectMapper.readValue(inputStream, MatchingServiceConfig.class);
        assertThat(msConfig.getEntityId()).isEqualTo("http://www.test-rp-ms.gov.uk/SAML2/MD");
        assertThat(msConfig.getEncryptionCertificate()).isNotNull();
        assertThat(msConfig.getSignatureVerificationCertificates()).hasSize(2);
    }

}
