package uk.gov.ida.hub.samlengine;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.truststore.TrustStoreConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SamlEngineModuleTest {

    @Mock
    private Binder binder;

    @Mock
    private AnnotatedBindingBuilder builder;

    @Mock
    private ScopedBindingBuilder scopedBindingBuilder;

    @Test
    public void configure() {
        // Given
        SamlEngineModule sem = new SamlEngineModule();
        when(binder.bind(any(Class.class))).thenReturn(builder);
        when(binder.bind(any(TypeLiteral.class))).thenReturn(builder);
        when(builder.toProvider(any(Class.class))).thenReturn(scopedBindingBuilder);

        // When
        sem.configure(binder);

        // Then
        verify(binder).bind(TrustStoreConfiguration.class);
    }
}
