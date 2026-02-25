package com.optimaxx.management.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "lens_prescriptions")
public class LensPrescription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_type_id", nullable = false)
    private TransactionType transactionType;

    @Column(name = "right_sphere", length = 16)
    private String rightSphere;

    @Column(name = "left_sphere", length = 16)
    private String leftSphere;

    @Column(name = "right_cylinder", length = 16)
    private String rightCylinder;

    @Column(name = "left_cylinder", length = 16)
    private String leftCylinder;

    @Column(name = "right_axis", length = 16)
    private String rightAxis;

    @Column(name = "left_axis", length = 16)
    private String leftAxis;

    @Column(name = "pd", length = 16)
    private String pd;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

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

    public String getRightSphere() {
        return rightSphere;
    }

    public void setRightSphere(String rightSphere) {
        this.rightSphere = rightSphere;
    }

    public String getLeftSphere() {
        return leftSphere;
    }

    public void setLeftSphere(String leftSphere) {
        this.leftSphere = leftSphere;
    }

    public String getRightCylinder() {
        return rightCylinder;
    }

    public void setRightCylinder(String rightCylinder) {
        this.rightCylinder = rightCylinder;
    }

    public String getLeftCylinder() {
        return leftCylinder;
    }

    public void setLeftCylinder(String leftCylinder) {
        this.leftCylinder = leftCylinder;
    }

    public String getRightAxis() {
        return rightAxis;
    }

    public void setRightAxis(String rightAxis) {
        this.rightAxis = rightAxis;
    }

    public String getLeftAxis() {
        return leftAxis;
    }

    public void setLeftAxis(String leftAxis) {
        this.leftAxis = leftAxis;
    }

    public String getPd() {
        return pd;
    }

    public void setPd(String pd) {
        this.pd = pd;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
