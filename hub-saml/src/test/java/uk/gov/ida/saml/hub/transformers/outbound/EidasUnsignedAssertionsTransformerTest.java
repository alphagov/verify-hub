package uk.gov.ida.saml.hub.transformers.outbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import uk.gov.ida.saml.core.IdaConstants;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;
import uk.gov.ida.saml.core.domain.PersistentId;
import uk.gov.ida.saml.core.extensions.eidas.CountrySamlResponse;
import uk.gov.ida.saml.core.extensions.eidas.EncryptedAssertionKeys;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.transformers.AuthnContextFactory;
import uk.gov.ida.saml.hub.domain.HubEidasAttributeQueryRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.extensions.EidasAuthnContext.EIDAS_LOA_SUBSTANTIAL;
import static uk.gov.ida.saml.core.extensions.IdaAuthnContext.LEVEL_2_AUTHN_CTX;

@RunWith(OpenSAMLMockitoRunner.class)
public class EidasUnsignedAssertionsTransformerTest {

    private EidasUnsignedAssertionsTransformer transformer;

    @Mock
    private HubEidasAttributeQueryRequest attributeQueryRequest;

    @Mock
    private CountrySignedResponseContainer countrySignedResponseContainer;

    @Mock
    private PersistentId persistentId;

    @Mock
    private AuthnContext authnContext;

    @Before
    public void setUp() throws Exception {
        transformer = new EidasUnsignedAssertionsTransformer(new OpenSamlXmlObjectFactory(), new AuthnContextFactory(), "hub connector entity id");
    }

    @Test
    public void shouldCreateAnUnsignedAssertionContainingOriginalEidasSamlResponseAndEncryptedKeys() {
        when(attributeQueryRequest.getCountrySignedResponseContainer()).thenReturn(Optional.of(countrySignedResponseContainer));
        when(attributeQueryRequest.getId()).thenReturn("attributeQueryRequest id");
        when(attributeQueryRequest.getPersistentId()).thenReturn(persistentId);
        when(attributeQueryRequest.getAuthnContext()).thenReturn(authnContext);
        when(authnContext.getUri()).thenReturn(LEVEL_2_AUTHN_CTX);
        when(persistentId.getNameId()).thenReturn("persistentId name id");
        when(countrySignedResponseContainer.getCountryEntityId()).thenReturn("a country entity id");
        when(countrySignedResponseContainer.getBase64SamlResponse()).thenReturn("original unsigned eidas saml response");
        when(countrySignedResponseContainer.getBase64encryptedKeys()).thenReturn(List.of("an encrypted key"));

        Assertion assertion = transformer.transform(attributeQueryRequest);

        Conditions conditions = assertion.getConditions();
        List<AudienceRestriction> audienceRestrictions = conditions.getAudienceRestrictions();
        assertThat(audienceRestrictions.size()).isEqualTo(1);
        List<Audience> audiences = audienceRestrictions.get(0).getAudiences();
        assertThat(audiences.size()).isEqualTo(1);
        Audience audience = audiences.get(0);
        assertThat(audience.getAudienceURI()).isEqualTo("hub connector entity id");
        assertThat(conditions.getNotOnOrAfter()).isNotNull();
        assertThat(conditions.getNotBefore()).isNotNull();
        assertThat(conditions.getNotBefore()).isLessThan(conditions.getNotOnOrAfter());

        assertThat(assertion.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getInResponseTo()).isEqualTo("attributeQueryRequest id");
        List<AuthnStatement> authnStatements = assertion.getAuthnStatements();
        assertThat(authnStatements.size()).isEqualTo(1);
        AuthnStatement authnStatement = authnStatements.iterator().next();
        assertThat(authnStatement.getAuthnContext().getAuthnContextClassRef().getAuthnContextClassRef()).isEqualTo(EIDAS_LOA_SUBSTANTIAL);
        List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
        assertThat(attributeStatements.size()).isEqualTo(1);
        AttributeStatement attributeStatement = attributeStatements.get(0);
        List<Attribute> attributes = attributeStatement.getAttributes();
        assertThat(attributeStatement.getEncryptedAttributes().size()).isEqualTo(0);
        assertThat(attributes.size()).isEqualTo(2);

        {
            Attribute samlResponse = attributes.get(0);
            assertThat(samlResponse.getName()).isEqualTo(IdaConstants.Eidas_Attributes.UnsignedAssertions.EidasSamlResponse.NAME);
            assertThat(samlResponse.getAttributeValues().size()).isEqualTo(1);
            CountrySamlResponse countrySamlResponseValue = (CountrySamlResponse) samlResponse.getAttributeValues().get(0);
            assertThat(countrySamlResponseValue.getValue()).isEqualTo("original unsigned eidas saml response");

        }

        {
            Attribute encryptedKeys = attributes.get(1);
            assertThat(encryptedKeys.getName()).isEqualTo(IdaConstants.Eidas_Attributes.UnsignedAssertions.EncryptedSecretKeys.NAME);
            assertThat(encryptedKeys.getAttributeValues().size()).isEqualTo(1);
            EncryptedAssertionKeys encryptedAssertionKeys = (EncryptedAssertionKeys) encryptedKeys.getAttributeValues().get(0);
            assertThat(encryptedAssertionKeys.getValue()).isEqualTo("an encrypted key");
        }
    }
}