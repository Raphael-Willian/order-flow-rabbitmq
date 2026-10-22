package com.orderflow.common;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(UUID orderId, String customer, String phone, BigDecimal amount, String description) {}
