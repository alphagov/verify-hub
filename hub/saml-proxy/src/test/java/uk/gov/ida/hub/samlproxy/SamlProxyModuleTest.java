package uk.gov.ida.hub.samlproxy;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.truststore.TrustStoreConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SamlProxyModuleTest {

    @Mock
    private Binder binder;

    @Mock
    private AnnotatedBindingBuilder builder;

    @Mock
    private ScopedBindingBuilder scopedBindingBuilder;

    @Test
    public void configure() {
        // Given
        SamlProxyModule sem = new SamlProxyModule();
        when(binder.bind(any(Class.class))).thenReturn(builder);
        when(binder.bind(any(TypeLiteral.class))).thenReturn(builder);
        when(builder.toProvider(any(Class.class))).thenReturn(scopedBindingBuilder);

        // When
        sem.configure(binder);

        // Then
        verify(binder).bind(TrustStoreConfiguration.class);
    }
}
