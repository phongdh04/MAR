package vn.mar.common.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.mar.common.dto.ApiMeta;
import vn.mar.common.exception.MarException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Pattern CONSTRAINT_PATTERN = Pattern.compile("constraint \\[(?<name>[^\\]]+)]");

    private final ErrorResponseFactory errorResponseFactory;
    private final ConstraintErrorMapper constraintErrorMapper;

    public GlobalExceptionHandler(
            ErrorResponseFactory errorResponseFactory,
            ConstraintErrorMapper constraintErrorMapper) {
        this.errorResponseFactory = errorResponseFactory;
        this.constraintErrorMapper = constraintErrorMapper;
    }

    @ExceptionHandler(MarException.class)
    public ResponseEntity<ErrorResponse> handleMarException(MarException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ErrorResponse response = errorResponseFactory.create(
                errorCode,
                exception.getMessage(),
                exception.getDetails()
        );
        return ResponseEntity.status(errorCode.httpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ErrorDetail> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorDetail)
                .toList();
        ErrorResponse response = errorResponseFactory.create(
                ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                details
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<ErrorDetail> details = exception.getConstraintViolations()
                .stream()
                .map(this::toConstraintViolationDetail)
                .toList();
        ErrorResponse response = errorResponseFactory.create(
                ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                details
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception) {
        Optional<ErrorCode> mappedCode = extractConstraintName(exception).flatMap(constraintErrorMapper::map);
        ErrorCode errorCode = mappedCode.orElse(ErrorCode.CONFLICT);
        ErrorResponse response = errorResponseFactory.create(errorCode, errorCode.defaultMessage(), List.of());
        return ResponseEntity.status(errorCode.httpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        LOGGER.error("unexpected error requestId={}", ApiMeta.current().requestId(), exception);
        ErrorResponse response = errorResponseFactory.create(ErrorCode.INTERNAL_ERROR);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.httpStatus()).body(response);
    }

    private ErrorDetail toFieldErrorDetail(FieldError fieldError) {
        return ErrorDetail.of(
                toJsonFieldName(fieldError.getField()),
                toValidationCode(fieldError.getCode()),
                safeDefaultMessage(fieldError.getDefaultMessage())
        );
    }

    private ErrorDetail toConstraintViolationDetail(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath() == null
                ? null
                : toJsonFieldName(violation.getPropertyPath().toString());
        return ErrorDetail.of(field, toValidationCode(violation.getConstraintDescriptor()
                .getAnnotation()
                .annotationType()
                .getSimpleName()), safeDefaultMessage(violation.getMessage()));
    }

    private String toValidationCode(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return "INVALID";
        }
        return switch (sourceCode) {
            case "NotBlank", "NotEmpty", "NotNull" -> "REQUIRED";
            case "Email" -> "INVALID_EMAIL";
            case "Min", "Positive", "PositiveOrZero" -> "MIN_VALUE";
            case "Max" -> "MAX_VALUE";
            case "Size" -> "INVALID_SIZE";
            case "Pattern" -> "INVALID_FORMAT";
            default -> sourceCode.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        };
    }

    private String toJsonFieldName(String javaFieldName) {
        if (javaFieldName == null || javaFieldName.isBlank()) {
            return javaFieldName;
        }
        return javaFieldName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private String safeDefaultMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Invalid value";
        }
        return message;
    }

    private Optional<String> extractConstraintName(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                Matcher matcher = CONSTRAINT_PATTERN.matcher(message);
                if (matcher.find()) {
                    return Optional.of(matcher.group("name"));
                }
            }
            current = current.getCause();
        }
        return Optional.empty();
    }
}
