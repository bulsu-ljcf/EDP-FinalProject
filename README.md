# 📖 Library BTAB — Library Management System

## Description

Library BTAB is a full-stack desktop application built with **Java (JavaFX)** and **MySQL**. It provides a complete library management solution for a school or institution, supporting two user roles: **Admin/Librarian** and **Student/Member**. Admins can manage books, members, loans, and fines through a dashboard interface, while students can browse the catalogue, self-borrow books, and track their loan and fine history.

The application follows the **MVC (Model-View-Controller)** architectural pattern, cleanly separating database logic, business logic, and UI rendering across three dedicated files.

---

## Features

### 👨‍💼 Admin / Librarian
- **Dashboard** — At-a-glance stats: total books, total members, active loans, overdue loans, and unpaid fines. Includes a Top 10 Overdue Loans table.
- **Book Management** — Add new books (with multiple copies), delete books, and browse the full catalogue with available/total copy counts.
- **Member Management** — View all student members, add new members, toggle account status (Active / Suspended), and delete accounts.
- **Loan Management** — Issue loans to members by copy ID and username, view all active loans, and process book returns with automatic overdue detection.
- **Fine Management** — View all outstanding fines and mark them as paid (Cash). Fine amounts are automatically calculated on overdue returns.

### 🎓 Student / Member
- **Self-Registration** — Students can create their own account from the login screen.
- **Book Catalogue** — Browse all available books and self-borrow with a single click. Duplicate borrow and unavailable copy scenarios are handled gracefully.
- **My Loans** — View personal loan history with issue date, due date, return date, and status.
- **My Fines** — View outstanding and paid fines associated with their account.
- **Profile** — View personal account details (name, email, contact, role, join date).

### 🔐 Authentication
- Role-based login: choose **Librarian** or **Member** on the selection screen before logging in.
- Session management with logout support.
- Suspended accounts are blocked from logging in.

---

## Technologies Used

| Layer | Technology |
|---|---|
| Language | Java 17+ |
| UI Framework | JavaFX |
| Architecture | MVC (Model-View-Controller) |
| Database | MySQL 8.x |
| DB Connectivity | JDBC (`java.sql`) |
| Database Name | `BATB_Library` |
| IDE (recommended) | IntelliJ IDEA / Eclipse |
| Build Tool | Manual classpath / Maven / Gradle |

---

## Project Structure

```
LibraryBTAB/
├── src/
│   ├── View.java           # UI layer — all JavaFX screens and navigation
│   ├── Controller.java     # Business logic — one controller per domain
│   └── Model.java          # Data layer — DB connection, DTOs, all SQL queries
└── sql/
    └── mysql_BATB_Library.sql  # Full DB schema, seed data, views, indexes
```

### MVC Breakdown

| File | Contains |
|---|---|
| `View.java` | `LibraryAppFX`, `SelectionView`, `LoginView`, `RegisterView`, `AdminDashboardView`, `StudentDashboardView`, `ViewUtils` |
| `Controller.java` | `AuthController`, `BookController`, `MemberController`, `LoanController`, `FineController`, `DashboardController` |
| `Model.java` | `DatabaseManager`, `AuthModel`, `BookModel`, `MemberModel`, `LoanModel`, `FineModel`, `UserRow`, `BookRow`, `LoanRow`, `FineRow` |

---

## Database Schema

The `BATB_Library` database contains the following tables:

| Table | Purpose |
|---|---|
| `MEMBER` | Stores all users (both Admins and Students) |
| `BOOK` | Book catalogue metadata (title, author, ISBN, category) |
| `COPY` | Individual physical copies of each book with availability status |
| `LOAN` | Borrow transactions linking a copy to a borrower |
| `FINE` | Penalty records generated from overdue returns |
| `PAYMENT` | Fine payment records with method and received-by tracking |
| `copy_audit_log` | Audit trail for copy status changes (via trigger) |

The SQL file also includes **6 database views** for reporting:
`v_active_loans`, `v_unpaid_fines`, `v_book_availability`, `v_monthly_summary`, `v_most_borrowed_books`, `v_fine_summary`

**Indexes** are applied on frequently queried columns (username, title, author, loan status, fine payment status) for query performance.

---

## Installation / Setup Guide

### Prerequisites
- Java JDK 17 or later
- JavaFX SDK 17 or later ([download](https://openjfx.io/))
- MySQL Server 8.x ([download](https://dev.mysql.com/downloads/))
- MySQL Connector/J (JDBC driver)

### Steps

1. **Clone or download** this repository.

2. **Set up the database** by opening MySQL Workbench or the MySQL CLI and running:
   ```sql
   SOURCE /path/to/mysql_BATB_Library.sql;
   ```
   This will create the `BATB_Library` database, all tables, views, indexes, and seed data.

3. **Configure the DB connection** by editing `Model.java` — update `DatabaseManager` if your MySQL credentials differ:
   ```java
   private static final String URL  = "jdbc:mysql://localhost:3307/BATB_Library";
   private static final String USER = "root";
   private static final String PASS = "your_password_here";
   ```
   > Default port is `3307`. Change to `3306` if using a standard MySQL installation.

4. **Add dependencies** to your project:
   - JavaFX SDK (add all JARs from the `lib/` folder to your classpath)
   - MySQL Connector/J JAR

5. **Run the application** by executing `View.java` as the main class.
   - In IntelliJ: Right-click `View.java` → Run
   - Via terminal:
     ```
     java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -cp .;mysql-connector.jar View
     ```

6. **Log in** using the default Admin account from the seed data, or register a new Student account from the selection screen.

---

## Screenshots

![alt text](<Screenshots/Screenshot 2026-04-04 201618.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 201018.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 201007.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 200958.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 200901.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 200725.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 200654.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 200641.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 200630.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 200619.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 200609.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 200558.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 200529.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 200522.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 200513.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 200501.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 200449.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 200418.png>) 
![alt text](<Screenshots/Screenshot 2026-04-04 195218.png>)

| Screen | Description |
|---|---|
| Selection Screen | Role picker — Librarian or Member |
| Login Screen | Username and password entry |
| Register Screen | New student self-registration |
| Admin Dashboard | Stats overview + Top 10 Overdue Loans |
| Admin Books Tab | Book catalogue with add/delete |
| Admin Members Tab | Member list with status toggle |
| Admin Loans Tab | Active loans and return processing |
| Admin Fines Tab | Fine list and payment marking |
| Student Dashboard | Browse books, borrow, loans, fines, profile |

---

## Contributors

| Name |
|---|---|
| *(Lyzander Jeptah C. Fabian)* 
| *(Benedict M. Mendoza)* 
| *(John Edrick S. Maigue)*
| *(Ivan Matthew D. Sumilang)* 

---

> **Security Note:** This project stores passwords as plain text strings in `password_hash` for academic/demo purposes. For production, replace with a proper hashing algorithm such as BCrypt.
