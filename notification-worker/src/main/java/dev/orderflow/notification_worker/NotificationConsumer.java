package dev.orderflow.notification_worker;


import com.orderflow.common.OrderPaidEvent;
import com.orderflow.common.RabbitTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private static final String WORKER = System.getenv().getOrDefault("HOSTNAME", "local");

    private final EvolutionClient evolutionClient;

    public NotificationConsumer(EvolutionClient evolutionClient) {
        this.evolutionClient = evolutionClient;
    }

    @RabbitListener(queues = RabbitTopology.NOTIFICATION_QUEUE)
    public void onOrderPaid(OrderPaidEvent event) {

        if(event.phone() == null || event.phone().isBlank()) {
            //Informa o worker que está processando o evento + id do pedido processado.
            log.info("[{}] pedido {} sem telefone: notificação ignorada", WORKER, event.orderId());
            return;
        }

        String shortId = event.orderId().toString().substring(0,8);
        String amount = String.format(Locale.of("pt", "BR"), "%.2f", event.amount());

        String text = """
                Olá, %s! 🎉
                Seu pedido *#%s* foi confirmado.
                Valor: R$ %s
                Pagamento: %s
                Obrigado pela compra!""".formatted(event.customer(), shortId, amount, event.paymentId());

        log.info("[{}] enviando Whatsapp do pedido {}", WORKER, event.orderId());

        evolutionClient.sendText(event.phone(), text);


    }
























}
