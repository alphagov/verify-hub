package uk.gov.ida.hub.samlengine.resources.translators;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.samlengine.domain.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.samlengine.services.CountryMatchingServiceRequestGeneratorService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class CountryMatchingServiceRequestGeneratorResourceTest {
    @InjectMocks
    private CountryMatchingServiceRequestGeneratorResource resource;

    @Mock
    private CountryMatchingServiceRequestGeneratorService service;

    @Mock
    private EidasAttributeQueryRequestDto eidasAttributeQueryRequestDto;

    @Test
    public void generateAttributeQueryIsSuccessful() {
        resource.generateAttributeQuery(eidasAttributeQueryRequestDto);

        verify(service).generate(eidasAttributeQueryRequestDto);
        verifyZeroInteractions(service);
    }
}
