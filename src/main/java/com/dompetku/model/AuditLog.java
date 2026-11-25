package com.dompetku.model;

import java.time.LocalDateTime;

public class AuditLog {

    private int id;
    private int userId;
    private String action;
    private String entityType;
    private Integer entityId;
    private String details;
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getActionLabel() {
        if ("CREATE".equals(action)) { return "Transaksi ditambahkan"; }
        if ("UPDATE".equals(action)) { return "Transaksi diperbarui"; }
        if ("DELETE".equals(action)) { return "Transaksi dipindahkan ke sampah"; }
        if ("RESTORE".equals(action)) { return "Transaksi dipulihkan"; }
        if ("IMPORT".equals(action)) { return "Import CSV"; }
        if ("PASSWORD_RESET".equals(action)) { return "Password direset"; }
        return action;
    }
}
