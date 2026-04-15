// =============================================================
//  MODEL LAYER  –  Model.java
//  Contains: DatabaseManager, all Row classes, and all Model
//  classes (AuthModel, BookModel, MemberModel, LoanModel,
//  FineModel). No JavaFX imports anywhere in this file.
// =============================================================


import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// -------------------------------------------------------------
//  DATABASE MANAGER
// -------------------------------------------------------------
class DatabaseManager {
    private static final String URL  = "jdbc:mysql://localhost:3307/BATB_Library";
    private static final String USER = "root";
    private static final String PASS = "12345678";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}

// -------------------------------------------------------------
//  ROW / DATA TRANSFER OBJECTS
// -------------------------------------------------------------
class UserRow {
    int id;
    String fullName, username, email, contact, status, joinDate;

    UserRow(int id, String fn, String u, String e, String c, String s, String j) {
        this.id = id;
        fullName = fn;
        username = u;
        email    = e;
        contact  = c == null ? "" : c;
        status   = s;
        joinDate = j;
    }
}

class BookRow {
    int id, availableCopies, totalCopies;
    String title, author, isbn, category;

    BookRow(int id, String t, String a, String i, String c, int av, int tot) {
        this.id = id;
        title           = t;
        author          = a;
        isbn            = i;
        category        = c == null ? "" : c;
        availableCopies = av;
        totalCopies     = tot;
    }
}

class LoanRow {
    int loanId, borrowerId, daysOverdue;
    String borrowerName, bookTitle, copyId, issueDate, dueDate, returnDate, status;

    LoanRow(int li, String bn, int bi, String bt, String ci,
            String id, String dd, String rd, String st, int dov) {
        loanId       = li;
        borrowerName = bn;
        borrowerId   = bi;
        bookTitle    = bt;
        copyId       = ci;
        issueDate    = id;
        dueDate      = dd;
        returnDate   = rd;
        status       = st;
        daysOverdue  = dov;
    }
}

class FineRow {
    int fineId, borrowerId;
    double amount;
    String borrowerName, bookTitle, reason, paymentStatus;

    FineRow(int fi, String bn, int bi, String bt, double am, String r, String ps) {
        fineId        = fi;
        borrowerName  = bn;
        borrowerId    = bi;
        bookTitle     = bt;
        amount        = am;
        reason        = r;
        paymentStatus = ps;
    }
}

// -------------------------------------------------------------
//  AUTH MODEL
// -------------------------------------------------------------
class AuthModel {

    /**
     * Attempts login. Returns int[]{borrower_id} on success,
     * or null if credentials are wrong / account suspended.
     * The role string is included so the caller can validate it.
     */
    public static class LoginResult {
        public final int    borrowerId;
        public final String role;
        LoginResult(int id, String role) { this.borrowerId = id; this.role = role; }
    }

    public LoginResult login(String username, String password) throws SQLException {
        String sql = "SELECT borrower_id, role FROM member "
                   + "WHERE username=? AND password_hash=? AND account_status='Active'";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new LoginResult(rs.getInt(1), rs.getString(2));
        }
        return null;
    }

    /** Self-registration (Student/Member role only). */
    public void register(String username, String password,
                         String fullName, String email) throws SQLException {
        String sql = "INSERT INTO member "
                   + "(username, password_hash, full_name, email, role, join_date) "
                   + "VALUES (?, ?, ?, ?, 'Student/Member', ?)";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, fullName);
            ps.setString(4, email);
            ps.setString(5, LocalDate.now().toString());
            ps.executeUpdate();
        }
    }

    /** Fetch profile fields for the given borrower_id. Returns null if not found. */
    public String[] fetchProfile(int borrowerId) throws SQLException {
        String sql = "SELECT full_name, username, email, contact_number, role, join_date "
                   + "FROM member WHERE borrower_id=?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, borrowerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new String[]{
                rs.getString(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6)
            };
        }
        return null;
    }
}

// -------------------------------------------------------------
//  BOOK MODEL
// -------------------------------------------------------------
class BookModel {

