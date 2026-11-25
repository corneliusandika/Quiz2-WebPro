package com.dompetku.dao;

import com.dompetku.db.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class AuthTokenDAO {

    public static final String PASSWORD_RESET = "password_reset";
    public static final String EMAIL_VERIFICATION = "email_verification";

    public void create(int userId, String purpose, String tokenHash, LocalDateTime expiresAt)
            throws SQLException {
        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM auth_tokens WHERE user_id = ? AND purpose = ?")) {
                    ps.setInt(1, userId);
                    ps.setString(2, purpose);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO auth_tokens (user_id, purpose, token_hash, expires_at) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, userId);
                    ps.setString(2, purpose);
                    ps.setString(3, tokenHash);
                    ps.setTimestamp(4, Timestamp.valueOf(expiresAt));
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public Integer findUserId(String tokenHash, String purpose) throws SQLException {
        String sql = "SELECT user_id FROM auth_tokens WHERE token_hash = ? AND purpose = ? "
                + "AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            ps.setString(2, purpose);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    public boolean hasPending(int userId, String purpose) throws SQLException {
        String sql = "SELECT COUNT(*) FROM auth_tokens WHERE user_id = ? AND purpose = ? "
                + "AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, purpose);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean consume(String tokenHash, String purpose) throws SQLException {
        String sql = "UPDATE auth_tokens SET used_at = CURRENT_TIMESTAMP "
                + "WHERE token_hash = ? AND purpose = ? AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            ps.setString(2, purpose);
            return ps.executeUpdate() > 0;
        }
    }

    public int pruneExpired() throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM auth_tokens WHERE expires_at < CURRENT_TIMESTAMP OR used_at IS NOT NULL")) {
            return ps.executeUpdate();
        }
    }
}
