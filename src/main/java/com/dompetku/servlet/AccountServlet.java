package com.dompetku.servlet;

import com.dompetku.dao.AuditDAO;
import com.dompetku.dao.AuthTokenDAO;
import com.dompetku.dao.UserDAO;
import com.dompetku.model.User;
import com.dompetku.service.MailService;
import com.dompetku.util.AppConfig;
import com.dompetku.util.PasswordUtil;
import com.dompetku.util.TokenUtil;
import com.dompetku.util.Tokens;
import com.dompetku.web.BaseServlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@WebServlet(urlPatterns = {"/forgot-password", "/reset-password", "/verify-email"})
public class AccountServlet extends BaseServlet {

    private static final int MAX_REQUESTS = 5;
    private static final long REQUEST_WINDOW_MS = 15 * 60 * 1000L;
    private static final Map<String, Deque<Long>> FORGOT_ATTEMPTS =
            Collections.synchronizedMap(new LinkedHashMap<String, Deque<Long>>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Deque<Long>> eldest) {
                    return size() > 5000;
                }
            });

    private final UserDAO userDAO = new UserDAO();
    private final AuthTokenDAO tokenDAO = new AuthTokenDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        String path = req.getServletPath();
        if ("/verify-email".equals(path)) {
            verifyEmail(req, resp);
        } else if ("/reset-password".equals(path)) {
            String token = param(req, "token");
            boolean valid = token != null;
            if (valid) {
                try {
                    valid = tokenDAO.findUserId(TokenUtil.hash(token), AuthTokenDAO.PASSWORD_RESET) != null;
                } catch (SQLException e) {
                    throw new ServletException(e);
                }
            }
            req.setAttribute("token", token);
            req.setAttribute("tokenValid", valid);
            render(req, resp, "auth/reset");
        } else {
            req.setAttribute("sent", "1".equals(param(req, "sent")));
            render(req, resp, "auth/forgot");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        String path = req.getServletPath();
        if ("/reset-password".equals(path)) {
            resetPassword(req, resp);
        } else {
            forgotPassword(req, resp);
        }
    }

    private void forgotPassword(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String key = req.getRemoteAddr();
        if (isLimited(key)) {
            req.setAttribute("error", "Terlalu banyak permintaan. Coba lagi nanti.");
            req.setAttribute("sent", false);
            render(req, resp, "auth/forgot");
            return;
        }
        recordRequest(key);

        String email = param(req, "email");
        String normalized = email == null ? null : email.toLowerCase(Locale.ROOT).trim();
        if (normalized != null && !normalized.isEmpty()) {
            try {
                User user = userDAO.findByEmail(normalized);
                if (user != null) {
                    String token = Tokens.newToken();
                    tokenDAO.create(user.getId(), AuthTokenDAO.PASSWORD_RESET,
                            TokenUtil.hash(token), LocalDateTime.now().plusMinutes(30));
                    String link = baseUrl(req) + "/reset-password?token=" + token;
                    try {
                        MailService.send(user.getEmail(), "Reset password Dompetku",
                                "Halo " + user.getName() + ",\n\n"
                                        + "Gunakan tautan berikut untuk membuat password baru:\n" + link
                                        + "\n\nTautan berlaku 30 menit. Abaikan email ini jika kamu tidak memintanya.");
                    } catch (Exception mailError) {
                        getServletContext().log("Password reset email failed for user " + user.getId() + ": "
                                + mailError.getMessage());
                    }
                }
            } catch (SQLException e) {
                throw new ServletException(e);
            }
        }

        // Always use the same response to avoid account enumeration.
        resp.sendRedirect(req.getContextPath() + "/forgot-password?sent=1");
    }

    private void resetPassword(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String token = param(req, "token");
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword");
        Map<String, String> errors = new java.util.HashMap<>();
        if (!validPassword(password)) {
            errors.put("password", "Password minimal 8 karakter dan harus mengandung huruf, angka, serta simbol.");
        }
        if (confirm == null || !confirm.equals(password)) {
            errors.put("confirmPassword", "Konfirmasi password tidak sama.");
        }
        Integer userId = null;
        try {
            userId = token == null ? null : tokenDAO.findUserId(TokenUtil.hash(token), AuthTokenDAO.PASSWORD_RESET);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        if (userId == null) {
            errors.put("token", "Tautan reset tidak valid atau sudah kedaluwarsa.");
        }
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("token", token);
            req.setAttribute("tokenValid", userId != null);
            render(req, resp, "auth/reset");
            return;
        }
        try {
            userDAO.updatePassword(userId, PasswordUtil.hash(password));
            tokenDAO.consume(TokenUtil.hash(token), AuthTokenDAO.PASSWORD_RESET);
            auditDAO.log(userId, "PASSWORD_RESET", "account", userId, "Password direset melalui email.");
            flash(req, "success", "Password berhasil diperbarui. Silakan masuk kembali.");
            redirect(req, resp, "/login");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void verifyEmail(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        String token = param(req, "token");
        boolean verified = false;
        if (token != null) {
            try {
                String hash = TokenUtil.hash(token);
                Integer userId = tokenDAO.findUserId(hash, AuthTokenDAO.EMAIL_VERIFICATION);
                if (userId != null && tokenDAO.consume(hash, AuthTokenDAO.EMAIL_VERIFICATION)) {
                    userDAO.markEmailVerified(userId);
                    verified = true;
                }
            } catch (SQLException e) {
                throw new ServletException(e);
            }
        }
        if (verified) {
            flash(req, "success", "Email berhasil diverifikasi. Silakan masuk.");
        } else {
            flash(req, "error", "Tautan verifikasi tidak valid atau sudah kedaluwarsa.");
        }
        redirect(req, resp, "/login");
    }

    private boolean validPassword(String password) {
        return password != null && password.length() >= 8
                && password.matches(".*[A-Za-z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[^A-Za-z0-9\\s].*");
    }

    private String baseUrl(HttpServletRequest req) {
        String configured = AppConfig.publicBaseUrl();
        if (!configured.isEmpty()) {
            return configured;
        }
        String port = req.getServerPort() == 80 || req.getServerPort() == 443
                ? "" : ":" + req.getServerPort();
        return req.getScheme() + "://" + req.getServerName() + port + req.getContextPath();
    }

    private boolean isLimited(String key) {
        Deque<Long> attempts = FORGOT_ATTEMPTS.get(key);
        if (attempts == null) {
            return false;
        }
        synchronized (attempts) {
            long cutoff = System.currentTimeMillis() - REQUEST_WINDOW_MS;
            while (!attempts.isEmpty() && attempts.peekFirst() < cutoff) {
                attempts.removeFirst();
            }
            return attempts.size() >= MAX_REQUESTS;
        }
    }

    private void recordRequest(String key) {
        Deque<Long> attempts;
        synchronized (FORGOT_ATTEMPTS) {
            attempts = FORGOT_ATTEMPTS.get(key);
            if (attempts == null) {
                attempts = new ArrayDeque<>();
                FORGOT_ATTEMPTS.put(key, attempts);
            }
        }
        synchronized (attempts) {
            attempts.addLast(System.currentTimeMillis());
        }
    }
}
