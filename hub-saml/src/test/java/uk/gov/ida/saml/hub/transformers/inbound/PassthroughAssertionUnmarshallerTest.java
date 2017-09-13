package uk.gov.ida.saml.hub.transformers.inbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.domain.FraudDetectedDetails;
import uk.gov.ida.saml.core.domain.PassthroughAssertion;
import uk.gov.ida.saml.core.extensions.EidasAuthnContext;
import uk.gov.ida.saml.core.extensions.IdaAuthnContext;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.transformers.AuthnContextFactory;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.AttributeStatementBuilder.anAttributeStatement;
import static uk.gov.ida.saml.core.test.builders.AuthnContextBuilder.anAuthnContext;
import static uk.gov.ida.saml.core.test.builders.AuthnContextClassRefBuilder.anAuthnContextClassRef;
import static uk.gov.ida.saml.core.test.builders.IPAddressAttributeBuilder.anIPAddress;
import static uk.gov.ida.saml.core.test.builders.IdpFraudEventIdAttributeBuilder.anIdpFraudEventIdAttribute;
import static uk.gov.ida.saml.core.test.builders.AuthnStatementBuilder.anAuthnStatement;
import static uk.gov.ida.saml.core.test.builders.Gpg45StatusAttributeBuilder.aGpg45StatusAttribute;

@RunWith(OpenSAMLMockitoRunner.class)
public class PassthroughAssertionUnmarshallerTest {

    @Mock
    private XmlObjectToBase64EncodedStringTransformer<Assertion> assertionStringTransformer;
    @Mock
    private AuthnContextFactory authnContextFactory;

    private PassthroughAssertionUnmarshaller unmarshaller;

    @Before
    public void setup() {
        unmarshaller = new PassthroughAssertionUnmarshaller(assertionStringTransformer, authnContextFactory);
    }

    @Test
    public void shouldMapEidasLoACorrectly() {
        final AuthnContextClassRef authnContextClassRef = anAuthnContextClassRef().withAuthnContextClasRefValue(EidasAuthnContext.EIDAS_LOA_SUBSTANTIAL).build();
        Assertion theAssertion = anAssertion()
            .addAuthnStatement(anAuthnStatement().withAuthnContext(anAuthnContext().withAuthnContextClassRef(authnContextClassRef).build()).build())
            .buildUnencrypted();
        when(authnContextFactory.mapFromEidasToLoA(EidasAuthnContext.EIDAS_LOA_SUBSTANTIAL)).thenReturn(AuthnContext.LEVEL_2);
        when(assertionStringTransformer.apply(theAssertion)).thenReturn("AUTHN_ASSERTION");

        PassthroughAssertion authnStatementAssertion = unmarshaller.fromAssertion(theAssertion, true);
        assertThat(authnStatementAssertion.getAuthnContext().isPresent()).isEqualTo(true);
        assertThat(authnStatementAssertion.getAuthnContext().get()).isEqualTo(AuthnContext.LEVEL_2);
    }

    @Test
    public void transform_shouldHandleFraudAuthnStatementAndSetThatAssertionIsForFraudulentEventAndSetFraudDetails() throws Exception {
        final AuthnContextClassRef authnContextClassRef = anAuthnContextClassRef().withAuthnContextClasRefValue(IdaAuthnContext.LEVEL_X_AUTHN_CTX).build();
        Assertion theAssertion = anAssertion()
                .addAuthnStatement(anAuthnStatement().withAuthnContext(anAuthnContext().withAuthnContextClassRef(authnContextClassRef).build()).build())
                .addAttributeStatement(anAttributeStatement().addAttribute(anIdpFraudEventIdAttribute().build()).addAttribute(aGpg45StatusAttribute().build()).build())
                .buildUnencrypted();
        when(authnContextFactory.authnContextForLevelOfAssurance(IdaAuthnContext.LEVEL_X_AUTHN_CTX)).thenReturn(AuthnContext.LEVEL_X);
        when(assertionStringTransformer.apply(theAssertion)).thenReturn("AUTHN_ASSERTION");

        PassthroughAssertion authnStatementAssertion = unmarshaller.fromAssertion(theAssertion);

        assertThat(authnStatementAssertion.isFraudulent()).isEqualTo(true);
        assertThat(authnStatementAssertion.getFraudDetectedDetails().isPresent()).isEqualTo(true);
    }

