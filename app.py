from flask import Flask, request, render_template_string
import pyodbc

app = Flask(__name__)

# Oracle ODBC Connection String
# Uses the native 'Oracle in XE' driver already installed with Oracle XE on Windows
ODBC_CONN_STR = "Driver={Oracle in XE};Dbq=localhost:1521/xe;Uid=system;Pwd=25bce1244;"

def get_db_connection():
    return pyodbc.connect(ODBC_CONN_STR)

HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Biometric DBMS - Python ODBC Application</title>
  <style>
    * { box-sizing: border-box; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
    body { background: #f4f6f9; color: #1e293b; margin: 0; padding: 24px; }
    .header { background: #0f172a; color: white; padding: 20px 24px; border-radius: 8px; margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center; }
    .header h1 { margin: 0; font-size: 20px; }
    .container { max-width: 1100px; margin: 0 auto; }
    .card { background: white; padding: 24px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); border: 1px solid #e2e8f0; margin-bottom: 24px; }
    .card h2 { margin-top: 0; font-size: 18px; border-bottom: 2px solid #e2e8f0; padding-bottom: 10px; color: #0f172a; }
    label { display: block; font-weight: 600; font-size: 13px; margin: 12px 0 4px; color: #475569; }
    input, select { width: 100%; padding: 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; }
    button { background: #0284c7; color: white; border: none; padding: 12px 20px; font-size: 14px; font-weight: 600; border-radius: 6px; cursor: pointer; margin-top: 16px; width: 100%; }
    button:hover { background: #0369a1; }
    .alert { padding: 16px; border-radius: 6px; margin-bottom: 24px; }
    .alert h3 { margin: 0 0 8px; font-size: 16px; }
    .alert p { margin: 4px 0; font-size: 14px; }
    .alert.green { background: #ecfdf5; border: 1px solid #10b981; color: #065f46; }
    .alert.red { background: #fef2f2; border: 1px solid #ef4444; color: #991b1b; }
    .alert.orange { background: #fffbeb; border: 1px solid #f59e0b; color: #92400e; }
    table { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 12px; }
    th, td { border: 1px solid #e2e8f0; padding: 10px; text-align: left; }
    th { background: #f1f5f9; color: #334155; font-weight: 600; }
    tr:hover { background: #f8fafc; }
    .badge { padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }
    .badge.active { background: #d1fae5; color: #065f46; }
    .badge.suspended { background: #fee2e2; color: #991b1b; }
    .badge.alumni { background: #fef3c7; color: #92400e; }
  </style>
</head>
<body>
<div class="container">
  <div class="header">
    <div>
      <h1>Biometric Database Management System</h1>
      <div style="font-size: 13px; color: #94a3b8; margin-top: 4px;">Python ODBC (pyodbc) & Oracle XE 11g Turnstile Simulator</div>
    </div>
    <div><span class="badge active">Connected: Oracle in XE (ODBC)</span></div>
  </div>

  {% if result %}
    <div class="alert {{ color }}">
      <h3>{{ result.title }}</h3>
      <p>{{ result.message }}</p>
    </div>
  {% endif %}

  <div class="card">
    <h2>1. Turnstile Biometric Verification</h2>
    <form method="POST">
      <label>Enter College ID (e.g. 1244, 2345, 5463, 6055):</label>
      <input type="number" name="college_id" placeholder="Enter College ID" required>
      <label>Gate Name:</label>
      <select name="gate">
        <option value="NORTH">NORTH GATE</option>
        <option value="SOUTH">SOUTH GATE</option>
        <option value="MAIN">MAIN GATE</option>
        <option value="EAST">EAST GATE</option>
        <option value="PARKING1">PARKING 1</option>
      </select>
      <button type="submit">Verify Biometric Access</button>
    </form>
  </div>

  <div class="card">
    <h2>2. Live Database Registry (USERS JOIN TERMINALS)</h2>
    <table>
      <thead>
        <tr>
          <th>User ID</th><th>College ID</th><th>Name</th><th>Role</th>
          <th>Dept</th><th>Status</th><th>Terminal ID</th><th>Gate</th>
        </tr>
      </thead>
      <tbody>
        {% for row in rows %}
        <tr>
          <td>{{ row[0]|int }}</td>
          <td><strong>{{ row[1]|int }}</strong></td>
          <td>{{ row[2] }} {{ row[3] }}</td>
          <td>{{ row[4] }}</td>
          <td>{{ row[5] }}</td>
          <td><span class="badge {{ row[6]|lower }}">{{ row[6] }}</span></td>
          <td>{{ row[7]|int if row[7] else '-' }}</td>
          <td>{{ row[8] if row[8] else '-' }}</td>
        </tr>
        {% endfor %}
      </tbody>
    </table>
  </div>
</div>
</body>
</html>
"""

@app.route("/", methods=["GET", "POST"])
def index():
    result = None
    color = "gray"
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    if request.method == "POST":
        cid = request.form.get("college_id")
        gate = request.form.get("gate")
        
        # In ODBC, use '?' placeholder
        cursor.execute("""
            SELECT firstname, lastname, user_type, status 
            FROM users WHERE college_id = ?
        """, [int(cid) if cid.isdigit() else cid])
        row = cursor.fetchone()
        
        if row:
            name, last, role, status = row
            full_name = f"{name} {last if last != '-' else ''}".strip()
            
            if status == "ACTIVE":
                result = {
                    "title": "ACCESS GRANTED",
                    "message": f"Biometric verification successful for {full_name} ({role} - Dept). Clearance approved at {gate} Gate."
                }
                color = "green"
            elif status == "SUSPENDED":
                result = {
                    "title": "ACCESS DENIED - SECURITY LOCKOUT",
                    "message": f"Account for {full_name} is SUSPENDED. Turnstile access refused at {gate} Gate."
                }
                color = "red"
            else:  # ALUMNI
                result = {
                    "title": "RESTRICTED VISITOR CLEARANCE",
                    "message": f"{full_name} is an ALUMNI. Please visit the Reception Desk for a temporary visitor badge."
                }
                color = "orange"
        else:
            result = {
                "title": "ACCESS DENIED - RECORD NOT FOUND",
                "message": f"College ID {cid} is not registered in the Oracle central database."
            }
            color = "red"

    cursor.execute("""
        SELECT u.user_id, u.college_id, u.firstname, u.lastname, u.user_type, u.dept, u.status, t.terminal_id, t.gate_name
        FROM users u 
        LEFT JOIN terminals t ON u.user_id = t.user_id 
        ORDER BY u.user_id
    """)
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    
    return render_template_string(HTML_TEMPLATE, rows=rows, result=result, color=color)

if __name__ == "__main__":
    print("==========================================================")
    print(" Biometric DBMS (Python ODBC) Server is starting...")
    print(" Connected via driver: Oracle in XE")
    print(" Open in browser: http://127.0.0.1:5000")
    print("==========================================================")
    app.run(port=5000, debug=True)