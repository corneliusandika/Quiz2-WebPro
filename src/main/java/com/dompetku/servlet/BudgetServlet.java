package com.dompetku.servlet;

import com.dompetku.dao.BudgetDAO;
import com.dompetku.dao.CategoryDAO;
import com.dompetku.dao.TransactionDAO;
import com.dompetku.model.Budget;
import com.dompetku.model.Category;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/app/budgets/*")
public class BudgetServlet extends BaseServlet {

    private final BudgetDAO budgetDAO = new BudgetDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = currentUser(req);
        String path = req.getPathInfo();
        try {
            if (path == null || "/".equals(path)) {
                LocalDate month = parseMonth(param(req, "month"));
                if (month == null) {
                    month = transactionDAO.latestMonth(user.getId());
                }
                list(req, resp, user, month);
            } else if ("/new".equals(path)) {
                form(req, resp, user, null);
            } else if (path.startsWith("/edit/")) {
                Integer id = parseInt(path.substring("/edit/".length()));
                Budget budget = id == null ? null : budgetDAO.findById(id, user.getId());
                if (budget == null) {
                    notFound(resp, "Budget tidak ditemukan.");
                    return;
                }
                form(req, resp, user, budget);
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
                if (budgetDAO.findById(id, user.getId()) == null) {
                    notFound(resp, "Budget tidak ditemukan.");
                    return;
                }
                budgetDAO.delete(id, user.getId());
                flash(req, "success", "Budget berhasil dihapus.");
                LocalDate returnDate = parseMonth(param(req, "returnMonth"));
                redirect(req, resp, "/app/budgets" + (returnDate == null ? "" : "?month=" + monthKey(returnDate)));
            } else {
                notFound(resp, "Halaman tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void list(HttpServletRequest req, HttpServletResponse resp, User user, LocalDate month)
            throws SQLException, ServletException, IOException {
        List<Budget> items = budgetDAO.findByMonth(user.getId(), month);
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        int over = 0;
        for (Budget budget : items) {
            totalBudget = totalBudget.add(budget.getAmount());
            totalSpent = totalSpent.add(budget.getSpent());
            if ("over".equals(budget.getStatus())) {
                over++;
            }
        }
        req.setAttribute("items", items);
        req.setAttribute("month", month);
        req.setAttribute("monthKey", monthKey(month));
        req.setAttribute("prevMonthKey", monthKey(month.minusMonths(1)));
        req.setAttribute("nextMonthKey", monthKey(month.plusMonths(1)));
        req.setAttribute("totalBudget", totalBudget);
        req.setAttribute("totalSpent", totalSpent);
        req.setAttribute("totalRemaining", totalBudget.subtract(totalSpent));
        req.setAttribute("overCount", over);
        render(req, resp, "budgets/list");
    }

    private void form(HttpServletRequest req, HttpServletResponse resp, User user, Budget budget)
            throws SQLException, ServletException, IOException {
        Budget item = budget;
        if (item == null) {
            item = new Budget();
            item.setUserId(user.getId());
            LocalDate month = parseMonth(param(req, "month"));
            if (month == null) {
                month = transactionDAO.latestMonth(user.getId());
            }
            item.setMonth(month);
        }
        req.setAttribute("budget", item);
        req.setAttribute("categories", categoryDAO.findByType(user.getId(), "expense"));
        req.setAttribute("mode", budget == null ? "create" : "edit");
        render(req, resp, "budgets/form");
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, User user, boolean update)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = new HashMap<>();
        Budget budget = readForm(req, user, errors);
        if (update) {
            if (budget.getId() <= 0 || budgetDAO.findById(budget.getId(), user.getId()) == null) {
                notFound(resp, "Budget tidak ditemukan.");
                return;
            }
        }
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("budget", budget);
            req.setAttribute("categories", categoryDAO.findByType(user.getId(), "expense"));
            req.setAttribute("mode", update ? "edit" : "create");
            render(req, resp, "budgets/form");
            return;
        }
        if (update) {
            budgetDAO.update(budget);
            flash(req, "success", "Budget berhasil diperbarui.");
        } else {
            budgetDAO.create(budget);
            flash(req, "success", "Budget baru berhasil dibuat.");
        }
        redirect(req, resp, "/app/budgets?month=" + budget.getMonthKey());
    }

    private Budget readForm(HttpServletRequest req, User user, Map<String, String> errors) throws SQLException {
        Budget budget = new Budget();
        budget.setUserId(user.getId());
        budget.setId(intParamOrZero(req, "id"));

        Integer categoryId = intParam(req, "categoryId");
        Category category = categoryId == null ? null : categoryDAO.findById(categoryId, user.getId());
        if (category == null || !"expense".equals(category.getType())) {
            errors.put("categoryId", "Pilih kategori pengeluaran yang valid.");
        } else {
            budget.setCategoryId(categoryId);
        }

        LocalDate month = parseMonth(param(req, "month"));
        if (month == null) {
            errors.put("month", "Pilih bulan budget.");
        } else {
            budget.setMonth(month);
        }

        BigDecimal amount = decimalParam(req, "amount");
        if (amount == null || amount.signum() <= 0) {
            errors.put("amount", "Batas budget harus lebih besar dari 0.");
        } else if (tooLarge(amount)) {
            errors.put("amount", "Batas budget terlalu besar.");
        }
        budget.setAmount(amount);

        if (budget.getCategoryId() > 0 && month != null
                && budgetDAO.exists(user.getId(), budget.getCategoryId(), month,
                budget.getId() == 0 ? null : budget.getId())) {
            errors.put("categoryId", "Budget untuk kategori ini pada bulan tersebut sudah ada.");
        }
        return budget;
    }

    private Integer parseInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }
}
