package com.dompetku.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Debt {

    private int id;
    private int userId;
    private String type = "payable";
    private String counterpart;
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private LocalDate dueDate;
    private String note;
    private LocalDateTime createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCounterpart() {
        return counterpart;
    }

    public void setCounterpart(String counterpart) {
        this.counterpart = counterpart;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount == null ? BigDecimal.ZERO : amount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount == null ? BigDecimal.ZERO : paidAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getRemaining() {
        BigDecimal remaining = amount.subtract(paidAmount);
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

    public int getProgressPercent() {
        if (amount.signum() <= 0) {
            return 0;
        }
        return Math.min(100, paidAmount.multiply(BigDecimal.valueOf(100))
                .divide(amount, 0, RoundingMode.HALF_UP).intValue());
    }

    public String getStatus() {
        if (paidAmount.compareTo(amount) >= 0) {
            return "paid";
        }
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            return "overdue";
        }
        if (paidAmount.signum() > 0) {
            return "partial";
        }
        return "unpaid";
    }

    public boolean isSettled() {
        return paidAmount.compareTo(amount) >= 0;
    }
}
