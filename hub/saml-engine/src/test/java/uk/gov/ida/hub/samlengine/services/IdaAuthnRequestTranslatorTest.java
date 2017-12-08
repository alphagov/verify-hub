package uk.gov.ida.hub.samlengine.services;

import org.joda.time.DateTime;
import org.junit.Test;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import uk.gov.ida.hub.samlengine.contracts.IdaAuthnRequestFromHubDto;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.hub.domain.IdaAuthnRequestFromHub;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class IdaAuthnRequestTranslatorTest {
    public static final String HUB_ENTITY_ID = "HUB_ENTITY_ID";
    private final IdaAuthnRequestTranslator idaAuthnRequestTranslator = new IdaAuthnRequestTranslator();


    @Test
    public void shouldUseExactComparisonTypeAndLevelsOfAssurance(){
        IdaAuthnRequestFromHubDto idaAuthnRequestFromHubDto = aRequestDto(asList(AuthnContext.LEVEL_2), true);

        IdaAuthnRequestFromHub idaAuthnRequestFromHub = idaAuthnRequestTranslator.getIdaAuthnRequestFromHub(idaAuthnRequestFromHubDto, URI.create("http://example.com"), HUB_ENTITY_ID);

        assertThat(idaAuthnRequestFromHub.getComparisonType()).isEqualTo(AuthnContextComparisonTypeEnumeration.EXACT);
        assertThat(idaAuthnRequestFromHub.getLevelsOfAssurance()).containsSequence(AuthnContext.LEVEL_2);
    }

    @Test
    public void shouldUseMinimumComparisonTypeAndDuplicateSingleLevelOfAssurance(){
        IdaAuthnRequestFromHubDto idaAuthnRequestFromHubDto = aRequestDto(asList(AuthnContext.LEVEL_2), false);

        IdaAuthnRequestFromHub idaAuthnRequestFromHub = idaAuthnRequestTranslator.getIdaAuthnRequestFromHub(idaAuthnRequestFromHubDto, URI.create("http://example.com"), HUB_ENTITY_ID);

        assertThat(idaAuthnRequestFromHub.getComparisonType()).isEqualTo(AuthnContextComparisonTypeEnumeration.MINIMUM);
        assertThat(idaAuthnRequestFromHub.getLevelsOfAssurance()).containsSequence(AuthnContext.LEVEL_2, AuthnContext.LEVEL_2);
    }

    @Test
    public void shouldUseMinimumComparisonTypeAndLevelsOfAssuranceAsIs(){
        IdaAuthnRequestFromHubDto idaAuthnRequestFromHubDto = aRequestDto(asList(AuthnContext.LEVEL_1, AuthnContext.LEVEL_2), false);

        IdaAuthnRequestFromHub idaAuthnRequestFromHub = idaAuthnRequestTranslator.getIdaAuthnRequestFromHub(idaAuthnRequestFromHubDto, URI.create("http://example.com"), HUB_ENTITY_ID);

        assertThat(idaAuthnRequestFromHub.getComparisonType()).isEqualTo(AuthnContextComparisonTypeEnumeration.MINIMUM);
        assertThat(idaAuthnRequestFromHub.getLevelsOfAssurance()).containsSequence(AuthnContext.LEVEL_1, AuthnContext.LEVEL_2);
    }

    @Test
    public void shouldUseMinimumComparisonTypeAndSendDuplicateLOAs(){
        IdaAuthnRequestFromHubDto idaAuthnRequestFromHubDto = aRequestDto(asList(AuthnContext.LEVEL_2), false);

        IdaAuthnRequestFromHub idaAuthnRequestFromHub = idaAuthnRequestTranslator.getIdaAuthnRequestFromHub(idaAuthnRequestFromHubDto, URI.create("http://example.com"), HUB_ENTITY_ID);

        assertThat(idaAuthnRequestFromHub.getComparisonType()).isEqualTo(AuthnContextComparisonTypeEnumeration.MINIMUM);
        assertThat(idaAuthnRequestFromHub.getLevelsOfAssurance()).containsSequence(AuthnContext.LEVEL_2, AuthnContext.LEVEL_2);
    }

    private IdaAuthnRequestFromHubDto aRequestDto(List<AuthnContext> levelsOfAssurance, boolean useExactComparisonType) {
        return new IdaAuthnRequestFromHubDto(
                "1",
                levelsOfAssurance,
                Optional.of(false),
                new DateTime(),
                "idpEntityId",
                useExactComparisonType
        );
    }

}
