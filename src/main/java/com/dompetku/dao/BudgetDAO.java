package com.dompetku.dao;

import com.dompetku.db.DB;
import com.dompetku.model.Budget;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BudgetDAO {

    private static final String SELECT =
            "SELECT b.*, c.name AS category_name, c.color AS category_color, c.icon AS category_icon, "
                    + "COALESCE((SELECT SUM(t.amount) FROM transactions t "
                    + "WHERE t.user_id = b.user_id AND t.category_id = b.category_id AND t.type = 'expense' "
                    + "AND t.transaction_date >= b.`month` AND t.transaction_date < DATE_ADD(b.`month`, INTERVAL 1 MONTH)), 0) AS spent "
                    + "FROM budgets b JOIN categories c ON c.id = b.category_id ";

    public List<Budget> findByMonth(int userId, LocalDate month) throws SQLException {
        String sql = SELECT + "WHERE b.user_id = ? AND b.`month` = ? ORDER BY b.id";
        List<Budget> items = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(month.withDayOfMonth(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        }
        return items;
    }

    public Budget findById(int id, int userId) throws SQLException {
        String sql = SELECT + "WHERE b.id = ? AND b.user_id = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean existsForCategory(int userId, int categoryId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(*) FROM budgets WHERE user_id = ? AND category_id = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void create(Budget budget) throws SQLException {
        String sql = "INSERT INTO budgets (user_id, category_id, `month`, amount) VALUES (?, ?, ?, ?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, budget.getUserId());
            ps.setInt(2, budget.getCategoryId());
            ps.setDate(3, Date.valueOf(budget.getMonth().withDayOfMonth(1)));
            ps.setBigDecimal(4, budget.getAmount());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    budget.setId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Budget budget) throws SQLException {
        String sql = "UPDATE budgets SET category_id = ?, `month` = ?, amount = ? WHERE id = ? AND user_id = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, budget.getCategoryId());
            ps.setDate(2, Date.valueOf(budget.getMonth().withDayOfMonth(1)));
            ps.setBigDecimal(3, budget.getAmount());
            ps.setInt(4, budget.getId());
            ps.setInt(5, budget.getUserId());
            ps.executeUpdate();
        }
    }

    public void delete(int id, int userId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM budgets WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public boolean exists(int userId, int categoryId, LocalDate month, Integer excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM budgets WHERE user_id = ? AND category_id = ? AND `month` = ?"
                + (excludeId == null ? "" : " AND id <> ?");
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, categoryId);
            ps.setDate(3, Date.valueOf(month.withDayOfMonth(1)));
            if (excludeId != null) {
                ps.setInt(4, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private Budget map(ResultSet rs) throws SQLException {
        Budget budget = new Budget();
        budget.setId(rs.getInt("id"));
        budget.setUserId(rs.getInt("user_id"));
        budget.setCategoryId(rs.getInt("category_id"));
        budget.setMonth(rs.getDate("month").toLocalDate());
        budget.setAmount(rs.getBigDecimal("amount"));
        budget.setSpent(rs.getBigDecimal("spent"));
        budget.setCategoryName(rs.getString("category_name"));
        budget.setCategoryColor(rs.getString("category_color"));
        budget.setCategoryIcon(rs.getString("category_icon"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            budget.setCreatedAt(created.toLocalDateTime());
        }
        return budget;
    }
}
