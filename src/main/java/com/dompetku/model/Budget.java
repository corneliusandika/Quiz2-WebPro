package com.dompetku.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Budget {

    private int id;
    private int userId;
    private int categoryId;
    private LocalDate month = LocalDate.now().withDayOfMonth(1);
    private BigDecimal amount = BigDecimal.ZERO;
    private LocalDateTime createdAt;

    private String categoryName;
    private String categoryColor;
    private String categoryIcon;
    private BigDecimal spent = BigDecimal.ZERO;

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

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public LocalDate getMonth() {
        return month;
    }

    public void setMonth(LocalDate month) {
        this.month = month;
    }

    public String getMonthKey() {
        return month == null ? "" : String.format("%04d-%02d", month.getYear(), month.getMonthValue());
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount == null ? BigDecimal.ZERO : amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public BigDecimal getSpent() {
        return spent;
    }

    public void setSpent(BigDecimal spent) {
        this.spent = spent == null ? BigDecimal.ZERO : spent;
    }

    public BigDecimal getRemaining() {
        return amount.subtract(spent);
    }

    public int getPercentage() {
        if (amount == null || amount.signum() <= 0) {
            return 0;
        }
        BigDecimal p = spent.multiply(BigDecimal.valueOf(100))
                .divide(amount, 0, RoundingMode.HALF_UP);
        return p.intValue();
    }

    public int getBarWidth() {
        return Math.min(100, Math.max(0, getPercentage()));
    }

    public String getStatus() {
        int p = getPercentage();
        if (p >= 100) {
            return "over";
        }
        if (p >= 80) {
            return "warning";
        }
        return "safe";
    }
}