    @Test(expected = IllegalStateException.class)
    public void transform_shouldThrowExceptionWhenFraudIndicatorAuthnStatementDoesNotContainUniqueId() throws Exception {
        Assertion theAssertion = anAssertion()
                .addAuthnStatement(anAuthnStatement()
                        .withAuthnContext(anAuthnContext()
                                .withAuthnContextClassRef(
                                        anAuthnContextClassRef()
                                                .withAuthnContextClasRefValue(IdaAuthnContext.LEVEL_X_AUTHN_CTX)
                                                .build())
                                .build())
                        .build())
                .buildUnencrypted();

        when(authnContextFactory.authnContextForLevelOfAssurance(IdaAuthnContext.LEVEL_X_AUTHN_CTX)).thenReturn(AuthnContext.LEVEL_X);

        when(assertionStringTransformer.apply(theAssertion)).thenReturn("AUTHN_ASSERTION");

        unmarshaller.fromAssertion(theAssertion);
    }

    @Test
    public void transform_shouldTransformTheIdpFraudEventIdForAFraudAssertion() throws Exception {
        String fraudEventId = "Fraud Id";
        Assertion theAssertion = anAssertion()
                .addAuthnStatement(anAuthnStatement()
                        .withAuthnContext(anAuthnContext()
                                .withAuthnContextClassRef(
                                        anAuthnContextClassRef()
                                                .withAuthnContextClasRefValue(IdaAuthnContext.LEVEL_X_AUTHN_CTX)
                                                .build())
                                .build())
                        .build())
                .addAttributeStatement(anAttributeStatement()
                        .addAttribute(anIdpFraudEventIdAttribute().withValue(fraudEventId).build())
                        .addAttribute(aGpg45StatusAttribute().build())
                        .build())
                .buildUnencrypted();

        when(authnContextFactory.authnContextForLevelOfAssurance(IdaAuthnContext.LEVEL_X_AUTHN_CTX)).thenReturn(AuthnContext.LEVEL_X);

        PassthroughAssertion passthroughAssertion = unmarshaller.fromAssertion(theAssertion);

        FraudDetectedDetails fraudDetectedDetails = passthroughAssertion.getFraudDetectedDetails().get();
        assertThat(fraudDetectedDetails.getIdpFraudEventId()).isEqualTo(fraudEventId);

    }

    @Test
    public void transform_shouldTransformTheGpg45StatusIt01ForAFraudAssertion() throws Exception {
        String gpg45Status = "IT01";
        Assertion theAssertion = givenAFraudEventAssertion(gpg45Status);

        PassthroughAssertion passthroughAssertion = unmarshaller.fromAssertion(theAssertion);

        FraudDetectedDetails fraudDetectedDetails = passthroughAssertion.getFraudDetectedDetails().get();
        assertThat(fraudDetectedDetails.getFraudIndicator()).isEqualTo(gpg45Status);
    }

    @Test
    public void transform_shouldTransformTheGpg45StatusFi01ForAFraudAssertion() throws Exception {
        String gpg45Status = "FI01";
        Assertion theAssertion = givenAFraudEventAssertion(gpg45Status);

        PassthroughAssertion passthroughAssertion = unmarshaller.fromAssertion(theAssertion);

        FraudDetectedDetails fraudDetectedDetails = passthroughAssertion.getFraudDetectedDetails().get();
        assertThat(fraudDetectedDetails.getFraudIndicator()).isEqualTo(gpg45Status);
    }

