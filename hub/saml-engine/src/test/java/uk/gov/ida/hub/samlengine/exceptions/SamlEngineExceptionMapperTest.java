package uk.gov.ida.hub.samlengine.exceptions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.event.Level;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.hub.exception.SamlDuplicateRequestIdException;
import uk.gov.ida.saml.hub.exception.SamlRequestTooOldException;
import uk.gov.ida.saml.metadata.exceptions.NoKeyConfiguredForEntityException;
import uk.gov.ida.saml.security.exception.SamlFailedToDecryptException;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.exceptions.ApplicationException.createAuditedException;
import static uk.gov.ida.exceptions.ApplicationException.createUnauditedException;
import static uk.gov.ida.hub.samlengine.builders.SamlTransformationFailureExceptionBuilder.aSamlTransformationFailureException;

@RunWith(MockitoJUnitRunner.class)
public class SamlEngineExceptionMapperTest {

    SamlEngineExceptionMapper samlEngineExceptionMapper;

    @Mock
    private LevelLoggerFactory<SamlEngineExceptionMapper> levelLoggerFactory;
    @Mock
    private LevelLogger levelLogger;

    @Before
    public void setUp() {
        when(levelLoggerFactory.createLevelLogger(SamlEngineExceptionMapper.class)).thenReturn(levelLogger);
        samlEngineExceptionMapper = new SamlEngineExceptionMapper(levelLoggerFactory);
    }

    @Test
    public void toResponse_shouldReturnAuditedErrorResponseWhenExceptionHasBeenAuditedAlready() throws Exception {
        ApplicationException applicationException = createAuditedException(ExceptionType.INVALID_SAML, UUID.randomUUID());

        final Response response = samlEngineExceptionMapper.toResponse(applicationException);

        final ErrorStatusDto entity = (ErrorStatusDto) response.getEntity();
        assertThat(entity.isAudited()).isTrue();

        checkLogLevel(applicationException.getExceptionType().getLevel());
    }

    @Test
    public void toResponse_shouldReturnUnauditedErrorResponseWhenExceptionHasNotBeenAudited() throws Exception {
        ApplicationException applicationException = createUnauditedExceptionThatShouldNotBeAudited();

        final Response response = samlEngineExceptionMapper.toResponse(applicationException);

        final ErrorStatusDto entity = (ErrorStatusDto) response.getEntity();
        assertThat(entity.isAudited()).isEqualTo(false);

        checkLogLevel(applicationException.getExceptionType().getLevel());
    }

    @Test
    public void shouldPassthroughErrorIdAndExceptionType() throws Exception {
        UUID errorId = UUID.randomUUID();
        final ExceptionType exceptionType = ExceptionType.IDA_SOAP;
        ApplicationException applicationException = createUnauditedException(exceptionType, errorId);

        final Response response = samlEngineExceptionMapper.toResponse(applicationException);
        assertThat(response.hasEntity()).isTrue();
        ErrorStatusDto errorStatusDto = (ErrorStatusDto) response.getEntity();
        assertThat(errorStatusDto.getErrorId()).isEqualTo(errorId);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(exceptionType);

        checkLogLevel(applicationException.getExceptionType().getLevel());
    }

    @Test
    public void toResponse_shouldCreateResponseWithUnauditedErrorStatus() throws Exception {
        SamlTransformationErrorException exception = aSamlTransformationFailureException().build();

        Response response = samlEngineExceptionMapper.toResponse(exception);

        assertThat(response.getEntity()).isNotNull();
        final ErrorStatusDto errorStatusDto = (ErrorStatusDto) response.getEntity();
        assertThat(errorStatusDto.isAudited()).isEqualTo(false);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);

