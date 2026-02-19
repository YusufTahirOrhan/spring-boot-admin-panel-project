package com.optimaxx.management.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "repair_orders")
public class RepairOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_type_id", nullable = false)
    private TransactionType transactionType;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private RepairStatus status;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "reserved_inventory_item_id")
    private java.util.UUID reservedInventoryItemId;

    @Column(name = "reserved_inventory_quantity")
    private Integer reservedInventoryQuantity;

    @Column(name = "inventory_released", nullable = false)
    private boolean inventoryReleased;

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RepairStatus getStatus() {
        return status;
    }

    public void setStatus(RepairStatus status) {
        this.status = status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public java.util.UUID getReservedInventoryItemId() {
        return reservedInventoryItemId;
    }

    public void setReservedInventoryItemId(java.util.UUID reservedInventoryItemId) {
        this.reservedInventoryItemId = reservedInventoryItemId;
    }

    public Integer getReservedInventoryQuantity() {
        return reservedInventoryQuantity;
    }

    public void setReservedInventoryQuantity(Integer reservedInventoryQuantity) {
        this.reservedInventoryQuantity = reservedInventoryQuantity;
    }

    public boolean isInventoryReleased() {
        return inventoryReleased;
    }

    public void setInventoryReleased(boolean inventoryReleased) {
        this.inventoryReleased = inventoryReleased;
    }
}
