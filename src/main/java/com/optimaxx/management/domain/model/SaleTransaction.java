package com.optimaxx.management.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sale_transactions")
public class SaleTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_type_id", nullable = false)
    private TransactionType transactionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "customer_name", nullable = false, length = 128)
    private String customerName;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "receipt_number", length = 32)
    private String receiptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SaleTransactionStatus status;

    @Column(name = "inventory_item_id")
    private UUID inventoryItemId;

    @Column(name = "inventory_quantity")
    private Integer inventoryQuantity;

    @Column(name = "stock_reverted", nullable = false)
    private boolean stockReverted;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public UUID getCustomerId() {
        return customer == null ? null : customer.getId();
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public SaleTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(SaleTransactionStatus status) {
        this.status = status;
    }

    public UUID getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(UUID inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public Integer getInventoryQuantity() {
        return inventoryQuantity;
    }

    public void setInventoryQuantity(Integer inventoryQuantity) {
        this.inventoryQuantity = inventoryQuantity;
    }

    public boolean isStockReverted() {
        return stockReverted;
    }

    public void setStockReverted(boolean stockReverted) {
        this.stockReverted = stockReverted;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
