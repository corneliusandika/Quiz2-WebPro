package com.dompetku.servlet;

import com.dompetku.dao.AuditDAO;
import com.dompetku.dao.AuthTokenDAO;
import com.dompetku.dao.CategoryDAO;
import com.dompetku.dao.TransactionDAO;
import com.dompetku.dao.UserDAO;
import com.dompetku.dao.WalletDAO;
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
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

@WebServlet(urlPatterns = {"/app/profile", "/app/profile/update", "/app/profile/password"})
public class ProfileServlet extends BaseServlet {

    private final UserDAO userDAO = new UserDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final WalletDAO walletDAO = new WalletDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final AuditDAO auditDAO = new AuditDAO();
    private final AuthTokenDAO tokenDAO = new AuthTokenDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User sessionUser = currentUser(req);
        try {
            User user = userDAO.findById(sessionUser.getId());
            if (user == null) {
                sessionExpired(req, resp);
                return;
            }
            req.setAttribute("profile", user);
            req.setAttribute("transactionCount", transactionDAO.countAll(user.getId()));
            req.setAttribute("walletCount", walletDAO.findAll(user.getId()).size());
            req.setAttribute("categoryCount", categoryDAO.findAll(user.getId()).size());
            req.setAttribute("auditLogs", auditDAO.recent(user.getId(), 8));
            render(req, resp, "profile/index");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User sessionUser = currentUser(req);
        String path = req.getServletPath();
        try {
            if (path.endsWith("/password")) {
                changePassword(req, resp, sessionUser);
            } else {
                updateProfile(req, resp, sessionUser);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void updateProfile(HttpServletRequest req, HttpServletResponse resp, User sessionUser)
            throws SQLException, ServletException, IOException {
        String name = safe(param(req, "name"), 100);
        String email = param(req, "email");
        String normalized = email == null ? null : email.toLowerCase(Locale.ROOT);

        Map<String, String> errors = new HashMap<>();
        if (name == null || name.length() < 2) {
            errors.put("name", "Nama minimal 2 karakter.");
        }
        if (normalized == null || !normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errors.put("email", "Format email tidak valid.");
        } else if (normalized.length() > 150) {
            errors.put("email", "Email maksimal 150 karakter.");
        } else if (userDAO.emailExists(normalized, sessionUser.getId())) {
            errors.put("email", "Email sudah dipakai akun lain.");
        }

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            User user = userDAO.findById(sessionUser.getId());
            if (user == null) {
                sessionExpired(req, resp);
                return;
            }
            user.setName(name);
            user.setEmail(email);
            req.setAttribute("profile", user);
            req.setAttribute("transactionCount", transactionDAO.countAll(user.getId()));
            req.setAttribute("walletCount", walletDAO.findAll(user.getId()).size());
            req.setAttribute("categoryCount", categoryDAO.findAll(user.getId()).size());
            req.setAttribute("auditLogs", auditDAO.recent(user.getId(), 8));
            render(req, resp, "profile/index");
            return;
        }

        boolean emailChanged = !normalized.equalsIgnoreCase(sessionUser.getEmail());
        userDAO.updateProfile(sessionUser.getId(), name, normalized);
        sessionUser.setName(name);
        sessionUser.setEmail(normalized);
        if (AppConfig.emailVerificationRequired() && emailChanged) {
            userDAO.markEmailUnverified(sessionUser.getId());
            String token = Tokens.newToken();
            tokenDAO.create(sessionUser.getId(), AuthTokenDAO.EMAIL_VERIFICATION,
                    TokenUtil.hash(token), LocalDateTime.now().plusHours(24));
            String link = baseUrl(req) + "/verify-email?token=" + token;
            try {
                MailService.send(normalized, "Verifikasi email Dompetku",
                        "Halo " + name + ",\n\n"
                                + "Verifikasi email baru akun Dompetku kamu dengan tautan berikut:\n" + link
                                + "\n\nTautan berlaku 24 jam.");
            } catch (Exception mailError) {
                getServletContext().log("Verification email after profile change failed for user "
                        + sessionUser.getId() + ": " + mailError.getMessage());
            }
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            flash(req, "success", "Email berhasil diubah. Verifikasi email baru sebelum masuk kembali.");
            redirect(req, resp, "/login");
            return;
        }
        flash(req, "success", "Profil berhasil diperbarui.");
        redirect(req, resp, "/app/profile");
    }

    private void changePassword(HttpServletRequest req, HttpServletResponse resp, User sessionUser)
            throws SQLException, ServletException, IOException {
        String current = req.getParameter("currentPassword");
        String password = req.getParameter("newPassword");
        String confirm = req.getParameter("confirmPassword");

        Map<String, String> errors = new HashMap<>();
        User stored = userDAO.findById(sessionUser.getId());
        if (stored == null) {
            sessionExpired(req, resp);
            return;
        }
        if (current == null || !PasswordUtil.verify(current, stored.getPasswordHash())) {
            errors.put("currentPassword", "Password saat ini salah.");
        }
        if (password == null || password.length() < 8) {
            errors.put("newPassword", "Password baru minimal 8 karakter.");
        } else if (!password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")
                || !password.matches(".*[^A-Za-z0-9\\s].*")) {
            errors.put("newPassword", "Password baru harus mengandung huruf, angka, dan simbol.");
        }
        if (confirm == null || !confirm.equals(password)) {
            errors.put("confirmPassword", "Konfirmasi password tidak sama.");
        }

        if (!errors.isEmpty()) {
            req.setAttribute("passwordErrors", errors);
            req.setAttribute("profile", stored);
            req.setAttribute("transactionCount", transactionDAO.countAll(stored.getId()));
            req.setAttribute("walletCount", walletDAO.findAll(stored.getId()).size());
            req.setAttribute("categoryCount", categoryDAO.findAll(stored.getId()).size());
            req.setAttribute("auditLogs", auditDAO.recent(stored.getId(), 8));
            render(req, resp, "profile/index");
            return;
        }
        userDAO.updatePassword(sessionUser.getId(), PasswordUtil.hash(password));
        User refreshed = userDAO.findById(sessionUser.getId());
        if (refreshed != null) {
            sessionUser.setSessionVersion(refreshed.getSessionVersion());
            req.getSession().setAttribute("user", sessionUser);
            req.getSession().setAttribute("sessionVersionCheckedAt", System.currentTimeMillis());
        }
        flash(req, "success", "Password berhasil diubah. Sesi lain telah dikeluarkan.");
        redirect(req, resp, "/app/profile");
    }

    private void sessionExpired(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        flash(req, "error", "Akun tidak lagi valid. Silakan masuk kembali.");
        redirect(req, resp, "/login");
    }

    private String baseUrl(HttpServletRequest req) {
        String configured = AppConfig.publicBaseUrl();
        if (!configured.isEmpty()) {
            return configured;
        }
        int port = req.getServerPort();
        String portPart = port == 80 || port == 443 ? "" : ":" + port;
        return req.getScheme() + "://" + req.getServerName() + portPart + req.getContextPath();
    }
}