    public List<BookRow> fetchAll() throws SQLException {
        List<BookRow> list = new ArrayList<>();
        String sql = "SELECT b.book_id, b.title, b.author, b.isbn, b.category, "
                   + "COUNT(c.copy_id) AS total_copies, "
                   + "COALESCE(SUM(c.availability = 'Available'), 0) AS available_copies "
                   + "FROM book b LEFT JOIN copy c ON b.book_id = c.book_id "
                   + "GROUP BY b.book_id ORDER BY b.title";
        try (Connection con = DatabaseManager.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            while (rs.next())
                list.add(new BookRow(
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5),
                    rs.getInt(7),   rs.getInt(6)));
        }
        return list;
    }

    public void addBook(String title, String author, String isbn,
                        String category, String publisher,
                        String year, int copies) throws SQLException {
        String insertBook = "INSERT INTO book(title, author, isbn, category, publisher, year_published) "
                          + "VALUES (?, ?, ?, ?, ?, ?)";
        String insertCopy = "INSERT INTO copy(copy_id, book_id, availability, date_acquired) "
                          + "VALUES (?, ?, 'Available', ?)";
        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            int bookId;
            try (PreparedStatement ps = con.prepareStatement(
                        insertBook, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, title);  ps.setString(2, author);
                ps.setString(3, isbn);   ps.setString(4, category);
                ps.setString(5, publisher); ps.setString(6, year);
                ps.executeUpdate();
                ResultSet gen = ps.getGeneratedKeys();
                gen.next();
                bookId = gen.getInt(1);
            }
            String today = LocalDate.now().toString();
            try (PreparedStatement ps2 = con.prepareStatement(insertCopy)) {
                for (int i = 1; i <= copies; i++) {
                    ps2.setString(1, "copy_" + bookId + "_" + i);
                    ps2.setInt(2, bookId);
                    ps2.setString(3, today);
                    ps2.addBatch();
                }
                ps2.executeBatch();
            }
            con.commit();
        }
    }

    public void deleteBook(int bookId) throws SQLException {
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM book WHERE book_id=?")) {
            ps.setInt(1, bookId);
            ps.executeUpdate();
        }
    }

    /** Returns one available copy_id for the given book, or null if none. */
    public String fetchAvailableCopy(int bookId) throws SQLException {
        String sql = "SELECT copy_id FROM copy "
                   + "WHERE book_id=? AND availability='Available' LIMIT 1";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        }
        return null;
    }

    /** Returns count of active/overdue loans for this member+book combination. */
    public int countActiveLoanForMember(int bookId, int borrowerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM loan l JOIN copy c ON l.copy_id = c.copy_id "
                   + "WHERE c.book_id=? AND l.borrower_id=? "
                   + "AND l.loan_status IN ('Active', 'Overdue')";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            ps.setInt(2, borrowerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }
}

// -------------------------------------------------------------
//  MEMBER MODEL
// -------------------------------------------------------------
class MemberModel {

    public List<UserRow> fetchAllStudents() throws SQLException {
        List<UserRow> list = new ArrayList<>();
        String sql = "SELECT borrower_id, full_name, username, email, "
                   + "contact_number, account_status, join_date "
                   + "FROM member WHERE role='Student/Member' ORDER BY borrower_id";
        try (Connection con = DatabaseManager.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            while (rs.next())
                list.add(new UserRow(
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5),
                    rs.getString(6), rs.getString(7)));
        }
        return list;
    }

    /** Admin-only: register a student with contact number. */
    public void addStudent(String username, String password, String fullName,
                           String email, String contact) throws SQLException {
        String sql = "INSERT INTO member "
                   + "(username, password_hash, full_name, email, contact_number, role, join_date) "
                   + "VALUES (?, ?, ?, ?, ?, 'Student/Member', ?)";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username); ps.setString(2, password);
            ps.setString(3, fullName); ps.setString(4, email);
            ps.setString(5, contact);  ps.setString(6, LocalDate.now().toString());
            ps.executeUpdate();
        }
    }

    public void updateStatus(int borrowerId, String newStatus) throws SQLException {
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE member SET account_status=? WHERE borrower_id=?")) {
            ps.setString(1, newStatus);
            ps.setInt(2, borrowerId);
            ps.executeUpdate();
        }
    }

    public void deleteMember(int borrowerId) throws SQLException {
        String deletePayments =
                "DELETE FROM payment WHERE fine_id IN " +
                        "(SELECT fine_id FROM fine WHERE borrower_id = ?)";

        String deleteFines =
                "DELETE FROM fine WHERE borrower_id = ?";

        String deleteLoans =
                "DELETE FROM loan WHERE borrower_id = ?";

        String deleteMember =
                "DELETE FROM member WHERE borrower_id = ?";

        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement p1 = con.prepareStatement(deletePayments);
                     PreparedStatement p2 = con.prepareStatement(deleteFines);
                     PreparedStatement p3 = con.prepareStatement(deleteLoans);
                     PreparedStatement p4 = con.prepareStatement(deleteMember)) {

                    p1.setInt(1, borrowerId); p1.executeUpdate();
                    p2.setInt(1, borrowerId); p2.executeUpdate();
                    p3.setInt(1, borrowerId); p3.executeUpdate();
                    p4.setInt(1, borrowerId); p4.executeUpdate();
                }
                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        }
    }
}

