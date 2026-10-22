package dev.orderflow.payment_worker;

import com.orderflow.common.OrderCreatedEvent;
import com.orderflow.common.OrderPaidEvent;
import com.orderflow.common.PaymentGatewayException;
import com.orderflow.common.PaymentRequest;
import com.orderflow.common.PaymentResponse;
import com.orderflow.common.PaymentStatus;
import com.orderflow.common.RabbitTopology;
import dev.orderflow.payment_worker.gateway.IdempotencyService;
import dev.orderflow.payment_worker.gateway.PaymentGatewayFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentConsumer.class);
    private static final String WORKER = System.getenv().getOrDefault("HOSTNAME", "local");

    private final PaymentGatewayFactory gatewayFactory;
    private final IdempotencyService idempotency;
    private final RabbitTemplate rabbitTemplate;

    public PaymentConsumer(PaymentGatewayFactory gatewayFactory,
                           IdempotencyService idempotency,
                           RabbitTemplate rabbitTemplate) {
        this.gatewayFactory = gatewayFactory;
        this.idempotency = idempotency;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitTopology.PAYMENT_QUEUE)
    public void process(OrderCreatedEvent event) throws InterruptedException {

        // Idempotência: não reprocessa pedido já visto
        if (idempotency.isProcessed(event.orderId().toString())) {
            PaymentStatus status = idempotency.getStatus(event.orderId().toString());
            log.info("[{}] pedido {} já processado anteriormente (status={}). Ignorando duplicata.",
                    WORKER, event.orderId(), status);
            return;
        }

        log.info("[{}] processando pagamento do pedido {} ({} - R$ {})",
                WORKER, event.orderId(), event.customer(), event.amount());

        PaymentRequest request = new PaymentRequest(
                event.orderId(),
                event.customer(),
                event.phone(),
                event.amount(),
                "Pedido " + event.orderId()
        );

        PaymentResponse response;
        try {
            response = gatewayFactory.createPayment(request);
        } catch (PaymentGatewayException ex) {
            log.error("[{}] falha no gateway para pedido {}: {}", WORKER, event.orderId(), ex.getMessage());
            idempotency.markProcessed(event.orderId().toString(), PaymentStatus.ERROR);
            throw new IllegalStateException("Falha no gateway de pagamento: " + ex.getMessage(), ex);
        }

        // Marca como processado para idempotência
        idempotency.markProcessed(event.orderId().toString(), response.status());

        if (response.status() == PaymentStatus.REJECTED) {
            log.warn("[{}] pagamento rejeitado para pedido {}", WORKER, event.orderId());
            throw new IllegalStateException("Pagamento recusado pelo gateway (id externo: " + response.externalPaymentId() + ")");
        }

        var paid = new OrderPaidEvent(
                event.orderId(),
                event.customer(),
                event.phone(),
                event.amount(),
                response.externalPaymentId(),
                response.externalPaymentId(),
                response.status()
        );

        rabbitTemplate.convertAndSend(RabbitTopology.EXCHANGE, RabbitTopology.RK_PAID, paid);
        log.info("[{}] pedido {} PAGO (externo={}, status={})", WORKER, event.orderId(),
                paid.externalPaymentId(), paid.status());
    }
}
