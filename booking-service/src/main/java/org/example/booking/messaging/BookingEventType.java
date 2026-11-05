package org.example.booking.messaging;

public enum BookingEventType {
    ORDER_CREATED("order.created"),
    ORDER_CANCELLED("order.cancelled"),
    REGISTRATION_CREATED("registration.created"),
    REGISTRATION_CANCELLED("registration.cancelled");

    private final String routingKey;

    BookingEventType(String routingKey) {
        this.routingKey = routingKey;
    }

    public String routingKey() {
        return routingKey;
    }
}
