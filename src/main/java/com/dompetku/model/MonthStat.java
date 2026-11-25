package com.dompetku.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MonthStat {

    private final LocalDate month;
    private BigDecimal income = BigDecimal.ZERO;
    private BigDecimal expense = BigDecimal.ZERO;

    public MonthStat(LocalDate month) {
        this.month = month;
    }

    public LocalDate getMonth() {
        return month;
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

    public String getKey() {
        return String.format("%04d-%02d", month.getYear(), month.getMonthValue());
    }

    public String getLabel() {
        String[] names = {"Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des"};
        return names[month.getMonthValue() - 1] + " " + String.format("%02d", month.getYear() % 100);
    }
}
