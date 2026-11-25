package com.dompetku.dao;

import com.dompetku.db.DB;
import com.dompetku.model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditDAO {

    public void log(int userId, String action, String entityType, Integer entityId, String details)
            throws SQLException {
        String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, entityType);
            if (entityId == null) {
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setInt(4, entityId);
            }
            ps.setString(5, details == null ? "-"
                    : details.length() > 500 ? details.substring(0, 500) : details);
            ps.executeUpdate();
        }
    }

    public List<AuditLog> recent(int userId, int limit) throws SQLException {
        String sql = "SELECT * FROM audit_logs WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT ?";
        List<AuditLog> items = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLog log = new AuditLog();
                    log.setId(rs.getInt("id"));
                    log.setUserId(rs.getInt("user_id"));
                    log.setAction(rs.getString("action"));
                    log.setEntityType(rs.getString("entity_type"));
                    int entityId = rs.getInt("entity_id");
                    log.setEntityId(rs.wasNull() ? null : entityId);
                    log.setDetails(rs.getString("details"));
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        log.setCreatedAt(createdAt.toLocalDateTime());
                    }
                    items.add(log);
                }
            }
        }
        return items;
    }

    public int pruneOlderThan(int days) throws SQLException {
        if (days <= 0) {
            return 0;
        }
        String sql = "DELETE FROM audit_logs WHERE created_at < ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(days)));
            return ps.executeUpdate();
        }
    }
}