// -------------------------------------------------------------
//  LOAN MODEL
// -------------------------------------------------------------
class LoanModel {

    private List<LoanRow> query(String sql, Object... params) throws SQLException {
        List<LoanRow> list = new ArrayList<>();
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Integer) ps.setInt(i + 1, (Integer) params[i]);
                else ps.setString(i + 1, (String) params[i]);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new LoanRow(
                    rs.getInt(1), rs.getString(2), rs.getInt(3),
                    rs.getString(4), rs.getString(5), rs.getString(6),
                    rs.getString(7),
                    rs.getString(8) == null ? "—" : rs.getString(8),
                    rs.getString(9), rs.getInt(10)));
        }
        return list;
    }

    private static final String BASE =
        "SELECT l.loan_id, m.full_name, m.borrower_id, b.title, l.copy_id, "
      + "l.issue_date, l.due_date, l.return_date, l.loan_status, "
      + "GREATEST(0, DATEDIFF(CURRENT_DATE, l.due_date)) AS days_overdue "
      + "FROM loan l "
      + "JOIN member m ON l.borrower_id = m.borrower_id "
      + "JOIN copy   c ON l.copy_id     = c.copy_id "
      + "JOIN book   b ON c.book_id     = b.book_id ";

    public List<LoanRow> fetchActive() throws SQLException {
        return query(BASE + "WHERE l.loan_status IN ('Active','Overdue') ORDER BY l.due_date");
    }

    public List<LoanRow> fetchAll() throws SQLException {
        return query(BASE + "ORDER BY l.loan_id DESC");
    }

    public List<LoanRow> fetchByBorrower(int borrowerId) throws SQLException {
        return query(BASE + "WHERE l.borrower_id=? ORDER BY l.issue_date DESC", borrowerId);
    }

    public List<LoanRow> fetchOverdueTop10() throws SQLException {
        return query(BASE
            + "WHERE l.loan_status='Overdue' "
            + "ORDER BY DATEDIFF(CURRENT_DATE, l.due_date) DESC LIMIT 10");
    }

    /** Admin issues a loan by copy ID + student username. */
    public void issueLoan(String copyId, String username, String dueDate) throws SQLException {
        String sql = "INSERT INTO loan(copy_id, borrower_id, issue_date, due_date, loan_status) "
                   + "SELECT ?, m.borrower_id, ?, ?, 'Active' "
                   + "FROM member m WHERE m.username=?";
        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql);
                 PreparedStatement p2 = con.prepareStatement(
                         "UPDATE copy SET availability='Borrowed' WHERE copy_id=?")) {
                ps.setString(1, copyId);
                ps.setString(2, LocalDate.now().toString());
                ps.setString(3, dueDate);
                ps.setString(4, username);
                int rows = ps.executeUpdate();
                if (rows == 0) throw new SQLException("Student username not found.");
                p2.setString(1, copyId);
                p2.executeUpdate();
            }
            con.commit();
        }
    }

    /** Student self-borrows by copy ID + borrower ID. */
    public void borrowBook(String copyId, int borrowerId, String dueDate) throws SQLException {
        String sql = "INSERT INTO loan(copy_id, borrower_id, issue_date, due_date, loan_status) "
                   + "VALUES (?, ?, ?, ?, 'Active')";
        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps1 = con.prepareStatement(sql);
                 PreparedStatement ps2 = con.prepareStatement(
                         "UPDATE copy SET availability='Borrowed' WHERE copy_id=?")) {
                ps1.setString(1, copyId);
                ps1.setInt(2, borrowerId);
                ps1.setString(3, LocalDate.now().toString());
                ps1.setString(4, dueDate);
                ps1.executeUpdate();
                ps2.setString(1, copyId);
                ps2.executeUpdate();
            }
            con.commit();
        }
    }

    /**
     * Marks a loan returned. If daysOverdue > 0 a fine row is automatically
     * inserted (PHP 10 / day). Returns the fine amount (0 if none).
     */
    public double returnLoan(LoanRow loan, int receivedBy) throws SQLException {
        String today = LocalDate.now().toString();
        double fineAmount = 0;
        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement p1 = con.prepareStatement(
                         "UPDATE loan SET return_date=?, loan_status='Returned' WHERE loan_id=?");
                 PreparedStatement p2 = con.prepareStatement(
                         "UPDATE copy SET availability='Available' WHERE copy_id=?")) {
                p1.setString(1, today);
                p1.setInt(2, loan.loanId);
                p1.executeUpdate();
                p2.setString(1, loan.copyId);
                p2.executeUpdate();
            }
            if (loan.daysOverdue > 0) {
                fineAmount = loan.daysOverdue * 10.0;
                try (PreparedStatement pf = con.prepareStatement(
                         "INSERT INTO fine(loan_id, borrower_id, amount, reason) "
                       + "VALUES (?, ?, ?, ?)")) {
                    pf.setInt(1, loan.loanId);
                    pf.setInt(2, loan.borrowerId);
                    pf.setDouble(3, fineAmount);
                    pf.setString(4, "Late return – " + loan.daysOverdue + " days @ PHP 10/day");
                    pf.executeUpdate();
                }
            }
            con.commit();
        }
        return fineAmount;
    }
}

