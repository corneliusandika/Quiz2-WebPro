package com.dompetku.db;

import com.dompetku.dao.AuditDAO;
import com.dompetku.dao.AuthTokenDAO;
import com.dompetku.web.ProductionGuard;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Exception last = null;
        for (int attempt = 1; attempt <= 10; attempt++) {
            try {
                DB.bootstrap();
                ProductionGuard.validate();
                try {
                    new AuditDAO().pruneOlderThan(retentionDays());
                    new AuthTokenDAO().pruneExpired();
                } catch (Exception cleanupError) {
                    sce.getServletContext().log("Audit retention cleanup skipped: " + cleanupError.getMessage());
                }
                sce.getServletContext().log("Dompetku database is ready (attempt " + attempt + ")");
                return;
            } catch (Exception e) {
                last = e;
                sce.getServletContext().log("Database not ready, retrying in 2s (attempt " + attempt + "): " + e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new IllegalStateException("Could not initialise Dompetku database. Is MySQL running?", last);
    }

    private int retentionDays() {
        String value = System.getenv("DOMPETKU_AUDIT_RETENTION_DAYS");
        if (value == null || value.trim().isEmpty()) {
            return 365;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 365;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DB.close();
    }
}
