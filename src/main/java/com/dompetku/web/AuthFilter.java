package com.dompetku.web;

import com.dompetku.dao.UserDAO;
import com.dompetku.model.User;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/** Protects every /app/* URL behind a logged-in session. */
public class AuthFilter implements Filter {

    private static final long SESSION_VERSION_CHECK_MS = 60000L;
    private final UserDAO userDAO = new UserDAO();
    private FilterConfig filterConfig;

    @Override
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        long now = System.currentTimeMillis();
        Long checkedAt = (Long) session.getAttribute("sessionVersionCheckedAt");
        if (checkedAt == null || now - checkedAt >= SESSION_VERSION_CHECK_MS) {
            try {
                User current = userDAO.findById(user.getId());
                if (current == null || current.getSessionVersion() != user.getSessionVersion()) {
                    session.invalidate();
                    resp.sendRedirect(req.getContextPath() + "/login");
                    return;
                }
                user.setSessionVersion(current.getSessionVersion());
                session.setAttribute("user", user);
                session.setAttribute("sessionVersionCheckedAt", now);
            } catch (SQLException e) {
                if (filterConfig != null) {
                    filterConfig.getServletContext().log("Session version check failed", e);
                }
                session.invalidate();
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
