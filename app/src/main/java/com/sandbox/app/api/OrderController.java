package com.sandbox.app.api;

import com.sandbox.application.command.CreateOrderCommand;
import com.sandbox.application.command.PayOrderCommand;
import com.sandbox.application.usecase.CreateOrderUseCase;
import com.sandbox.application.usecase.GetOrderDetailsUseCase;
import com.sandbox.application.usecase.PayOrderUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderDetailsUseCase getOrderDetailsUseCase;
    private final PayOrderUseCase payOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase,
                           GetOrderDetailsUseCase getOrderDetailsUseCase,
                           PayOrderUseCase payOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.getOrderDetailsUseCase = getOrderDetailsUseCase;
        this.payOrderUseCase = payOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<OrderCreatedResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        var command = new CreateOrderCommand(request.customerId(),
                request.lines().stream()
                        .map(line -> new CreateOrderCommand.Line(
                                line.productId(), line.quantity(), line.unitPrice(), line.currency()))
                        .toList());
        var order = createOrderUseCase.execute(command);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.id().value()))
                .body(new OrderCreatedResponse(order.id().value().toString(), order.status().name()));
    }

    @GetMapping("/{orderId}")
    public GetOrderDetailsUseCase.OrderDetailsView details(@PathVariable String orderId) {
        return getOrderDetailsUseCase.execute(orderId);
    }

    /**
     * Idempotency-Key es obligatorio: sin el, un reintento del cliente (o del balanceador)
     * cobraba dos veces al mismo cliente. Repetir la peticion con la misma clave devuelve
     * el pago ya registrado sin volver a llamar al gateway.
     */
    @PostMapping("/{orderId}/payments")
    public ResponseEntity<PaymentResponse> pay(@PathVariable String orderId,
                                               @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey) {
        var payment = payOrderUseCase.execute(new PayOrderCommand(orderId, idempotencyKey));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new PaymentResponse(payment.id().value().toString(), payment.status().name()));
    }

    public record CreateOrderRequest(
            @NotBlank String customerId,
            @NotEmpty List<@Valid LineRequest> lines) {
    }

    public record LineRequest(
            @NotBlank String productId,
            @Positive int quantity,
            @Positive BigDecimal unitPrice,
            @NotBlank String currency) {
    }

    public record OrderCreatedResponse(String orderId, String status) {
    }

    public record PaymentResponse(String paymentId, String status) {
    }
}
