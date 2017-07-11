package uk.gov.ida.saml.metadata.transformers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.metadata.Organization;
import org.opensaml.saml.saml2.metadata.OrganizationDisplayName;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.metadata.domain.OrganisationDto;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.builders.metadata.OrganizationBuilder.anOrganization;
import static uk.gov.ida.saml.core.test.builders.metadata.OrganizationDisplayNameBuilder.anOrganizationDisplayName;

@RunWith(OpenSAMLMockitoRunner.class)
public class OrganizationMarshallerTest {

    @Test
    public void transform_shouldTransformOrganization() throws Exception {
        String name = UUID.randomUUID().toString();
        String organizationDisplayName = UUID.randomUUID().toString();

        OrganizationDisplayName displayName = anOrganizationDisplayName().withName(organizationDisplayName).build();
        Organization organisation = anOrganization().withDisplayName(displayName).withName(name).withUrl("/foo").build();
        final OrganisationDto result = new OrganizationMarshaller().toDto(organisation);

        assertThat(result.getDisplayName()).isEqualTo(organizationDisplayName);
    }
}
