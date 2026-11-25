package com.dompetku.dao;

import com.dompetku.db.DB;
import com.dompetku.model.Goal;
import com.dompetku.model.GoalDeposit;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class GoalDAO {

    public List<Goal> findAll(int userId) throws SQLException {
        String sql = "SELECT * FROM goals WHERE user_id = ? ORDER BY created_at DESC, id DESC";
        List<Goal> items = new ArrayList<>();
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

    public Goal findById(int id, int userId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM goals WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void create(Goal goal) throws SQLException {
        String sql = "INSERT INTO goals (user_id, name, target_amount, saved_amount, deadline, color) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, goal.getUserId());
            ps.setString(2, goal.getName());
            ps.setBigDecimal(3, goal.getTargetAmount());
            ps.setBigDecimal(4, goal.getSavedAmount());
            if (goal.getDeadline() == null) {
                ps.setNull(5, java.sql.Types.DATE);
            } else {
                ps.setDate(5, Date.valueOf(goal.getDeadline()));
            }
            ps.setString(6, goal.getColor());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    goal.setId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Goal goal) throws SQLException {
        String sql = "UPDATE goals SET name = ?, target_amount = ?, deadline = ?, color = ? "
                + "WHERE id = ? AND user_id = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, goal.getName());
            ps.setBigDecimal(2, goal.getTargetAmount());
            if (goal.getDeadline() == null) {
                ps.setNull(3, java.sql.Types.DATE);
            } else {
                ps.setDate(3, Date.valueOf(goal.getDeadline()));
            }
            ps.setString(4, goal.getColor());
            ps.setInt(5, goal.getId());
            ps.setInt(6, goal.getUserId());
            ps.executeUpdate();
        }
    }

    public void delete(int id, int userId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM goals WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public List<GoalDeposit> findDeposits(int goalId, int userId) throws SQLException {
        String sql = "SELECT d.*, g.name AS goal_name FROM goal_deposits d "
                + "JOIN goals g ON g.id = d.goal_id WHERE d.goal_id = ? AND g.user_id = ? "
                + "ORDER BY d.deposit_date DESC, d.id DESC";
        List<GoalDeposit> items = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapDeposit(rs));
                }
            }
        }
        return items;
    }

    public GoalDeposit findDeposit(int id, int userId) throws SQLException {
        String sql = "SELECT d.*, g.name AS goal_name FROM goal_deposits d "
                + "JOIN goals g ON g.id = d.goal_id WHERE d.id = ? AND g.user_id = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapDeposit(rs) : null;
            }
        }
    }

    public boolean updateDeposit(GoalDeposit deposit, int userId) throws SQLException {
        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);
            try {
                int goalId;
                java.math.BigDecimal oldAmount;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT d.goal_id, d.amount FROM goal_deposits d JOIN goals g ON g.id = d.goal_id "
                                + "WHERE d.id = ? AND g.user_id = ? FOR UPDATE")) {
                    ps.setInt(1, deposit.getId());
                    ps.setInt(2, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                        goalId = rs.getInt("goal_id");
                        oldAmount = rs.getBigDecimal("amount");
                    }
                }
                if (deposit.getGoalId() != goalId) {
                    con.rollback();
                    return false;
                }
                java.math.BigDecimal delta = deposit.getAmount().subtract(oldAmount);
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE goal_deposits SET amount = ?, deposit_date = ?, note = ? WHERE id = ?")) {
                    ps.setBigDecimal(1, deposit.getAmount());
                    ps.setDate(2, Date.valueOf(deposit.getDepositDate()));
                    ps.setString(3, deposit.getNote());
                    ps.setInt(4, deposit.getId());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE goals SET saved_amount = GREATEST(0, saved_amount + ?) WHERE id = ? AND user_id = ?")) {
                    ps.setBigDecimal(1, delta);
                    ps.setInt(2, goalId);
                    ps.setInt(3, userId);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public boolean addDeposit(GoalDeposit deposit, int userId) throws SQLException {
        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);
            try {
                boolean owned;
                try (PreparedStatement check = con.prepareStatement(
                        "SELECT id FROM goals WHERE id = ? AND user_id = ?")) {
                    check.setInt(1, deposit.getGoalId());
                    check.setInt(2, userId);
                    try (ResultSet rs = check.executeQuery()) {
                        owned = rs.next();
                    }
                }
                if (!owned) {
                    con.rollback();
                    return false;
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO goal_deposits (goal_id, amount, deposit_date, note) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, deposit.getGoalId());
                    ps.setBigDecimal(2, deposit.getAmount());
                    ps.setDate(3, Date.valueOf(deposit.getDepositDate()));
                    ps.setString(4, deposit.getNote());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE goals SET saved_amount = saved_amount + ? WHERE id = ?")) {
                    ps.setBigDecimal(1, deposit.getAmount());
                    ps.setInt(2, deposit.getGoalId());
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public boolean deleteDeposit(int depositId, int userId) throws SQLException {
        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);
            try {
                int goalId;
                java.math.BigDecimal amount;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT d.goal_id, d.amount FROM goal_deposits d JOIN goals g ON g.id = d.goal_id "
                                + "WHERE d.id = ? AND g.user_id = ?")) {
                    ps.setInt(1, depositId);
                    ps.setInt(2, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                        goalId = rs.getInt("goal_id");
                        amount = rs.getBigDecimal("amount");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM goal_deposits WHERE id = ?")) {
                    ps.setInt(1, depositId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE goals SET saved_amount = GREATEST(0, saved_amount - ?) WHERE id = ?")) {
                    ps.setBigDecimal(1, amount);
                    ps.setInt(2, goalId);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private Goal map(ResultSet rs) throws SQLException {
        Goal goal = new Goal();
        goal.setId(rs.getInt("id"));
        goal.setUserId(rs.getInt("user_id"));
        goal.setName(rs.getString("name"));
        goal.setTargetAmount(rs.getBigDecimal("target_amount"));
        goal.setSavedAmount(rs.getBigDecimal("saved_amount"));
        Date deadline = rs.getDate("deadline");
        if (deadline != null) {
            goal.setDeadline(deadline.toLocalDate());
        }
        goal.setColor(rs.getString("color"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            goal.setCreatedAt(created.toLocalDateTime());
        }
        return goal;
    }

    private GoalDeposit mapDeposit(ResultSet rs) throws SQLException {
        GoalDeposit deposit = new GoalDeposit();
        deposit.setId(rs.getInt("id"));
        deposit.setGoalId(rs.getInt("goal_id"));
        deposit.setAmount(rs.getBigDecimal("amount"));
        deposit.setDepositDate(rs.getDate("deposit_date").toLocalDate());
        deposit.setNote(rs.getString("note"));
        deposit.setGoalName(rs.getString("goal_name"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            deposit.setCreatedAt(created.toLocalDateTime());
        }
        return deposit;
    }
}
