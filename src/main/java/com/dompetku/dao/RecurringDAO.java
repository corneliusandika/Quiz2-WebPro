package com.dompetku.dao;

import com.dompetku.db.DB;
import com.dompetku.model.RecurringRunResult;
import com.dompetku.model.RecurringTransaction;

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

public class RecurringDAO {

    private static final String SELECT =
            "SELECT r.*, w.name AS wallet_name, c.name AS category_name, "
                    + "c.color AS category_color, c.icon AS category_icon "
                    + "FROM recurring_transactions r "
                    + "JOIN wallets w ON w.id = r.wallet_id "
                    + "LEFT JOIN categories c ON c.id = r.category_id ";

    public List<RecurringTransaction> findAll(int userId) throws SQLException {
        String sql = SELECT + "WHERE r.user_id = ? ORDER BY r.active DESC, r.next_date, r.id";
        List<RecurringTransaction> items = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        }
        return items;
    }

    public RecurringTransaction findById(int id, int userId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE r.id = ? AND r.user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean existsForWallet(int walletId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(*) FROM recurring_transactions WHERE wallet_id = ?")) {
            ps.setInt(1, walletId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void create(RecurringTransaction recurring) throws SQLException {
        String sql = "INSERT INTO recurring_transactions (user_id, wallet_id, category_id, name, type, amount, "
                + "frequency, next_date, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, recurring, false);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    recurring.setId(keys.getInt(1));
                }
            }
        }
    }

    public void update(RecurringTransaction recurring) throws SQLException {
        String sql = "UPDATE recurring_transactions SET wallet_id = ?, category_id = ?, name = ?, type = ?, "
                + "amount = ?, frequency = ?, next_date = ?, active = ? WHERE id = ? AND user_id = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, recurring.getWalletId());
            if (recurring.getCategoryId() == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, recurring.getCategoryId());
            }
            ps.setString(3, recurring.getName());
            ps.setString(4, recurring.getType());
            ps.setBigDecimal(5, recurring.getAmount());
            ps.setString(6, recurring.getFrequency());
            ps.setDate(7, Date.valueOf(recurring.getNextDate()));
            ps.setBoolean(8, recurring.isActive());
            ps.setInt(9, recurring.getId());
            ps.setInt(10, recurring.getUserId());
            ps.executeUpdate();
        }
    }

    public void delete(int id, int userId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM recurring_transactions WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void setActive(int id, int userId, boolean active) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE recurring_transactions SET active = ? WHERE id = ? AND user_id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public RecurringRunResult runNow(int id, int userId) throws SQLException {
        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);
            try {
                RecurringTransaction recurring;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT * FROM recurring_transactions WHERE id = ? AND user_id = ? FOR UPDATE")) {
                    ps.setInt(1, id);
                    ps.setInt(2, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        recurring = rs.next() ? map(rs) : null;
                    }
                }
                if (recurring == null) {
                    con.rollback();
                    return null;
                }
                LocalDate today = LocalDate.now();
                LocalDate scheduled = recurring.getNextDate();
                int created = 0;
                int maxCatchUp = 60;
                if (scheduled.isAfter(today)) {
                    RecurringRunResult result = new RecurringRunResult();
                    result.setCreated(0);
                    result.setNextDate(scheduled);
                    result.setMoreDue(false);
                    con.commit();
                    return result;
                }
                while (!scheduled.isAfter(today) && created < maxCatchUp) {
                    insertRun(con, recurring, scheduled, userId);
                    scheduled = advance(scheduled, recurring.getFrequency());
                    created++;
                }
                boolean moreDue = !scheduled.isAfter(today);
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE recurring_transactions SET next_date = ? WHERE id = ? AND user_id = ?")) {
                    ps.setDate(1, Date.valueOf(scheduled));
                    ps.setInt(2, id);
                    ps.setInt(3, userId);
                    ps.executeUpdate();
                }
                con.commit();
                RecurringRunResult result = new RecurringRunResult();
                result.setCreated(created);
                result.setNextDate(scheduled);
                result.setMoreDue(moreDue);
                return result;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private void insertRun(Connection con, RecurringTransaction recurring, LocalDate date, int userId)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO transactions (user_id, wallet_id, category_id, type, amount, transaction_date, note) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, userId);
            ps.setInt(2, recurring.getWalletId());
            if (recurring.getCategoryId() == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, recurring.getCategoryId());
            }
            ps.setString(4, recurring.getType());
            ps.setBigDecimal(5, recurring.getAmount());
            ps.setDate(6, Date.valueOf(date));
            ps.setString(7, recurring.getName() + " (otomatis)");
            ps.executeUpdate();
        }
    }

    private static LocalDate advance(LocalDate date, String frequency) {
        if ("daily".equals(frequency)) {
            return date.plusDays(1);
        }
        if ("weekly".equals(frequency)) {
            return date.plusWeeks(1);
        }
        return date.plusMonths(1);
    }

    private void bind(PreparedStatement ps, RecurringTransaction recurring, boolean update) throws SQLException {
        if (!update) {
            ps.setInt(1, recurring.getUserId());
            ps.setInt(2, recurring.getWalletId());
            if (recurring.getCategoryId() == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, recurring.getCategoryId());
            }
            ps.setString(4, recurring.getName());
            ps.setString(5, recurring.getType());
            ps.setBigDecimal(6, recurring.getAmount());
            ps.setString(7, recurring.getFrequency());
            ps.setDate(8, Date.valueOf(recurring.getNextDate()));
            ps.setBoolean(9, recurring.isActive());
        }
    }

    private RecurringTransaction map(ResultSet rs) throws SQLException {
        RecurringTransaction item = new RecurringTransaction();
        item.setId(rs.getInt("id"));
        item.setUserId(rs.getInt("user_id"));
        item.setWalletId(rs.getInt("wallet_id"));
        int categoryId = rs.getInt("category_id");
        item.setCategoryId(rs.wasNull() ? null : categoryId);
        item.setName(rs.getString("name"));
        item.setType(rs.getString("type"));
        item.setAmount(rs.getBigDecimal("amount"));
        item.setFrequency(rs.getString("frequency"));
        item.setNextDate(rs.getDate("next_date").toLocalDate());
        item.setActive(rs.getBoolean("active"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            item.setCreatedAt(created.toLocalDateTime());
        }
        try {
            item.setWalletName(rs.getString("wallet_name"));
            item.setCategoryName(rs.getString("category_name"));
            item.setCategoryColor(rs.getString("category_color"));
            item.setCategoryIcon(rs.getString("category_icon"));
        } catch (SQLException ignored) {
            // projection without joins (lock query)
        }
        return item;
    }
}
