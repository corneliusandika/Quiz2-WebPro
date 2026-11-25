package com.dompetku.servlet;

import com.dompetku.dao.DebtDAO;
import com.dompetku.model.Debt;
import com.dompetku.model.User;
import com.dompetku.web.BaseServlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/app/debts/*")
public class DebtServlet extends BaseServlet {

    private final DebtDAO debtDAO = new DebtDAO();

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
                Debt debt = id == null ? null : debtDAO.findById(id, user.getId());
                if (debt == null) {
                    notFound(resp, "Utang/piutang tidak ditemukan.");
                    return;
                }
                form(req, resp, user, debt);
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
                if (debtDAO.findById(id, user.getId()) == null) {
                    notFound(resp, "Utang/piutang tidak ditemukan.");
                    return;
                }
                debtDAO.delete(id, user.getId());
                flash(req, "success", "Catatan utang/piutang berhasil dihapus.");
                redirect(req, resp, "/app/debts");
            } else if ("/pay".equals(path)) {
                pay(req, resp, user);
            } else {
                notFound(resp, "Halaman tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void list(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, ServletException, IOException {
        String type = param(req, "type");
        if (!"payable".equals(type) && !"receivable".equals(type)) {
            type = null;
        }
        String status = param(req, "status");
        if (status != null && !status.matches("unpaid|partial|overdue|paid")) {
            status = null;
        }
        String q = param(req, "q");

        List<Debt> all = debtDAO.findAll(user.getId(), type, q);
        List<Debt> items = new ArrayList<>();
        int unsettled = 0;
        for (Debt debt : all) {
            if (!debt.isSettled()) {
                unsettled++;
            }
            if (status == null || status.equals(debt.getStatus())) {
                items.add(debt);
            }
        }
        req.setAttribute("items", items);
        req.setAttribute("unsettled", unsettled);
        req.setAttribute("filterType", type == null ? "" : type);
        req.setAttribute("filterStatus", status == null ? "" : status);
        req.setAttribute("filterQ", q == null ? "" : q);
        req.setAttribute("payable", debtDAO.remaining(user.getId(), "payable"));
        req.setAttribute("receivable", debtDAO.remaining(user.getId(), "receivable"));
        render(req, resp, "debts/list");
    }

    private void form(HttpServletRequest req, HttpServletResponse resp, User user, Debt debt)
            throws ServletException, IOException {
        Debt item = debt;
        if (item == null) {
            item = new Debt();
            item.setUserId(user.getId());
            item.setType("payable");
            item.setDueDate(LocalDate.now().plusDays(14));
        }
        req.setAttribute("debt", item);
        req.setAttribute("mode", debt == null ? "create" : "edit");
        render(req, resp, "debts/form");
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, User user, boolean update)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = new HashMap<>();
        Debt debt = new Debt();
        debt.setUserId(user.getId());
        debt.setId(intParamOrZero(req, "id"));

        String type = param(req, "type");
        if (!"payable".equals(type) && !"receivable".equals(type)) {
            errors.put("type", "Pilih jenis catatan.");
            type = "payable";
        }
        debt.setType(type);

        String counterpart = safe(param(req, "counterpart"), 100);
        if (counterpart == null || counterpart.length() < 2) {
            errors.put("counterpart", "Nama pihak terkait minimal 2 karakter.");
        }
        debt.setCounterpart(counterpart);

        BigDecimal amount = decimalParam(req, "amount");
        if (amount == null || amount.signum() <= 0) {
            errors.put("amount", "Nominal harus lebih besar dari 0.");
        } else if (tooLarge(amount)) {
            errors.put("amount", "Nominal terlalu besar.");
        }
        debt.setAmount(amount);

        BigDecimal paid = decimalParam(req, "paidAmount");
        if (paid == null) {
            paid = BigDecimal.ZERO;
        }
        if (paid.signum() < 0) {
            errors.put("paidAmount", "Nominal dibayar tidak boleh negatif.");
        } else if (tooLarge(paid)) {
            errors.put("paidAmount", "Nominal dibayar terlalu besar.");
        }
        if (amount != null && paid.compareTo(amount) > 0) {
            errors.put("paidAmount", "Nominal dibayar tidak boleh melebihi total.");
        }
        debt.setPaidAmount(paid);
        debt.setDueDate(dateParam(req, "dueDate"));
        debt.setNote(safe(param(req, "note"), 255));

        if (update) {
            if (debt.getId() <= 0 || debtDAO.findById(debt.getId(), user.getId()) == null) {
                notFound(resp, "Utang/piutang tidak ditemukan.");
                return;
            }
        }
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("debt", debt);
            req.setAttribute("mode", update ? "edit" : "create");
            render(req, resp, "debts/form");
            return;
        }
        if (update) {
            debtDAO.update(debt);
            flash(req, "success", "Catatan utang/piutang berhasil diperbarui.");
        } else {
            debtDAO.create(debt);
            flash(req, "success", "Catatan utang/piutang berhasil dibuat.");
        }
        redirect(req, resp, "/app/debts");
    }

    private void pay(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, ServletException, IOException {
        int id = intParamOrZero(req, "id");
        Debt debt = debtDAO.findById(id, user.getId());
        if (debt == null) {
            notFound(resp, "Utang/piutang tidak ditemukan.");
            return;
        }
        BigDecimal amount = decimalParam(req, "amount");
        if (amount == null || amount.signum() <= 0) {
            flash(req, "error", "Nominal pembayaran harus lebih besar dari 0.");
        } else if (amount.compareTo(debt.getRemaining()) > 0) {
            flash(req, "error", "Nominal pembayaran melebihi sisa " + debt.getCounterpart() + ".");
        } else {
            debtDAO.pay(id, user.getId(), amount);
            flash(req, "success", "Pembayaran sebesar " + com.dompetku.util.Formats.rupiah(amount) + " berhasil dicatat.");
        }
        redirect(req, resp, "/app/debts");
    }

    private Integer parseInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }
}
