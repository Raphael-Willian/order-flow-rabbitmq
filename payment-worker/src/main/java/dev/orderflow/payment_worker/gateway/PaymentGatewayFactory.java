package dev.orderflow.payment_worker.gateway;

import com.orderflow.common.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayFactory {

    private final ApplicationContext context;
    private final String provider;

    public PaymentGatewayFactory(ApplicationContext context,
                                 @Value("${payment.provider:mock}") String provider) {
        this.context = context;
        this.provider = provider;
    }

    public PaymentGateway gateway() throws PaymentGatewayException {
        String beanName = provider.toLowerCase() + "Gateway";
        try {
            return context.getBean(beanName, PaymentGateway.class);
        } catch (Exception ex) {
            throw new PaymentGatewayException("Provedor de pagamento não encontrado: " + provider +
                    " (bean esperado: " + beanName + ")", ex);
        }
    }

    public PaymentResponse createPayment(PaymentRequest request) throws PaymentGatewayException {
        return gateway().createPayment(request);
    }

    public PaymentStatus checkPayment(String externalPaymentId) throws PaymentGatewayException {
        return gateway().checkPayment(externalPaymentId);
    }
}
