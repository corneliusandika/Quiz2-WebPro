package com.dompetku.servlet;

import com.dompetku.dao.BudgetDAO;
import com.dompetku.dao.TransactionDAO;
import com.dompetku.dao.WalletDAO;
import com.dompetku.model.CategoryTotal;
import com.dompetku.model.DayStat;
import com.dompetku.model.MonthStat;
import com.dompetku.model.Transaction;
import com.dompetku.model.User;
import com.dompetku.service.ReportPdfService;
import com.dompetku.util.Json;
import com.dompetku.web.BaseServlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@WebServlet(urlPatterns = {"/app/reports", "/app/reports/export", "/app/reports/pdf"})
public class ReportServlet extends BaseServlet {

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final WalletDAO walletDAO = new WalletDAO();
    private final BudgetDAO budgetDAO = new BudgetDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = currentUser(req);
        try {
            LocalDate month = parseMonth(param(req, "month"));
            if (month == null) {
                month = transactionDAO.latestMonth(user.getId());
            }
            String key = monthKey(month);

            if (req.getServletPath().endsWith("/pdf")) {
                exportPdf(req, resp, user, month, key);
                return;
            }

            if (req.getServletPath().endsWith("/export")) {
                export(req, resp, user, month, key);
                return;
            }

            BigDecimal income = transactionDAO.sum(user.getId(), "income", month);
            BigDecimal expense = transactionDAO.sum(user.getId(), "expense", month);
            List<CategoryTotal> categories = transactionDAO.expenseByCategory(user.getId(), month);
            LocalDate dailyEnd = month.equals(LocalDate.now().withDayOfMonth(1))
                    ? LocalDate.now().plusDays(1)
                    : month.plusMonths(1);
            List<DayStat> daily = transactionDAO.dailySeries(user.getId(), month, dailyEnd);
            List<MonthStat> monthly = transactionDAO.monthlySeries(user.getId(), month.minusMonths(5), 6);

            List<String> dayLabels = new ArrayList<>();
            List<BigDecimal> dayIncome = new ArrayList<>();
            List<BigDecimal> dayExpense = new ArrayList<>();
            for (DayStat stat : daily) {
                dayLabels.add(String.valueOf(stat.getDay()));
                dayIncome.add(stat.getIncome());
                dayExpense.add(stat.getExpense());
            }
            List<String> monthLabels = new ArrayList<>();
            List<BigDecimal> monthIncome = new ArrayList<>();
            List<BigDecimal> monthExpense = new ArrayList<>();
            for (MonthStat stat : monthly) {
                monthLabels.add(stat.getLabel());
                monthIncome.add(stat.getIncome());
                monthExpense.add(stat.getExpense());
            }
            String reportJson = "{"
                    + "\"days\":" + Json.stringArray(dayLabels)
                    + ",\"dayIncome\":" + Json.numberArray(dayIncome)
                    + ",\"dayExpense\":" + Json.numberArray(dayExpense)
                    + ",\"months\":" + Json.stringArray(monthLabels)
                    + ",\"monthIncome\":" + Json.numberArray(monthIncome)
                    + ",\"monthExpense\":" + Json.numberArray(monthExpense)
                    + "}";

            req.setAttribute("reportJson", reportJson);
            req.setAttribute("month", month);
            req.setAttribute("monthKey", key);
            req.setAttribute("prevMonthKey", monthKey(month.minusMonths(1)));
            req.setAttribute("nextMonthKey", monthKey(month.plusMonths(1)));
            req.setAttribute("income", income);
            req.setAttribute("expense", expense);
            req.setAttribute("net", income.subtract(expense));
            req.setAttribute("categories", categories);
            req.setAttribute("daily", daily);
            req.setAttribute("monthly", monthly);
            req.setAttribute("topExpenses", transactionDAO.topExpenses(user.getId(), month, 5));
            req.setAttribute("wallets", walletDAO.findAll(user.getId()));
            req.setAttribute("budgets", budgetDAO.findByMonth(user.getId(), month));
            req.setAttribute("transactionCount", transactionDAO.countFiltered(
                    user.getId(), key, null, null, null, null));
            req.setAttribute("needsCharts", true);
            render(req, resp, "reports/index");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void exportPdf(HttpServletRequest req, HttpServletResponse resp, User user,
                           LocalDate month, String key) throws IOException, SQLException {
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=\"dompetku-laporan-" + key + ".pdf\"");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        try (OutputStream out = resp.getOutputStream()) {
            new ReportPdfService().write(user, month, out);
        }
    }

    private void export(HttpServletRequest req, HttpServletResponse resp, User user,
                        LocalDate month, String key) throws SQLException, IOException {
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"dompetku-" + key + ".csv\"");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        PrintWriter out = resp.getWriter();
        out.write('\ufeff');
        out.println("Tanggal,Jenis,Kategori,Dompet,Nominal,Catatan");
        int pageSize = 1000;
        int offset = 0;
        while (true) {
            List<Transaction> rows = transactionDAO.find(user.getId(), key, null, null, null, null,
                    pageSize, offset);
            for (Transaction tx : rows) {
                out.println(csv(tx.getTransactionDate().toString())
                        + "," + csv("income".equals(tx.getType()) ? "Pemasukan" : "Pengeluaran")
                        + "," + csv(tx.getCategoryName() == null ? "Tanpa kategori" : tx.getCategoryName())
                        + "," + csv(tx.getWalletName())
                        + "," + tx.getAmount().toPlainString()
                        + "," + csv(tx.getNote()));
            }
            if (rows.size() < pageSize) {
                break;
            }
            offset += pageSize;
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value.replace("\"", "\"\"");
        if (!escaped.isEmpty() && "=+-@\t\r".indexOf(escaped.charAt(0)) >= 0) {
            escaped = "'" + escaped;
        }
        return "\"" + escaped + "\"";
    }
}
