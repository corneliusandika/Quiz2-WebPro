package com.dompetku.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.tomcat.jdbc.pool.DataSource;

import com.dompetku.util.AppConfig;

/** Thin JDBC helper. Connection details can be overridden with environment variables. */
public final class DB {

    private static final String DB_NAME;
    private static final String URL;
    private static final String SERVER_URL;
    private static final String USER;
    private static final String PASSWORD;
    private static final String SSL_MODE;
    private static final DataSource POOL;

    static {
        Properties props = new Properties();
        try (InputStream in = DB.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        String host = env("DOMPETKU_DB_HOST", props.getProperty("db.host", "127.0.0.1"));
        String port = env("DOMPETKU_DB_PORT", props.getProperty("db.port", "3306"));
        DB_NAME = env("DOMPETKU_DB_NAME", props.getProperty("db.name", "dompetku"));
        USER = env("DOMPETKU_DB_USER", props.getProperty("db.user", "root"));
        PASSWORD = env("DOMPETKU_DB_PASSWORD", props.getProperty("db.password", ""));

        SSL_MODE = normalizeSslMode(env("DOMPETKU_DB_SSL_MODE", "DISABLED"));
        String options = buildJdbcOptions(SSL_MODE);
        SERVER_URL = "jdbc:mysql://" + host + ":" + port + "/" + options;
        URL = "jdbc:mysql://" + host + ":" + port + "/" + DB_NAME + options;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL JDBC driver not found in WEB-INF/lib");
        }

        POOL = new DataSource();
        POOL.setDriverClassName("com.mysql.cj.jdbc.Driver");
        POOL.setUrl(URL);
        POOL.setUsername(USER);
        POOL.setPassword(PASSWORD);
        POOL.setInitialSize(0);
        POOL.setMinIdle(0);
        POOL.setMaxActive(20);
        POOL.setMaxIdle(10);
        POOL.setMaxWait(10000);
        POOL.setValidationQuery("SELECT 1");
        POOL.setTestOnBorrow(true);
        POOL.setValidationInterval(30000);
        POOL.setRemoveAbandoned(true);
        POOL.setRemoveAbandonedTimeout(60);
        POOL.setLogAbandoned(true);
    }

    private DB() {
    }

    public static Connection getConnection() throws SQLException {
        return POOL.getConnection();
    }

    public static void close() {
        POOL.close();
    }

    public static String getDatabaseName() {
        return DB_NAME;
    }

    public static String getDatabaseUser() {
        return USER;
    }

    public static String getDatabaseSslMode() {
        return SSL_MODE;
    }

    public static boolean hasDatabasePassword() {
        return PASSWORD != null && !PASSWORD.isEmpty();
    }

    /** Creates the schema on first run, then loads demo data only when explicitly enabled. */
    public static void bootstrap() throws Exception {
        createDatabaseIfMissing();
        runScript("db/schema.sql");
        ensureEmailVerifiedColumn();
        ensureSessionVersionColumn();
        if (count("users") == 0 && AppConfig.demoEnabled()) {
            runScript("db/seed.sql");
        }
    }

    /** Idempotent migration for databases created before session revocation existed. */
    private static void ensureSessionVersionColumn() throws SQLException {
        try (Connection con = getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            boolean exists;
            try (ResultSet rs = meta.getColumns(DB_NAME, null, "users", "session_version")) {
                exists = rs.next();
            }
            if (!exists) {
                try (Statement st = con.createStatement()) {
                    st.executeUpdate("ALTER TABLE users ADD COLUMN session_version INT NOT NULL DEFAULT 0 "
                            + "AFTER password_hash");
                }
            }
        }
    }

    /** Idempotent migration for databases created before email verification existed. */
    private static void ensureEmailVerifiedColumn() throws SQLException {
        try (Connection con = getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            boolean exists;
            try (ResultSet rs = meta.getColumns(DB_NAME, null, "users", "email_verified_at")) {
                exists = rs.next();
            }
            if (!exists) {
                try (Statement st = con.createStatement()) {
                    st.executeUpdate("ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMP NULL "
                            + "DEFAULT CURRENT_TIMESTAMP AFTER password_hash");
                    st.executeUpdate("UPDATE users SET email_verified_at = created_at "
                            + "WHERE email_verified_at IS NULL");
                }
            }
        }
    }

    /**
     * MySQL TLS is configurable because a remote/managed database must not be
     * reached with the local-only useSSL=false default. Set
     * DOMPETKU_DB_SSL_MODE=VERIFY_IDENTITY (or REQUIRED) in production.
     */
    private static String buildJdbcOptions(String mode) {
        boolean localTls = "DISABLED".equals(mode);
        return "?sslMode=" + mode
                + "&allowPublicKeyRetrieval=" + localTls
                + "&serverTimezone=Asia/Jakarta"
                + "&characterEncoding=UTF-8&useUnicode=true&connectTimeout=5000";
    }

    private static String normalizeSslMode(String value) {
        String mode = value == null ? "DISABLED" : value.toUpperCase().trim();
        if (!"DISABLED".equals(mode) && !"PREFERRED".equals(mode)
                && !"REQUIRED".equals(mode) && !"VERIFY_CA".equals(mode)
                && !"VERIFY_IDENTITY".equals(mode)) {
            return "VERIFY_IDENTITY";
        }
        return mode;
    }

    private static void createDatabaseIfMissing() throws SQLException {
        if ("false".equalsIgnoreCase(env("DOMPETKU_DB_CREATE_DATABASE", "true"))) {
            return;
        }
        try (Connection con = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             Statement st = con.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + DB_NAME
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
        }
    }

    private static int count(String table) throws SQLException {
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void runScript(String resource) throws Exception {
        String sql = readResource(resource);
        StringBuilder clean = new StringBuilder();
        for (String line : sql.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            clean.append(line).append('\n');
        }
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            for (String statement : clean.toString().split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    st.execute(trimmed);
                }
            }
        }
    }

    private static String readResource(String resource) throws Exception {
        try (InputStream in = DB.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing SQL resource: " + resource);
            }
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            return out.toString();
        }
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
