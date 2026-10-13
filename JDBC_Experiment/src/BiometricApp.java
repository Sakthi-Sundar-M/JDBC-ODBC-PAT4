import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class BiometricApp {
    // Database Configuration - Adjust user and password to match your Oracle XE setup
    private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String DB_USER = "system";
    private static final String DB_PASS = "25bce1244"; // Change to your local Oracle password
    private static final int PORT = 9090;

    public static void main(String[] args) throws Exception {
        // Load Oracle JDBC Driver
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            System.out.println("[+] Oracle JDBC Driver registered successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("[!] Oracle JDBC Driver not found in classpath. Include ojdbc8.jar/ojdbc6.jar.");
            e.printStackTrace();
            return;
        }

        // Test Connection
        try (Connection conn = getConnection()) {
            System.out.println("[+] Connected to Oracle DB: " + conn.getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            System.err.println("[!] Failed to connect to Oracle: " + e.getMessage());
            System.err.println("[!] Please check DB_URL, DB_USER, and DB_PASS.");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new MainHandler());
        server.setExecutor(null);
        System.out.println("==========================================================");
        System.out.println(" Biometric DBMS Web Server is running!");
        System.out.println(" Open your browser: http://localhost:" + PORT);
        System.out.println("==========================================================");
        server.start();
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    static class MainHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            Map<String, String> params = new HashMap<>();

            if ("POST".equalsIgnoreCase(method)) {
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                params = parseFormData(sb.toString());
            }

            String action = params.getOrDefault("action", "home");
            String alertHtml = "";

            // Handle Biometric Gate Verification Processing
            if ("verify_access".equals(action)) {
                String collegeIdStr = params.getOrDefault("college_id", "").trim();
                String gate = params.getOrDefault("gate_name", "MAIN");
                String direction = params.getOrDefault("direction", "ENTRY");

                if (!collegeIdStr.isEmpty()) {
                    alertHtml = processBiometricVerification(collegeIdStr, gate, direction);
                }
            } 
            // Handle New User & Terminal Registration
            else if ("register_user".equals(action)) {
                alertHtml = processRegistration(params);
            }

            // Build HTML Response
            String html = generatePageHtml(alertHtml);
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }

        private String processBiometricVerification(String collegeIdStr, String gate, String direction) {
            String sql = "SELECT u.user_id, u.college_id, u.firstname, u.lastname, u.user_type, u.dept, u.status, " +
                         "TO_CHAR(u.dob, 'YYYY-MM-DD') AS dob_str, t.terminal_id, t.gate_name, t.direction, t.ip_address " +
                         "FROM users u " +
                         "LEFT JOIN terminals t ON u.user_id = t.user_id " +
                         "WHERE u.college_id = ?";

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(collegeIdStr));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String name = rs.getString("firstname") + " " + rs.getString("lastname");
                        String userType = rs.getString("user_type");
                        String dept = rs.getString("dept");
                        String status = rs.getString("status");
                        int terminalId = rs.getInt("terminal_id");

                        if ("ACTIVE".equalsIgnoreCase(status)) {
                            return "<div class='alert success'>" +
                                   "<h3>ACCESS GRANTED</h3>" +
                                   "<p><strong>User:</strong> " + name + " (" + userType + " - " + dept + ")</p>" +
                                   "<p><strong>Gate:</strong> " + gate + " | <strong>Direction:</strong> " + direction + "</p>" +
                                   "<p><strong>Assigned Terminal ID:</strong> " + terminalId + " | <strong>Status:</strong> <span class='badge active'>ACTIVE</span></p>" +
                                   "<p><em>Clearance verified by central Oracle engine. Turnstile unlatched.</em></p>" +
                                   "</div>";
                        } else if ("SUSPENDED".equalsIgnoreCase(status)) {
                            return "<div class='alert danger'>" +
                                   "<h3>ACCESS DENIED - SECURITY LOCKOUT</h3>" +
                                   "<p><strong>User:</strong> " + name + " (" + userType + " - " + dept + ")</p>" +
                                   "<p><strong>Status:</strong> <span class='badge suspended'>SUSPENDED</span></p>" +
                                   "<p><strong>Reason:</strong> Administrative Hold / Disciplinary suspension. Entry refused at Gate " + gate + ".</p>" +
                                   "</div>";
                        } else { // ALUMNI
                            return "<div class='alert warning'>" +
                                   "<h3>RESTRICTED VISITOR CLEARANCE</h3>" +
                                   "<p><strong>User:</strong> " + name + " (" + userType + " - " + dept + ")</p>" +
                                   "<p><strong>Status:</strong> <span class='badge alumni'>ALUMNI</span></p>" +
                                   "<p><strong>Notice:</strong> Former student credential detected. Please register at Visitor Desk for day badge.</p>" +
                                   "</div>";
                        }
                    } else {
                        return "<div class='alert danger'>" +
                               "<h3>ACCESS DENIED - RECORD NOT FOUND</h3>" +
                               "<p>No biometric user found matching College ID <strong>" + collegeIdStr + "</strong> in Oracle database.</p>" +
                               "</div>";
                    }
                }
            } catch (Exception e) {
                return "<div class='alert danger'><h3>DATABASE ERROR</h3><p>" + e.getMessage() + "</p></div>";
            }
        }

        private String processRegistration(Map<String, String> p) {
            String insertUserSql = "INSERT INTO users (user_id, college_id, firstname, lastname, user_type, dept, status, dob) " +
                                   "VALUES (?, ?, ?, ?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'))";
            String insertTermSql = "INSERT INTO terminals (user_id, terminal_id, gate_name, direction, ip_address) " +
                                   "VALUES (?, ?, ?, ?, ?)";

            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                try {
                    int userId = Integer.parseInt(p.get("user_id"));
                    int collegeId = Integer.parseInt(p.get("college_id"));
                    int termId = Integer.parseInt(p.get("terminal_id"));

                    // Insert User
                    try (PreparedStatement psUser = conn.prepareStatement(insertUserSql)) {
                        psUser.setInt(1, userId);
                        psUser.setInt(2, collegeId);
                        psUser.setString(3, p.get("firstname"));
                        psUser.setString(4, p.getOrDefault("lastname", "-"));
                        psUser.setString(5, p.get("user_type"));
                        psUser.setString(6, p.get("dept"));
                        psUser.setString(7, p.get("status"));
                        psUser.setString(8, p.get("dob"));
                        psUser.executeUpdate();
                    }

                    // Insert Terminal
                    try (PreparedStatement psTerm = conn.prepareStatement(insertTermSql)) {
                        psTerm.setInt(1, userId);
                        psTerm.setInt(2, termId);
                        psTerm.setString(3, p.get("gate_name"));
                        psTerm.setString(4, p.get("direction"));
                        psTerm.setString(5, p.get("ip_address"));
                        psTerm.executeUpdate();
                    }

                    conn.commit();
                    return "<div class='alert success'><h3>SUCCESS</h3><p>Biometric user #" + userId + " & terminal #" + termId + " successfully registered in Oracle!</p></div>";
                } catch (Exception ex) {
                    conn.rollback();
                    return "<div class='alert danger'><h3>REGISTRATION FAILED</h3><p>" + ex.getMessage() + "</p></div>";
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                return "<div class='alert danger'><h3>DB CONNECTION ERROR</h3><p>" + e.getMessage() + "</p></div>";
            }
        }

        private String generatePageHtml(String alertHtml) {
            StringBuilder tableRows = new StringBuilder();
            String sql = "SELECT u.user_id, u.college_id, u.firstname, u.lastname, u.user_type, u.dept, u.status, " +
                         "TO_CHAR(u.dob, 'YYYY-MM-DD') AS dob_str, t.terminal_id, t.gate_name, t.direction, t.ip_address " +
                         "FROM users u " +
                         "LEFT JOIN terminals t ON u.user_id = t.user_id " +
                         "ORDER BY u.user_id";

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String status = rs.getString("status");
                    String badgeClass = "badge " + (status != null ? status.toLowerCase() : "active");
                    tableRows.append("<tr>")
                            .append("<td>").append(rs.getInt("user_id")).append("</td>")
                            .append("<td><strong>").append(rs.getInt("college_id")).append("</strong></td>")
                            .append("<td>").append(rs.getString("firstname")).append(" ").append(rs.getString("lastname")).append("</td>")
                            .append("<td>").append(rs.getString("user_type")).append("</td>")
                            .append("<td>").append(rs.getString("dept")).append("</td>")
                            .append("<td><span class='").append(badgeClass).append("'>").append(status).append("</span></td>")
                            .append("<td>").append(rs.getString("dob_str")).append("</td>")
                            .append("<td>").append(rs.getInt("terminal_id")).append("</td>")
                            .append("<td>").append(rs.getString("gate_name")).append("</td>")
                            .append("<td>").append(rs.getString("direction")).append("</td>")
                            .append("<td><code>").append(rs.getString("ip_address")).append("</code></td>")
                            .append("</tr>");
                }
            } catch (Exception e) {
                tableRows.append("<tr><td colspan='11' style='color:red;'>Failed to load table: ").append(e.getMessage()).append("</td></tr>");
            }

            return "<!DOCTYPE html>\n" +
                   "<html lang='en'>\n" +
                   "<head>\n" +
                   "  <meta charset='UTF-8'>\n" +
                   "  <title>Biometric DBMS - JDBC Access Controller</title>\n" +
                   "  <style>\n" +
                   "    * { box-sizing: border-box; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }\n" +
                   "    body { background: #f4f6f9; color: #1e293b; margin: 0; padding: 24px; }\n" +
                   "    .header { background: #0f172a; color: white; padding: 20px 30px; border-radius: 8px; margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center; }\n" +
                   "    .header h1 { margin: 0; font-size: 22px; }\n" +
                   "    .container { max-width: 1200px; margin: 0 auto; }\n" +
                   "    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-bottom: 24px; }\n" +
                   "    .card { background: white; padding: 24px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); border: 1px solid #e2e8f0; }\n" +
                   "    .card h2 { margin-top: 0; font-size: 18px; border-bottom: 2px solid #e2e8f0; padding-bottom: 10px; color: #0f172a; }\n" +
                   "    label { display: block; font-weight: 600; font-size: 13px; margin: 12px 0 4px; color: #475569; }\n" +
                   "    input, select { width: 100%; padding: 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; }\n" +
                   "    button { background: #0284c7; color: white; border: none; padding: 12px 20px; font-size: 14px; font-weight: 600; border-radius: 6px; cursor: pointer; margin-top: 16px; width: 100%; }\n" +
                   "    button:hover { background: #0369a1; }\n" +
                   "    .alert { padding: 16px; border-radius: 6px; margin-bottom: 24px; }\n" +
                   "    .alert h3 { margin: 0 0 8px; font-size: 16px; }\n" +
                   "    .alert p { margin: 4px 0; font-size: 14px; }\n" +
                   "    .alert.success { background: #ecfdf5; border: 1px solid #10b981; color: #065f46; }\n" +
                   "    .alert.danger { background: #fef2f2; border: 1px solid #ef4444; color: #991b1b; }\n" +
                   "    .alert.warning { background: #fffbeb; border: 1px solid #f59e0b; color: #92400e; }\n" +
                   "    table { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 12px; }\n" +
                   "    th, td { border: 1px solid #e2e8f0; padding: 10px; text-align: left; }\n" +
                   "    th { background: #f1f5f9; color: #334155; font-weight: 600; }\n" +
                   "    tr:hover { background: #f8fafc; }\n" +
                   "    .badge { padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }\n" +
                   "    .badge.active { background: #d1fae5; color: #065f46; }\n" +
                   "    .badge.suspended { background: #fee2e2; color: #991b1b; }\n" +
                   "    .badge.alumni { background: #fef3c7; color: #92400e; }\n" +
                   "  </style>\n" +
                   "</head>\n" +
                   "<body>\n" +
                   "<div class='container'>\n" +
                   "  <div class='header'>\n" +
                   "    <div>\n" +
                   "      <h1>Biometric Database Management System</h1>\n" +
                   "      <div style='font-size: 13px; color: #94a3b8; margin-top: 4px;'>Java JDBC & Oracle XE 11g Turnstile Security Simulator</div>\n" +
                   "    </div>\n" +
                   "    <div><span class='badge active'>Connected: Oracle XE (Port 1521)</span></div>\n" +
                   "  </div>\n" +
                   alertHtml +
                   "  <div class='grid'>\n" +
                   "    <!-- Form 1: Biometric Access Authorization -->\n" +
                   "    <div class='card'>\n" +
                   "      <h2>1. Turnstile Biometric Verification</h2>\n" +
                   "      <p style='font-size:13px; color:#64748b;'>Scan card or enter student/faculty College ID to simulate turnstile gate clearance.</p>\n" +
                   "      <form method='POST'>\n" +
                   "        <input type='hidden' name='action' value='verify_access'>\n" +
                   "        <label>College ID (e.g., 1244, 2345, 5463):</label>\n" +
                   "        <input type='number' name='college_id' placeholder='Enter College ID' required>\n" +
                   "        <label>Gate Name:</label>\n" +
                   "        <select name='gate_name'>\n" +
                   "          <option value='NORTH'>NORTH GATE</option>\n" +
                   "          <option value='SOUTH'>SOUTH GATE</option>\n" +
                   "          <option value='MAIN'>MAIN GATE</option>\n" +
                   "          <option value='EAST'>EAST GATE</option>\n" +
                   "          <option value='PARKING1'>PARKING 1</option>\n" +
                   "        </select>\n" +
                   "        <label>Direction:</label>\n" +
                   "        <select name='direction'>\n" +
                   "          <option value='ENTRY'>ENTRY</option>\n" +
                   "          <option value='EXIT'>EXIT</option>\n" +
                   "        </select>\n" +
                   "        <button type='submit'>Verify Biometric Access</button>\n" +
                   "      </form>\n" +
                   "    </div>\n" +
                   "\n" +
                   "    <!-- Form 2: Register New Biometric User -->\n" +
                   "    <div class='card'>\n" +
                   "      <h2>2. Enroll New Biometric User (DML Insert)</h2>\n" +
                   "      <form method='POST'>\n" +
                   "        <input type='hidden' name='action' value='register_user'>\n" +
                   "        <div style='display:grid; grid-template-columns: 1fr 1fr; gap: 8px;'>\n" +
                   "          <div><label>User ID:</label><input type='number' name='user_id' value='10' required></div>\n" +
                   "          <div><label>College ID:</label><input type='number' name='college_id' value='9988' required></div>\n" +
                   "        </div>\n" +
                   "        <div style='display:grid; grid-template-columns: 1fr 1fr; gap: 8px;'>\n" +
                   "          <div><label>First Name:</label><input type='text' name='firstname' required></div>\n" +
                   "          <div><label>Last Name:</label><input type='text' name='lastname' value='-'></div>\n" +
                   "        </div>\n" +
                   "        <div style='display:grid; grid-template-columns: 1fr 1fr; gap: 8px;'>\n" +
                   "          <div><label>User Type:</label><select name='user_type'><option>STUDENT</option><option>STAFF</option><option>FACULTY</option><option>ADMIN</option></select></div>\n" +
                   "          <div><label>Dept:</label><input type='text' name='dept' value='CSE'></div>\n" +
                   "        </div>\n" +
                   "        <div style='display:grid; grid-template-columns: 1fr 1fr; gap: 8px;'>\n" +
                   "          <div><label>Status:</label><select name='status'><option>ACTIVE</option><option>SUSPENDED</option><option>ALUMNI</option></select></div>\n" +
                   "          <div><label>DOB (YYYY-MM-DD):</label><input type='text' name='dob' value='2004-05-10'></div>\n" +
                   "        </div>\n" +
                   "        <div style='display:grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px;'>\n" +
                   "          <div><label>Terminal ID:</label><input type='number' name='terminal_id' value='1010'></div>\n" +
                   "          <div><label>Gate:</label><input type='text' name='gate_name' value='NORTH'></div>\n" +
                   "          <div><label>IP Address:</label><input type='text' name='ip_address' value='192.168.1.50'></div>\n" +
                   "        </div>\n" +
                   "        <input type='hidden' name='direction' value='ENTRY'>\n" +
                   "        <button type='submit' style='background:#10b981;'>Enroll Biometric User</button>\n" +
                   "      </form>\n" +
                   "    </div>\n" +
                   "  </div>\n" +
                   "\n" +
                   "  <!-- Table: Current Oracle DB Records -->\n" +
                   "  <div class='card'>\n" +
                   "    <h2>3. Live Database Registry (USERS JOIN TERMINALS)</h2>\n" +
                   "    <table>\n" +
                   "      <thead>\n" +
                   "        <tr>\n" +
                   "          <th>User ID</th><th>College ID</th><th>Name</th><th>User Type</th><th>Dept</th>\n" +
                   "          <th>Status</th><th>DOB</th><th>Terminal ID</th><th>Gate</th><th>Direction</th><th>IP Address</th>\n" +
                   "        </tr>\n" +
                   "      </thead>\n" +
                   "      <tbody>\n" +
                   tableRows.toString() +
                   "      </tbody>\n" +
                   "    </table>\n" +
                   "  </div>\n" +
                   "</div>\n" +
                   "</body>\n" +
                   "</html>";
        }

        private static Map<String, String> parseFormData(String formData) {
            Map<String, String> map = new HashMap<>();
            String[] pairs = formData.split("&");
            for (String pair : pairs) {
                String[] parts = pair.split("=");
                if (parts.length > 0) {
                    String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                    String val = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
                    map.put(key, val);
                }
            }
            return map;
        }
    }
}