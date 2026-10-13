-- ====================================================================
-- BIOMETRIC DATABASE MANAGEMENT SYSTEM - ORACLE SQL*PLUS SETUP SCRIPT
-- Course: Database Management Systems (PAT-4 Lab Assessment)
-- Database: Oracle Database 11g / 19c / 21c (XE)
-- Tables: USERS and TERMINALS
-- ====================================================================

-- --------------------------------------------------------------------
-- 1. CLEANUP EXISTING TABLES
-- --------------------------------------------------------------------
DROP TABLE terminals CASCADE CONSTRAINTS;
DROP TABLE users CASCADE CONSTRAINTS;

-- --------------------------------------------------------------------
-- 2. DDL: TABLE DEFINITIONS & INTEGRITY CONSTRAINTS
-- --------------------------------------------------------------------

-- Table 1: USERS
CREATE TABLE users (
    user_id     NUMBER(10)      PRIMARY KEY,
    college_id  NUMBER(10)      UNIQUE NOT NULL,
    firstname   VARCHAR2(50)    NOT NULL,
    lastname    VARCHAR2(50)    DEFAULT '-',
    user_type   VARCHAR2(20)    DEFAULT 'STUDENT' 
                CHECK (user_type IN ('STUDENT', 'STAFF', 'FACULTY', 'ADMIN')),
    dept        VARCHAR2(20),
    status      VARCHAR2(20)    
                CHECK (status IN ('ACTIVE', 'ALUMNI', 'SUSPENDED')),
    dob         DATE            NOT NULL
);

-- Table 2: TERMINALS
CREATE TABLE terminals (
    user_id     NUMBER(10),
    terminal_id NUMBER(10)      PRIMARY KEY,
    gate_name   VARCHAR2(50),
    direction   VARCHAR2(10)    
                CHECK (direction IN ('ENTRY', 'EXIT')),
    ip_address  VARCHAR2(50),
    CONSTRAINT fk_terminals_user FOREIGN KEY (user_id) 
        REFERENCES users(user_id) ON DELETE CASCADE
);

-- --------------------------------------------------------------------
-- 3. DML: POPULATION OF SEED RECORDS
-- --------------------------------------------------------------------

-- Populate USERS
INSERT INTO users (user_id, college_id, firstname, lastname, user_type, dept, status, dob)
VALUES (1, 1244, 'Sakthi', 'Sundar', 'STUDENT', 'CSE', 'ACTIVE', TO_DATE('2005-04-15', 'YYYY-MM-DD'));

INSERT INTO users (user_id, college_id, firstname, lastname, user_type, dept, status, dob)
VALUES (2, 1234, 'Murugesan', '-', 'STAFF', 'LANG', 'ACTIVE', TO_DATE('1988-11-20', 'YYYY-MM-DD'));

INSERT INTO users (user_id, college_id, firstname, lastname, user_type, dept, status, dob)
VALUES (3, 5463, 'Sriram', '-', 'STUDENT', 'ECE', 'ALUMNI', TO_DATE('2001-08-10', 'YYYY-MM-DD'));

INSERT INTO users (user_id, college_id, firstname, lastname, user_type, dept, status, dob)
VALUES (4, 1088, 'Rathika', 'Senthilkumar', 'STUDENT', 'MECH', 'ALUMNI', TO_DATE('2000-12-05', 'YYYY-MM-DD'));

INSERT INTO users (user_id, college_id, firstname, lastname, user_type, dept, status, dob)
VALUES (5, 2345, 'Hariharan', '-', 'STUDENT', 'EEE', 'SUSPENDED', TO_DATE('2004-02-28', 'YYYY-MM-DD'));

INSERT INTO users (user_id, college_id, firstname, lastname, user_type, dept, status, dob)
VALUES (6, 6055, 'Ramkumar', '-', 'FACULTY', 'PHY', 'ACTIVE', TO_DATE('1982-06-18', 'YYYY-MM-DD'));

INSERT INTO users (user_id, college_id, firstname, lastname, user_type, dept, status, dob)
VALUES (7, 5088, 'Karthika', 'Pichaimuthu', 'FACULTY', 'CHY', 'ACTIVE', TO_DATE('1979-09-22', 'YYYY-MM-DD'));

INSERT INTO users (user_id, college_id, firstname, lastname, user_type, dept, status, dob)
VALUES (8, 5065, 'Bhuvana', 'Palaniswamy', 'FACULTY', 'LANG', 'SUSPENDED', TO_DATE('1985-03-30', 'YYYY-MM-DD'));

INSERT INTO users (user_id, college_id, firstname, lastname, user_type, dept, status, dob)
VALUES (9, 3424, 'Sekar', 'Viswanathan', 'ADMIN', 'ACAD', 'ACTIVE', TO_DATE('1975-01-14', 'YYYY-MM-DD'));

-- Populate TERMINALS
INSERT INTO terminals (user_id, terminal_id, gate_name, direction, ip_address)
VALUES (1, 1001, 'NORTH', 'ENTRY', '0000x32ert5');

INSERT INTO terminals (user_id, terminal_id, gate_name, direction, ip_address)
VALUES (2, 1002, 'NORTH', 'EXIT', '90988hhioj');

INSERT INTO terminals (user_id, terminal_id, gate_name, direction, ip_address)
VALUES (3, 1003, 'MAIN', 'ENTRY', '6tyt65ygy');

INSERT INTO terminals (user_id, terminal_id, gate_name, direction, ip_address)
VALUES (4, 1004, 'PARKING1', 'ENTRY', 'u6767uyg689');

INSERT INTO terminals (user_id, terminal_id, gate_name, direction, ip_address)
VALUES (5, 1005, 'SOUTH', 'EXIT', 'i777878nh');

INSERT INTO terminals (user_id, terminal_id, gate_name, direction, ip_address)
VALUES (6, 1006, 'MAIN', 'EXIT', '8ytftthjgh');

INSERT INTO terminals (user_id, terminal_id, gate_name, direction, ip_address)
VALUES (7, 1007, 'NORTH', 'EXIT', 'i766yuru6t');

INSERT INTO terminals (user_id, terminal_id, gate_name, direction, ip_address)
VALUES (8, 1008, 'SOUTH', 'ENTRY', '7hggfggt98');

INSERT INTO terminals (user_id, terminal_id, gate_name, direction, ip_address)
VALUES (9, 1009, 'EAST', 'ENTRY', '3439djehs');

COMMIT;

-- --------------------------------------------------------------------
-- 4. VERIFICATION QUERY (JOIN USERS & TERMINALS)
-- --------------------------------------------------------------------
SELECT u.user_id, u.college_id, u.firstname, u.lastname, u.user_type, u.dept, u.status,
       t.terminal_id, t.gate_name, t.direction, t.ip_address
FROM users u
INNER JOIN terminals t ON u.user_id = t.user_id
ORDER BY u.user_id;
