package com.dompetku.web;

import com.dompetku.dao.UserDAO;
import com.dompetku.db.DB;
import com.dompetku.util.AppConfig;

import java.util.ArrayList;
import java.util.List;

/** Refuses to start a public deployment with unsafe defaults. */
public final class ProductionGuard {

    private static final String DEMO_EMAIL = "demo@dompetku.test";

    private ProductionGuard() {
    }

    public static void validate() throws Exception {
        if (!AppConfig.isProduction()) {
            return;
        }
        List<String> errors = new ArrayList<>();
        String publicUrl = AppConfig.publicBaseUrl();
        if (!publicUrl.startsWith("https://")) {
            errors.add("DOMPETKU_PUBLIC_URL harus memakai https://");
        }
        if (!AppConfig.secureCookies()) {
            errors.add("DOMPETKU_SECURE_COOKIES harus true");
        }
        if ("true".equalsIgnoreCase(AppConfig.value("DOMPETKU_SEED_DEMO", "false"))) {
            errors.add("DOMPETKU_SEED_DEMO harus false/tidak diset");
        }
        if ("root".equalsIgnoreCase(DB.getDatabaseUser())) {
            errors.add("jangan memakai user database root");
        }
        if (!DB.hasDatabasePassword()) {
            errors.add("password database production wajib diisi");
        }
        String sslMode = DB.getDatabaseSslMode();
        if ("DISABLED".equals(sslMode) || "PREFERRED".equals(sslMode)) {
            errors.add("DOMPETKU_DB_SSL_MODE harus REQUIRED/VERIFY_CA/VERIFY_IDENTITY");
        }
        if (AppConfig.emailVerificationRequired()
                && (AppConfig.value("DOMPETKU_SMTP_HOST", "").isEmpty()
                || AppConfig.value("DOMPETKU_SMTP_FROM", "").isEmpty())) {
            errors.add("SMTP wajib dikonfigurasi saat verifikasi email aktif");
        }
        if (new UserDAO().findByEmail(DEMO_EMAIL) != null) {
            errors.add("akun demo masih ada; jalankan tools/remove_demo_account.sql");
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Production configuration rejected: "
                    + String.join("; ", errors));
        }
    }
}
