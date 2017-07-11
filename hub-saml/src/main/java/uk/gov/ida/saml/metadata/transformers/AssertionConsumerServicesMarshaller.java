package uk.gov.ida.saml.metadata.transformers;

import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import uk.gov.ida.saml.metadata.domain.AssertionConsumerServiceEndpointDto;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.ida.saml.metadata.domain.AssertionConsumerServiceEndpointDto.createAssertionConsumerService;


public class AssertionConsumerServicesMarshaller {
    public List<AssertionConsumerServiceEndpointDto> toDto(List<AssertionConsumerService> assertionConsumerServices) {
        List<AssertionConsumerServiceEndpointDto> transformedList = new ArrayList<>();

        for (AssertionConsumerService assertionConsumerService : assertionConsumerServices) {
            URI location = URI.create(assertionConsumerService.getLocation());
            Boolean isDefault = assertionConsumerService.isDefault();
            Integer index = assertionConsumerService.getIndex();
            transformedList.add(createAssertionConsumerService(location, isDefault, index));
        }

        return transformedList;
    }
}
