package com.dompetku.dao;

import com.dompetku.db.DB;
import com.dompetku.model.Debt;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class DebtDAO {

    public List<Debt> findAll(int userId, String type, String q) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM debts WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if ("payable".equals(type) || "receivable".equals(type)) {
            sql.append(" AND type = ?");
            params.add(type);
        }
        if (q != null && !q.trim().isEmpty()) {
            sql.append(" AND (counterpart LIKE ? OR note LIKE ?)");
            params.add("%" + q.trim() + "%");
            params.add("%" + q.trim() + "%");
        }
        sql.append(" ORDER BY CASE WHEN paid_amount >= amount THEN 1 ELSE 0 END, due_date IS NULL, due_date, id DESC");
        List<Debt> items = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        }
        return items;
    }

    public Debt findById(int id, int userId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM debts WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void create(Debt debt) throws SQLException {
        String sql = "INSERT INTO debts (user_id, type, counterpart, amount, paid_amount, due_date, note) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, debt.getUserId());
            ps.setString(2, debt.getType());
            ps.setString(3, debt.getCounterpart());
            ps.setBigDecimal(4, debt.getAmount());
            ps.setBigDecimal(5, debt.getPaidAmount());
            if (debt.getDueDate() == null) {
                ps.setNull(6, java.sql.Types.DATE);
            } else {
                ps.setDate(6, Date.valueOf(debt.getDueDate()));
            }
            ps.setString(7, debt.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    debt.setId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Debt debt) throws SQLException {
        String sql = "UPDATE debts SET type = ?, counterpart = ?, amount = ?, paid_amount = ?, "
                + "due_date = ?, note = ? WHERE id = ? AND user_id = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, debt.getType());
            ps.setString(2, debt.getCounterpart());
            ps.setBigDecimal(3, debt.getAmount());
            ps.setBigDecimal(4, debt.getPaidAmount());
            if (debt.getDueDate() == null) {
                ps.setNull(5, java.sql.Types.DATE);
            } else {
                ps.setDate(5, Date.valueOf(debt.getDueDate()));
            }
            ps.setString(6, debt.getNote());
            ps.setInt(7, debt.getId());
            ps.setInt(8, debt.getUserId());
            ps.executeUpdate();
        }
    }

    public void delete(int id, int userId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM debts WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public boolean pay(int id, int userId, BigDecimal amount) throws SQLException {
        String sql = "UPDATE debts SET paid_amount = LEAST(amount, paid_amount + ?) WHERE id = ? AND user_id = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, id);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public BigDecimal remaining(int userId, String type) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount - paid_amount), 0) FROM debts "
                + "WHERE user_id = ? AND type = ? AND paid_amount < amount";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    private Debt map(ResultSet rs) throws SQLException {
        Debt debt = new Debt();
        debt.setId(rs.getInt("id"));
        debt.setUserId(rs.getInt("user_id"));
        debt.setType(rs.getString("type"));
        debt.setCounterpart(rs.getString("counterpart"));
        debt.setAmount(rs.getBigDecimal("amount"));
        debt.setPaidAmount(rs.getBigDecimal("paid_amount"));
        Date due = rs.getDate("due_date");
        if (due != null) {
            debt.setDueDate(due.toLocalDate());
        }
        debt.setNote(rs.getString("note"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            debt.setCreatedAt(created.toLocalDateTime());
        }
        return debt;
    }
}
