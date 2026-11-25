package com.dompetku.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DayStat {

    private final LocalDate date;
    private BigDecimal income = BigDecimal.ZERO;
    private BigDecimal expense = BigDecimal.ZERO;

    public DayStat(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income == null ? BigDecimal.ZERO : income;
    }

    public BigDecimal getExpense() {
        return expense;
    }

    public void setExpense(BigDecimal expense) {
        this.expense = expense == null ? BigDecimal.ZERO : expense;
    }

    public int getDay() {
        return date.getDayOfMonth();
    }
}
