package uk.gov.ida.hub.config.domain.filters;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IdpEntityIdExtractorTest {

    @Test
    public void apply_shouldReturnEntityId() throws Exception {
        IdentityProviderConfig identityProviderConfig = mock(IdentityProviderConfig.class);
        String idpEntityId = "idp entity id";
        when(identityProviderConfig.getEntityId()).thenReturn(idpEntityId);
        IdpEntityIdExtractor idpEntityIdExtractor = new IdpEntityIdExtractor();

        String extractedEntityId = idpEntityIdExtractor.apply(identityProviderConfig);

        assertThat(extractedEntityId).isEqualTo(idpEntityId);
    }
}
