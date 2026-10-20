# Biometric Database Management System (JDBC & ODBC)

A dual-stack database-backed access control web application developed for **DBMS Lab Assessment (PAT-4)**. The project connects to a local **Oracle Database (11g XE)** to simulate campus turnstile biometric clearance, access role validation, and terminal management.

---

##  Project Overview

This repository demonstrates enterprise relational database connectivity using two standard industry paradigms:
1. **JDBC (Java Database Connectivity)**: A pure Java web application (`BiometricApp.java`) utilizing the **Type 4 Thin Driver** (`ojdbc6.jar`) and an embedded HTTP server running on port `9090`.
2. **ODBC (Open Database Connectivity)**: A Python Flask web application (`app.py`) connecting via **`pyodbc`** and the native Windows **`Oracle in XE`** ODBC driver on port `5000`.

---

## Database Schema & Relational Design

The database maintains two interrelated tables hosted in **Oracle SQL*Plus**:

### 1. `USERS` Table (Master Entity)
Stores personal, departmental, and clearance status for campus personnel.
* `user_id` (`NUMBER(10)`): **Primary Key**
* `college_id` (`NUMBER(10)`): **Unique Key** (e.g., student/faculty ID)
* `firstname` (`VARCHAR2(50)`): Not Null
* `lastname` (`VARCHAR2(50)`): Default `'-'`
* `user_type` (`VARCHAR2(20)`): Constraint `CHECK (user_type IN ('STUDENT', 'STAFF', 'FACULTY', 'ADMIN'))`
* `dept` (`VARCHAR2(20)`): Department code (CSE, ECE, MECH, etc.)
* `status` (`VARCHAR2(20)`): Constraint `CHECK (status IN ('ACTIVE', 'ALUMNI', 'SUSPENDED'))`
* `dob` (`DATE`): Not Null

### 2. `TERMINALS` Table (Turnstile Gates)
Maintains hardware gate readers and network endpoints.
* `terminal_id` (`NUMBER(10)`): **Primary Key**
* `user_id` (`NUMBER(10)`): **Foreign Key** referencing `users(user_id)` with `ON DELETE CASCADE`
* `gate_name` (`VARCHAR2(50)`): Gate location (`NORTH`, `SOUTH`, `MAIN`, `EAST`, `PARKING1`)
* `direction` (`VARCHAR2(10)`): Constraint `CHECK (direction IN ('ENTRY', 'EXIT'))`
* `ip_address` (`VARCHAR2(50)`): Terminal hardware address

---

## Quick Start & Setup

### Step 1: Initialize Database in Oracle SQL*Plus
Run the included SQL script [database_setup.sql](database_setup.sql):
```sql
sqlplus system/your_password@localhost:1521/xe
@database_setup.sql
```

---

### Step 2A: Run Java Web Application (JDBC)

The Java application requires **no external server** (like Tomcat). It uses Java's built-in `com.sun.net.httpserver.HttpServer`.

```powershell
# Navigate to JDBC directory
cd JDBC_Experiment

# Compile with ojdbc6.jar
javac -cp ".;lib/ojdbc6.jar" -d bin src/BiometricApp.java

# Run application on Port 9090
java -cp ".;bin;lib/ojdbc6.jar" BiometricApp
```
* **URL**: [http://localhost:9090](http://localhost:9090)

---

### Step 2B: Run Python Web Application (ODBC)

Connects to Oracle via Windows native ODBC (`Oracle in XE` driver):

```powershell
# Install prerequisites
pip install flask pyodbc

# Run Flask application on Port 5000
python app.py
```
* **URL**: [http://127.0.0.1:5000](http://127.0.0.1:5000)

---

## Verification & Test Cases

The application processes user credentials against in-engine database constraints:

| Test Case | College ID | Sample User | In-Engine Status | Resulting Action |
| :--- | :---: | :--- | :---: | :--- |
| **Active Student** | `1244` | Sakthi Sundar | `ACTIVE` | **ACCESS GRANTED** (Turnstile unlatched) |
| **Active Faculty** | `6055` | Ramkumar | `ACTIVE` | **ACCESS GRANTED** (Unrestricted 24/7 clearance) |
| **Suspended Account** | `2345` | Hariharan | `SUSPENDED` | **ACCESS DENIED** (Security lockout alert) |
| **Alumni Visitor** | `5463` | Sriram | `ALUMNI` | **RESTRICTED PASS** (Redirect to Visitor Desk) |
| **Unregistered ID** | `9999` | Unknown | *N/A* | **RECORD NOT FOUND** (Biometric rejection) |

---

## Repository Structure

```
├── database_setup.sql         # Complete DDL & DML initialization script for Oracle
├── app.py                     # Python Flask web application using ODBC (pyodbc)
├── README.md                  # Comprehensive documentation and execution guide
└── JDBC_Experiment/
    ├── lib/
    │   └── ojdbc6.jar         # Oracle JDBC Thin Driver (Type 4)
    ├── src/
    │   └── BiometricApp.java  # Java HTTP server & JDBC access control logic
    └── .vscode/
        └── settings.json      # VS Code Java classpath configuration
```

---

## Author

* **Student Name**: M. Sakthi Sundar
* **Register Number**: 25BCE1244
* **Department**: B.Tech Computer Science and Engineering
* **Course**: Database Management Systems (DBMS Lab)
