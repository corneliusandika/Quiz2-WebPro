package com.dompetku.servlet;

import com.dompetku.dao.GoalDAO;
import com.dompetku.model.Goal;
import com.dompetku.model.GoalDeposit;
import com.dompetku.model.User;
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

@WebServlet("/app/goals/*")
public class GoalServlet extends BaseServlet {

    private final GoalDAO goalDAO = new GoalDAO();

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
                Goal goal = id == null ? null : goalDAO.findById(id, user.getId());
                if (goal == null) {
                    notFound(resp, "Target tabungan tidak ditemukan.");
                    return;
                }
                form(req, resp, user, goal);
            } else if (path.startsWith("/view/")) {
                Integer id = parseInt(path.substring("/view/".length()));
                detail(req, resp, user, id);
            } else if (path.startsWith("/deposit/edit/")) {
                Integer id = parseInt(path.substring("/deposit/edit/".length()));
                GoalDeposit deposit = id == null ? null : goalDAO.findDeposit(id, user.getId());
                if (deposit == null) {
                    notFound(resp, "Setoran tidak ditemukan.");
                    return;
                }
                Goal goal = goalDAO.findById(deposit.getGoalId(), user.getId());
                if (goal == null) {
                    notFound(resp, "Target tabungan tidak ditemukan.");
                    return;
                }
                req.setAttribute("deposit", deposit);
                req.setAttribute("goal", goal);
                req.setAttribute("today", LocalDate.now());
                render(req, resp, "goals/deposit-form");
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
                if (goalDAO.findById(id, user.getId()) == null) {
                    notFound(resp, "Target tabungan tidak ditemukan.");
                    return;
                }
                goalDAO.delete(id, user.getId());
                flash(req, "success", "Target tabungan berhasil dihapus.");
                redirect(req, resp, "/app/goals");
            } else if ("/deposit".equals(path)) {
                addDeposit(req, resp, user);
            } else if ("/deposit/update".equals(path)) {
                updateDeposit(req, resp, user);
            } else if ("/deposit/delete".equals(path)) {
                int depositId = intParamOrZero(req, "id");
                int goalId = intParamOrZero(req, "goalId");
                if (goalDAO.deleteDeposit(depositId, user.getId())) {
                    flash(req, "success", "Setoran berhasil dihapus dan saldo target disesuaikan.");
                } else {
                    flash(req, "error", "Setoran tidak ditemukan.");
                }
                redirect(req, resp, "/app/goals/view/" + goalId);
            } else {
                notFound(resp, "Halaman tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void list(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, ServletException, IOException {
        List<Goal> items = goalDAO.findAll(user.getId());
        BigDecimal totalTarget = BigDecimal.ZERO;
        BigDecimal totalSaved = BigDecimal.ZERO;
        int completed = 0;
        for (Goal goal : items) {
            totalTarget = totalTarget.add(goal.getTargetAmount());
            totalSaved = totalSaved.add(goal.getSavedAmount());
            if (goal.isCompleted()) {
                completed++;
            }
        }
        req.setAttribute("items", items);
        req.setAttribute("totalTarget", totalTarget);
        req.setAttribute("totalSaved", totalSaved);
        req.setAttribute("completed", completed);
        render(req, resp, "goals/list");
    }

    private void detail(HttpServletRequest req, HttpServletResponse resp, User user, Integer id)
            throws SQLException, ServletException, IOException {
        Goal goal = id == null ? null : goalDAO.findById(id, user.getId());
        if (goal == null) {
            notFound(resp, "Target tabungan tidak ditemukan.");
            return;
        }
        List<GoalDeposit> deposits = goalDAO.findDeposits(id, user.getId());
        BigDecimal totalDeposits = BigDecimal.ZERO;
        for (GoalDeposit deposit : deposits) {
            totalDeposits = totalDeposits.add(deposit.getAmount());
        }
        req.setAttribute("today", LocalDate.now());
        req.setAttribute("goal", goal);
        req.setAttribute("deposits", deposits);
        req.setAttribute("totalDeposits", totalDeposits);
        req.setAttribute("avgDeposit", deposits.isEmpty() ? BigDecimal.ZERO
                : totalDeposits.divide(BigDecimal.valueOf(deposits.size()), 0, RoundingMode.HALF_UP));
        render(req, resp, "goals/detail");
    }

    private void form(HttpServletRequest req, HttpServletResponse resp, User user, Goal goal)
            throws ServletException, IOException {
        Goal item = goal;
        if (item == null) {
            item = new Goal();
            item.setUserId(user.getId());
            item.setColor("#7c3aed");
        }
        req.setAttribute("goal", item);
        req.setAttribute("mode", goal == null ? "create" : "edit");
        render(req, resp, "goals/form");
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, User user, boolean update)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = new HashMap<>();
        Goal goal = new Goal();
        goal.setUserId(user.getId());
        goal.setId(intParamOrZero(req, "id"));

        String name = safe(param(req, "name"), 100);
        if (name == null || name.length() < 2) {
            errors.put("name", "Nama target minimal 2 karakter.");
        }
        goal.setName(name);

        BigDecimal target = decimalParam(req, "targetAmount");
        if (target == null || target.signum() <= 0) {
            errors.put("targetAmount", "Target nominal harus lebih besar dari 0.");
        } else if (tooLarge(target)) {
            errors.put("targetAmount", "Target nominal terlalu besar.");
        }
        goal.setTargetAmount(target);

        LocalDate deadline = dateParam(req, "deadline");
        goal.setDeadline(deadline);

        String color = param(req, "color");
        if (color == null || !color.matches("^#[0-9a-fA-F]{6}$")) {
            color = "#7c3aed";
        }
        goal.setColor(color);

        if (update) {
            Goal existing = goal.getId() <= 0 ? null : goalDAO.findById(goal.getId(), user.getId());
            if (existing == null) {
                notFound(resp, "Target tabungan tidak ditemukan.");
                return;
            }
            goal.setSavedAmount(existing.getSavedAmount());
        }
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("goal", goal);
            req.setAttribute("mode", update ? "edit" : "create");
            render(req, resp, "goals/form");
            return;
        }
        if (update) {
            goalDAO.update(goal);
            flash(req, "success", "Target tabungan berhasil diperbarui.");
            redirect(req, resp, "/app/goals/view/" + goal.getId());
        } else {
            goal.setSavedAmount(BigDecimal.ZERO);
            goalDAO.create(goal);
            flash(req, "success", "Target tabungan baru berhasil dibuat.");
            redirect(req, resp, "/app/goals/view/" + goal.getId());
        }
    }

    private void addDeposit(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, ServletException, IOException {
        int goalId = intParamOrZero(req, "goalId");
        Goal goal = goalDAO.findById(goalId, user.getId());
        if (goal == null) {
            notFound(resp, "Target tabungan tidak ditemukan.");
            return;
        }
        Map<String, String> errors = new HashMap<>();
        GoalDeposit deposit = new GoalDeposit();
        deposit.setGoalId(goalId);
        BigDecimal amount = decimalParam(req, "amount");
        if (amount == null || amount.signum() <= 0) {
            errors.put("amount", "Nominal setoran harus lebih besar dari 0.");
        } else if (tooLarge(amount)) {
            errors.put("amount", "Nominal setoran terlalu besar.");
        }
        deposit.setAmount(amount);
        LocalDate date = dateParam(req, "date");
        if (date == null) {
            date = LocalDate.now();
        }
        deposit.setDepositDate(date);
        deposit.setNote(safe(param(req, "note"), 255));

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("today", LocalDate.now());
            req.setAttribute("deposit", deposit);
            req.setAttribute("goal", goal);
            List<GoalDeposit> deposits = goalDAO.findDeposits(goalId, user.getId());
            BigDecimal totalDeposits = sumDeposits(deposits);
            req.setAttribute("deposits", deposits);
            req.setAttribute("totalDeposits", totalDeposits);
            req.setAttribute("avgDeposit", deposits.isEmpty() ? BigDecimal.ZERO
                    : totalDeposits.divide(BigDecimal.valueOf(deposits.size()), 0, RoundingMode.HALF_UP));
            render(req, resp, "goals/detail");
            return;
        }
        if (goalDAO.addDeposit(deposit, user.getId())) {
            flash(req, "success", "Setoran berhasil dicatat. Semangat mencapai target!");
        } else {
            flash(req, "error", "Gagal mencatat setoran.");
        }
        redirect(req, resp, "/app/goals/view/" + goalId);
    }

    private void updateDeposit(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, ServletException, IOException {
        int id = intParamOrZero(req, "id");
        GoalDeposit deposit = goalDAO.findDeposit(id, user.getId());
        if (deposit == null) {
            notFound(resp, "Setoran tidak ditemukan.");
            return;
        }
        Goal goal = goalDAO.findById(deposit.getGoalId(), user.getId());
        if (goal == null) {
            notFound(resp, "Target tabungan tidak ditemukan.");
            return;
        }

        Map<String, String> errors = new HashMap<>();
        BigDecimal amount = decimalParam(req, "amount");
        if (amount == null || amount.signum() <= 0) {
            errors.put("amount", "Nominal setoran harus lebih besar dari 0.");
        } else if (tooLarge(amount)) {
            errors.put("amount", "Nominal setoran terlalu besar.");
        }
        deposit.setAmount(amount);
        LocalDate date = dateParam(req, "date");
        if (date == null) {
            date = LocalDate.now();
        }
        deposit.setDepositDate(date);
        deposit.setNote(safe(param(req, "note"), 255));

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("deposit", deposit);
            req.setAttribute("goal", goal);
            req.setAttribute("today", LocalDate.now());
            render(req, resp, "goals/deposit-form");
            return;
        }
        if (goalDAO.updateDeposit(deposit, user.getId())) {
            flash(req, "success", "Setoran berhasil diperbarui dan saldo target telah disesuaikan.");
        } else {
            flash(req, "error", "Gagal memperbarui setoran.");
        }
        redirect(req, resp, "/app/goals/view/" + goal.getId());
    }

    private Integer parseInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal sumDeposits(List<GoalDeposit> deposits) {
        BigDecimal total = BigDecimal.ZERO;
        for (GoalDeposit deposit : deposits) {
            total = total.add(deposit.getAmount());
        }
        return total;
    }
}
