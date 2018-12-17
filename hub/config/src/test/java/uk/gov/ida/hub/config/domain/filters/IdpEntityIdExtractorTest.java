package uk.gov.ida.hub.config.domain.filters;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IdpEntityIdExtractorTest {

    @Test
    public void apply_shouldReturnEntityId() throws Exception {
        IdentityProviderConfigEntityData identityProviderConfigEntityData = mock(IdentityProviderConfigEntityData.class);
        String idpEntityId = "idp entity id";
        when(identityProviderConfigEntityData.getEntityId()).thenReturn(idpEntityId);
        IdpEntityIdExtractor idpEntityIdExtractor = new IdpEntityIdExtractor();

        String extractedEntityId = idpEntityIdExtractor.apply(identityProviderConfigEntityData);

        assertThat(extractedEntityId).isEqualTo(idpEntityId);
    }
}
