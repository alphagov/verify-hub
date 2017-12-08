package uk.gov.ida.hub.samlengine.resources.translators;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.samlengine.domain.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.samlengine.services.CountryMatchingServiceRequestGeneratorService;

import java.io.IOException;

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
    public void generateAttributeQueryIsSuccessful() throws IOException {
        resource.generateAttributeQuery(eidasAttributeQueryRequestDto);

        verify(service).generate(eidasAttributeQueryRequestDto);
        verifyZeroInteractions(service);
    }
}
