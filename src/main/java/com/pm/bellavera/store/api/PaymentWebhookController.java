package com.pm.bellavera.store.api;

import com.pm.bellavera.store.PaymentApplicationService;
import com.pm.bellavera.store.payment.PaymentGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The payment provider's callback. Unauthenticated by necessity - Stripe holds no JWT - and safe
 * because {@link PaymentGateway#parseWebhook} refuses a payload whose signature does not verify.
 *
 * <p>The body is taken as a raw {@code String}: signature verification hashes the exact bytes
 * Stripe sent, so anything that re-serializes the JSON first would break it.
 *
 * <p>Always answers 200 once the payload verifies. Stripe retries non-2xx responses, and there is
 * nothing to retry for an event we understood and chose to ignore.
 */
@RestController
@RequestMapping("/api/v1/webhooks/stripe")
public class PaymentWebhookController {

    private final PaymentGateway paymentGateway;
    private final PaymentApplicationService paymentApplicationService;

    public PaymentWebhookController(PaymentGateway paymentGateway,
                                     PaymentApplicationService paymentApplicationService) {
        this.paymentGateway = paymentGateway;
        this.paymentApplicationService = paymentApplicationService;
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody String payload,
                                         @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        paymentGateway.parseWebhook(payload, signature)
                .ifPresent(paymentApplicationService::apply);
        return ResponseEntity.ok().build();
    }
}
