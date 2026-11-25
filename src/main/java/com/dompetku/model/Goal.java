package com.dompetku.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Goal {

    private int id;
    private int userId;
    private String name;
    private BigDecimal targetAmount = BigDecimal.ZERO;
    private BigDecimal savedAmount = BigDecimal.ZERO;
    private LocalDate deadline;
    private String color = "#7c3aed";
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount == null ? BigDecimal.ZERO : targetAmount;
    }

    public BigDecimal getSavedAmount() {
        return savedAmount;
    }

    public void setSavedAmount(BigDecimal savedAmount) {
        this.savedAmount = savedAmount == null ? BigDecimal.ZERO : savedAmount;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getProgressPercent() {
        if (targetAmount == null || targetAmount.signum() <= 0) {
            return 0;
        }
        BigDecimal p = savedAmount.multiply(BigDecimal.valueOf(100))
                .divide(targetAmount, 0, RoundingMode.HALF_UP);
        return Math.min(100, Math.max(0, p.intValue()));
    }

    public BigDecimal getRemaining() {
        BigDecimal remaining = targetAmount.subtract(savedAmount);
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

    public long getDaysLeft() {
        if (deadline == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), deadline);
    }

    public boolean isCompleted() {
        return savedAmount.compareTo(targetAmount) >= 0;
    }
}