// -------------------------------------------------------------
//  FINE MODEL
// -------------------------------------------------------------
class FineModel {

    private static final String BASE =
        "SELECT f.fine_id, m.full_name, m.borrower_id, b.title, "
      + "f.amount, f.reason, f.payment_status "
      + "FROM fine f "
      + "JOIN member m ON f.borrower_id = m.borrower_id "
      + "JOIN loan   l ON f.loan_id     = l.loan_id "
      + "JOIN copy   c ON l.copy_id     = c.copy_id "
      + "JOIN book   b ON c.book_id     = b.book_id ";

    public List<FineRow> fetchAll() throws SQLException {
        List<FineRow> list = new ArrayList<>();
        try (Connection con = DatabaseManager.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(BASE + "ORDER BY f.fine_id DESC")) {
            while (rs.next())
                list.add(row(rs));
        }
        return list;
    }

    public List<FineRow> fetchByBorrower(int borrowerId) throws SQLException {
        List<FineRow> list = new ArrayList<>();
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     BASE + "WHERE f.borrower_id=? ORDER BY f.fine_id DESC")) {
            ps.setInt(1, borrowerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(row(rs));
        }
        return list;
    }

    public void markPaid(FineRow fine, int receivedByBorrowerId) throws SQLException {
        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement p1 = con.prepareStatement(
                         "UPDATE fine SET payment_status='Paid' WHERE fine_id=?");
                 PreparedStatement p2 = con.prepareStatement(
                         "INSERT INTO payment"
                       + "(fine_id, borrower_id, paid_amount, payment_date, payment_method, received_by) "
                       + "VALUES (?, ?, ?, ?, 'Cash', ?)")) {
                p1.setInt(1, fine.fineId);
                p1.executeUpdate();
                p2.setInt(1, fine.fineId);
                p2.setInt(2, fine.borrowerId);
                p2.setDouble(3, fine.amount);
                p2.setString(4, LocalDate.now().toString());
                p2.setInt(5, receivedByBorrowerId);
                p2.executeUpdate();
            }
            con.commit();
        }
    }

    /** Reads dashboard stat: count of unpaid fines. */
    public int countUnpaid() throws SQLException {
        try (Connection con = DatabaseManager.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(
                     "SELECT COUNT(*) FROM fine WHERE payment_status='Unpaid'")) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private FineRow row(ResultSet rs) throws SQLException {
        return new FineRow(rs.getInt(1), rs.getString(2), rs.getInt(3),
                rs.getString(4), rs.getDouble(5), rs.getString(6), rs.getString(7));
    }
}
