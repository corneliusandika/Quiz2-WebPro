package com.dompetku.util;

/** Central switches for local development versus a public deployment. */
public final class AppConfig {

    private AppConfig() {
    }

    public static boolean isProduction() {
        String environment = env("DOMPETKU_ENV", "development");
        return "production".equalsIgnoreCase(environment)
                || "true".equalsIgnoreCase(env("DOMPETKU_PRODUCTION", "false"));
    }

    public static boolean demoEnabled() {
        return !isProduction() && "true".equalsIgnoreCase(env("DOMPETKU_SEED_DEMO", "false"));
    }

    public static boolean secureCookies() {
        return "true".equalsIgnoreCase(env("DOMPETKU_SECURE_COOKIES", "false"));
    }

    public static boolean emailVerificationRequired() {
        return "true".equalsIgnoreCase(env("DOMPETKU_REQUIRE_EMAIL_VERIFICATION", "false"));
    }

    public static String publicBaseUrl() {
        String configured = env("DOMPETKU_PUBLIC_URL", "");
        return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
    }

    public static String value(String key, String fallback) {
        return env(key, fallback);
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
