package com.dompetku.dao;

import com.dompetku.db.DB;
import com.dompetku.model.Wallet;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class WalletDAO {

    private static final String BALANCE_QUERY =
            "w.initial_balance + COALESCE((SELECT SUM(CASE WHEN t.type = 'income' THEN t.amount ELSE -t.amount END) "
                    + "FROM transactions t WHERE t.wallet_id = w.id), 0) AS balance";

    public List<Wallet> findAll(int userId) throws SQLException {
        String sql = "SELECT w.*, " + BALANCE_QUERY + " FROM wallets w WHERE w.user_id = ? ORDER BY w.id";
        List<Wallet> items = new ArrayList<>();
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

    public Wallet findById(int id, int userId) throws SQLException {
        String sql = "SELECT w.*, " + BALANCE_QUERY + " FROM wallets w WHERE w.id = ? AND w.user_id = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void create(Wallet wallet) throws SQLException {
        String sql = "INSERT INTO wallets (user_id, name, type, initial_balance, color) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, wallet.getUserId());
            ps.setString(2, wallet.getName());
            ps.setString(3, wallet.getType());
            ps.setBigDecimal(4, wallet.getInitialBalance());
            ps.setString(5, wallet.getColor());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    wallet.setId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Wallet wallet) throws SQLException {
        String sql = "UPDATE wallets SET name = ?, type = ?, initial_balance = ?, color = ? WHERE id = ? AND user_id = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, wallet.getName());
            ps.setString(2, wallet.getType());
            ps.setBigDecimal(3, wallet.getInitialBalance());
            ps.setString(4, wallet.getColor());
            ps.setInt(5, wallet.getId());
            ps.setInt(6, wallet.getUserId());
            ps.executeUpdate();
        }
    }

    public void delete(int id, int userId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM wallets WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public boolean hasTransactions(int walletId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT (SELECT COUNT(*) FROM transactions WHERE wallet_id = ?) "
                             + "+ (SELECT COUNT(*) FROM transactions_archive WHERE wallet_id = ?)")) {
            ps.setInt(1, walletId);
            ps.setInt(2, walletId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public BigDecimal totalBalance(int userId) throws SQLException {
        String sql = "SELECT COALESCE((SELECT SUM(initial_balance) FROM wallets WHERE user_id = ?), 0) "
                + "+ COALESCE((SELECT SUM(CASE WHEN type = 'income' THEN amount ELSE -amount END) "
                + "FROM transactions WHERE user_id = ?), 0) AS total";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO;
            }
        }
    }

    private Wallet map(ResultSet rs) throws SQLException {
        Wallet wallet = new Wallet();
        wallet.setId(rs.getInt("id"));
        wallet.setUserId(rs.getInt("user_id"));
        wallet.setName(rs.getString("name"));
        wallet.setType(rs.getString("type"));
        wallet.setInitialBalance(rs.getBigDecimal("initial_balance"));
        wallet.setColor(rs.getString("color"));
        BigDecimal balance = rs.getBigDecimal("balance");
        wallet.setBalance(balance == null ? wallet.getInitialBalance() : balance);
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            wallet.setCreatedAt(created.toLocalDateTime());
        }
        return wallet;
    }
}
