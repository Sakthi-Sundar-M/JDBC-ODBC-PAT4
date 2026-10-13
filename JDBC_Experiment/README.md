# Java JDBC Biometric Application

This directory contains the Java implementation of the Biometric Access Controller using **JDBC (Type 4 Thin Driver)**.

## Structure
* `src/BiometricApp.java`: Main Java source file containing the HTTP web server and JDBC access control logic.
* `lib/ojdbc6.jar`: Oracle 11g JDBC Thin Driver.
* `.vscode/settings.json`: Configures the VS Code classpath to reference `ojdbc6.jar`.

## Compilation & Run
```powershell
# Compile to bin directory
javac -cp ".;lib/ojdbc6.jar" -d bin src/BiometricApp.java

# Run Web Server on Port 9090
java -cp ".;bin;lib/ojdbc6.jar" BiometricApp
```
Access the application in your browser at: `http://localhost:9090`
