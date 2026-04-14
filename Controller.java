// =============================================================
//  CONTROLLER LAYER  –  Controller.java
//  Contains: AuthController, BookController, MemberController,
//  LoanController, FineController, DashboardController.
//  No JavaFX UI nodes. Only ObservableLists, session state,
//  and action methods that delegate to the Model layer.
// =============================================================

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.SQLException;
import java.util.List;

// -------------------------------------------------------------
//  AUTH CONTROLLER  –  session state + login/register actions
// -------------------------------------------------------------
class AuthController {
    private final AuthModel model = new AuthModel();

    // Session state (read by other controllers and views)
    public int    currentBorrowerId = -1;
    public String currentUsername   = "";
    public String selectedRole      = "";   // "Admin/Librarian" | "Student/Member"

    /**
     * Attempts login. Returns the LoginResult on success so the
     * caller can check the role and navigate accordingly.
     * Throws on DB error; returns null if credentials don't match.
     */
    public AuthModel.LoginResult login(String username, String password) throws SQLException {
        AuthModel.LoginResult result = model.login(username, password);
        if (result != null) {
            currentBorrowerId = result.borrowerId;
            currentUsername   = username;
        }
        return result;
    }

    /** Self-registration (Student role). Throws on DB/constraint error. */
    public void register(String username, String password,
                         String fullName, String email) throws SQLException {
        model.register(username, password, fullName, email);
    }

    /** Returns a 6-element String array: name, user, email, contact, role, joinDate. */
    public String[] fetchProfile() throws SQLException {
        return model.fetchProfile(currentBorrowerId);
    }

    /** Clears session state on logout. */
    public void logout() {
        currentBorrowerId = -1;
        currentUsername   = "";
        selectedRole      = "";
    }
}

// -------------------------------------------------------------
//  BOOK CONTROLLER
// -------------------------------------------------------------
class BookController {
    private final BookModel model = new BookModel();
    public final ObservableList<BookRow> books = FXCollections.observableArrayList();

    /** Reloads the full book catalogue from DB into the observable list. */
    public void refresh() throws SQLException {
        books.setAll(model.fetchAll());
    }

    public void addBook(String title, String author, String isbn,
                        String category, String publisher,
                        String year, int copies) throws SQLException {
        model.addBook(title, author, isbn, category, publisher, year, copies);
        refresh();
    }

    public void deleteBook(int bookId) throws SQLException {
        model.deleteBook(bookId);
        refresh();
    }

    /**
     * Returns one available copy_id for the book, or null.
     * Also validates that the member doesn't already have it borrowed.
     * Throws IllegalStateException with a user-friendly message
     * that the View can display directly.
     */
    public String prepareForBorrow(int bookId, int borrowerId) throws SQLException {
        if (model.countActiveLoanForMember(bookId, borrowerId) > 0)
            throw new IllegalStateException("You are already borrowing a copy of this book.");
        String copyId = model.fetchAvailableCopy(bookId);
        if (copyId == null)
            throw new IllegalStateException("No copies are currently available.");
        return copyId;
    }
}

// -------------------------------------------------------------
//  MEMBER CONTROLLER
// -------------------------------------------------------------
class MemberController {
    private final MemberModel model = new MemberModel();
    public final ObservableList<UserRow> members = FXCollections.observableArrayList();

    public void refresh() throws SQLException {
        members.setAll(model.fetchAllStudents());
    }

    /** Admin-side registration with contact number. */
    public void addStudent(String username, String password, String fullName,
                           String email, String contact) throws SQLException {
        model.addStudent(username, password, fullName, email, contact);
        refresh();
    }

    /**
     * Toggles the selected member between Active and Suspended.
     * Returns the new status string so the View can show confirmation.
     */
    public String toggleStatus(UserRow member) throws SQLException {
        String newStatus = "Active".equals(member.status) ? "Suspended" : "Active";
        model.updateStatus(member.id, newStatus);
        refresh();
        return newStatus;
    }

    public void deleteMember(int borrowerId) throws SQLException {
        model.deleteMember(borrowerId);
        refresh();
    }
}

// -------------------------------------------------------------
//  LOAN CONTROLLER
// -------------------------------------------------------------
class LoanController {
    private final LoanModel model = new LoanModel();
    public final ObservableList<LoanRow> loans = FXCollections.observableArrayList();

