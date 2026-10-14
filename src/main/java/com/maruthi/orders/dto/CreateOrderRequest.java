package com.maruthi.orders.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotBlank(message = "customerId is required")
        String customerId,

        @NotBlank(message = "product is required")
        String product,

        @Min(value = 1, message = "quantity must be at least 1")
        int quantity,

        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.01", message = "unitPrice must be positive")
        BigDecimal unitPrice
) {
}
