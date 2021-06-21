package uk.gov.ida.hub.samlproxy.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SamlProxyExceptionMapperTest {
    @Mock
    private LevelLogger levelLogger;

    private SamlProxyExceptionMapper exceptionMapper;

    @Mock
    private LevelLoggerFactory<SamlProxyExceptionMapper> levelLoggerFactory;

    @BeforeEach
    public void setUp() {
        when(levelLoggerFactory.createLevelLogger(SamlProxyExceptionMapper.class)).thenReturn(levelLogger);
        exceptionMapper = new SamlProxyExceptionMapper(null, levelLoggerFactory);
    }

    @Test
    public void shouldCreateAuditedErrorResponse() {
        Response response = exceptionMapper.handleException(new RuntimeException());

        ErrorStatusDto responseEntity = (ErrorStatusDto) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertThat(responseEntity.isAudited()).isTrue();
    }

    @Test
    public void shouldLogExceptionAtErrorLevel() {
        RuntimeException exception = new RuntimeException();
        exceptionMapper.handleException(exception);

        verify(levelLogger).log(eq(Level.ERROR), eq(exception), any(UUID.class));
    }
}
