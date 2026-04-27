package com.claudej.domain.order.event;

import lombok.Getter;

import java.util.Objects;

/**
 * Order item information value object - used for event data transfer
 */
@Getter
public final class OrderItemInfo {

    private final String productId;
    private final String productName;
    private final int quantity;

    public OrderItemInfo(String productId, String productName, int quantity) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("productId must not be null or empty");
        }
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("productName must not be null or empty");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderItemInfo that = (OrderItemInfo) o;
        return quantity == that.quantity &&
                Objects.equals(productId, that.productId) &&
                Objects.equals(productName, that.productName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, productName, quantity);
    }

    @Override
    public String toString() {
        return "OrderItemInfo{" +
                "productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
