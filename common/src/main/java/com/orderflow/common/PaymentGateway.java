package com.orderflow.common;

/**
 * Contrato de integração com gateways de pagamento.
 *
 * Qualquer provedor (Mercado Pago, Stripe, PagSeguro, mock, etc.) deve
 * implementar esta interface. O payment-worker depende apenas dela, o que
 * permite trocar o provedor por variável de ambiente sem alterar o restante
 * do sistema.
 */
public interface PaymentGateway {

    /**
     * Inicia um pagamento junto ao provedor.
     *
     * @param request dados do pedido a ser cobrado
     * @return resposta contendo o identificador externo, URL de checkout e status inicial
     * @throws PaymentGatewayException em caso de falha de comunicação ou rejeição imediata
     */
    PaymentResponse createPayment(PaymentRequest request) throws PaymentGatewayException;

    /**
     * Consulta o status de um pagamento já iniciado.
     *
     * @param externalPaymentId identificador do pagamento no provedor externo
     * @return status atualizado
     * @throws PaymentGatewayException em caso de falha de comunicação
     */
    PaymentStatus checkPayment(String externalPaymentId) throws PaymentGatewayException;
}
