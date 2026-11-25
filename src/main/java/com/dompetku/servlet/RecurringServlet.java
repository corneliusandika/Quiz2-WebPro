package com.dompetku.servlet;

import com.dompetku.dao.CategoryDAO;
import com.dompetku.dao.RecurringDAO;
import com.dompetku.dao.WalletDAO;
import com.dompetku.model.Category;
import com.dompetku.model.RecurringRunResult;
import com.dompetku.model.RecurringTransaction;
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
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/app/recurring/*")
public class RecurringServlet extends BaseServlet {

    private final RecurringDAO recurringDAO = new RecurringDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final WalletDAO walletDAO = new WalletDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = currentUser(req);
        String path = req.getPathInfo();
        try {
            if (path == null || "/".equals(path)) {
                List<RecurringTransaction> items = recurringDAO.findAll(user.getId());
                int dueSoon = 0;
                BigDecimal monthlyExpense = BigDecimal.ZERO;
                for (RecurringTransaction item : items) {
                    if (item.isDueSoon()) {
                        dueSoon++;
                    }
                    if (item.isActive() && "expense".equals(item.getType())) {
                        if ("monthly".equals(item.getFrequency())) {
                            monthlyExpense = monthlyExpense.add(item.getAmount());
                        } else if ("weekly".equals(item.getFrequency())) {
                            monthlyExpense = monthlyExpense.add(item.getAmount().multiply(BigDecimal.valueOf(4)));
                        } else {
                            monthlyExpense = monthlyExpense.add(item.getAmount().multiply(BigDecimal.valueOf(30)));
                        }
                    }
                }
                req.setAttribute("items", items);
                req.setAttribute("dueSoon", dueSoon);
                req.setAttribute("monthlyExpense", monthlyExpense);
                render(req, resp, "recurring/list");
            } else if ("/new".equals(path)) {
                form(req, resp, user, null);
            } else if (path.startsWith("/edit/")) {
                Integer id = parseInt(path.substring("/edit/".length()));
                RecurringTransaction item = id == null ? null : recurringDAO.findById(id, user.getId());
                if (item == null) {
                    notFound(resp, "Transaksi berulang tidak ditemukan.");
                    return;
                }
                form(req, resp, user, item);
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
                if (recurringDAO.findById(id, user.getId()) == null) {
                    notFound(resp, "Transaksi berulang tidak ditemukan.");
                    return;
                }
                recurringDAO.delete(id, user.getId());
                flash(req, "success", "Transaksi berulang berhasil dihapus.");
                redirect(req, resp, "/app/recurring");
            } else if ("/toggle".equals(path)) {
                int id = intParamOrZero(req, "id");
                if (recurringDAO.findById(id, user.getId()) == null) {
                    notFound(resp, "Transaksi berulang tidak ditemukan.");
                    return;
                }
                boolean active = "1".equals(param(req, "active"));
                recurringDAO.setActive(id, user.getId(), active);
                flash(req, "success", active ? "Jadwal diaktifkan kembali." : "Jadwal dijeda.");
                redirect(req, resp, "/app/recurring");
            } else if ("/run".equals(path)) {
                int id = intParamOrZero(req, "id");
                RecurringTransaction item = recurringDAO.findById(id, user.getId());
                if (item == null) {
                    notFound(resp, "Transaksi berulang tidak ditemukan.");
                    return;
                }
                if (!item.isActive()) {
                    flash(req, "error", "Aktifkan jadwal terlebih dahulu sebelum menjalankannya.");
                    redirect(req, resp, "/app/recurring");
                    return;
                }
                RecurringRunResult result = recurringDAO.runNow(id, user.getId());
                if (result == null) {
                    flash(req, "error", "Jadwal tidak ditemukan.");
                } else if (result.getCreated() == 0) {
                    flash(req, "error", "Jadwal " + item.getName() + " belum jatuh tempo.");
                } else {
                    String message = result.getCreated() > 1
                            ? result.getCreated() + " transaksi tertunggak berhasil dicatat untuk " + item.getName() + "."
                            : "Transaksi " + item.getName() + " (" + Formats.rupiah(item.getAmount()) + ") berhasil dicatat.";
                    message += " Jadwal berikutnya: " + Formats.date(result.getNextDate()) + ".";
                    if (result.isMoreDue()) {
                        message += " Masih ada jadwal tertunggak; klik Jalankan lagi untuk melanjutkan.";
                    }
                    flash(req, "success", message);
                }
                redirect(req, resp, "/app/recurring");
            } else {
                notFound(resp, "Halaman tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void form(HttpServletRequest req, HttpServletResponse resp, User user, RecurringTransaction item)
            throws SQLException, ServletException, IOException {
        RecurringTransaction form = item;
        if (form == null) {
            form = new RecurringTransaction();
            form.setUserId(user.getId());
            form.setType("expense");
            form.setFrequency("monthly");
            form.setNextDate(LocalDate.now().plusDays(7));
            form.setActive(true);
            List<Wallet> wallets = walletDAO.findAll(user.getId());
            if (!wallets.isEmpty()) {
                form.setWalletId(wallets.get(0).getId());
            }
        }
        req.setAttribute("item", form);
        req.setAttribute("mode", item == null ? "create" : "edit");
        req.setAttribute("wallets", walletDAO.findAll(user.getId()));
        req.setAttribute("categories", categoryDAO.findAll(user.getId()));
        render(req, resp, "recurring/form");
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, User user, boolean update)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = new HashMap<>();
        RecurringTransaction item = new RecurringTransaction();
        item.setUserId(user.getId());
        item.setId(intParamOrZero(req, "id"));

        String name = safe(param(req, "name"), 100);
        if (name == null || name.length() < 2) {
            errors.put("name", "Nama jadwal minimal 2 karakter.");
        }
        item.setName(name);

        String type = param(req, "type");
        if (!"income".equals(type) && !"expense".equals(type)) {
            errors.put("type", "Pilih jenis transaksi.");
            type = "expense";
        }
        item.setType(type);

        Integer walletId = intParam(req, "walletId");
        Wallet wallet = walletId == null ? null : walletDAO.findById(walletId, user.getId());
        if (wallet == null) {
            errors.put("walletId", "Pilih dompet yang valid.");
        } else {
            item.setWalletId(walletId);
        }

        Integer categoryId = intParam(req, "categoryId");
        Category category = categoryId == null ? null : categoryDAO.findById(categoryId, user.getId());
        if (category == null) {
            errors.put("categoryId", "Pilih kategori yang valid.");
        } else if (!category.getType().equals(type)) {
            errors.put("categoryId", "Kategori tidak sesuai dengan jenis transaksi.");
        } else {
            item.setCategoryId(categoryId);
        }

        BigDecimal amount = decimalParam(req, "amount");
        if (amount == null || amount.signum() <= 0) {
            errors.put("amount", "Nominal harus lebih besar dari 0.");
        } else if (tooLarge(amount)) {
            errors.put("amount", "Nominal terlalu besar.");
        }
        item.setAmount(amount);

        String frequency = param(req, "frequency");
        if (!"daily".equals(frequency) && !"weekly".equals(frequency) && !"monthly".equals(frequency)) {
            errors.put("frequency", "Pilih frekuensi yang valid.");
            frequency = "monthly";
        }
        item.setFrequency(frequency);

        LocalDate nextDate = dateParam(req, "nextDate");
        if (nextDate == null) {
            errors.put("nextDate", "Tanggal jadwal berikutnya wajib diisi.");
            nextDate = LocalDate.now();
        }
        item.setNextDate(nextDate);
        item.setActive(req.getParameter("active") != null);

        if (update) {
            if (item.getId() <= 0 || recurringDAO.findById(item.getId(), user.getId()) == null) {
                notFound(resp, "Transaksi berulang tidak ditemukan.");
                return;
            }
        }
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("item", item);
            req.setAttribute("mode", update ? "edit" : "create");
            req.setAttribute("wallets", walletDAO.findAll(user.getId()));
            req.setAttribute("categories", categoryDAO.findAll(user.getId()));
            render(req, resp, "recurring/form");
            return;
        }
        if (update) {
            recurringDAO.update(item);
            flash(req, "success", "Transaksi berulang berhasil diperbarui.");
        } else {
            recurringDAO.create(item);
            flash(req, "success", "Transaksi berulang berhasil dibuat.");
        }
        redirect(req, resp, "/app/recurring");
    }

    private Integer parseInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }
}
