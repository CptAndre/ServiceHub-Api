package com.example.servicehub_api.exception;

import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            @NonNull Exception ex, @Nullable Object body, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode, @NonNull WebRequest request) {
        // O Spring escolhe o status e os headers; a API padroniza somente o corpo.
        if (statusCode.is5xxServerError()) {
            log.error("Falha HTTP interna: status={} exceptionType={}", statusCode.value(), ex.getClass().getSimpleName());
        }
        ApiError error = body instanceof ApiError apiError ? apiError : errorFor(ex, statusCode);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return super.handleExceptionInternal(ex, error, responseHeaders, statusCode, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrityViolation() {
        log.warn("Operação rejeitada por restrição de integridade do banco");
        return response(HttpStatus.CONFLICT, "Operação não permitida",
                List.of("O registro viola uma regra de unicidade, validade ou relacionamento do banco de dados."));
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiError> concurrentModification() {
        return response(HttpStatus.CONFLICT, "Conflito de atualização",
                List.of("O recurso está sendo alterado. Consulte o estado atual e tente novamente."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDenied() {
        return response(HttpStatus.FORBIDDEN, "Acesso negado",
                List.of("Seu perfil não tem permissão para esta operação"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generic(Exception ex) {
        log.error("Falha interna: exceptionType={}", ex.getClass().getSimpleName());
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                List.of("Não foi possível processar a solicitação"));
    }

    private ApiError errorFor(Exception ex, HttpStatusCode status) {
        if (status.is5xxServerError()) {
            return error(status, "Erro interno", List.of("Não foi possível processar a solicitação"));
        }
        if (ex instanceof MethodArgumentNotValidException validation) {
            List<String> messages = validation.getBindingResult().getFieldErrors().stream()
                    .map(field -> field.getField() + ": " + field.getDefaultMessage()).toList();
            return error(status, "Dados inválidos", messages);
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return error(status, "Corpo da requisição inválido",
                    List.of("JSON malformado ou tipo de dado incompatível"));
        }
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return error(status, "Parâmetro inválido",
                    List.of("O parâmetro '" + mismatch.getName() + "' possui formato inválido"));
        }
        if (ex instanceof ResponseStatusException declared) {
            HttpStatus knownStatus = HttpStatus.resolve(status.value());
            String title = knownStatus == null ? "Erro HTTP" : knownStatus.getReasonPhrase();
            return error(status, title, List.of(declared.getReason() == null ? title : declared.getReason()));
        }
        String message = switch (status.value()) {
            case 400 -> "Parâmetros obrigatórios ausentes ou inválidos";
            case 404 -> "Recurso não encontrado";
            case 405 -> "Método HTTP não suportado para este recurso";
            case 406 -> "O formato solicitado não está disponível";
            case 413 -> "O tamanho máximo permitido foi excedido";
            case 415 -> "Tipo de conteúdo não suportado";
            case 429 -> "Limite de requisições excedido";
            default -> "Não foi possível processar a requisição";
        };
        return error(status, status.value() == 405 ? "Método não permitido" : message, List.of(message));
    }

    private ApiError error(HttpStatusCode status, String title, List<String> messages) {
        return new ApiError(OffsetDateTime.now(), status.value(), title, messages);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String title, List<String> messages) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(error(status, title, messages));
    }
}
