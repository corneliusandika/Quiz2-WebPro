package com.dompetku.servlet;

import com.dompetku.dao.AuditDAO;
import com.dompetku.dao.CategoryDAO;
import com.dompetku.dao.TransactionDAO;
import com.dompetku.dao.WalletDAO;
import com.dompetku.model.Category;
import com.dompetku.model.Transaction;
import com.dompetku.model.User;
import com.dompetku.model.Wallet;
import com.dompetku.util.Formats;
import com.dompetku.web.BaseServlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/app/transactions/*")
public class TransactionServlet extends BaseServlet {

    private static final int PAGE_SIZE = 10;

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final WalletDAO walletDAO = new WalletDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = currentUser(req);
        String path = req.getPathInfo();
        try {
            if (path == null || "/".equals(path)) {
                list(req, resp, user);
            } else if ("/new".equals(path)) {
                form(req, resp, user, null);
            } else if (path.startsWith("/edit/")) {
                Integer id = parseInt(path.substring("/edit/".length()));
                Transaction transaction = id == null ? null : transactionDAO.findById(id, user.getId());
                if (transaction == null) {
                    notFound(resp, "Transaksi tidak ditemukan.");
                    return;
                }
                form(req, resp, user, transaction);
            } else if ("/trash".equals(path)) {
                req.setAttribute("items", transactionDAO.findArchived(user.getId(), 100, 0));
                req.setAttribute("total", transactionDAO.countArchived(user.getId()));
                render(req, resp, "transactions/trash");
            } else {
                notFound(resp, "Halaman tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = currentUser(req);
        String path = req.getPathInfo();
        try {
            if ("/create".equals(path)) {
                save(req, resp, user, false);
            } else if ("/update".equals(path)) {
                save(req, resp, user, true);
            } else if ("/delete".equals(path)) {
                int id = intParamOrZero(req, "id");
                Transaction transaction = transactionDAO.findById(id, user.getId());
                if (transaction == null) {
                    notFound(resp, "Transaksi tidak ditemukan.");
                    return;
                }
                if (!transactionDAO.archive(id, user.getId(), user.getId())) {
                    notFound(resp, "Transaksi tidak ditemukan.");
                    return;
                }
                auditDAO.log(user.getId(), "DELETE", "transaction", id, auditDetails(transaction));
                req.getSession().setAttribute("undoTransactionId", id);
                flash(req, "success", "Transaksi berhasil dipindahkan ke sampah.");
                redirect(req, resp, "/app/transactions");
            } else if ("/restore".equals(path)) {
                int id = intParamOrZero(req, "id");
                if (!transactionDAO.restore(id, user.getId())) {
                    notFound(resp, "Transaksi tidak ditemukan di sampah.");
                    return;
                }
                auditDAO.log(user.getId(), "RESTORE", "transaction", id, "Transaksi #" + id + " dipulihkan.");
                flash(req, "success", "Transaksi berhasil dipulihkan.");
                redirect(req, resp, "/app/transactions");
            } else {
                notFound(resp, "Halaman tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void list(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, ServletException, IOException {
        String monthParam = param(req, "month");
        LocalDate monthDate = parseMonth(monthParam);
        String month = monthDate == null ? null : monthKey(monthDate);
        String type = param(req, "type");
        if (!"income".equals(type) && !"expense".equals(type)) {
            type = null;
        }
        Integer categoryId = intParam(req, "category");
        Integer walletId = intParam(req, "wallet");
        String q = param(req, "q");
        int page = Math.max(1, intParamOrZero(req, "page"));

        int total = transactionDAO.countFiltered(user.getId(), month, type, categoryId, walletId, q);
        int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page > totalPages) {
            page = totalPages;
        }
        List<Transaction> items = transactionDAO.find(user.getId(), month, type, categoryId, walletId, q,
                PAGE_SIZE, (page - 1) * PAGE_SIZE);

        req.setAttribute("items", items);
        req.setAttribute("wallets", walletDAO.findAll(user.getId()));
        req.setAttribute("categories", categoryDAO.findAll(user.getId()));
        req.setAttribute("filterMonth", month == null ? "" : month);
        req.setAttribute("filterType", type == null ? "" : type);
        req.setAttribute("filterCategory", categoryId);
        req.setAttribute("filterWallet", walletId);
        req.setAttribute("filterQ", q == null ? "" : q);
        req.setAttribute("page", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("total", total);
        req.setAttribute("monthIncome", transactionDAO.sum(user.getId(), "income", monthDate));
        req.setAttribute("monthExpense", transactionDAO.sum(user.getId(), "expense", monthDate));
        Object undoId = req.getSession().getAttribute("undoTransactionId");
        if (undoId != null) {
            req.setAttribute("undoTransactionId", undoId);
            req.getSession().removeAttribute("undoTransactionId");
        }
        render(req, resp, "transactions/list");
    }

    private void form(HttpServletRequest req, HttpServletResponse resp, User user, Transaction transaction)
            throws SQLException, ServletException, IOException {
        Transaction tx = transaction;
        if (tx == null) {
            tx = new Transaction();
            tx.setUserId(user.getId());
            tx.setType("expense");
            tx.setTransactionDate(LocalDate.now());
            List<Wallet> wallets = walletDAO.findAll(user.getId());
            if (!wallets.isEmpty()) {
                tx.setWalletId(wallets.get(0).getId());
            }
        }
        req.setAttribute("tx", tx);
        req.setAttribute("mode", transaction == null ? "create" : "edit");
        loadFormLists(req, user);
        render(req, resp, "transactions/form");
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, User user, boolean update)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = new HashMap<>();
        Transaction tx = readForm(req, user, errors);
        if (update) {
            if (tx.getId() <= 0) {
                notFound(resp, "Transaksi tidak ditemukan.");
                return;
            }
            if (transactionDAO.findById(tx.getId(), user.getId()) == null) {
                notFound(resp, "Transaksi tidak ditemukan.");
                return;
            }
        }
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("tx", tx);
            req.setAttribute("mode", update ? "edit" : "create");
            loadFormLists(req, user);
            render(req, resp, "transactions/form");
            return;
        }
        if (update) {
            transactionDAO.update(tx);
            Transaction saved = transactionDAO.findById(tx.getId(), user.getId());
            auditDAO.log(user.getId(), "UPDATE", "transaction", tx.getId(),
                    auditDetails(saved == null ? tx : saved));
            flash(req, "success", "Transaksi berhasil diperbarui.");
        } else {
            transactionDAO.create(tx);
            Transaction saved = transactionDAO.findById(tx.getId(), user.getId());
            auditDAO.log(user.getId(), "CREATE", "transaction", tx.getId(),
                    auditDetails(saved == null ? tx : saved));
            flash(req, "success", "Transaksi baru berhasil disimpan.");
        }
        redirect(req, resp, "/app/transactions?month=" + monthKey(tx.getTransactionDate()));
    }

    private void loadFormLists(HttpServletRequest req, User user) throws SQLException {
        req.setAttribute("wallets", walletDAO.findAll(user.getId()));
        req.setAttribute("categories", categoryDAO.findAll(user.getId()));
    }

    private Transaction readForm(HttpServletRequest req, User user, Map<String, String> errors)
            throws SQLException {
        Transaction tx = new Transaction();
        tx.setUserId(user.getId());
        tx.setId(intParamOrZero(req, "id"));

        String type = param(req, "type");
        if (!"income".equals(type) && !"expense".equals(type)) {
            errors.put("type", "Pilih jenis transaksi.");
            type = "expense";
        }
        tx.setType(type);

        BigDecimal amount = decimalParam(req, "amount");
        if (amount == null || amount.signum() <= 0) {
            errors.put("amount", "Nominal harus lebih besar dari 0.");
        } else if (tooLarge(amount)) {
            errors.put("amount", "Nominal terlalu besar.");
        } else if (amount.scale() > 2) {
            amount = amount.setScale(2, RoundingMode.HALF_UP);
        }
        tx.setAmount(amount);

        LocalDate date = dateParam(req, "date");
        if (date == null) {
            errors.put("date", "Tanggal transaksi wajib diisi.");
            date = LocalDate.now();
        }
        tx.setTransactionDate(date);

        Integer walletId = intParam(req, "walletId");
        Wallet wallet = walletId == null ? null : walletDAO.findById(walletId, user.getId());
        if (wallet == null) {
            errors.put("walletId", "Pilih dompet yang valid.");
        } else {
            tx.setWalletId(walletId);
        }

        Integer categoryId = intParam(req, "categoryId");
        if (categoryId != null) {
            Category category = categoryDAO.findById(categoryId, user.getId());
            if (category == null) {
                errors.put("categoryId", "Pilih kategori yang valid.");
            } else if (!category.getType().equals(type)) {
                errors.put("categoryId", "Kategori tidak sesuai dengan jenis transaksi.");
            } else {
                tx.setCategoryId(categoryId);
            }
        }

        tx.setNote(safe(param(req, "note"), 255));
        return tx;
    }

    private Integer parseInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String auditDetails(Transaction tx) {
        StringBuilder details = new StringBuilder();
        details.append("income".equals(tx.getType()) ? "Pemasukan" : "Pengeluaran")
                .append(" ").append(Formats.rupiah(tx.getAmount()))
                .append(" · ").append(Formats.date(tx.getTransactionDate()));
        if (tx.getWalletName() != null) {
            details.append(" · ").append(tx.getWalletName());
        }
        if (tx.getCategoryName() != null) {
            details.append(" · ").append(tx.getCategoryName());
        }
        if (tx.getNote() != null && !tx.getNote().trim().isEmpty()) {
            details.append(" · ").append(tx.getNote());
        }
        return details.toString();
    }
}