        checkLogLevel(exception.getLogLevel());
    }

    @Test
    public void shouldCreateUnauditedErrorResponse() throws Exception {
        final SamlTransformationErrorException exception = new SamlTransformationErrorException("error", new RuntimeException(), Level.DEBUG);
        Response response = samlEngineExceptionMapper.toResponse(exception);

        ErrorStatusDto responseEntity = (ErrorStatusDto) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(responseEntity.isAudited()).isFalse();

        checkLogLevel(exception.getLogLevel());
    }

    @Test
    public void shouldHandleSamlTransformationErrorExceptionCorrectly() throws Exception {
        SamlTransformationErrorException exception = new SamlTransformationErrorException("error", new RuntimeException(), Level.DEBUG);

        final Response response = samlEngineExceptionMapper.toResponse(exception);

        ErrorStatusDto responseEntity = (ErrorStatusDto) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(responseEntity.isAudited()).isFalse();
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);

        checkLogLevel(exception.getLogLevel());
    }

    @Test
    public void shouldHandleSamlFailedToDecryptErrorExceptionCorrectly() throws Exception {
        SamlTransformationErrorException exception = new SamlFailedToDecryptException("error", new RuntimeException(), Level.DEBUG);

        final Response response = samlEngineExceptionMapper.toResponse(exception);

        ErrorStatusDto responseEntity = (ErrorStatusDto) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(responseEntity.isAudited()).isFalse();
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML_FAILED_TO_DECRYPT);

        checkLogLevel(exception.getLogLevel());
    }

    @Test
    public void shouldHandleSamlRequestTooOldExceptionCorrectly() throws Exception {
        SamlTransformationErrorException exception = new SamlRequestTooOldException("error", new RuntimeException(), Level.DEBUG);

        final Response response = samlEngineExceptionMapper.toResponse(exception);

        ErrorStatusDto responseEntity = (ErrorStatusDto) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(responseEntity.isAudited()).isFalse();
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML_REQUEST_TOO_OLD);

        checkLogLevel(exception.getLogLevel());
    }

    @Test
    public void shouldHandleSamlDuplicateRequestIdExceptionCorrectly() throws Exception {
        SamlTransformationErrorException exception = new SamlDuplicateRequestIdException("error", new RuntimeException(), Level.DEBUG);

        final Response response = samlEngineExceptionMapper.toResponse(exception);

        ErrorStatusDto responseEntity = (ErrorStatusDto) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(responseEntity.isAudited()).isFalse();
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML_DUPLICATE_REQUEST_ID);

        checkLogLevel(exception.getLogLevel());
    }

    @Test
    public void shouldHandleUnableToGenerateSamlExceptionCorrectly() throws Exception {
        final UnableToGenerateSamlException exception = new UnableToGenerateSamlException("error", new RuntimeException(), Level.DEBUG);
        Response response = samlEngineExceptionMapper.toResponse(exception);

        ErrorStatusDto responseEntity = (ErrorStatusDto) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(responseEntity.isAudited()).isFalse();
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.INVALID_INPUT);
        checkLogLevel(exception.getLogLevel());
    }

    @Test
    public void shouldHandleSamlContextExceptionCorrectly() throws Exception {
        final SamlContextException exception = new SamlContextException(UUID.randomUUID().toString(), "entityId", new SamlTransformationErrorException("error", Level.ERROR));
        Response response = samlEngineExceptionMapper.toResponse(exception);

        ErrorStatusDto responseEntity = (ErrorStatusDto) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(responseEntity.isAudited()).isFalse();
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
        checkLogLevel(exception.getLogLevel());
    }

    @Test
    public void shouldHandleSamlContextExceptionWithFailedToDecryptCorrectly() throws Exception {
        final SamlContextException exception = new SamlContextException(UUID.randomUUID().toString(), "entityId", new SamlFailedToDecryptException("error", Level.ERROR));
        Response response = samlEngineExceptionMapper.toResponse(exception);

        ErrorStatusDto responseEntity = (ErrorStatusDto) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(responseEntity.isAudited()).isFalse();
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML_FAILED_TO_DECRYPT);
        checkLogLevel(exception.getLogLevel());
    }

    @Test
    public void shouldReturnBadRequestForUnrecognizedException() {
        Response response = samlEngineExceptionMapper.toResponse(new RuntimeException("error"));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response.hasEntity()).isFalse();
        checkLogLevel(Level.WARN);
    }

    @Test
    public void shouldReturnBadRequestForNoKeyConfiguredForEntityException() {
        Response response = samlEngineExceptionMapper.toResponse(new NoKeyConfiguredForEntityException("error"));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        final ErrorStatusDto errorStatusDto = (ErrorStatusDto) response.getEntity();
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.NO_KEY_CONFIGURED_FOR_ENTITY);
        checkLogLevel(Level.ERROR);
    }

    private void checkLogLevel(Level logLevel) {
        ArgumentCaptor<Level> argumentCaptor = ArgumentCaptor.forClass(Level.class);
        verify(levelLogger, times(1)).log(argumentCaptor.capture(), any(Exception.class), any(UUID.class));
        assertThat(argumentCaptor.getValue()).isEqualTo(logLevel);
    }

    private ApplicationException createUnauditedExceptionThatShouldNotBeAudited() {
        return createUnauditedException(
                ExceptionType.NETWORK_ERROR,
                UUID.randomUUID(),
                URI.create("/some-uri")
        );
    }

}