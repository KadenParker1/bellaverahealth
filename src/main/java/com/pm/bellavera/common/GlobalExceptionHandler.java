package com.pm.bellavera.common;

import com.pm.bellavera.store.payment.PaymentException;
import com.pm.bellavera.store.payment.UnresolvedPaymentException;
import java.net.URI;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain and framework exceptions into RFC 7807 {@link ProblemDetail} responses so
 * every API error, from every controller, has the same shape.
 *
 * <p>The dividing line is whether the message is <em>written for the caller</em>. Domain
 * exceptions ({@link NotFoundException}, {@link ValidationException}, {@link ConflictException})
 * carry text that was composed to be read by a user, so it is returned verbatim. Everything else -
 * a failed provider call, a constraint violation, a bug - is logged in full with a correlation id
 * and answered with a fixed sentence plus that id. Echoing {@code ex.getMessage()} from an
 * arbitrary exception is how internals like configuration keys and SQL end up in a browser.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String PROBLEM_BASE = "https://bellavera.app/problems/";

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), "not-found");
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidation(ValidationException ex) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, ex.getMessage(), "validation");
        detail.setProperty("errors", ex.getErrors());
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleBeanValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation failed", "validation");
        detail.setProperty("errors", errors);
        return detail;
    }

    /** A malformed body. The parser's own message names Java types and offsets - do not forward it. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("Rejected an unreadable request body", ex);
        return problem(HttpStatus.BAD_REQUEST, "The request body could not be read as JSON.", "validation");
    }

    /** A deliberate state conflict, phrased for the caller. */
    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), "conflict");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "You do not have access to this resource.", "forbidden");
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ProblemDetail> handleRateLimited(TooManyRequestsException ex) {
        ProblemDetail detail = problem(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), "rate-limited");
        detail.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.getRetryAfterSeconds()))
                .body(detail);
    }

    /**
     * Two writers touched the same row - a second admin fulfilling the order you just fulfilled,
     * say. Same answer as any other conflicting state transition: 409, reload, look again.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT,
                "Someone else changed this while you were working on it. Reload and try again.", "conflict");
    }

    /**
     * A uniqueness or foreign-key constraint refused the write. The driver's message quotes the
     * index name and the offending values, so it is logged rather than returned.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        String errorId = newErrorId();
        log.warn("Data integrity violation [{}]", errorId, ex);
        return withErrorId(problem(HttpStatus.CONFLICT,
                "That change conflicts with data that already exists.", "conflict"), errorId);
    }

    /** A provider call failed, or a webhook payload did not verify. Details stay in the log. */
    @ExceptionHandler(PaymentException.class)
    public ProblemDetail handlePayment(PaymentException ex) {
        String errorId = newErrorId();
        log.warn("Payment operation failed [{}]", errorId, ex);
        return withErrorId(problem(HttpStatus.BAD_REQUEST,
                "The payment request could not be processed.", "payment"), errorId);
    }

    /**
     * A verified event we could not match to an order. Answered 503 so the provider retries -
     * see {@link UnresolvedPaymentException}.
     */
    @ExceptionHandler(UnresolvedPaymentException.class)
    public ProblemDetail handleUnresolvedPayment(UnresolvedPaymentException ex) {
        String errorId = newErrorId();
        log.error("Verified payment event could not be applied [{}]: {}", errorId, ex.getMessage());
        return withErrorId(problem(HttpStatus.SERVICE_UNAVAILABLE,
                "The event could not be applied yet. Retry.", "payment"), errorId);
    }

    /**
     * A bug or a misconfiguration - not something the caller did. This used to be mapped to 409
     * with its raw message, which both told clients to retry unretryable failures and published
     * text like missing configuration keys.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        String errorId = newErrorId();
        log.error("Unexpected illegal state [{}]", errorId, ex);
        return withErrorId(problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on our end.", "internal"), errorId);
    }

    private static ProblemDetail problem(HttpStatus status, String detail, String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(PROBLEM_BASE + type));
        return problem;
    }

    /** Lets a user quote the id from an error page and have it found in the logs. */
    private static ProblemDetail withErrorId(ProblemDetail detail, String errorId) {
        detail.setProperty("errorId", errorId);
        return detail;
    }

    private static String newErrorId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