    public void refreshActive() throws SQLException {
        loans.setAll(model.fetchActive());
    }

    public void refreshAll() throws SQLException {
        loans.setAll(model.fetchAll());
    }

    public void refreshByBorrower(int borrowerId) throws SQLException {
        loans.setAll(model.fetchByBorrower(borrowerId));
    }

    public List<LoanRow> fetchOverdueTop10() throws SQLException {
        return model.fetchOverdueTop10();
    }

    /** Admin issues a loan. Throws SQLException with a message the View can surface. */
    public void issueLoan(String copyId, String username, String dueDate) throws SQLException {
        model.issueLoan(copyId, username, dueDate);
        refreshActive();
    }

    /**
     * Student self-borrows. The View must call BookController.prepareForBorrow()
     * first to obtain the copyId and validate eligibility.
     */
    public void borrowBook(String copyId, int borrowerId, String dueDate) throws SQLException {
        model.borrowBook(copyId, borrowerId, dueDate);
    }

    /**
     * Returns a loan and inserts a fine if overdue.
     * Returns the fine amount (0.0 if the return was on time).
     */
    public double returnLoan(LoanRow loan, int receivedBy) throws SQLException {
        double fine = model.returnLoan(loan, receivedBy);
        refreshActive();
        return fine;
    }
}

// -------------------------------------------------------------
//  FINE CONTROLLER
// -------------------------------------------------------------
class FineController {
    private final FineModel model = new FineModel();
    public final ObservableList<FineRow> fines = FXCollections.observableArrayList();

    public void refreshAll() throws SQLException {
        fines.setAll(model.fetchAll());
    }

    public void refreshByBorrower(int borrowerId) throws SQLException {
        fines.setAll(model.fetchByBorrower(borrowerId));
    }

    /**
     * Marks a fine as paid (Cash) and records a payment row.
     * Throws IllegalStateException if the fine is already paid.
     */
    public void markPaid(FineRow fine, int receivedByBorrowerId) throws SQLException {
        if ("Paid".equals(fine.paymentStatus))
            throw new IllegalStateException("This fine is already paid.");
        model.markPaid(fine, receivedByBorrowerId);
        refreshAll();
    }
}

// -------------------------------------------------------------
//  DASHBOARD CONTROLLER  –  aggregate stats for admin home
// -------------------------------------------------------------
class DashboardController {
    private final BookModel   bookModel   = new BookModel();
    private final MemberModel memberModel = new MemberModel();
    private final LoanModel   loanModel   = new LoanModel();
    private final FineModel   fineModel   = new FineModel();

    /** Holds the five summary counts shown on the admin home tab. */
    public static class Stats {
        public int totalBooks, totalMembers, activeLoans, overdueLoans, unpaidFines;
    }

    /**
     * Queries all five dashboard counters in a single method.
     * The View calls this and updates its labels directly.
     */
    public Stats fetchStats() throws SQLException {
        Stats s = new Stats();
        String[] queries = {
            "SELECT COUNT(*) FROM book",
            "SELECT COUNT(*) FROM member WHERE role='Student/Member'",
            "SELECT COUNT(*) FROM loan WHERE loan_status IN ('Active','Overdue')",
            "SELECT COUNT(*) FROM loan WHERE loan_status='Overdue'",
            "SELECT COUNT(*) FROM fine WHERE payment_status='Unpaid'"
        };
        try (java.sql.Connection con = DatabaseManager.getConnection()) {
            for (int i = 0; i < queries.length; i++) {
                try (java.sql.Statement st = con.createStatement();
                     java.sql.ResultSet rs = st.executeQuery(queries[i])) {
                    if (rs.next()) {
                        switch (i) {
                            case 0 -> s.totalBooks    = rs.getInt(1);
                            case 1 -> s.totalMembers  = rs.getInt(1);
                            case 2 -> s.activeLoans   = rs.getInt(1);
                            case 3 -> s.overdueLoans  = rs.getInt(1);
                            case 4 -> s.unpaidFines   = rs.getInt(1);
                        }
                    }
                }
            }
        }
        return s;
    }

    /** Top 10 overdue loans for the dashboard table. */
    public List<LoanRow> fetchOverdueTop10() throws SQLException {
        return loanModel.fetchOverdueTop10();
    }
}
