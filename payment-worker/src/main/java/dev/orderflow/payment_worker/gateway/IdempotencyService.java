package dev.orderflow.payment_worker.gateway;

import com.orderflow.common.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de idempotência em memória para o payment-worker.
 *
 * Garante que um mesmo orderId não seja processado duas vezes, evitando
 * cobranças duplicadas quando o consumidor reprocessa mensagens por
 * falha de ack/retry. Em produção, substituir por cache distribuído
 * (Redis) ou persistência em banco.
 */
@Component
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final Map<String, PaymentStatus> processed = new ConcurrentHashMap<>();

    public boolean isProcessed(String orderId) {
        return processed.containsKey(orderId);
    }

    public void markProcessed(String orderId, PaymentStatus status) {
        log.info("[Idempotency] pedido {} marcado como processado com status {}", orderId, status);
        processed.put(orderId, status);
    }

    public PaymentStatus getStatus(String orderId) {
        return processed.get(orderId);
    }
}
