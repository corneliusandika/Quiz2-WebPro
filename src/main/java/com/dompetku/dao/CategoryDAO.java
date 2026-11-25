package com.dompetku.dao;

import com.dompetku.db.DB;
import com.dompetku.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    public List<Category> findAll(int userId) throws SQLException {
        String sql = "SELECT c.*, (SELECT COUNT(*) FROM transactions t WHERE t.category_id = c.id) AS tx_count "
                + "FROM categories c WHERE c.user_id = ? ORDER BY c.type DESC, c.name";
        List<Category> items = new ArrayList<>();
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

    public List<Category> findByType(int userId, String type) throws SQLException {
        String sql = "SELECT c.*, (SELECT COUNT(*) FROM transactions t WHERE t.category_id = c.id) AS tx_count "
                + "FROM categories c WHERE c.user_id = ? AND c.type = ? ORDER BY c.name";
        List<Category> items = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        }
        return items;
    }

    public Category findById(int id, int userId) throws SQLException {
        String sql = "SELECT c.*, (SELECT COUNT(*) FROM transactions t WHERE t.category_id = c.id) AS tx_count "
                + "FROM categories c WHERE c.id = ? AND c.user_id = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void create(Category category) throws SQLException {
        String sql = "INSERT INTO categories (user_id, name, type, color, icon) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, category.getUserId());
            ps.setString(2, category.getName());
            ps.setString(3, category.getType());
            ps.setString(4, category.getColor());
            ps.setString(5, category.getIcon());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    category.setId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ?, type = ?, color = ?, icon = ? WHERE id = ? AND user_id = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getType());
            ps.setString(3, category.getColor());
            ps.setString(4, category.getIcon());
            ps.setInt(5, category.getId());
            ps.setInt(6, category.getUserId());
            ps.executeUpdate();
        }
    }

    public void delete(int id, int userId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM categories WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public boolean exists(int userId, String name, String type, Integer excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categories WHERE user_id = ? AND name = ? AND type = ?"
                + (excludeId == null ? "" : " AND id <> ?");
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setString(3, type);
            if (excludeId != null) {
                ps.setInt(4, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private Category map(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setId(rs.getInt("id"));
        category.setUserId(rs.getInt("user_id"));
        category.setName(rs.getString("name"));
        category.setType(rs.getString("type"));
        category.setColor(rs.getString("color"));
        category.setIcon(rs.getString("icon"));
        category.setTransactionCount(rs.getInt("tx_count"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            category.setCreatedAt(created.toLocalDateTime());
        }
        return category;
    }
}
