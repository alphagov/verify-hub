package uk.gov.ida.saml.hub.transformers.outbound;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.extensions.IdaAuthnContext;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.hub.domain.IdaAuthnRequestFromHub;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.jodatime.api.Assertions.assertThat;
import static uk.gov.ida.saml.hub.test.builders.IdaAuthnRequestBuilder.anIdaAuthnRequest;

@RunWith(OpenSAMLMockitoRunner.class)
public class IdaAuthnRequestFromHubToAuthnRequestTransformerTest {
    private IdaAuthnRequestFromHubToAuthnRequestTransformer transformer;

    @Before
    public void setup() {
        transformer = new IdaAuthnRequestFromHubToAuthnRequestTransformer(new OpenSamlXmlObjectFactory());
    }

    @Test
    public void shouldUseTheOriginalRequestIdForTheTransformedRequest() {
        String originalRequestId = UUID.randomUUID().toString();
        IdaAuthnRequestFromHub originalRequestFromHub = anIdaAuthnRequest().withId(originalRequestId).buildFromHub();

        AuthnRequest transformedRequest = transformer.apply(originalRequestFromHub);

        assertThat(transformedRequest.getID()).isEqualTo(originalRequestId);
    }

    @Test
    public void shouldUseTheOriginalExpiryTimestampToSetTheNotOnOrAfter() {
        DateTime sessionExpiry = DateTime.now().plusHours(2);
        IdaAuthnRequestFromHub originalRequestFromHub = anIdaAuthnRequest().withSessionExpiryTimestamp(sessionExpiry).buildFromHub();

        AuthnRequest transformedRequest = transformer.apply(originalRequestFromHub);
        assertThat(transformedRequest.getConditions().getNotOnOrAfter()).isEqualTo(sessionExpiry);
    }

    @Test
    public void shouldUseTheOriginalRequestIssuerIdForTheTransformedRequest() {
        String originalIssuerId = UUID.randomUUID().toString();
        IdaAuthnRequestFromHub originalRequestFromHub = anIdaAuthnRequest().withIssuer(originalIssuerId).buildFromHub();

        AuthnRequest transformedRequest = transformer.apply(originalRequestFromHub);

        assertThat(transformedRequest.getIssuer().getValue()).isEqualTo(originalIssuerId);
    }

    @Test
    public void shouldCreateAProxyElementWithAProxyCountOfZeroInTheTransformedRequest() {
        AuthnRequest transformedRequest = transformer.apply(anIdaAuthnRequest().buildFromHub());

        assertThat(transformedRequest.getScoping().getProxyCount()).isEqualTo(0);
    }

    @Test
    public void shouldCreateANameIdPolicyElementWithAFormatOfPersistentInTheTransformedRequest() {
        AuthnRequest transformedRequest = transformer.apply(anIdaAuthnRequest().buildFromHub());

        assertThat(transformedRequest.getNameIDPolicy().getFormat()).isEqualTo(NameIDType.PERSISTENT);
    }
    
    @Test
    public void shouldCorrectlyMapLevelsOfAssurance() {
        List<AuthnContext> levelsOfAssurance = Arrays.asList(AuthnContext.LEVEL_1, AuthnContext.LEVEL_2);
        List<String> expected = Arrays.asList(IdaAuthnContext.LEVEL_1_AUTHN_CTX, IdaAuthnContext.LEVEL_2_AUTHN_CTX);


        IdaAuthnRequestFromHub originalRequestFromHub = anIdaAuthnRequest()
                .withLevelsOfAssurance(levelsOfAssurance).buildFromHub();
        AuthnRequest transformedRequest = transformer.apply(originalRequestFromHub);
        RequestedAuthnContext requestedAuthnContext = transformedRequest.getRequestedAuthnContext();

        List<String> actual = requestedAuthnContext.getAuthnContextClassRefs().stream()
                .map(AuthnContextClassRef::getAuthnContextClassRef)
                .collect(Collectors.toList());

        assertThat(actual).containsAll(expected);
    }

    @Test
    public void shouldPropagateComparisonType() {
        IdaAuthnRequestFromHub originalRequestFromHub = anIdaAuthnRequest()
                .withComparisonType(AuthnContextComparisonTypeEnumeration.MINIMUM)
                .buildFromHub();
        AuthnRequest transformedRequest = transformer.apply(originalRequestFromHub);

        RequestedAuthnContext requestedAuthnContext = transformedRequest.getRequestedAuthnContext();

        assertThat(requestedAuthnContext.getComparison()).isEqualTo(AuthnContextComparisonTypeEnumeration.MINIMUM);
    }

    @Test
    public void shouldMaintainTheAuthnContextsInPreferenceOrder() {
        IdaAuthnRequestFromHub originalRequestFromHub = anIdaAuthnRequest()
                .withLevelsOfAssurance(Arrays.asList(AuthnContext.LEVEL_1, AuthnContext.LEVEL_2))
                .buildFromHub();
        AuthnRequest transformedRequest = transformer.apply(originalRequestFromHub);

        RequestedAuthnContext requestedAuthnContext = transformedRequest.getRequestedAuthnContext();

        List<AuthnContextClassRef> authnContextClassRefs = requestedAuthnContext.getAuthnContextClassRefs();
        List<String> authnContexts = authnContextClassRefs.stream()
                .map(AuthnContextClassRef::getAuthnContextClassRef).collect(Collectors.toList());

        assertThat(authnContexts).containsSequence(IdaAuthnContext.LEVEL_1_AUTHN_CTX, IdaAuthnContext.LEVEL_2_AUTHN_CTX);
    }

    @Test
    public void shouldSetAllowCreateToTrue() {
        IdaAuthnRequestFromHub originalRequestFromHub = anIdaAuthnRequest().buildFromHub();
        AuthnRequest transformedRequest = transformer.apply(originalRequestFromHub);

        NameIDPolicy nameIDPolicy = transformedRequest.getNameIDPolicy();

        assertThat(nameIDPolicy.getAllowCreate()).isEqualTo(true);
    }

    @Test
    public void shouldSetForceAuthnToTrue() {
        IdaAuthnRequestFromHub originalRequestFromTransaction = anIdaAuthnRequest()
                .withForceAuthentication(Optional.of(true))
                .buildFromHub();

        AuthnRequest transformedRequest = transformer.apply(originalRequestFromTransaction);

        assertThat(transformedRequest.isForceAuthn()).isEqualTo(true);

    }

    @Test
    public void shouldSetForceAuthnToFalse() {
        IdaAuthnRequestFromHub originalRequestFromTransaction = anIdaAuthnRequest()
                .withForceAuthentication(Optional.of(false))
                .buildFromHub();

        AuthnRequest transformedRequest = transformer.apply(originalRequestFromTransaction);

        assertThat(transformedRequest.isForceAuthn()).isEqualTo(false);

        originalRequestFromTransaction = anIdaAuthnRequest()
                .withForceAuthentication(Optional.empty())
                .buildFromHub();

        transformedRequest = transformer.apply(originalRequestFromTransaction);

        assertThat(transformedRequest.isForceAuthn()).isEqualTo(false);

    }

    @Test
    public void shouldSetProtocolBindingToPost() {
        IdaAuthnRequestFromHub originalRequestFromTransaction = anIdaAuthnRequest()
            .buildFromHub();

        AuthnRequest transformedRequest = transformer.apply(originalRequestFromTransaction);

        assertThat(transformedRequest.getProtocolBinding()).isEqualTo(SAMLConstants.SAML2_POST_BINDING_URI);
    }
}
