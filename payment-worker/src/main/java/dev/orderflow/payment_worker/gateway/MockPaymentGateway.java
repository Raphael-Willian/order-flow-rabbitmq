package dev.orderflow.payment_worker.gateway;

import com.orderflow.common.PaymentGateway;
import com.orderflow.common.PaymentGatewayException;
import com.orderflow.common.PaymentRequest;
import com.orderflow.common.PaymentResponse;
import com.orderflow.common.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component("mockPaymentGateway")
public class MockPaymentGateway implements PaymentGateway {

    private static final BigDecimal LIMIT = new BigDecimal(5000);

    @Override
    public PaymentResponse createPayment(PaymentRequest request) throws PaymentGatewayException {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentGatewayException("Interrupção durante simulação de pagamento", e);
        }

        if (request.amount().compareTo(LIMIT) > 0) {
            return new PaymentResponse(
                    request.orderId(),
                    "MOCK-REJ-" + UUID.randomUUID(),
                    null,
                    PaymentStatus.REJECTED,
                    request.amount()
            );
        }

        return new PaymentResponse(
                request.orderId(),
                "MOCK-" + UUID.randomUUID(),
                null,
                PaymentStatus.APPROVED,
                request.amount()
        );
    }

    @Override
    public PaymentStatus checkPayment(String externalPaymentId) throws PaymentGatewayException {
        return PaymentStatus.APPROVED;
    }
}
