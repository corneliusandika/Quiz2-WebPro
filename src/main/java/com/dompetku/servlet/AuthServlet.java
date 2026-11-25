package com.dompetku.servlet;

import com.dompetku.dao.AuthTokenDAO;
import com.dompetku.dao.UserDAO;
import com.dompetku.model.User;
import com.dompetku.service.DefaultDataService;
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
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@WebServlet(urlPatterns = {"/login", "/register", "/logout"})
public class AuthServlet extends BaseServlet {

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final int MAX_FAILURE_ENTRIES = 5000;
    private static final long LOGIN_WINDOW_MS = 15 * 60 * 1000L;
    private static final int MAX_REGISTRATIONS = 10;
    private static final long REGISTRATION_WINDOW_MS = 15 * 60 * 1000L;
    private static final String DEMO_EMAIL = "demo@dompetku.test";
    private static final Map<String, Deque<Long>> LOGIN_FAILURES =
            Collections.synchronizedMap(new LinkedHashMap<String, Deque<Long>>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Deque<Long>> eldest) {
                    return size() > MAX_FAILURE_ENTRIES;
                }
            });
    private static final Map<String, Deque<Long>> REGISTRATION_ATTEMPTS =
            Collections.synchronizedMap(new LinkedHashMap<String, Deque<Long>>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Deque<Long>> eldest) {
                    return size() > MAX_FAILURE_ENTRIES;
                }
            });

    private final UserDAO userDAO = new UserDAO();
    private final AuthTokenDAO tokenDAO = new AuthTokenDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if ("/login".equals(req.getServletPath()) || "/register".equals(req.getServletPath())) {
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
        }
        String path = req.getServletPath();
        if ("/logout".equals(path)) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        if (currentUser(req) != null) {
            redirect(req, resp, "/app/dashboard");
            return;
        }
        if ("/register".equals(path)) {
            render(req, resp, "auth/register");
        } else {
            render(req, resp, "auth/login");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/logout".equals(path)) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            redirect(req, resp, "/login");
            return;
        }
        if ("/register".equals(path)) {
            register(req, resp);
        } else {
            login(req, resp);
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String email = param(req, "email");
        String password = req.getParameter("password");
        String normalized = email == null ? null : email.toLowerCase(Locale.ROOT);

        if (normalized == null || password == null || password.isEmpty()) {
            req.setAttribute("error", "Email dan password wajib diisi.");
            req.setAttribute("email", email);
            render(req, resp, "auth/login");
            return;
        }
        if (DEMO_EMAIL.equals(normalized) && !AppConfig.demoEnabled()) {
            req.setAttribute("error", "Akun demo dinonaktifkan pada lingkungan ini.");
            req.setAttribute("email", email);
            render(req, resp, "auth/login");
            return;
        }
        String rateKey = req.getRemoteAddr() + "|" + normalized;
        if (isRateLimited(rateKey)) {
            resp.setStatus(429);
            req.setAttribute("error", "Terlalu banyak percobaan login. Coba lagi dalam 15 menit.");
            req.setAttribute("email", email);
            render(req, resp, "auth/login");
            return;
        }
        try {
            User user = userDAO.findByEmail(normalized);
            if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
                recordFailure(rateKey);
                req.setAttribute("error", "Email atau password salah. Silakan coba lagi.");
                req.setAttribute("email", email);
                render(req, resp, "auth/login");
                return;
            }
            clearFailures(rateKey);
            if (AppConfig.emailVerificationRequired() && user.getEmailVerifiedAt() == null) {
                req.setAttribute("error", "Verifikasi email terlebih dahulu. Periksa kotak masukmu.");
                req.setAttribute("email", email);
                render(req, resp, "auth/login");
                return;
            }
            if (PasswordUtil.needsRehash(user.getPasswordHash())) {
                try {
                    userDAO.updatePasswordHash(user.getId(), PasswordUtil.hash(password));
                } catch (SQLException upgradeError) {
                    getServletContext().log("Password rehash skipped for user " + user.getId()
                            + ": " + upgradeError.getMessage());
                }
            }
            HttpSession old = req.getSession(false);
            if (old != null) {
                old.invalidate();
            }
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("csrfToken", Tokens.newToken());
            session.setMaxInactiveInterval(60 * 60 * 8);
            flash(req, "success", "Selamat datang kembali, " + user.getName() + "!");
            redirect(req, resp, "/app/dashboard");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private boolean isRateLimited(String key) {
        Deque<Long> attempts = LOGIN_FAILURES.get(key);
        if (attempts == null) {
            return false;
        }
        synchronized (attempts) {
            long cutoff = System.currentTimeMillis() - LOGIN_WINDOW_MS;
            while (!attempts.isEmpty() && attempts.peekFirst() < cutoff) {
                attempts.removeFirst();
            }
            return attempts.size() >= MAX_LOGIN_FAILURES;
        }
    }

    private void recordFailure(String key) {
        Deque<Long> attempts;
        synchronized (LOGIN_FAILURES) {
            attempts = LOGIN_FAILURES.get(key);
            if (attempts == null) {
                attempts = new ArrayDeque<>();
                LOGIN_FAILURES.put(key, attempts);
            }
        }
        synchronized (attempts) {
            attempts.addLast(System.currentTimeMillis());
        }
    }

    private void clearFailures(String key) {
        LOGIN_FAILURES.remove(key);
    }

    private boolean isRegistrationLimited(String key) {
        Deque<Long> attempts = REGISTRATION_ATTEMPTS.get(key);
        if (attempts == null) {
            return false;
        }
        synchronized (attempts) {
            long cutoff = System.currentTimeMillis() - REGISTRATION_WINDOW_MS;
            while (!attempts.isEmpty() && attempts.peekFirst() < cutoff) {
                attempts.removeFirst();
            }
            return attempts.size() >= MAX_REGISTRATIONS;
        }
    }

    private void recordRegistration(String key) {
        Deque<Long> attempts;
        synchronized (REGISTRATION_ATTEMPTS) {
            attempts = REGISTRATION_ATTEMPTS.get(key);
            if (attempts == null) {
                attempts = new ArrayDeque<>();
                REGISTRATION_ATTEMPTS.put(key, attempts);
            }
        }
        synchronized (attempts) {
            attempts.addLast(System.currentTimeMillis());
        }
    }

    private void register(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String rateKey = req.getRemoteAddr();
        if (isRegistrationLimited(rateKey)) {
            resp.setStatus(429);
            req.setAttribute("error", "Terlalu banyak percobaan registrasi. Coba lagi dalam 15 menit.");
            render(req, resp, "auth/register");
            return;
        }
        recordRegistration(rateKey);

        String name = safe(param(req, "name"), 100);
        String email = param(req, "email");
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword");

        java.util.Map<String, String> errors = new java.util.HashMap<>();
        if (name == null || name.length() < 2) {
            errors.put("name", "Nama minimal 2 karakter.");
        }
        String normalized = email == null ? null : email.toLowerCase(Locale.ROOT);
        if (normalized == null || !normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errors.put("email", "Format email tidak valid.");
        } else if (normalized.length() > 150) {
            errors.put("email", "Email maksimal 150 karakter.");
        }
        if (password == null || password.length() < 8) {
            errors.put("password", "Password minimal 8 karakter.");
        } else if (!password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")
                || !password.matches(".*[^A-Za-z0-9\\s].*")) {
            errors.put("password", "Password harus mengandung huruf, angka, dan simbol.");
        }
        if (confirm == null || !confirm.equals(password)) {
            errors.put("confirmPassword", "Konfirmasi password tidak sama.");
        }
        String agree = param(req, "agree");
        if (!"1".equals(agree)) {
            errors.put("agree", "Kamu perlu menyetujui ketentuan penggunaan Dompetku.");
        }

        try {
            if (normalized != null && userDAO.emailExists(normalized, null)) {
                errors.put("email", "Email sudah terdaftar. Gunakan email lain.");
            }
            if (!errors.isEmpty()) {
                req.setAttribute("errors", errors);
                req.setAttribute("name", name);
                req.setAttribute("email", email);
                req.setAttribute("password", password);
                req.setAttribute("confirmPassword", confirm);
                req.setAttribute("agree", agree);
                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                resp.setHeader("Pragma", "no-cache");
                render(req, resp, "auth/register");
                return;
            }
            User user = new User();
            user.setName(name);
            user.setEmail(normalized);
            user.setPasswordHash(PasswordUtil.hash(password));
            try {
                userDAO.create(user);
            } catch (SQLException duplicate) {
                if (!isDuplicateKey(duplicate)) {
                    throw duplicate;
                }
                errors.put("email", "Email sudah terdaftar. Gunakan email lain.");
                req.setAttribute("errors", errors);
                req.setAttribute("name", name);
                req.setAttribute("email", email);
                req.setAttribute("password", password);
                req.setAttribute("confirmPassword", confirm);
                req.setAttribute("agree", agree);
                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                resp.setHeader("Pragma", "no-cache");
                render(req, resp, "auth/register");
                return;
            }
            DefaultDataService.seed(user.getId());

            if (AppConfig.emailVerificationRequired()) {
                userDAO.markEmailUnverified(user.getId());
                String token = Tokens.newToken();
                tokenDAO.create(user.getId(), AuthTokenDAO.EMAIL_VERIFICATION,
                        TokenUtil.hash(token), LocalDateTime.now().plusHours(24));
                String link = baseUrl(req) + "/verify-email?token=" + token;
                try {
                    MailService.send(user.getEmail(), "Verifikasi email Dompetku",
                            "Halo " + user.getName() + ",\n\n"
                                    + "Verifikasi email akun Dompetku kamu dengan tautan berikut:\n" + link
                                    + "\n\nTautan berlaku 24 jam.");
                    req.setAttribute("email", normalized);
                } catch (Exception mailError) {
                    getServletContext().log("Verification email failed for user " + user.getId() + ": "
                            + mailError.getMessage());
                    req.setAttribute("email", normalized);
                    req.setAttribute("mailError", "Email verifikasi gagal dikirim. Hubungi administrator.");
                }
                render(req, resp, "auth/verify-sent");
                return;
            }

            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("csrfToken", Tokens.newToken());
            session.setMaxInactiveInterval(60 * 60 * 8);
            flash(req, "success", "Akun berhasil dibuat. Selamat datang di Dompetku!");
            redirect(req, resp, "/app/dashboard");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
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

    private boolean isDuplicateKey(SQLException exception) {
        return exception.getErrorCode() == 1062
                || "23000".equals(exception.getSQLState());
    }
}
