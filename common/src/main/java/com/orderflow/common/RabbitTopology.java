package com.orderflow.common;


import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * Topologia compartilhada: todos os serviços declaram tudo (é idempotente),
 * então a ordem em que sobem não importa e nenhuma mensagem é perdida.
 */
@Configuration
public class RabbitTopology {

    //Definição de Exchanges e filas de notificação e pagamento.
    public static final String EXCHANGE = "orders.exchange";
    public static final String DLX = "orders.dlx";


    public static final String PAYMENT_QUEUE = "order.payment.queue";
    public static final String PAYMENT_DLQ = "order.payment.dlq";
    public static final String NOTIFICATION_QUEUE = "order.notification.queue";
    public static final String NOTIFICATION_DLQ = "order.notification.dlq";

    public static final String RK_CREATED = "order.created";
    public static final String RK_PAID = "order.paid";


    //Exchanges
    @Bean
    TopicExchange ordersExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    //Configurando QUEUES
    @Bean
    Queue paymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(PAYMENT_DLQ)
                .build();
    }

    @Bean
    Queue paymentDlq() {
        return QueueBuilder.durable(PAYMENT_DLQ).build();
    }

    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    Queue notificationDlq() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    //Bindings
    @Bean
    Binding paymentBinding() {
        return BindingBuilder.bind(paymentQueue()).to(ordersExchange()).with(RK_CREATED);
    }

    @Bean
    Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(ordersExchange()).with(RK_PAID);
    }

    @Bean
    Binding paymentDlqBinding() {
        return BindingBuilder.bind(paymentDlq()).to(deadLetterExchange()).with(PAYMENT_DLQ);
    }

    @Bean
    Binding notificationDlqBinding() {
        return BindingBuilder.bind(notificationDlq()).to(deadLetterExchange()).with(NOTIFICATION_DLQ);
    }

    // json como formato de message

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter("com.orderflow.common");
    }

}
