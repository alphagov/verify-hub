package uk.gov.ida.saml.metadata.transformers;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import uk.gov.ida.saml.core.DateTimeFreezer;
import uk.gov.ida.saml.core.test.OpenSAMLRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.builders.metadata.EntityDescriptorBuilder.anEntityDescriptor;

@RunWith(OpenSAMLRunner.class)
public class ValidUntilExtractorTest {

    private ValidUntilExtractor extractor;

    @Before
    public void setUp() throws Exception {
        extractor = new ValidUntilExtractor();
    }

    @Test
    public void extract_shouldUseValidUntilToGetExpiryForTheDto() throws Exception {
        DateTimeFreezer.freezeTime();
        EntityDescriptor entityDescriptor = anEntityDescriptor()
                .withValidUntil(DateTime.now())
                .withCacheDuration(1_000L)
                .build();

        DateTime expires = extractor.extract(entityDescriptor);

        org.assertj.jodatime.api.Assertions.assertThat(expires).isEqualTo(DateTime.now());
    }

    @Test
    public void extract_shouldUseCacheDurationToGetExpiryForTheDto() throws Exception {
        DateTimeFreezer.freezeTime();
        EntityDescriptor entityDescriptor = anEntityDescriptor()
                .withValidUntil(null)
                .withCacheDuration(1_000L)
                .build();

        DateTime metadataDto = extractor.extract(entityDescriptor);

        assertThat(metadataDto).isEqualTo(DateTime.now().plus(1_000));
    }

    @After
    public void teardown(){
        DateTimeFreezer.unfreezeTime();
    }
}
