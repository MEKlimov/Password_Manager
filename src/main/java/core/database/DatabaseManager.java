package core.database;

import core.crypto.CryptoService;
import core.models.PasswordRecord;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class DatabaseManager {
    private static final String URL;
    static {
        String userHome = System.getProperty("user.home");
        URL = "jdbc:sqlite:" + userHome + "/passwords.db";
    }
    public static void initialize() {
        String sqlRecords = "CREATE TABLE IF NOT EXISTS records (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "category TEXT," +
                     "title TEXT," +
                     "username TEXT," +
                     "encrypted_password TEXT" +
                     ");";
        String sqlSettings = "CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT);";
        
        // НОВОЕ: Таблица для журнала аудита
        String sqlAudit = "CREATE TABLE IF NOT EXISTS audit_log (" +
                          "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                          "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                          "status TEXT" +
                          ");";
        
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlRecords);
            stmt.execute(sqlSettings);
            stmt.execute(sqlAudit);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // НОВОЕ: Запись попытки входа в БД
    public static void logLoginAttempt(boolean isSuccess) {
        String status = isSuccess ? "УСПЕШНО" : "ОШИБКА ДОСТУПА";
        String sql = "INSERT INTO audit_log (status) VALUES (?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // НОВОЕ: Получение истории входов
    public static List<String> getAuditLogs() {
        List<String> logs = new ArrayList<>();
        // Берем последние 50 записей, сортируем от новых к старым
        String sql = "SELECT datetime(timestamp, 'localtime') as local_time, status FROM audit_log ORDER BY timestamp DESC LIMIT 50";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(rs.getString("local_time") + "  —  " + rs.getString("status"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public static boolean isMasterPasswordSet() {
        String sql = "SELECT value FROM settings WHERE key = 'hash'";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public static void setMasterPassword(String password) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        
        SecretKey key = CryptoService.deriveKey(password.toCharArray(), salt);
        String hashBase64 = Base64.getEncoder().encodeToString(key.getEncoded());
        String saltBase64 = Base64.getEncoder().encodeToString(salt);

        String sql = "INSERT INTO settings (key, value) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "salt"); pstmt.setString(2, saltBase64); pstmt.executeUpdate();
            pstmt.setString(1, "hash"); pstmt.setString(2, hashBase64); pstmt.executeUpdate();
        }
    }

    public static SecretKey verifyMasterPassword(String password) throws Exception {
        String saltBase64 = null;
        String savedHash = null;

        String sql = "SELECT key, value FROM settings WHERE key IN ('salt', 'hash')";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                if ("salt".equals(rs.getString("key"))) saltBase64 = rs.getString("value");
                if ("hash".equals(rs.getString("key"))) savedHash = rs.getString("value");
            }
        }

        if (saltBase64 == null || savedHash == null) return null;

        byte[] salt = Base64.getDecoder().decode(saltBase64);
        SecretKey key = CryptoService.deriveKey(password.toCharArray(), salt);
        String calculatedHash = Base64.getEncoder().encodeToString(key.getEncoded());

        if (calculatedHash.equals(savedHash)) {
            return key; 
        }
        return null; 
    }

    public static void addRecord(String category, String title, String username, String encryptedPassword) {
        String sql = "INSERT INTO records(category, title, username, encrypted_password) VALUES(?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category); pstmt.setString(2, title);
            pstmt.setString(3, username); pstmt.setString(4, encryptedPassword);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void updateRecord(int id, String category, String title, String username, String encryptedPassword) {
        String sql = "UPDATE records SET category = ?, title = ?, username = ?, encrypted_password = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category); pstmt.setString(2, title);
            pstmt.setString(3, username); pstmt.setString(4, encryptedPassword);
            pstmt.setInt(5, id);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void deleteRecord(int id) {
        String sql = "DELETE FROM records WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id); pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static List<PasswordRecord> getAllRecords() {
        List<PasswordRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM records";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                records.add(new PasswordRecord(rs.getInt("id"), rs.getString("category"),
                        rs.getString("title"), rs.getString("username"), rs.getString("encrypted_password")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return records;
    }
}