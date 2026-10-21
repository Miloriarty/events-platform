package org.example.booking.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItemsModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrdersModel order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketsModel ticket;

    // копия на момент покупки: история не меняется, если тип билета потом переименуют
    @Column(nullable = false)
    private String ticketType;

    @Column(nullable = false)
    private Integer quantity;

    // цена за один билет на момент покупки
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    public OrderItemsModel() {}

    public Long getId() { return id; }
    public OrdersModel getOrder() { return order; }
    public void setOrder(OrdersModel order) { this.order = order; }
    public TicketsModel getTicket() { return ticket; }
    public void setTicket(TicketsModel ticket) { this.ticket = ticket; }
    public String getTicketType() { return ticketType; }
    public void setTicketType(String ticketType) { this.ticketType = ticketType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}