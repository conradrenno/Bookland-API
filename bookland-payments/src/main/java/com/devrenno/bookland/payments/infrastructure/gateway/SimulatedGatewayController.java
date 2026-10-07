package com.devrenno.bookland.payments.infrastructure.gateway;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A switch for the simulated payment gateway, so a person can take it down and bring it back while
 * watching the application run — the payment waiting PENDING, its attempts growing, the order
 * confirming once the gateway is back.
 *
 * <p>Dev profile only: the route does not exist in the prod profile (the compose stack), and it is
 * under {@code /api/v1/admin}, so even in dev only an admin reaches it. It talks to the simulator
 * directly, not through a use case: it is part of the simulation, not of the business.
 */
@Profile("dev")
@RestController
@RequestMapping("/api/v1/admin/dev/payment-gateway")
public class SimulatedGatewayController {

    private final SimulatedPaymentGatewayAdapter gateway;

    public SimulatedGatewayController(SimulatedPaymentGatewayAdapter gateway) {
        this.gateway = gateway;
    }

    @GetMapping
    public ResponseEntity<GatewayState> state() {
        return ResponseEntity.ok(stateNow());
    }

    /** {@code {"down": true}} — every charge and refund gets no answer until {@code {"down": false}}. */
    @PutMapping("/outage")
    public ResponseEntity<GatewayState> outage(@Valid @RequestBody OutageRequest request) {
        gateway.setDown(request.down());
        return ResponseEntity.ok(stateNow());
    }

    private GatewayState stateNow() {
        return new GatewayState(gateway.isDown(), gateway.chargesMade(), gateway.refundsMade());
    }

    public record OutageRequest(@NotNull Boolean down) {
    }

    /** Whether the gateway is down, and the charges and refunds it actually made since the start. */
    public record GatewayState(boolean down, int chargesMade, int refundsMade) {
    }
}
