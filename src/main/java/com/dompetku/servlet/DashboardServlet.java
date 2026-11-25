package com.dompetku.servlet;

import com.dompetku.dao.BudgetDAO;
import com.dompetku.dao.DebtDAO;
import com.dompetku.dao.GoalDAO;
import com.dompetku.dao.TransactionDAO;
import com.dompetku.dao.WalletDAO;
import com.dompetku.model.Budget;
import com.dompetku.model.CategoryTotal;
import com.dompetku.model.DayStat;
import com.dompetku.model.Debt;
import com.dompetku.model.Goal;
import com.dompetku.model.MonthStat;
import com.dompetku.model.Transaction;
import com.dompetku.model.User;
import com.dompetku.model.Wallet;
import com.dompetku.util.Json;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@WebServlet("/app/dashboard")
public class DashboardServlet extends BaseServlet {

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final WalletDAO walletDAO = new WalletDAO();
    private final BudgetDAO budgetDAO = new BudgetDAO();
    private final GoalDAO goalDAO = new GoalDAO();
    private final DebtDAO debtDAO = new DebtDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = currentUser(req);
        int userId = user.getId();
        try {
            LocalDate month = parseMonth(param(req, "month"));
            if (month == null) {
                month = transactionDAO.latestMonth(userId);
            }
            LocalDate previous = month.minusMonths(1);

            BigDecimal income = transactionDAO.sum(userId, "income", month);
            BigDecimal expense = transactionDAO.sum(userId, "expense", month);
            BigDecimal prevIncome = transactionDAO.sum(userId, "income", previous);
            BigDecimal prevExpense = transactionDAO.sum(userId, "expense", previous);
            BigDecimal net = income.subtract(expense);
            int savingsRate = income.signum() > 0
                    ? net.multiply(BigDecimal.valueOf(100)).divide(income, 0, RoundingMode.HALF_UP).intValue()
                    : 0;

            List<Wallet> wallets = walletDAO.findAll(userId);
            BigDecimal totalBalance = walletDAO.totalBalance(userId);

            List<Budget> budgets = budgetDAO.findByMonth(userId, month);
            BigDecimal budgetTotal = BigDecimal.ZERO;
            BigDecimal budgetSpent = BigDecimal.ZERO;
            int overBudget = 0;
            for (Budget budget : budgets) {
                budgetTotal = budgetTotal.add(budget.getAmount());
                budgetSpent = budgetSpent.add(budget.getSpent());
                if ("over".equals(budget.getStatus())) {
                    overBudget++;
                }
            }
            List<Budget> budgetHighlights = new ArrayList<>(budgets);
            budgetHighlights.sort(Comparator.comparingInt(Budget::getPercentage).reversed());
            if (budgetHighlights.size() > 4) {
                budgetHighlights = budgetHighlights.subList(0, 4);
            }

            List<Goal> goals = goalDAO.findAll(userId);
            BigDecimal goalTotal = BigDecimal.ZERO;
            BigDecimal goalSaved = BigDecimal.ZERO;
            for (Goal goal : goals) {
                goalTotal = goalTotal.add(goal.getTargetAmount());
                goalSaved = goalSaved.add(goal.getSavedAmount());
            }
            List<Goal> goalHighlights = goals.size() > 3 ? goals.subList(0, 3) : goals;

            List<Debt> allDebts = debtDAO.findAll(userId, null, null);
            List<Debt> debtHighlights = new ArrayList<>();
            for (Debt debt : allDebts) {
                if (!debt.isSettled()) {
                    debtHighlights.add(debt);
                }
            }
            debtHighlights.sort(Comparator.comparing(Debt::getDueDate,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            if (debtHighlights.size() > 4) {
                debtHighlights = debtHighlights.subList(0, 4);
            }
            BigDecimal debtPayable = debtDAO.remaining(userId, "payable");
            BigDecimal debtReceivable = debtDAO.remaining(userId, "receivable");

            List<Transaction> recent = transactionDAO.recent(userId, 6);
            List<MonthStat> monthly = transactionDAO.monthlySeries(userId, month.minusMonths(5), 6);
            LocalDate dailyEnd = month.equals(LocalDate.now().withDayOfMonth(1))
                    ? LocalDate.now().plusDays(1)
                    : month.plusMonths(1);
            List<DayStat> daily = transactionDAO.dailySeries(userId, month, dailyEnd);
            List<CategoryTotal> byCategory = transactionDAO.expenseByCategory(userId, month);

            List<String> monthLabels = new ArrayList<>();
            List<String> monthKeys = new ArrayList<>();
            List<BigDecimal> monthIncome = new ArrayList<>();
            List<BigDecimal> monthExpense = new ArrayList<>();
            boolean cashflowHasData = false;
            for (MonthStat stat : monthly) {
                monthLabels.add(stat.getLabel());
                monthKeys.add(stat.getKey());
                monthIncome.add(stat.getIncome());
                monthExpense.add(stat.getExpense());
                if (stat.getIncome().signum() > 0 || stat.getExpense().signum() > 0) {
                    cashflowHasData = true;
                }
            }
            List<String> dayLabels = new ArrayList<>();
            List<BigDecimal> dayIncome = new ArrayList<>();
            List<BigDecimal> dayExpense = new ArrayList<>();
            boolean dailyHasData = false;
            for (DayStat stat : daily) {
                dayLabels.add(String.valueOf(stat.getDay()));
                dayIncome.add(stat.getIncome());
                dayExpense.add(stat.getExpense());
                if (stat.getIncome().signum() > 0 || stat.getExpense().signum() > 0) {
                    dailyHasData = true;
                }
            }
            List<String> categoryLabels = new ArrayList<>();
            List<BigDecimal> categoryValues = new ArrayList<>();
            List<String> categoryColors = new ArrayList<>();
            for (CategoryTotal item : byCategory) {
                categoryLabels.add(item.getName());
                categoryValues.add(item.getTotal());
                categoryColors.add(item.getColor());
            }

            String chartJson = "{"
                    + "\"months\":" + Json.stringArray(monthLabels)
                    + ",\"monthKeys\":" + Json.stringArray(monthKeys)
                    + ",\"monthIncome\":" + Json.numberArray(monthIncome)
                    + ",\"monthExpense\":" + Json.numberArray(monthExpense)
                    + ",\"days\":" + Json.stringArray(dayLabels)
                    + ",\"dayIncome\":" + Json.numberArray(dayIncome)
                    + ",\"dayExpense\":" + Json.numberArray(dayExpense)
                    + ",\"categoryLabels\":" + Json.stringArray(categoryLabels)
                    + ",\"categoryValues\":" + Json.numberArray(categoryValues)
                    + ",\"categoryColors\":" + Json.stringArray(categoryColors)
                    + "}";

            req.setAttribute("month", month);
            req.setAttribute("monthKey", monthKey(month));
            req.setAttribute("prevMonthKey", monthKey(month.minusMonths(1)));
            req.setAttribute("nextMonthKey", monthKey(month.plusMonths(1)));
            req.setAttribute("income", income);
            req.setAttribute("expense", expense);
            req.setAttribute("net", net);
            req.setAttribute("savingsRate", savingsRate);
            req.setAttribute("incomeChange", changePercent(income, prevIncome));
            req.setAttribute("expenseChange", changePercent(expense, prevExpense));
            req.setAttribute("totalBalance", totalBalance);
            req.setAttribute("wallets", wallets);
            req.setAttribute("budgets", budgetHighlights);
            req.setAttribute("budgetTotal", budgetTotal);
            req.setAttribute("budgetSpent", budgetSpent);
            req.setAttribute("overBudget", overBudget);
            req.setAttribute("goals", goalHighlights);
            req.setAttribute("goalTotal", goalTotal);
            req.setAttribute("goalSaved", goalSaved);
            req.setAttribute("debts", debtHighlights);
            req.setAttribute("debtPayable", debtPayable);
            req.setAttribute("debtReceivable", debtReceivable);
            req.setAttribute("recent", recent);
            req.setAttribute("byCategory", byCategory);
            req.setAttribute("daily", daily);
            req.setAttribute("monthly", monthly);
            req.setAttribute("chartJson", chartJson);
            req.setAttribute("cashflowHasData", cashflowHasData);
            req.setAttribute("dailyHasData", dailyHasData);
            req.setAttribute("needsCharts", true);
            render(req, resp, "dashboard");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private int changePercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return current != null && current.signum() > 0 ? 100 : 0;
        }
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 0, RoundingMode.HALF_UP).intValue();
    }
}
