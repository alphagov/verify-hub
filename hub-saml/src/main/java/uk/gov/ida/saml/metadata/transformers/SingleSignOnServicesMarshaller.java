package uk.gov.ida.saml.metadata.transformers;

import java.util.List;
import com.google.common.collect.Lists;

import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import uk.gov.ida.saml.metadata.domain.SamlEndpointDto;

import static java.util.stream.Collectors.toList;

public class SingleSignOnServicesMarshaller {

    private EndpointMarshaller endpointMarshaller;

    public SingleSignOnServicesMarshaller(EndpointMarshaller endpointMarshaller) {
        this.endpointMarshaller = endpointMarshaller;
    }

    public List<SamlEndpointDto> toDto(List<SingleSignOnService> singleSignOnServices) {
        return singleSignOnServices.stream()
            .map(item -> endpointMarshaller.toDto(item))
            .collect(toList());
    }
}
