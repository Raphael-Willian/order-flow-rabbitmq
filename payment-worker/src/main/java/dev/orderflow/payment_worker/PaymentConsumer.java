package dev.orderflow.payment_worker;


import com.orderflow.common.OrderCreatedEvent;
import com.orderflow.common.OrderPaidEvent;
import com.orderflow.common.RabbitTopology;
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
    private static final BigDecimal LIMIT = new BigDecimal(5000);

    private static final String WORKER = System.getenv().getOrDefault("HOSTNAME", "local");

    private final RabbitTemplate rabbitTemplate;

    public PaymentConsumer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitTopology.PAYMENT_QUEUE)
    public void process(OrderCreatedEvent event) throws InterruptedException{

        log.info("[{}] processando pagamento do pedido {} ({} - R$ {})",
                WORKER, event.orderId(), event.customer(), event.amount());

        Thread.sleep(2000); //Simulando getawey de pagamento que poderá serimplementado posteriormente.

        if(event.amount().compareTo(LIMIT) > 0 ) {
            throw new IllegalStateException("Pagamento recusado, acima do limite.");
        }

        var paid = new OrderPaidEvent(event.orderId(), event.customer(),event.phone(), event.amount(),
                "SIM-" + UUID.randomUUID().toString().substring(0, 8));

        rabbitTemplate.convertAndSend(RabbitTopology.EXCHANGE, RabbitTopology.RK_PAID, paid);
        log.info("[{}] pedido {} PAGO ({})", WORKER, event.orderId(), paid.paymentId());

    }

}
