package uk.gov.ida.hub.samlengine.services;

import io.prometheus.client.Gauge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA1;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.impl.SignatureImpl;
import uk.gov.ida.hub.samlengine.builders.BuilderHelper;
import uk.gov.ida.hub.samlengine.builders.SamlAuthnRequestDtoBuilder;
import uk.gov.ida.hub.samlengine.builders.TranslatedAuthnRequestDtoBuilder;
import uk.gov.ida.hub.samlengine.contracts.SamlRequestWithAuthnRequestInformationDto;
import uk.gov.ida.hub.samlengine.contracts.TranslatedAuthnRequestDto;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;
import uk.gov.ida.saml.core.test.builders.AuthnRequestBuilder;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.domain.AuthnRequestFromRelyingParty;
import uk.gov.ida.saml.hub.transformers.inbound.AuthnRequestToIdaRequestFromRelyingPartyTransformer;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.samlengine.builders.AuthnRequestFromRelyingPartyBuilder.anAuthnRequestFromRelyingParty;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.builders.SignatureBuilder.aSignature;

@ExtendWith(OpenSAMLExtension.class)
@ExtendWith(MockitoExtension.class)
public class RpAuthnRequestTranslatorServiceTest {

    private static final SignatureRSASHA1 SIGNATURE_ALGORITHM = new SignatureRSASHA1();

    @Mock
    private StringToOpenSamlObjectTransformer<AuthnRequest> stringToAuthnRequestTransformer;

    @Mock
    private AuthnRequestToIdaRequestFromRelyingPartyTransformer samlAuthnRequestToAuthnRequestFromRelyingPartyTransformer;

    @Mock
    private Gauge vspVersionGauge;

    @Mock
    private Gauge.Child childGauge;

    @Test
    public void shouldTranslateSamlAuthnRequest()  {
        RpAuthnRequestTranslatorService service = new RpAuthnRequestTranslatorService(
                stringToAuthnRequestTransformer,
                samlAuthnRequestToAuthnRequestFromRelyingPartyTransformer,
                vspVersionGauge
            );

        boolean forceAuthentication = true;
        String id = UUID.randomUUID().toString();
        String issuer = UUID.randomUUID().toString();
        URI assertionConsumerServiceUrl = URI.create("http://someassertionuri");
        int assertionConsumerServiceIndex = 1;
        Signature signature = aSignature().withSignatureAlgorithm(SIGNATURE_ALGORITHM).build();
        ((SignatureImpl) signature).setXMLSignature(BuilderHelper.createXMLSignature(SIGNATURE_ALGORITHM, new DigestSHA256()));

        SamlRequestWithAuthnRequestInformationDto samlRequestWithAuthnRequestInformationDto = SamlAuthnRequestDtoBuilder.aSamlAuthnRequest()
                .withId(id)
                .withIssuer(issuer)
                .withForceAuthentication(forceAuthentication)
                .withAssertionConsumerIndex(assertionConsumerServiceIndex)
                .withPublicCert(TEST_RP_PUBLIC_SIGNING_CERT)
                .withPrivateKey(TEST_RP_PRIVATE_SIGNING_KEY)
                .build();

        AuthnRequest authnRequest = AuthnRequestBuilder.anAuthnRequest().build();

        TranslatedAuthnRequestDto expected = TranslatedAuthnRequestDtoBuilder.aTranslatedAuthnRequest()
                .withId(id)
                .withIssuer(issuer)
                .withForceAuthentication(forceAuthentication)
                .withAssertionConsumerServiceUrl(assertionConsumerServiceUrl)
                .withAssertionConsumerServiceIndex(assertionConsumerServiceIndex)
                .build();

        AuthnRequestFromRelyingParty intermediateBlah = anAuthnRequestFromRelyingParty()
                .withId(id)
                .withIssuer(issuer)
                .withForceAuthentication(forceAuthentication)
                .withAssertionConsumerServiceUrl(assertionConsumerServiceUrl)
                .withAssertionConsumerServiceIndex(assertionConsumerServiceIndex)
                .withSignature(signature)
                .build();

        when(stringToAuthnRequestTransformer.apply(samlRequestWithAuthnRequestInformationDto.getSamlMessage())).thenReturn(authnRequest);
        when(samlAuthnRequestToAuthnRequestFromRelyingPartyTransformer.apply(authnRequest)).thenReturn(intermediateBlah);
        when(vspVersionGauge.labels(anyString(), anyString())).thenReturn(childGauge);

        TranslatedAuthnRequestDto actual = service.translate(samlRequestWithAuthnRequestInformationDto);

        assertThat(actual).isEqualToComparingFieldByField(expected);

        verify(vspVersionGauge).labels(
                intermediateBlah.getIssuer(),
                intermediateBlah.getVerifyServiceProviderVersion().get()
            );
        verify(childGauge).set(1.0);
    }
}
