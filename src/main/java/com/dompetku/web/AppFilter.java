package com.dompetku.web;

import com.dompetku.util.AppConfig;
import com.dompetku.util.Formats;
import com.dompetku.util.Tokens;

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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** UTF-8 everywhere, security headers, and a session-scoped CSRF token for every POST. */
public class AppFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        req.setAttribute("currencyMode", Formats.currencyMode());
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "SAMEORIGIN");
        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        resp.setHeader("Content-Security-Policy",
                "default-src 'self'; "
                        + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                        + "font-src 'self' https://fonts.gstatic.com; "
                        + "script-src 'self' 'unsafe-inline'; "
                        + "img-src 'self' data:; connect-src 'self'; form-action 'self'; "
                        + "frame-ancestors 'self'; object-src 'none'; base-uri 'self'");
        if (req.isSecure() || "https".equalsIgnoreCase(req.getHeader("X-Forwarded-Proto"))) {
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.startsWith("/app/")) {
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
        }
        if (AppConfig.isProduction()
                && !req.isSecure()
                && !"https".equalsIgnoreCase(req.getHeader("X-Forwarded-Proto"))
                && !"/health".equals(path)) {
            String base = AppConfig.publicBaseUrl();
            if (base.startsWith("https://")) {
                String query = req.getQueryString();
                String location = base + path + (query == null ? "" : "?" + query);
                resp.setStatus(308);
                resp.setHeader("Location", location);
                return;
            }
        }
        if (path.startsWith("/assets/") || "/health".equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(true);
        String token = (String) session.getAttribute("csrfToken");
        if (token == null) {
            token = Tokens.newToken();
            session.setAttribute("csrfToken", token);
        }

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            String sent = req.getParameter("_csrf");
            if (sent == null || !constantTimeEquals(token, sent)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid security token. Please refresh the page.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    @Override
    public void destroy() {
    }
}
