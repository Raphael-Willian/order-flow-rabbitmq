package dev.orderflow.payment_worker.gateway;

import com.orderflow.common.PaymentGateway;
import com.orderflow.common.PaymentGatewayException;
import com.orderflow.common.PaymentRequest;
import com.orderflow.common.PaymentResponse;
import com.orderflow.common.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component("mercadopagoGateway")
public class MercadoPagoGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoGateway.class);

    private final RestClient http;
    private final String baseUrl;
    private final String accessToken;
    private final String checkoutMode;

    public MercadoPagoGateway(
            @Value("${payment.mercadopago.base-url:https://api.mercadopago.com}") String baseUrl,
            @Value("${payment.mercadopago.access-token}") String accessToken,
            @Value("${payment.mercadopago.checkout-mode:redirect}") String checkoutMode) {
        this.baseUrl = baseUrl;
        this.accessToken = accessToken;
        this.checkoutMode = checkoutMode;

        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(15));

        this.http = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request) throws PaymentGatewayException {
        if (accessToken == null || accessToken.isBlank()) {
            throw new PaymentGatewayException("MERCADO_PAGO_ACCESS_TOKEN não configurado");
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("items", java.util.List.of(Map.of(
                    "title", request.description() != null ? request.description() : "Pedido " + request.orderId(),
                    "quantity", 1,
                    "unit_price", request.amount().doubleValue(),
                    "currency_id", "BRL"
            )));
            body.put("payer", Map.of(
                    "name", request.customer(),
                    "phone", Map.of("number", request.phone())
            ));
            body.put("external_reference", request.orderId().toString());
            body.put("notification_url", "http://order-api:8080/webhooks/payment/mercadopago");
            body.put("statement_descriptor", "ORDERFLOW");
            body.put("payment_methods", Map.of(
                    "excluded_payment_types", java.util.List.of(),
                    "excluded_payment_methods", java.util.List.of(),
                    "installments", 1
            ));
            body.put("back_urls", Map.of(
                    "success", "https://mp-checkout-return.vercel.app/success?payment_id=123456789&status=approved&external_reference=pedido-demo-001&payment_type=credit_card",
                    "failure", "https://mp-checkout-return.vercel.app/pending?payment_id=123456789&status=pending&external_reference=pedido-demo-002&payment_type=ticket",
                    "pending", "https://mp-checkout-return.vercel.app/failure?payment_id=123456789&status=rejected&external_reference=pedido-demo-003&payment_type=credit_card"
            ));
            body.put("auto_return", "approved");
            body.put("mode", checkoutMode);

            var response = http.post()
                    .uri("/checkout/preferences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new PaymentGatewayException("Mercado Pago retornou corpo vazio ao criar preferência");
            }

            String externalPaymentId = String.valueOf(responseBody.get("id"));
            String checkoutUrl = String.valueOf(responseBody.get("init_point"));

            log.info("Mercado Pago: preferência criada id={} para pedido {}", externalPaymentId, request.orderId());

            return new PaymentResponse(
                    request.orderId(),
                    externalPaymentId,
                    checkoutUrl,
                    PaymentStatus.PENDING,
                    request.amount()
            );
        } catch (RestClientException ex) {
            log.error("Mercado Pago: falha ao criar preferência para pedido {}", request.orderId(), ex);
            throw new PaymentGatewayException("Falha ao comunicar com Mercado Pago: " + ex.getMessage(), ex);
        }
    }

    @Override
    public PaymentStatus checkPayment(String externalPaymentId) throws PaymentGatewayException {
        try {
            var response = http.get()
                    .uri("/v1/payments/{id}", externalPaymentId)
                    .retrieve()
                    .toEntity(Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) {
                return PaymentStatus.ERROR;
            }

            String status = String.valueOf(body.getOrDefault("status", ""));
            return switch (status) {
                case "approved" -> PaymentStatus.APPROVED;
                case "rejected" -> PaymentStatus.REJECTED;
                case "pending", "in_process" -> PaymentStatus.PENDING;
                case "cancelled", "refunded" -> PaymentStatus.REJECTED;
                default -> PaymentStatus.ERROR;
            };
        } catch (RestClientException ex) {
            log.error("Mercado Pago: falha ao consultar pagamento {}", externalPaymentId, ex);
            throw new PaymentGatewayException("Falha ao consultar Mercado Pago: " + ex.getMessage(), ex);
        }
    }
}