    @Test
    public void transform_shouldTransformTheGpg45StatusDF01ForAFraudAssertion() throws Exception {
        String gpg45Status = "DF01";
        Assertion theAssertion = givenAFraudEventAssertion(gpg45Status);

        PassthroughAssertion passthroughAssertion = unmarshaller.fromAssertion(theAssertion);

        FraudDetectedDetails fraudDetectedDetails = passthroughAssertion.getFraudDetectedDetails().get();
        assertThat(fraudDetectedDetails.getFraudIndicator()).isEqualTo(gpg45Status);
    }

    @Test(expected = IllegalStateException.class)
    public void transform_shouldThrowExceptionIfGpg45StatusIsNotRecognised() throws Exception {
        String gpg45Status = "status not known";
        Assertion theAssertion = givenAFraudEventAssertion(gpg45Status);

        unmarshaller.fromAssertion(theAssertion);
    }

    @Test
    public void transform_shouldNotSetFraudlentFlagForNotFraudulentEvent() throws Exception {
        final AuthnContextClassRef authnContextClassRef = anAuthnContextClassRef().withAuthnContextClasRefValue(IdaAuthnContext.LEVEL_3_AUTHN_CTX).build();
        Assertion theAssertion = anAssertion()
                .addAuthnStatement(anAuthnStatement().withAuthnContext(anAuthnContext().withAuthnContextClassRef(authnContextClassRef).build()).build())
                .buildUnencrypted();

        when(authnContextFactory.authnContextForLevelOfAssurance(IdaAuthnContext.LEVEL_3_AUTHN_CTX)).thenReturn(AuthnContext.LEVEL_3);
        when(assertionStringTransformer.apply(theAssertion)).thenReturn("AUTHN_ASSERTION");

        PassthroughAssertion authnStatementAssertion = unmarshaller.fromAssertion(theAssertion);
        assertThat(authnStatementAssertion.isFraudulent()).isEqualTo(false);
        assertThat(authnStatementAssertion.getFraudDetectedDetails().isPresent()).isEqualTo(false);
    }


    @Test
    public void transform_shouldTransformIpAddress() throws Exception {
        String ipAddy = "1.2.3.4";
        Assertion theAssertion = anAssertion()
                .addAttributeStatement(anAttributeStatement().addAttribute(anIPAddress().withValue(ipAddy).build()).build())
                .buildUnencrypted();

        PassthroughAssertion authnStatementAssertion = unmarshaller.fromAssertion(theAssertion);
        assertThat(authnStatementAssertion.getPrincipalIpAddressAsSeenByIdp().isPresent()).isEqualTo(true);
        assertThat(authnStatementAssertion.getPrincipalIpAddressAsSeenByIdp().get()).isEqualTo(ipAddy);
    }

    private Assertion givenAFraudEventAssertion(final String gpg45Status) {
        Assertion theAssertion = anAssertion()
                .addAuthnStatement(anAuthnStatement()
                        .withAuthnContext(anAuthnContext()
                                .withAuthnContextClassRef(
                                        anAuthnContextClassRef()
                                                .withAuthnContextClasRefValue(IdaAuthnContext.LEVEL_X_AUTHN_CTX)
                                                .build()
                                )
                                .build())
                        .build())
                .addAttributeStatement(
                        anAttributeStatement()
                                .addAttribute(
                                        anIdpFraudEventIdAttribute()
                                                .withValue("my-fraud-event-id")
                                                .build())
                                .addAttribute(
                                        aGpg45StatusAttribute()
                                                .withValue(gpg45Status)
                                                .build())
                                .build())
                .buildUnencrypted();

        when(authnContextFactory.authnContextForLevelOfAssurance(IdaAuthnContext.LEVEL_X_AUTHN_CTX)).thenReturn(AuthnContext.LEVEL_X);
        return theAssertion;
    }
}
