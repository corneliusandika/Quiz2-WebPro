package com.dompetku.servlet;

import com.dompetku.dao.RecurringDAO;
import com.dompetku.dao.WalletDAO;
import com.dompetku.model.User;
import com.dompetku.model.Wallet;
import com.dompetku.web.BaseServlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/app/wallets/*")
public class WalletServlet extends BaseServlet {

    private final WalletDAO walletDAO = new WalletDAO();
    private final RecurringDAO recurringDAO = new RecurringDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = currentUser(req);
        String path = req.getPathInfo();
        try {
            if (path == null || "/".equals(path)) {
                req.setAttribute("items", walletDAO.findAll(user.getId()));
                req.setAttribute("totalBalance", walletDAO.totalBalance(user.getId()));
                render(req, resp, "wallets/list");
            } else if ("/new".equals(path)) {
                form(req, resp, user, null);
            } else if (path.startsWith("/edit/")) {
                Integer id = parseInt(path.substring("/edit/".length()));
                Wallet wallet = id == null ? null : walletDAO.findById(id, user.getId());
                if (wallet == null) {
                    notFound(resp, "Dompet tidak ditemukan.");
                    return;
                }
                form(req, resp, user, wallet);
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
                Wallet wallet = walletDAO.findById(id, user.getId());
                if (wallet == null) {
                    notFound(resp, "Dompet tidak ditemukan.");
                    return;
                }
                if (walletDAO.hasTransactions(id)) {
                    flash(req, "error", "Dompet tidak bisa dihapus karena masih memiliki transaksi.");
                } else if (recurringDAO.existsForWallet(id)) {
                    flash(req, "error", "Dompet tidak bisa dihapus karena masih dipakai oleh transaksi berulang.");
                } else {
                    walletDAO.delete(id, user.getId());
                    flash(req, "success", "Dompet berhasil dihapus.");
                }
                redirect(req, resp, "/app/wallets");
            } else {
                notFound(resp, "Halaman tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void form(HttpServletRequest req, HttpServletResponse resp, User user, Wallet wallet)
            throws ServletException, IOException {
        Wallet item = wallet;
        if (item == null) {
            item = new Wallet();
            item.setUserId(user.getId());
            item.setType("cash");
            item.setColor("#2563eb");
        }
        req.setAttribute("wallet", item);
        req.setAttribute("mode", wallet == null ? "create" : "edit");
        render(req, resp, "wallets/form");
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, User user, boolean update)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = new HashMap<>();
        Wallet wallet = readForm(req, user, errors);
        if (update) {
            if (wallet.getId() <= 0 || walletDAO.findById(wallet.getId(), user.getId()) == null) {
                notFound(resp, "Dompet tidak ditemukan.");
                return;
            }
        }
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("wallet", wallet);
            req.setAttribute("mode", update ? "edit" : "create");
            render(req, resp, "wallets/form");
            return;
        }
        if (update) {
            walletDAO.update(wallet);
            flash(req, "success", "Dompet berhasil diperbarui.");
        } else {
            walletDAO.create(wallet);
            flash(req, "success", "Dompet baru berhasil dibuat.");
        }
        redirect(req, resp, "/app/wallets");
    }

    private Wallet readForm(HttpServletRequest req, User user, Map<String, String> errors) {
        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId());
        wallet.setId(intParamOrZero(req, "id"));

        String name = safe(param(req, "name"), 80);
        if (name == null || name.length() < 2) {
            errors.put("name", "Nama dompet minimal 2 karakter.");
        }
        wallet.setName(name);

        String type = param(req, "type");
        if (!"cash".equals(type) && !"bank".equals(type) && !"ewallet".equals(type)) {
            errors.put("type", "Pilih tipe dompet.");
            type = "cash";
        }
        wallet.setType(type);

        BigDecimal balance = decimalParam(req, "initialBalance");
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        if (balance.signum() < 0) {
            errors.put("initialBalance", "Saldo awal tidak boleh negatif.");
        } else if (tooLarge(balance)) {
            errors.put("initialBalance", "Saldo awal terlalu besar.");
        }
        wallet.setInitialBalance(balance);

        String color = param(req, "color");
        if (color == null || !color.matches("^#[0-9a-fA-F]{6}$")) {
            color = "#2563eb";
        }
        wallet.setColor(color);
        return wallet;
    }

    private Integer parseInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }
}
