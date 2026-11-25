package com.dompetku.web;

import com.dompetku.model.User;
import com.dompetku.util.MoneyParser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

public abstract class BaseServlet extends HttpServlet {

    protected User currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }

    protected void render(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, resp);
    }

    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    protected void flash(HttpServletRequest req, String type, String message) {
        HttpSession session = req.getSession();
        session.setAttribute("flashType", type);
        session.setAttribute("flashMessage", message);
    }

    protected String param(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    protected Integer intParam(HttpServletRequest req, String name) {
        String value = param(req, name);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected int intParamOrZero(HttpServletRequest req, String name) {
        Integer value = intParam(req, name);
        return value == null ? 0 : value;
    }

    protected boolean tooLarge(BigDecimal value) {
        return value != null && value.compareTo(new BigDecimal("9999999999999.99")) > 0;
    }

    protected BigDecimal decimalParam(HttpServletRequest req, String name) {
        return MoneyParser.parse(param(req, name));
    }

    protected LocalDate dateParam(HttpServletRequest req, String name) {
        String value = param(req, name);
        return parseDate(value);
    }

    protected LocalDate parseDate(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    protected LocalDate parseMonth(String value) {
        if (value == null || !value.matches("\\d{4}-\\d{2}")) {
            return null;
        }
        try {
            return LocalDate.of(Integer.parseInt(value.substring(0, 4)),
                    Integer.parseInt(value.substring(5, 7)), 1);
        } catch (Exception e) {
            return null;
        }
    }

    protected String monthKey(LocalDate month) {
        return month == null ? "" : String.format("%04d-%02d", month.getYear(), month.getMonthValue());
    }

    protected void notFound(HttpServletResponse resp, String message) throws IOException {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, message);
    }

    protected String safe(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}
