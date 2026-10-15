import java.sql.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class DatabaseHelper {
    private static final String SERVER_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "currency_db";
    private static final String DB_URL = SERVER_URL + DB_NAME;
    private static final String USER = "root";
    private static final String PASS = "crichd!@#";

    private static void initializeDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(SERVER_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS exchange_rates ("
                    + "from_currency VARCHAR(3) NOT NULL, "
                    + "to_currency VARCHAR(3) NOT NULL, "
                    + "rate DECIMAL(18, 8) NOT NULL, "
                    + "PRIMARY KEY (from_currency, to_currency))");
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS conversion_history ("
                    + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                    + "from_currency VARCHAR(3) NOT NULL, "
                    + "to_currency VARCHAR(3) NOT NULL, "
                    + "original_amount DECIMAL(18, 4) NOT NULL, "
                    + "converted_amount DECIMAL(18, 4) NOT NULL, "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM exchange_rates")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    seedDefaultRates(conn);
                }
            }
        }
    }

    private static void seedDefaultRates(Connection conn) throws SQLException {
        HashMap<String, Double> defaults = new HashMap<>();
        loadDefaultRates(defaults);
        String query = "INSERT INTO exchange_rates (from_currency, to_currency, rate) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            for (Map.Entry<String, Double> rate : defaults.entrySet()) {
                String[] currencies = rate.getKey().split("_", 2);
                pstmt.setString(1, currencies[0]);
                pstmt.setString(2, currencies[1]);
                pstmt.setDouble(3, rate.getValue());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public static List<Object[]> getExchangeRateRows() throws SQLException {
        initializeDatabase();
        List<Object[]> rows = new ArrayList<>();
        String query = "SELECT from_currency, to_currency, rate "
                + "FROM exchange_rates ORDER BY from_currency, to_currency";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                rows.add(new Object[] {
                        rs.getString("from_currency"),
                        rs.getString("to_currency"),
                        rs.getDouble("rate")
                });
            }
        }
        return rows;
    }

    public static List<Object[]> getConversionHistoryRows() throws SQLException {
        initializeDatabase();
        List<Object[]> rows = new ArrayList<>();
        String query = "SELECT id, from_currency, to_currency, original_amount, "
                + "converted_amount, created_at FROM conversion_history ORDER BY id DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                rows.add(new Object[] {
                        rs.getLong("id"),
                        rs.getString("from_currency"),
                        rs.getString("to_currency"),
                        rs.getBigDecimal("original_amount"),
                        rs.getBigDecimal("converted_amount"),
                        rs.getTimestamp("created_at")
                });
            }
        }
        return rows;
    }

    public static HashMap<String, Double> loadRates() {
        HashMap<String, Double> rates = new HashMap<>();
        try {
            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("Could not initialize MySQL database: " + e.getMessage()
                    + ". Falling back to default rates.");
        }
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT from_currency, to_currency, rate FROM exchange_rates")) {
            
            while (rs.next()) {
                String from = rs.getString("from_currency");
                String to = rs.getString("to_currency");
                double rate = rs.getDouble("rate");
                rates.put(from + "_" + to, rate);
                rates.put(to + "_" + from, 1.0 / rate);
            }
            System.out.println("Rates loaded from MySQL database successfully.");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage()
                    + ". Falling back to default rates.");
        }
        loadDefaultRates(rates);
        completeMissingRates(rates);
        return rates;
    }

    private static void completeMissingRates(HashMap<String, Double> rates) {
        Map<String, Map<String, Double>> graph = new HashMap<>();
        for (Map.Entry<String, Double> entry : rates.entrySet()) {
            String[] currencies = entry.getKey().split("_", 2);
            if (currencies.length == 2) {
                graph.computeIfAbsent(currencies[0], key -> new HashMap<>())
                     .put(currencies[1], entry.getValue());
            }
        }

        for (String from : graph.keySet()) {
            Queue<String> queue = new ArrayDeque<>();
            Map<String, Double> pathRates = new HashMap<>();
            Set<String> visited = new HashSet<>();
            queue.add(from);
            pathRates.put(from, 1.0);
            visited.add(from);

            while (!queue.isEmpty()) {
                String current = queue.remove();
                for (Map.Entry<String, Double> edge : graph.get(current).entrySet()) {
                    String to = edge.getKey();
                    if (visited.add(to)) {
                        pathRates.put(to, pathRates.get(current) * edge.getValue());
                        queue.add(to);
                    }
                }
            }

            for (Map.Entry<String, Double> pathRate : pathRates.entrySet()) {
                if (!from.equals(pathRate.getKey())) {
                    rates.putIfAbsent(from + "_" + pathRate.getKey(), pathRate.getValue());
                }
            }
        }
    }

    private static void loadDefaultRates(HashMap<String, Double> rates) {
        rates.putIfAbsent("USD_INR", 87.56);
        rates.putIfAbsent("USD_EUR", 0.92);
        rates.putIfAbsent("USD_GBP", 0.78);
        rates.putIfAbsent("USD_JPY", 149.32);
        rates.putIfAbsent("USD_CAD", 1.38);
        rates.putIfAbsent("USD_AUD", 1.56);
        rates.putIfAbsent("USD_CHF", 0.89);
        rates.putIfAbsent("USD_CNY", 7.13);
        rates.putIfAbsent("USD_AED", 3.67);
        rates.putIfAbsent("USD_SAR", 3.75);
        rates.putIfAbsent("USD_PKR", 277.50);
        rates.putIfAbsent("USD_NPR", 139.75);
        rates.putIfAbsent("USD_KRW", 1375.00);
        rates.putIfAbsent("USD_ZAR", 18.50);

        Map<String, Double> copy = new HashMap<>(rates);
        for (Map.Entry<String, Double> entry : copy.entrySet()) {
            String[] parts = entry.getKey().split("_");
            rates.putIfAbsent(parts[1] + "_" + parts[0], 1.0 / entry.getValue());
        }
    }

    public static void saveConversionHistory(double amount, String from, double converted, String to) {
        String query = "INSERT INTO conversion_history (from_currency, to_currency, original_amount, converted_amount) VALUES (?, ?, ?, ?)";
        try {
            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("Failed to initialize database for conversion history: " + e.getMessage());
            return;
        }
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
             
            pstmt.setString(1, from);
            pstmt.setString(2, to);
            pstmt.setDouble(3, amount);
            pstmt.setDouble(4, converted);
            pstmt.executeUpdate();
            System.out.println("Conversion saved to database history.");
            
        } catch (SQLException e) {
            System.err.println("Failed to save conversion history: " + e.getMessage());
        }
    }

    public static boolean testConnection() {
        try {
            initializeDatabase();
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                return conn != null && !conn.isClosed() && conn.isValid(2);
            }
        } catch (Exception e) {
            System.err.println("Database test connection failed: " + e.getMessage());
            return false;
        }
    }

    public static String getConnectionInfo() {
        return "Host: localhost:3306 | DB: " + DB_NAME + " | User: " + USER;
    }

    public static int getConversionHistoryCount() throws SQLException {
        initializeDatabase();
        String query = "SELECT COUNT(*) FROM conversion_history";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public static int getExchangeRateCount() throws SQLException {
        initializeDatabase();
        String query = "SELECT COUNT(*) FROM exchange_rates";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public static void addOrUpdateRate(String from, String to, double rate) throws SQLException {
        if (from == null || to == null || from.trim().isEmpty() || to.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency codes cannot be empty.");
        }
        from = from.trim().toUpperCase();
        to = to.trim().toUpperCase();
        if (from.equals(to)) {
            throw new IllegalArgumentException("From and To currencies must be different.");
        }
        if (rate <= 0) {
            throw new IllegalArgumentException("Exchange rate must be greater than zero.");
        }

        initializeDatabase();
        String query = "INSERT INTO exchange_rates (from_currency, to_currency, rate) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE rate = VALUES(rate)";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                // Forward rate
                pstmt.setString(1, from);
                pstmt.setString(2, to);
                pstmt.setDouble(3, rate);
                pstmt.executeUpdate();

                // Inverse rate
                pstmt.setString(1, to);
                pstmt.setString(2, from);
                pstmt.setDouble(3, 1.0 / rate);
                pstmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static void deleteRate(String from, String to) throws SQLException {
        initializeDatabase();
        String query = "DELETE FROM exchange_rates WHERE (from_currency = ? AND to_currency = ?) "
                + "OR (from_currency = ? AND to_currency = ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, from);
            pstmt.setString(2, to);
            pstmt.setString(3, to);
            pstmt.setString(4, from);
            pstmt.executeUpdate();
        }
    }

    public static void clearConversionHistory() throws SQLException {
        initializeDatabase();
        String query = "TRUNCATE TABLE conversion_history";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(query);
        }
    }

    public static void deleteConversionHistoryRow(long id) throws SQLException {
        initializeDatabase();
        String query = "DELETE FROM conversion_history WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    public static void resetDefaultRates() throws SQLException {
        initializeDatabase();
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("TRUNCATE TABLE exchange_rates");
            seedDefaultRates(conn);
        }
    }
}
