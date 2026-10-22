package com.orderflow.common;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(UUID orderId, String externalPaymentId, String checkoutUrl, PaymentStatus status, BigDecimal amount) {}
