package com.dompetku.dao;

import com.dompetku.db.DB;
import com.dompetku.model.CategoryTotal;
import com.dompetku.model.DayStat;
import com.dompetku.model.MonthStat;
import com.dompetku.model.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionDAO {

    private static final String SELECT =
            "SELECT t.*, w.name AS wallet_name, c.name AS category_name, "
                    + "c.color AS category_color, c.icon AS category_icon "
                    + "FROM transactions t "
                    + "JOIN wallets w ON w.id = t.wallet_id "
                    + "LEFT JOIN categories c ON c.id = t.category_id ";

    public List<Transaction> find(int userId, String month, String type, Integer categoryId,
                                  Integer walletId, String q, int limit, int offset) throws SQLException {
        Query query = buildQuery(userId, month, type, categoryId, walletId, q);
        String sql = SELECT + query.where + " ORDER BY t.transaction_date DESC, t.id DESC LIMIT ? OFFSET ?";
        List<Transaction> items = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int index = bind(ps, query.params, 1);
            ps.setInt(index++, limit);
            ps.setInt(index, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        }
        return items;
    }

    public int countFiltered(int userId, String month, String type, Integer categoryId,
                             Integer walletId, String q) throws SQLException {
        Query query = buildQuery(userId, month, type, categoryId, walletId, q);
        String sql = "SELECT COUNT(*) FROM transactions t "
                + "JOIN wallets w ON w.id = t.wallet_id "
                + "LEFT JOIN categories c ON c.id = t.category_id " + query.where;
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, query.params, 1);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public Transaction findById(int id, int userId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE t.id = ? AND t.user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void create(Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transactions (user_id, wallet_id, category_id, type, amount, transaction_date, note) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindTransaction(ps, transaction);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    transaction.setId(keys.getInt(1));
                }
            }
        }
    }

    public void createBatch(List<Transaction> transactions) throws SQLException {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO transactions (user_id, wallet_id, category_id, type, amount, transaction_date, note) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (Transaction transaction : transactions) {
                    bindTransaction(ps, transaction);
                    ps.addBatch();
                }
                ps.executeBatch();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public List<Transaction> findDuplicateCandidates(int userId, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = "SELECT wallet_id, category_id, type, amount, transaction_date, note "
                + "FROM transactions WHERE user_id = ? AND transaction_date BETWEEN ? AND ?";
        List<Transaction> items = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Transaction transaction = new Transaction();
                    transaction.setUserId(userId);
                    transaction.setWalletId(rs.getInt("wallet_id"));
                    int categoryId = rs.getInt("category_id");
                    transaction.setCategoryId(rs.wasNull() ? null : categoryId);
                    transaction.setType(rs.getString("type"));
                    transaction.setAmount(rs.getBigDecimal("amount"));
                    transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
                    transaction.setNote(rs.getString("note"));
                    items.add(transaction);
                }
            }
        }
        return items;
    }

    public void update(Transaction transaction) throws SQLException {
        String sql = "UPDATE transactions SET wallet_id = ?, category_id = ?, type = ?, amount = ?, "
                + "transaction_date = ?, note = ? WHERE id = ? AND user_id = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, transaction.getWalletId());
            if (transaction.getCategoryId() == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, transaction.getCategoryId());
            }
            ps.setString(3, transaction.getType());
            ps.setBigDecimal(4, transaction.getAmount());
            ps.setDate(5, Date.valueOf(transaction.getTransactionDate()));
            ps.setString(6, transaction.getNote());
            ps.setInt(7, transaction.getId());
            ps.setInt(8, transaction.getUserId());
            ps.executeUpdate();
        }
    }

    public void delete(int id, int userId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM transactions WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /** Moves a transaction to the trash table so it can be restored. */
    public boolean archive(int id, int userId, Integer deletedBy) throws SQLException {
        String insert = "INSERT INTO transactions_archive "
                + "(id, user_id, wallet_id, category_id, type, amount, transaction_date, note, created_at, deleted_by) "
                + "SELECT id, user_id, wallet_id, category_id, type, amount, transaction_date, note, created_at, ? "
                + "FROM transactions WHERE id = ? AND user_id = ?";
        String remove = "DELETE FROM transactions WHERE id = ? AND user_id = ?";
        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(insert)) {
                    if (deletedBy == null) {
                        ps.setNull(1, java.sql.Types.INTEGER);
                    } else {
                        ps.setInt(1, deletedBy);
                    }
                    ps.setInt(2, id);
                    ps.setInt(3, userId);
                    if (ps.executeUpdate() == 0) {
                        con.rollback();
                        return false;
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(remove)) {
                    ps.setInt(1, id);
                    ps.setInt(2, userId);
                    if (ps.executeUpdate() == 0) {
                        con.rollback();
                        return false;
                    }
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

    /** Restores a trashed transaction, dropping a category that no longer exists. */
    public boolean restore(int id, int userId) throws SQLException {
        String insert = "INSERT INTO transactions "
                + "(id, user_id, wallet_id, category_id, type, amount, transaction_date, note, created_at) "
                + "SELECT a.id, a.user_id, a.wallet_id, "
                + "(SELECT c.id FROM categories c WHERE c.id = a.category_id AND c.user_id = a.user_id), "
                + "a.type, a.amount, a.transaction_date, a.note, a.created_at "
                + "FROM transactions_archive a "
                + "JOIN wallets w ON w.id = a.wallet_id AND w.user_id = a.user_id "
                + "WHERE a.id = ? AND a.user_id = ?";
        String remove = "DELETE FROM transactions_archive WHERE id = ? AND user_id = ?";
        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(insert)) {
                    ps.setInt(1, id);
                    ps.setInt(2, userId);
                    if (ps.executeUpdate() == 0) {
                        con.rollback();
                        return false;
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(remove)) {
                    ps.setInt(1, id);
                    ps.setInt(2, userId);
                    if (ps.executeUpdate() == 0) {
                        con.rollback();
                        return false;
                    }
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

    public BigDecimal sum(int userId, String type, LocalDate month) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE user_id = ? AND type = ?");
        if (month != null) {
            sql.append(" AND transaction_date >= ? AND transaction_date < ?");
        }
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            if (month != null) {
                ps.setDate(3, Date.valueOf(month.withDayOfMonth(1)));
                ps.setDate(4, Date.valueOf(month.withDayOfMonth(1).plusMonths(1)));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    public List<Transaction> recent(int userId, int limit) throws SQLException {
        return find(userId, null, null, null, null, null, limit, 0);
    }

    public LocalDate latestMonth(int userId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT MAX(transaction_date) FROM transactions WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date max = rs.getDate(1);
                    if (max != null) {
                        return max.toLocalDate().withDayOfMonth(1);
                    }
                }
            }
        }
        return LocalDate.now().withDayOfMonth(1);
    }

    public List<MonthStat> monthlySeries(int userId, LocalDate startMonth, int count) throws SQLException {
        List<MonthStat> stats = new ArrayList<>();
        Map<String, MonthStat> byKey = new HashMap<>();
        for (int i = 0; i < count; i++) {
            MonthStat stat = new MonthStat(startMonth.plusMonths(i));
            stats.add(stat);
            byKey.put(stat.getKey(), stat);
        }
        LocalDate end = startMonth.plusMonths(count);
        String sql = "SELECT DATE_FORMAT(transaction_date, '%Y-%m') AS ym, type, SUM(amount) AS total "
                + "FROM transactions WHERE user_id = ? AND transaction_date >= ? AND transaction_date < ? "
                + "GROUP BY ym, type";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(startMonth));
            ps.setDate(3, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MonthStat stat = byKey.get(rs.getString("ym"));
                    if (stat != null) {
                        if ("income".equals(rs.getString("type"))) {
                            stat.setIncome(rs.getBigDecimal("total"));
                        } else {
                            stat.setExpense(rs.getBigDecimal("total"));
                        }
                    }
                }
            }
        }
        return stats;
    }

    public List<DayStat> dailySeries(int userId, LocalDate start, LocalDate endExclusive) throws SQLException {
        List<DayStat> stats = new ArrayList<>();
        Map<LocalDate, DayStat> byDate = new HashMap<>();
        for (LocalDate day = start; day.isBefore(endExclusive); day = day.plusDays(1)) {
            DayStat stat = new DayStat(day);
            stats.add(stat);
            byDate.put(day, stat);
        }
        String sql = "SELECT transaction_date, type, SUM(amount) AS total FROM transactions "
                + "WHERE user_id = ? AND transaction_date >= ? AND transaction_date < ? "
                + "GROUP BY transaction_date, type";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(endExclusive));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DayStat stat = byDate.get(rs.getDate("transaction_date").toLocalDate());
                    if (stat != null) {
                        if ("income".equals(rs.getString("type"))) {
                            stat.setIncome(rs.getBigDecimal("total"));
                        } else {
                            stat.setExpense(rs.getBigDecimal("total"));
                        }
                    }
                }
            }
        }
        return stats;
    }

    public List<CategoryTotal> expenseByCategory(int userId, LocalDate month) throws SQLException {
        String sql = "SELECT COALESCE(c.name, 'Tanpa kategori') AS name, COALESCE(c.color, '#94a3b8') AS color, "
                + "SUM(t.amount) AS total FROM transactions t "
                + "LEFT JOIN categories c ON c.id = t.category_id "
                + "WHERE t.user_id = ? AND t.type = 'expense' AND t.transaction_date >= ? AND t.transaction_date < ? "
                + "GROUP BY c.id, c.name, c.color ORDER BY total DESC";
        List<CategoryTotal> items = new ArrayList<>();
        BigDecimal grand = BigDecimal.ZERO;
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(month.withDayOfMonth(1)));
            ps.setDate(3, Date.valueOf(month.withDayOfMonth(1).plusMonths(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CategoryTotal item = new CategoryTotal();
                    item.setName(rs.getString("name"));
                    item.setColor(rs.getString("color"));
                    item.setTotal(rs.getBigDecimal("total"));
                    items.add(item);
                    grand = grand.add(item.getTotal());
                }
            }
        }
        if (grand.signum() > 0) {
            for (CategoryTotal item : items) {
                item.setPercentage(item.getTotal().multiply(BigDecimal.valueOf(100))
                        .divide(grand, 0, RoundingMode.HALF_UP).intValue());
            }
        }
        return items;
    }

    public List<Transaction> topExpenses(int userId, LocalDate month, int limit) throws SQLException {
        String sql = SELECT + "WHERE t.user_id = ? AND t.type = 'expense' AND t.transaction_date >= ? "
                + "AND t.transaction_date < ? ORDER BY t.amount DESC, t.id DESC LIMIT ?";
        List<Transaction> items = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(month.withDayOfMonth(1)));
            ps.setDate(3, Date.valueOf(month.withDayOfMonth(1).plusMonths(1)));
            ps.setInt(4, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        }
        return items;
    }

    public int countAll(int userId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM transactions WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<Transaction> findArchived(int userId, int limit, int offset) throws SQLException {
        String sql = "SELECT a.*, w.name AS wallet_name, c.name AS category_name, "
                + "c.color AS category_color, c.icon AS category_icon "
                + "FROM transactions_archive a "
                + "LEFT JOIN wallets w ON w.id = a.wallet_id "
                + "LEFT JOIN categories c ON c.id = a.category_id "
                + "WHERE a.user_id = ? ORDER BY a.deleted_at DESC, a.id DESC LIMIT ? OFFSET ?";
        List<Transaction> items = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        }
        return items;
    }

    public int countArchived(int userId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(*) FROM transactions_archive WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static final class Query {
        private final StringBuilder where = new StringBuilder(" WHERE t.user_id = ? ");
        private final List<Object> params = new ArrayList<>();
    }

    private Query buildQuery(int userId, String month, String type, Integer categoryId,
                             Integer walletId, String q) {
        Query query = new Query();
        query.params.add(userId);
        if (month != null && month.matches("\\d{4}-\\d{2}")) {
            try {
                LocalDate start = LocalDate.of(Integer.parseInt(month.substring(0, 4)),
                        Integer.parseInt(month.substring(5, 7)), 1);
                query.where.append(" AND t.transaction_date >= ? AND t.transaction_date < ? ");
                query.params.add(start);
                query.params.add(start.plusMonths(1));
            } catch (java.time.DateTimeException ignored) {
                // Invalid month values are treated as no month filter.
            }
        }
        if ("income".equals(type) || "expense".equals(type)) {
            query.where.append(" AND t.type = ? ");
            query.params.add(type);
        }
        if (categoryId != null) {
            query.where.append(" AND t.category_id = ? ");
            query.params.add(categoryId);
        }
        if (walletId != null) {
            query.where.append(" AND t.wallet_id = ? ");
            query.params.add(walletId);
        }
        if (q != null && !q.trim().isEmpty()) {
            query.where.append(" AND (t.note LIKE ? OR c.name LIKE ? OR w.name LIKE ?) ");
            String like = "%" + q.trim() + "%";
            query.params.add(like);
            query.params.add(like);
            query.params.add(like);
        }
        return query;
    }

    private int bind(PreparedStatement ps, List<Object> params, int startIndex) throws SQLException {
        int index = startIndex;
        for (Object param : params) {
            if (param instanceof Integer) {
                ps.setInt(index++, (Integer) param);
            } else if (param instanceof LocalDate) {
                ps.setDate(index++, Date.valueOf((LocalDate) param));
            } else if (param instanceof BigDecimal) {
                ps.setBigDecimal(index++, (BigDecimal) param);
            } else {
                ps.setString(index++, String.valueOf(param));
            }
        }
        return index;
    }

    private void bindTransaction(PreparedStatement ps, Transaction t) throws SQLException {
        ps.setInt(1, t.getUserId());
        ps.setInt(2, t.getWalletId());
        if (t.getCategoryId() == null) {
            ps.setNull(3, java.sql.Types.INTEGER);
        } else {
            ps.setInt(3, t.getCategoryId());
        }
        ps.setString(4, t.getType());
        ps.setBigDecimal(5, t.getAmount());
        ps.setDate(6, Date.valueOf(t.getTransactionDate()));
        ps.setString(7, t.getNote());
    }

    private Transaction map(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getInt("id"));
        t.setUserId(rs.getInt("user_id"));
        t.setWalletId(rs.getInt("wallet_id"));
        int categoryId = rs.getInt("category_id");
        t.setCategoryId(rs.wasNull() ? null : categoryId);
        t.setType(rs.getString("type"));
        t.setAmount(rs.getBigDecimal("amount"));
        t.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
        t.setNote(rs.getString("note"));
        t.setWalletName(rs.getString("wallet_name"));
        t.setCategoryName(rs.getString("category_name"));
        t.setCategoryColor(rs.getString("category_color"));
        t.setCategoryIcon(rs.getString("category_icon"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            t.setCreatedAt(created.toLocalDateTime());
        }
        return t;
    }
}
