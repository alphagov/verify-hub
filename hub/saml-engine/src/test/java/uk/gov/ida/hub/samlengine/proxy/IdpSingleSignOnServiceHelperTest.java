package uk.gov.ida.hub.samlengine.proxy;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.xmlsec.signature.support.SignatureException;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.saml.core.IdaSamlBootstrap;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_ONE;
import static uk.gov.ida.saml.core.test.builders.metadata.EndpointBuilder.anEndpoint;
import static uk.gov.ida.saml.core.test.builders.metadata.EntityDescriptorBuilder.anEntityDescriptor;
import static uk.gov.ida.saml.core.test.builders.metadata.IdpSsoDescriptorBuilder.anIdpSsoDescriptor;

@RunWith(MockitoJUnitRunner.class)
public class IdpSingleSignOnServiceHelperTest {

    private IdpSingleSignOnServiceHelper idpSingleSignOnServiceHelper;

    private static final String idpEntityId = STUB_IDP_ONE;
    private final URI idpSSOUri = URI.create("http://stub.idp.one/SSO");

    private final MetadataResolver metadataProvider = mock(MetadataResolver.class);

    @Before
    public void setUp() throws ResolverException, MarshallingException, SignatureException {
        IdaSamlBootstrap.bootstrap();
        CriteriaSet criteria = new CriteriaSet(new EntityIdCriterion(idpEntityId));
        EntityDescriptor idpEntityDescriptor = anEntityDescriptor().withIdpSsoDescriptor(anIdpSsoDescriptor().withSingleSignOnService(anEndpoint().withLocation(idpSSOUri.toASCIIString()).buildSingleSignOnService()).build()).build();
        when(metadataProvider.resolveSingle(eq(criteria))).thenReturn(idpEntityDescriptor);
        when(metadataProvider.resolveSingle(not(eq(criteria)))).thenReturn(null);

        idpSingleSignOnServiceHelper = new IdpSingleSignOnServiceHelper(metadataProvider);
    }

    @Test
    public void shouldReturnSSOUriForIDP() {
        final URI singleSignOn = idpSingleSignOnServiceHelper.getSingleSignOn(idpEntityId);
        assertThat(singleSignOn).isEqualTo(idpSSOUri);
    }

    @Test
    public void shouldThrowExceptionIfIDPIsNotContainedInMetadata() {
        try {
            idpSingleSignOnServiceHelper.getSingleSignOn("thisIdpDoesNotExist");
            fail("should have thrown exception");
        } catch(ApplicationException e) {
            assertThat(e.getExceptionType()).isEqualTo(ExceptionType.NOT_FOUND);
        }
    }

    @Test(expected=RuntimeException.class)
    public void shouldThrowExceptionWhenMetadataProviderThrowsOne() throws ResolverException {
        MetadataResolver metadataResolver = mock(MetadataResolver.class);
        when(metadataResolver.resolveSingle(any(CriteriaSet.class))).thenThrow(new ResolverException());
        idpSingleSignOnServiceHelper = new IdpSingleSignOnServiceHelper(metadataResolver);
        idpSingleSignOnServiceHelper.getSingleSignOn(idpEntityId);
    }

}
