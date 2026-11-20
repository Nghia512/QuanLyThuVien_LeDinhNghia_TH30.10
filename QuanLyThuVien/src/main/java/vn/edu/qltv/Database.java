package vn.edu.qltv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Kết nối SQL Server; Windows Authentication được dùng mặc định. */
public final class Database {
    private Database() {}
    public static Connection get() throws SQLException {
        String url = env("QTTV_DB_URL", "jdbc:sqlserver://localhost:1433;databaseName=quanlythuvien;encrypt=true;trustServerCertificate=true");
        boolean windowsAuth = Boolean.parseBoolean(env("QTTV_DB_INTEGRATED", "true"));
        if (windowsAuth) {
            if (!url.toLowerCase().contains("integratedsecurity=")) url += ";integratedSecurity=true";
            return DriverManager.getConnection(url);
        }
        String user = env("QTTV_DB_USER", "sa");
        String password = env("QTTV_DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }
    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
