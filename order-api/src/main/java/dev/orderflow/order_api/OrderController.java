package dev.orderflow.order_api;

import com.orderflow.common.OrderCreatedEvent;
import com.orderflow.common.RabbitTopology;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    public final RabbitTemplate rabbitTemplate;

    public OrderController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public record CreateOrderRequest(@NotBlank String customer, @NotBlank @Pattern(regexp = "\\\\d{12,13}",
            message ="use DDI+DDD+número, só dígitos (ex.: 5511999999999)") String phone,
                                     @NotNull @DecimalMin("0.01") BigDecimal amount){}


    //Criação de pedido e envio de notificação de evento para exchange
    @PostMapping
    public ResponseEntity<OrderCreatedEvent> create(@Valid @RequestBody CreateOrderRequest request) {
        var event = new OrderCreatedEvent(UUID.randomUUID(), request.customer, request.phone, request.amount);
        rabbitTemplate.convertAndSend(RabbitTopology.EXCHANGE, RabbitTopology.RK_CREATED, event);
        return ResponseEntity.accepted().body(event);
    }

    //Demonstração de workers dividindo a fila com Orders sem telefone
    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulk(@RequestParam(defaultValue = "12") int count) {
        int n = Math.min(Math.max(count, 1), 100);
        for (int i = 1; i <= n; i++) {
            var event = new OrderCreatedEvent(UUID.randomUUID(), "Cliente " + i, null, BigDecimal.valueOf(100L + i));
            rabbitTemplate.convertAndSend(RabbitTopology.EXCHANGE, RabbitTopology.RK_CREATED, event);
        }
        return ResponseEntity.accepted().body(Map.of("published", n));
    }













}
