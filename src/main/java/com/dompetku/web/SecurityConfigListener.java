package com.dompetku.web;

import com.dompetku.util.AppConfig;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/** Enables secure session cookies only in HTTPS deployments. */
@WebListener
public class SecurityConfigListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        event.getServletContext().getSessionCookieConfig().setHttpOnly(true);
        if (AppConfig.secureCookies()) {
            event.getServletContext().getSessionCookieConfig().setSecure(true);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}
