package com.dompetku.service;

import com.dompetku.db.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Creates a friendly starting set of categories and wallets for every new account. */
public final class DefaultDataService {

    private static final String[][] CATEGORIES = {
            {"Gaji", "income", "#10b981", "salary"},
            {"Freelance", "income", "#0ea5e9", "briefcase"},
            {"Investasi", "income", "#8b5cf6", "trending-up"},
            {"Bonus", "income", "#f59e0b", "gift"},
            {"Makan & Minum", "expense", "#f97316", "utensils"},
            {"Transportasi", "expense", "#3b82f6", "car"},
            {"Belanja", "expense", "#ec4899", "shopping-bag"},
            {"Tagihan", "expense", "#ef4444", "receipt"},
            {"Hiburan", "expense", "#a855f7", "film"},
            {"Kesehatan", "expense", "#14b8a6", "heart-pulse"},
            {"Pendidikan", "expense", "#6366f1", "book"},
            {"Kos", "expense", "#64748b", "home"},
            {"Kopi & Jajan", "expense", "#d97706", "coffee"}
    };

    private static final String[][] WALLETS = {
            {"Dompet Tunai", "cash", "0", "#f59e0b"},
            {"Rekening Bank", "bank", "0", "#2563eb"},
            {"E-Wallet", "ewallet", "0", "#10b981"}
    };

    private DefaultDataService() {
    }

    public static void seed(int userId) throws SQLException {
        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO categories (user_id, name, type, color, icon) VALUES (?, ?, ?, ?, ?)")) {
                    for (String[] category : CATEGORIES) {
                        ps.setInt(1, userId);
                        ps.setString(2, category[0]);
                        ps.setString(3, category[1]);
                        ps.setString(4, category[2]);
                        ps.setString(5, category[3]);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO wallets (user_id, name, type, initial_balance, color) VALUES (?, ?, ?, ?, ?)")) {
                    for (String[] wallet : WALLETS) {
                        ps.setInt(1, userId);
                        ps.setString(2, wallet[0]);
                        ps.setString(3, wallet[1]);
                        ps.setBigDecimal(4, new java.math.BigDecimal(wallet[2]));
                        ps.setString(5, wallet[3]);
                        ps.addBatch();
                    }
                    ps.executeBatch();
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
}
