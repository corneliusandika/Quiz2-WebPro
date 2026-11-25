package com.dompetku.model;

import java.time.LocalDate;

public class RecurringRunResult {

    private int created;
    private LocalDate nextDate;
    private boolean moreDue;

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public LocalDate getNextDate() {
        return nextDate;
    }

    public void setNextDate(LocalDate nextDate) {
        this.nextDate = nextDate;
    }

    public boolean isMoreDue() {
        return moreDue;
    }

    public void setMoreDue(boolean moreDue) {
        this.moreDue = moreDue;
    }
}
