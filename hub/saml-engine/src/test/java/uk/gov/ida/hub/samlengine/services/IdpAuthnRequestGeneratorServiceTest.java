package uk.gov.ida.hub.samlengine.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.hub.samlengine.contracts.IdaAuthnRequestFromHubDto;
import uk.gov.ida.hub.samlengine.domain.SamlRequestDto;
import uk.gov.ida.hub.samlengine.proxy.IdpSingleSignOnServiceHelper;
import uk.gov.ida.saml.hub.domain.IdaAuthnRequestFromHub;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IdpAuthnRequestGeneratorServiceTest {

    private  static final String HUB_ENTITY_ID = "HUB_ENTITY_ID";

    private IdpAuthnRequestGeneratorService idpAuthnRequestGeneratorService;

    @Mock
    private Function<IdaAuthnRequestFromHub, String> idaAuthnRequestFromHubStringTransformer;

    @Mock
    private IdpSingleSignOnServiceHelper idpSingleSignOnServiceHelper;

    @Mock
    private IdaAuthnRequestTranslator idaAuthnRequestTranslator;

    @BeforeEach
    public void setUp() {
        idpAuthnRequestGeneratorService = new IdpAuthnRequestGeneratorService(idpSingleSignOnServiceHelper, idaAuthnRequestFromHubStringTransformer, idaAuthnRequestTranslator, HUB_ENTITY_ID);
    }

    @Test
    public void get_sendAuthnRequest_shouldReturnDtoWithSamlRequestAndPostLocation(){
        String idpEntityId = UUID.randomUUID().toString();
        IdaAuthnRequestFromHubDto dto = new IdaAuthnRequestFromHubDto("1", null, Optional.of(false), null, idpEntityId, false);
        String samlRequest = "samlRequest";
        URI ssoUri = UriBuilder.fromPath(UUID.randomUUID().toString()).build();
        when(idaAuthnRequestFromHubStringTransformer.apply(ArgumentMatchers.any())).thenReturn(samlRequest);
        when(idpSingleSignOnServiceHelper.getSingleSignOn(idpEntityId)).thenReturn(ssoUri);
        when(idaAuthnRequestTranslator.getIdaAuthnRequestFromHub(dto, ssoUri, HUB_ENTITY_ID)).thenReturn(null);

        final SamlRequestDto output = idpAuthnRequestGeneratorService.generateSaml(dto);

        assertThat(output.getSamlRequest()).isEqualTo(samlRequest);
        assertThat(output.getSsoUri()).isEqualTo(ssoUri);
    }

}
