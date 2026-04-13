import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

public class LibraryAppFX extends Application {

    // -------------------------------------------------------
    //  DATABASE CONNECTION
    // -------------------------------------------------------
    private static final String DB_URL  = "jdbc:mysql://localhost:3307/BATB_Library";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "12345678";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // -------------------------------------------------------
    //  UI STATE
    // -------------------------------------------------------
    private StackPane mainPanel;
    private String selectedRole      = "";
    private int    currentBorrowerId = -1;
    private String currentUsername   = "";

    private final ObservableList<UserRow> memberList = FXCollections.observableArrayList();
    private final ObservableList<BookRow> bookList   = FXCollections.observableArrayList();
    private final ObservableList<LoanRow> loanList   = FXCollections.observableArrayList();
    private final ObservableList<FineRow> fineList   = FXCollections.observableArrayList();

    private TextField     loginUserField;
    private PasswordField loginPassField;
    private Label         createAccLabel;

    private VBox       selectionScreen;
    private VBox       loginScreen;
    private VBox       registerScreen;
    private BorderPane adminDashboard;
    private BorderPane studentDashboard;

    // -------------------------------------------------------
    //  MAIN
    // -------------------------------------------------------
    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Library BTAB");
        mainPanel = new StackPane();

        selectionScreen = createSelectionScreen();
        loginScreen     = createLoginScreen();
        registerScreen  = createRegisterScreen();

        mainPanel.getChildren().addAll(selectionScreen, loginScreen, registerScreen);
        showScreen(selectionScreen);

        stage.setScene(new Scene(mainPanel, 1200, 750));
        stage.show();
    }

    // -------------------------------------------------------
    //  SCREEN HELPERS
    // -------------------------------------------------------
    private void showScreen(Pane target) {
        mainPanel.getChildren().forEach(n -> n.setVisible(false));
        target.setVisible(true);
    }

    private void clearAuthFields() {
        if (loginUserField != null) loginUserField.clear();
        if (loginPassField != null) loginPassField.clear();
    }

    // -------------------------------------------------------
    //  SCREEN 1 – ROLE SELECTION
    // -------------------------------------------------------
    private VBox createSelectionScreen() {
        VBox outer = new VBox();
        outer.setAlignment(Pos.CENTER);
        outer.setStyle("-fx-background-color: #e6ebf5;");

        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(520, 320);
        card.setStyle("-fx-background-color: white; -fx-border-color: #d2d7e1; -fx-border-width: 1; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 3);");
        card.setPadding(new Insets(30, 40, 30, 40));

        Label icon  = new Label("📖"); icon.setFont(Font.font("Segoe UI", 44));
        Label title = new Label("Library BTAB");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        Label sub = new Label("Select your role to continue");
        sub.setStyle("-fx-text-fill: #888;");

        HBox roles = new HBox(20); roles.setAlignment(Pos.CENTER);
        roles.setPadding(new Insets(15, 0, 0, 0));
        roles.getChildren().addAll(
                createRoleBox("\uD83D\uDD11", "Librarian", "Admin/Librarian"),
                createRoleBox("🎓", "Member",    "Student/Member")
        );

        card.getChildren().addAll(icon, title, sub, roles);
        outer.getChildren().add(card);
        return outer;
    }

    private VBox createRoleBox(String icon, String label, String roleType) {
        VBox box = new VBox(10); box.setAlignment(Pos.CENTER);
        box.setPrefSize(190, 130); box.setPadding(new Insets(15));
        String base  = "-fx-background-color: white; -fx-border-color: #e6ebf5; "
                + "-fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;";
        String hover = "-fx-background-color: #f0f4ff; -fx-border-color: #6450f0; "
                + "-fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
        box.setStyle(base);
        Label ic = new Label(icon);  ic.setFont(Font.font("Segoe UI", 34));
        Label nm = new Label(label); nm.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        box.getChildren().addAll(ic, nm);
        box.setOnMouseEntered(e -> box.setStyle(hover));
        box.setOnMouseExited (e -> box.setStyle(base));
        box.setOnMouseClicked(e -> {
            selectedRole = roleType; clearAuthFields();
            createAccLabel.setVisible(selectedRole.equals("Student/Member"));
            showScreen(loginScreen);
        });
        return box;
    }

    // -------------------------------------------------------3
    //  SCREEN 2 – LOGIN
    // -------------------------------------------------------
    private VBox createLoginScreen() {
        VBox outer = new VBox(); outer.setAlignment(Pos.CENTER);
        outer.setStyle("-fx-background-color: #e6ebf5;");

        VBox card = new VBox(12); card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxSize(400, 480);
        card.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 3);");
        card.setPadding(new Insets(35, 40, 35, 40));

        Label back = new Label("← Back to role selection");
        back.setStyle("-fx-cursor: hand; -fx-text-fill: #6450f0;");
        back.setOnMouseClicked(e -> { clearAuthFields(); showScreen(selectionScreen); });

        Label title = new Label("Welcome Back");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        Label sub = new Label("Sign in to Library BTAB");
        sub.setStyle("-fx-text-fill: #888;");

        loginUserField = new TextField(); loginUserField.setPromptText("Enter your username");
        loginUserField.setPrefHeight(38); styleField(loginUserField);
        loginPassField = new PasswordField(); loginPassField.setPromptText("Enter your password");
        loginPassField.setPrefHeight(38); styleField(loginPassField);
        loginPassField.setOnAction(e -> handleLogin());

        Button loginBtn = new Button("Login →");
        loginBtn.setPrefHeight(42); loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle("-fx-background-color: #6450f0; -fx-text-fill: white; "
                + "-fx-cursor: hand; -fx-font-size: 14; -fx-background-radius: 6;");
        loginBtn.setOnAction(e -> handleLogin());

        createAccLabel = new Label("Don't have an account? Create one");
        createAccLabel.setStyle("-fx-text-fill: #6450f0; -fx-cursor: hand;");
        createAccLabel.setOnMouseClicked(e -> { clearAuthFields(); showScreen(registerScreen); });

        card.getChildren().addAll(back, title, sub,
                bold("Username"), loginUserField, bold("Password"), loginPassField, loginBtn, createAccLabel);
        outer.getChildren().add(card);
        return outer;
    }

    private void styleField(Control field) {
        field.setStyle("-fx-border-color: #d2d7e1; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 5;");
    }

    private void handleLogin() {
        String u = loginUserField.getText().trim();
        String p = loginPassField.getText();
        if (u.isEmpty() || p.isEmpty()) { showAlert("Please enter username and password."); return; }

        String sql = "SELECT borrower_id, role FROM member "
                + "WHERE username=? AND password_hash=? AND account_status='Active'";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u); ps.setString(2, p);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String dbRole = rs.getString("role");
                if (!dbRole.equals(selectedRole)) {
                    showAlert("Access Denied: Your account is \"" + dbRole + "\".\nPlease go back and choose the correct role."); return;
                }
                currentBorrowerId = rs.getInt("borrower_id"); currentUsername = u;
                if (selectedRole.equals("Admin/Librarian")) {
                    adminDashboard = createAdminDashboard();
                    mainPanel.getChildren().add(adminDashboard); showScreen(adminDashboard);
                } else {
                    studentDashboard = createStudentDashboard();
                    mainPanel.getChildren().add(studentDashboard); showScreen(studentDashboard);
                }
            } else { showAlert("Invalid username or password, or account is suspended."); }
        } catch (SQLException ex) { showAlert("Database error: " + ex.getMessage()); }
    }

    // -------------------------------------------------------
    //  SCREEN 3 – REGISTER
    // -------------------------------------------------------
    private VBox createRegisterScreen() {
        VBox outer = new VBox(); outer.setAlignment(Pos.CENTER);
        outer.setStyle("-fx-background-color: #e6ebf5;");
        VBox card = new VBox(10); card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxSize(420, 560);
        card.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 3);");
        card.setPadding(new Insets(35, 40, 35, 40));

        Label back = new Label("← Back to Login");
        back.setStyle("-fx-cursor: hand; -fx-text-fill: #6450f0;");
        back.setOnMouseClicked(e -> { clearAuthFields(); showScreen(loginScreen); });
        Label title = new Label("Create Member Account");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        TextField nameF  = new TextField(); nameF.setPromptText("Full Name");  styleField(nameF);
        TextField emailF = new TextField(); emailF.setPromptText("Email");     styleField(emailF);
        TextField userF  = new TextField(); userF.setPromptText("Username");   styleField(userF);
        PasswordField passF = new PasswordField(); passF.setPromptText("Password"); styleField(passF);

        Button signUpBtn = new Button("Create Account");
        signUpBtn.setPrefHeight(42); signUpBtn.setMaxWidth(Double.MAX_VALUE);
        signUpBtn.setStyle("-fx-background-color: #6450f0; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;");
        signUpBtn.setOnAction(e -> {
            if (nameF.getText().isBlank() || emailF.getText().isBlank()
                    || userF.getText().isBlank() || passF.getText().isBlank()) {
                showAlert("All fields are required."); return;
            }
            String sql = "INSERT INTO member (username,password_hash,full_name,email,role,join_date) VALUES(?,?,?,?,'Student/Member',?)";
            try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, userF.getText().trim()); ps.setString(2, passF.getText());
                ps.setString(3, nameF.getText().trim()); ps.setString(4, emailF.getText().trim());
                ps.setString(5, LocalDate.now().toString());
                ps.executeUpdate();
                showAlert("Account created! You can now log in.");
                nameF.clear(); emailF.clear(); userF.clear(); passF.clear(); showScreen(loginScreen);
            } catch (SQLIntegrityConstraintViolationException ex) {
                showAlert("Username or email already exists!");
            } catch (SQLException ex) { showAlert("Database error: " + ex.getMessage()); }
        });

        card.getChildren().addAll(back, title,
                bold("Full Name"), nameF, bold("Email"), emailF,
                bold("Username"), userF, bold("Password"), passF, signUpBtn);
        outer.getChildren().add(card);
        return outer;
    }

    // -------------------------------------------------------
    //  SCREEN 4 – ADMIN DASHBOARD
    // -------------------------------------------------------
    private BorderPane createAdminDashboard() {
        BorderPane bp = new BorderPane();
        bp.setStyle("-fx-background-color: #f4f7fa;");

        HBox nav = new HBox(8);
        nav.setStyle("-fx-background-color: #1e1e2e;");
        nav.setPadding(new Insets(12, 20, 12, 20));
        nav.setAlignment(Pos.CENTER_LEFT);

        Label appTitle = new Label("📖 Library BTAB");
        appTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        appTitle.setStyle("-fx-text-fill: white;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button homeBtn        = navBtn("🏠 Home");
        Button addMemberBtn   = navBtn("➕ Add Member");
        Button viewMemberBtn  = navBtn("👥 Members");
        Button addBooksBtn    = navBtn("📚 Add Book");
        Button viewBooksBtn   = navBtn("🔍 Books");
        Button activeLoansBtn = navBtn("📋 Active Loans");
        Button allLoansBtn    = navBtn("📜 History");
        Button finesBtn       = navBtn("💰 Fines");
        Button profileBtn     = navBtn("👤 Profile");
        Button logoutBtn      = navBtn("🚪 Logout");
        logoutBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 12; -fx-background-radius: 4;");

        nav.getChildren().addAll(appTitle, spacer,
                homeBtn, addMemberBtn, viewMemberBtn, addBooksBtn,
                viewBooksBtn, activeLoansBtn, allLoansBtn, finesBtn, profileBtn, logoutBtn);
        bp.setTop(nav);

        StackPane content = new StackPane();
        bp.setCenter(content);

        VBox homeTab        = createAdminHomeTab();
        VBox addMemberTab   = createAddMemberScreen();
        VBox viewMemberTab  = createViewMemberScreen();
        VBox addBookTab     = createAddBookScreen();
        VBox viewBookTab    = createViewBookScreen();
        VBox activeLoansTab = createActiveLoansScreen();
        VBox allLoansTab    = createAllLoansScreen();
        VBox finesTab       = createFinesScreen(true);

        content.getChildren().addAll(homeTab, addMemberTab, viewMemberTab, addBookTab,
                viewBookTab, activeLoansTab, allLoansTab, finesTab);

        homeBtn.setOnAction(e ->        { refreshAdminHome(homeTab);  showTab(content, homeTab); });
        addMemberBtn.setOnAction(e ->   showTab(content, addMemberTab));
        viewMemberBtn.setOnAction(e ->  { refreshMembers();     showTab(content, viewMemberTab); });
        addBooksBtn.setOnAction(e ->    showTab(content, addBookTab));
        viewBooksBtn.setOnAction(e ->   { refreshBooks();       showTab(content, viewBookTab); });
        activeLoansBtn.setOnAction(e -> { refreshActiveLoans(); showTab(content, activeLoansTab); });
        allLoansBtn.setOnAction(e ->    { refreshAllLoans();    showTab(content, allLoansTab); });
        finesBtn.setOnAction(e ->       { refreshFines();       showTab(content, finesTab); });
        profileBtn.setOnAction(e ->     showProfileDialog());
        logoutBtn.setOnAction(e -> {
            clearAuthFields(); currentBorrowerId = -1;
            mainPanel.getChildren().remove(adminDashboard); showScreen(selectionScreen);
        });

        refreshAdminHome(homeTab); showTab(content, homeTab);
        return bp;
    }

    // -------------------------------------------------------
    //  ADMIN HOME – Dashboard
    // -------------------------------------------------------
    private VBox createAdminHomeTab() {
        VBox box = new VBox(20); box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: #f4f7fa;");

        Label title = new Label("Dashboard Overview");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        HBox metrics = new HBox(15);
        metrics.getChildren().addAll(
                statCard("📚", "Total Books",   "—", "#6450f0", "#ede9ff"),
                statCard("👥", "Total Members", "—", "#0891b2", "#e0f7fa"),
                statCard("📋", "Active Loans",  "—", "#d97706", "#fef3c7"),
                statCard("⚠️", "Overdue Loans", "—", "#dc2626", "#fee2e2"),
                statCard("💰", "Unpaid Fines",  "—", "#16a34a", "#dcfce7")
        );

        Label recentTitle = new Label("Top Overdue Loans");
        recentTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        TableView<LoanRow> overdueTable = new TableView<>(loanList);
        overdueTable.setMaxHeight(260); overdueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        overdueTable.getColumns().addAll(
                col("Borrower",     r -> new SimpleStringProperty(r.borrowerName)),
                col("Book",         r -> new SimpleStringProperty(r.bookTitle)),
                col("Due Date",     r -> new SimpleStringProperty(r.dueDate)),
                col("Days Overdue", r -> new SimpleStringProperty(r.daysOverdue + " days ⚠️"))
        );
        colorOverdueRows(overdueTable);

        box.getChildren().addAll(title, metrics, recentTitle, overdueTable);
        return box;
    }

    private void refreshAdminHome(VBox homeTab) {
        HBox metrics = (HBox) homeTab.getChildren().get(1);
        try (Connection con = getConnection()) {
            String[] queries = {
                    "SELECT COUNT(*) FROM book",
                    "SELECT COUNT(*) FROM member WHERE role='Student/Member'",
                    "SELECT COUNT(*) FROM loan WHERE loan_status IN ('Active','Overdue')",
                    "SELECT COUNT(*) FROM loan WHERE loan_status='Overdue'",
                    "SELECT COUNT(*) FROM fine WHERE payment_status='Unpaid'"
            };
            for (int i = 0; i < queries.length; i++) {
                try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(queries[i])) {
                    if (rs.next()) {
                        VBox card = (VBox) metrics.getChildren().get(i);
                        ((Label) card.getChildren().get(2)).setText(String.valueOf(rs.getInt(1)));
                    }
                }
            }
        } catch (SQLException ex) { /* skip quietly */ }

        loanList.clear();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT l.loan_id, m.full_name, m.borrower_id, b.title, l.copy_id, "
                             + "l.issue_date, l.due_date, l.return_date, l.loan_status, "
                             + "GREATEST(0, DATEDIFF(CURRENT_DATE, l.due_date)) AS days_overdue "
                             + "FROM loan l JOIN member m ON l.borrower_id=m.borrower_id "
                             + "JOIN copy c ON l.copy_id=c.copy_id JOIN book b ON c.book_id=b.book_id "
                             + "WHERE l.loan_status='Overdue' ORDER BY days_overdue DESC LIMIT 10")) {
            loadLoanRows(ps);
        } catch (SQLException ex) { /* skip quietly */ }
    }

    private VBox statCard(String icon, String label, String value, String accent, String bg) {
        VBox card = new VBox(4); card.setPadding(new Insets(18)); card.setPrefWidth(180);
        card.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + accent + "; "
                + "-fx-border-width: 0 0 0 4; -fx-border-radius: 6; -fx-background-radius: 6;");
        Label ic  = new Label(icon + "  " + label); ic.setStyle("-fx-text-fill: #555; -fx-font-size: 12;");
        Label val = new Label(value);
        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        val.setStyle("-fx-text-fill: " + accent + ";");
        card.getChildren().addAll(ic, new Label(), val);
        return card;
    }

    private void showTab(StackPane container, Pane tab) {
        container.getChildren().forEach(n -> n.setVisible(false));
        tab.setVisible(true);
    }

    // -------------------------------------------------------
    //  ADD MEMBER
    // -------------------------------------------------------
    private VBox createAddMemberScreen() {
        VBox outer = new VBox(); outer.setAlignment(Pos.CENTER);
        outer.setStyle("-fx-background-color: #f4f7fa;");
        VBox form = new VBox(10); form.setMaxWidth(430); form.setPadding(new Insets(30));
        form.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label lbl = new Label("Register New Student"); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        TextField   mName  = new TextField(); mName.setPromptText("Full Name");      styleField(mName);
        TextField   mUser  = new TextField(); mUser.setPromptText("Username");       styleField(mUser);
        TextField   mEmail = new TextField(); mEmail.setPromptText("Email");         styleField(mEmail);
        TextField   mPhone = new TextField(); mPhone.setPromptText("Contact Number");styleField(mPhone);
        PasswordField mPass = new PasswordField(); mPass.setPromptText("Password");  styleField(mPass);

        Button submit = new Button("Register Student");
        submit.setPrefHeight(40); submit.setMaxWidth(Double.MAX_VALUE);
        submit.setStyle("-fx-background-color: #6450f0; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        submit.setOnAction(e -> {
            if (mName.getText().isBlank() || mUser.getText().isBlank()
                    || mEmail.getText().isBlank() || mPass.getText().isBlank()) {
                showAlert("Name, username, email, and password are required."); return;
            }
            String sql = "INSERT INTO member (username,password_hash,full_name,email,contact_number,role,join_date) "
                    + "VALUES(?,?,?,?,?,'Student/Member',?)";
            try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, mUser.getText().trim()); ps.setString(2, mPass.getText());
                ps.setString(3, mName.getText().trim()); ps.setString(4, mEmail.getText().trim());
                ps.setString(5, mPhone.getText().trim()); ps.setString(6, LocalDate.now().toString());
                ps.executeUpdate();
                showAlert("✅ Student account created!");
                mName.clear(); mUser.clear(); mEmail.clear(); mPhone.clear(); mPass.clear();
            } catch (SQLIntegrityConstraintViolationException ex) {
                showAlert("Username or email already exists!");
            } catch (SQLException ex) { showAlert("Database error: " + ex.getMessage()); }
        });

        form.getChildren().addAll(lbl, bold("Full Name"), mName, bold("Username"), mUser,
                bold("Email"), mEmail, bold("Contact"), mPhone, bold("Password"), mPass, submit);
        outer.getChildren().add(form);
        return outer;
    }

    // -------------------------------------------------------
    //  VIEW MEMBERS
    // -------------------------------------------------------
    private VBox createViewMemberScreen() {
        VBox box = new VBox(10); box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #f4f7fa;");
        Label title = new Label("Student Members"); title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TextField search = new TextField(); search.setPromptText("🔍  Search by name, username, or email…");
        search.setPrefHeight(38); styleField(search);

        FilteredList<UserRow> filtered = new FilteredList<>(memberList, p -> true);
        search.textProperty().addListener((obs, old, val) -> {
            String lo = val.toLowerCase();
            filtered.setPredicate(r -> val.isEmpty()
                    || r.fullName.toLowerCase().contains(lo)
                    || r.username.toLowerCase().contains(lo)
                    || r.email.toLowerCase().contains(lo));
        });

        TableView<UserRow> table = new TableView<>(filtered);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getColumns().addAll(
                col("ID",        r -> new SimpleStringProperty(String.valueOf(r.id))),
                col("Full Name", r -> new SimpleStringProperty(r.fullName)),
                col("Username",  r -> new SimpleStringProperty(r.username)),
                col("Email",     r -> new SimpleStringProperty(r.email)),
                col("Contact",   r -> new SimpleStringProperty(r.contact)),
                col("Status",    r -> new SimpleStringProperty(r.status)),
                col("Join Date", r -> new SimpleStringProperty(r.joinDate))
        );

        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(UserRow item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null && "Suspended".equals(item.status))
                    setStyle("-fx-background-color: #fee2e2;");
                else setStyle("");
            }
        });

        Button toggleBtn = new Button("🔄 Toggle Suspend/Active");
        Button deleteBtn = new Button("🗑 Delete Member");
        deleteBtn.setStyle("-fx-background-color: #fee2e2;");

        toggleBtn.setOnAction(e -> {
            UserRow sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Select a member first."); return; }
            String newStatus = sel.status.equals("Active") ? "Suspended" : "Active";
            if (!confirm("Change \"" + sel.fullName + "\" status to " + newStatus + "?")) return;
            try (Connection con = getConnection();
                 PreparedStatement ps = con.prepareStatement("UPDATE member SET account_status=? WHERE borrower_id=?")) {
                ps.setString(1, newStatus); ps.setInt(2, sel.id); ps.executeUpdate();
                refreshMembers(); showAlert("Status updated to " + newStatus + ".");
            } catch (SQLException ex) { showAlert("Error: " + ex.getMessage()); }
        });

        deleteBtn.setOnAction(e -> {
            UserRow sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Select a member first."); return; }
            if (!confirm("Delete member \"" + sel.fullName + "\"? This cannot be undone.")) return;
            try (Connection con = getConnection();
                 PreparedStatement ps = con.prepareStatement("DELETE FROM member WHERE borrower_id=?")) {
                ps.setInt(1, sel.id); ps.executeUpdate(); refreshMembers(); showAlert("Member deleted.");
            } catch (SQLException ex) { showAlert("Error: " + ex.getMessage()); }
        });

        box.getChildren().addAll(title, search, table, new HBox(10, toggleBtn, deleteBtn));
        return box;
    }

    // -------------------------------------------------------
    //  ADD BOOK
    // -------------------------------------------------------
    private VBox createAddBookScreen() {
        VBox outer = new VBox(); outer.setAlignment(Pos.CENTER);
        outer.setStyle("-fx-background-color: #f4f7fa;");
        VBox form = new VBox(10); form.setMaxWidth(450); form.setPadding(new Insets(30));
        form.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label lbl = new Label("Add New Book"); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        TextField titleF  = new TextField(); titleF.setPromptText("Book Title");     styleField(titleF);
        TextField authorF = new TextField(); authorF.setPromptText("Author");        styleField(authorF);
        TextField isbnF   = new TextField(); isbnF.setPromptText("ISBN (13 digits)");styleField(isbnF);
        TextField catF    = new TextField(); catF.setPromptText("Category");         styleField(catF);
        TextField pubF    = new TextField(); pubF.setPromptText("Publisher");        styleField(pubF);
        TextField yearF   = new TextField(); yearF.setPromptText("Year Published");  styleField(yearF);
        TextField copiesF = new TextField(); copiesF.setPromptText("Number of Copies"); styleField(copiesF);

        Button submit = new Button("Add Book");
        submit.setPrefHeight(40); submit.setMaxWidth(Double.MAX_VALUE);
        submit.setStyle("-fx-background-color: #6450f0; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        submit.setOnAction(e -> {
            if (titleF.getText().isBlank() || authorF.getText().isBlank() || isbnF.getText().isBlank()) {
                showAlert("Title, author, and ISBN are required."); return;
            }
            String insertBook = "INSERT INTO book(title,author,isbn,category,publisher,year_published) VALUES(?,?,?,?,?,?)";
            String insertCopy = "INSERT INTO copy(copy_id,book_id,availability,date_acquired) VALUES(?,?,'Available',?)";
            try (Connection con = getConnection()) {
                con.setAutoCommit(false);
                int bookId;
                try (PreparedStatement ps = con.prepareStatement(insertBook, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, titleF.getText().trim()); ps.setString(2, authorF.getText().trim());
                    ps.setString(3, isbnF.getText().trim());  ps.setString(4, catF.getText().trim());
                    ps.setString(5, pubF.getText().trim());   ps.setString(6, yearF.getText().trim());
                    ps.executeUpdate();
                    ResultSet gen = ps.getGeneratedKeys(); gen.next(); bookId = gen.getInt(1);
                }
                int copies = Integer.parseInt(copiesF.getText().trim());
                String today = LocalDate.now().toString();
                try (PreparedStatement ps2 = con.prepareStatement(insertCopy)) {
                    for (int i = 1; i <= copies; i++) {
                        ps2.setString(1, "copy_" + bookId + "_" + i);
                        ps2.setInt(2, bookId); ps2.setString(3, today); ps2.addBatch();
                    }
                    ps2.executeBatch();
                }
                con.commit();
                showAlert("✅ Book added with " + copies + " copy/copies!");
                titleF.clear(); authorF.clear(); isbnF.clear(); catF.clear(); pubF.clear(); yearF.clear(); copiesF.clear();
            } catch (Exception ex) { showAlert("Error: " + ex.getMessage()); }
        });

        form.getChildren().addAll(lbl, bold("Title"), titleF, bold("Author"), authorF, bold("ISBN"), isbnF,
                bold("Category"), catF, bold("Publisher"), pubF, bold("Year"), yearF,
                bold("Number of Copies"), copiesF, submit);
        outer.getChildren().add(form);
        return outer;
    }

    // -------------------------------------------------------
    //  VIEW BOOKS
    // -------------------------------------------------------
    private VBox createViewBookScreen() {
        VBox box = new VBox(10); box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #f4f7fa;");
        Label title = new Label("Book Catalogue"); title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TextField search = new TextField(); search.setPromptText("🔍  Search by title, author, ISBN, or category…");
        search.setPrefHeight(38); styleField(search);

        FilteredList<BookRow> filtered = new FilteredList<>(bookList, p -> true);
        search.textProperty().addListener((obs, old, val) -> {
            String lo = val.toLowerCase();
            filtered.setPredicate(r -> val.isEmpty()
                    || r.title.toLowerCase().contains(lo)
                    || r.author.toLowerCase().contains(lo)
                    || r.isbn.toLowerCase().contains(lo)
                    || r.category.toLowerCase().contains(lo));
        });

        TableView<BookRow> table = new TableView<>(filtered);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getColumns().addAll(
                col("ID",        r -> new SimpleStringProperty(String.valueOf(r.id))),
                col("Title",     r -> new SimpleStringProperty(r.title)),
                col("Author",    r -> new SimpleStringProperty(r.author)),
                col("ISBN",      r -> new SimpleStringProperty(r.isbn)),
                col("Category",  r -> new SimpleStringProperty(r.category)),
                col("Available", r -> new SimpleStringProperty(r.availableCopies + " / " + r.totalCopies))
        );

        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(BookRow item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null && item.availableCopies == 0)
                    setStyle("-fx-background-color: #fff3cd;");
                else setStyle("");
            }
        });

        Button deleteBtn = new Button("🗑 Delete Selected Book");
        deleteBtn.setStyle("-fx-background-color: #fee2e2; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            BookRow sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Select a book first."); return; }
            if (!confirm("Delete \"" + sel.title + "\"? All its copies will also be deleted.")) return;
            try (Connection con = getConnection();
                 PreparedStatement ps = con.prepareStatement("DELETE FROM book WHERE book_id=?")) {
                ps.setInt(1, sel.id); ps.executeUpdate(); refreshBooks(); showAlert("Book deleted.");
            } catch (SQLException ex) { showAlert("Error: " + ex.getMessage()); }
        });

        box.getChildren().addAll(title, search, table, deleteBtn);
        return box;
    }

    // -------------------------------------------------------
    //  ACTIVE LOANS
    // -------------------------------------------------------
    private VBox createActiveLoansScreen() {
        VBox box = new VBox(10); box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #f4f7fa;");
        Label lbl = new Label("Active & Overdue Loans"); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TableView<LoanRow> table = new TableView<>(loanList);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getColumns().addAll(
                col("Loan ID",      r -> new SimpleStringProperty(String.valueOf(r.loanId))),
                col("Borrower",     r -> new SimpleStringProperty(r.borrowerName)),
                col("Book",         r -> new SimpleStringProperty(r.bookTitle)),
                col("Copy ID",      r -> new SimpleStringProperty(r.copyId)),
                col("Due Date",     r -> new SimpleStringProperty(r.dueDate)),
                col("Days Overdue", r -> new SimpleStringProperty(r.daysOverdue > 0 ? r.daysOverdue + " days ⚠️" : "On time")),
                col("Status",       r -> new SimpleStringProperty(r.status))
        );
        colorOverdueRows(table);

        Button issueBtn  = new Button("📤 Issue Loan");
        issueBtn.setStyle("-fx-background-color: #6450f0; -fx-text-fill: white; -fx-cursor: hand;");
        Button returnBtn = new Button("✅ Mark as Returned");
        returnBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-cursor: hand;");

        issueBtn.setOnAction(e -> showIssueLoanDialog());
        returnBtn.setOnAction(e -> {
            LoanRow sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Select a loan first."); return; }
            String msg = sel.daysOverdue > 0
                    ? "Return this book?\nIt is " + sel.daysOverdue + " days overdue.\n"
                    + "A fine of PHP " + (sel.daysOverdue * 10.0) + " will be recorded."
                    : "Mark this loan as returned? No fine will be charged.";
            if (!confirm(msg)) return;
            String today = LocalDate.now().toString();
            try (Connection con = getConnection()) {
                con.setAutoCommit(false);
                try (PreparedStatement p1 = con.prepareStatement("UPDATE loan SET return_date=?,loan_status='Returned' WHERE loan_id=?");
                     PreparedStatement p2 = con.prepareStatement("UPDATE copy SET availability='Available' WHERE copy_id=?")) {
                    p1.setString(1, today); p1.setInt(2, sel.loanId); p1.executeUpdate();
                    p2.setString(1, sel.copyId); p2.executeUpdate();
                }
                if (sel.daysOverdue > 0) {
                    double amount = sel.daysOverdue * 10.0;
                    try (PreparedStatement pf = con.prepareStatement(
                            "INSERT INTO fine(loan_id,borrower_id,amount,reason) VALUES(?,?,?,?)")) {
                        pf.setInt(1, sel.loanId); pf.setInt(2, sel.borrowerId);
                        pf.setDouble(3, amount);
                        pf.setString(4, "Late return – " + sel.daysOverdue + " days @ PHP 10/day");
                        pf.executeUpdate();
                    }
                    con.commit(); showAlert("Book returned. Fine of PHP " + amount + " recorded.");
                } else { con.commit(); showAlert("Book returned on time. No fine."); }
                refreshActiveLoans();
            } catch (SQLException ex) { showAlert("Error: " + ex.getMessage()); }
        });

        box.getChildren().addAll(lbl, table, new HBox(10, issueBtn, returnBtn));
        return box;
    }

    // Issue Loan Dialog
    private void showIssueLoanDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Issue New Loan");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(25));

        TextField copyField = new TextField(); copyField.setPromptText("e.g. copy_3_2"); styleField(copyField);
        TextField userField = new TextField(); userField.setPromptText("Student username"); styleField(userField);
        DatePicker duePicker = new DatePicker(LocalDate.now().plusDays(14));

        grid.add(new Label("Copy ID:"),  0, 0); grid.add(copyField, 1, 0);
        grid.add(new Label("Username:"), 0, 1); grid.add(userField, 1, 1);
        grid.add(new Label("Due Date:"), 0, 2); grid.add(duePicker, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                if (duePicker.getValue() == null || duePicker.getValue().isBefore(LocalDate.now())) {
                    showAlert("Due date must be today or in the future."); return;
                }
                String sql = "INSERT INTO loan(copy_id,borrower_id,issue_date,due_date,loan_status) "
                        + "SELECT ?,m.borrower_id,?,?,'Active' FROM member m WHERE m.username=?";
                try (Connection con = getConnection()) {
                    con.setAutoCommit(false);
                    try (PreparedStatement ps = con.prepareStatement(sql);
                         PreparedStatement p2 = con.prepareStatement("UPDATE copy SET availability='Borrowed' WHERE copy_id=?")) {
                        ps.setString(1, copyField.getText().trim());
                        ps.setString(2, LocalDate.now().toString());
                        ps.setString(3, duePicker.getValue().toString());
                        ps.setString(4, userField.getText().trim());
                        int rows = ps.executeUpdate();
                        if (rows == 0) { showAlert("Student username not found."); con.rollback(); return; }
                        p2.setString(1, copyField.getText().trim()); p2.executeUpdate();
                    }
                    con.commit(); showAlert("✅ Loan issued successfully!"); refreshActiveLoans();
                } catch (SQLException ex) { showAlert("Error: " + ex.getMessage()); }
            }
        });
    }

    // -------------------------------------------------------
    //  LOAN HISTORY
    // -------------------------------------------------------
    private VBox createAllLoansScreen() {
        VBox box = new VBox(10); box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #f4f7fa;");
        Label lbl = new Label("Full Loan History"); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TableView<LoanRow> table = new TableView<>(loanList);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getColumns().addAll(
                col("Loan ID",     r -> new SimpleStringProperty(String.valueOf(r.loanId))),
                col("Borrower",    r -> new SimpleStringProperty(r.borrowerName)),
                col("Book",        r -> new SimpleStringProperty(r.bookTitle)),
                col("Issue Date",  r -> new SimpleStringProperty(r.issueDate)),
                col("Due Date",    r -> new SimpleStringProperty(r.dueDate)),
                col("Return Date", r -> new SimpleStringProperty(r.returnDate)),
                col("Status",      r -> new SimpleStringProperty(r.status))
        );
        colorOverdueRows(table);

        box.getChildren().addAll(lbl, table);
        return box;
    }

    // -------------------------------------------------------
    //  FINES (Paid=green, Unpaid=red,
    // -------------------------------------------------------
    private VBox createFinesScreen(boolean isLibrarian){
        VBox box = new VBox(10); box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #f4f7fa;");
        Label lbl = new Label("Fines & Penalties"); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TableView<FineRow> table = new TableView<>(fineList);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getColumns().addAll(
                col("Fine ID",  r -> new SimpleStringProperty(String.valueOf(r.fineId))),
                col("Borrower", r -> new SimpleStringProperty(r.borrowerName)),
                col("Book",     r -> new SimpleStringProperty(r.bookTitle)),
                col("Amount",   r -> new SimpleStringProperty("PHP " + String.format("%.2f", r.amount))),
                col("Reason",   r -> new SimpleStringProperty(r.reason)),
                col("Status",   r -> new SimpleStringProperty(r.paymentStatus))
        );

        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(FineRow item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    if ("Unpaid".equals(item.paymentStatus))    setStyle("-fx-background-color: #fee2e2;");
                    else if ("Paid".equals(item.paymentStatus)) setStyle("-fx-background-color: #dcfce7;");
                    else setStyle("-fx-background-color: #fef3c7;");
                } else setStyle("");
            }
        });

        Button markPaidBtn = new Button("✅ Mark as Paid (Cash)");
        markPaidBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-cursor: hand;");

        markPaidBtn.setOnAction(e -> {
            FineRow sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Select a fine first."); return; }
            if ("Paid".equals(sel.paymentStatus)) { showAlert("This fine is already paid."); return; }
            if (!confirm("Mark PHP " + String.format("%.2f", sel.amount) + " fine as paid (Cash)?")) return;
            try (Connection con = getConnection()) {
                con.setAutoCommit(false);
                try (PreparedStatement p1 = con.prepareStatement("UPDATE fine SET payment_status='Paid' WHERE fine_id=?");
                     PreparedStatement p2 = con.prepareStatement(
                             "INSERT INTO payment(fine_id,borrower_id,paid_amount,payment_date,payment_method,received_by) VALUES(?,?,?,?,?,?)")) {
                    p1.setInt(1, sel.fineId); p1.executeUpdate();
                    p2.setInt(1, sel.fineId); p2.setInt(2, sel.borrowerId);
                    p2.setDouble(3, sel.amount); p2.setString(4, LocalDate.now().toString());
                    p2.setString(5, "Cash"); p2.setInt(6, currentBorrowerId);
                    p2.executeUpdate();
                }
                con.commit(); showAlert("Fine marked as paid."); refreshFines();
            } catch (SQLException ex) { showAlert("Error: " + ex.getMessage()); }
        });
        if (isLibrarian) {
            box.getChildren().addAll(lbl, table, new HBox(10, markPaidBtn));
        } else {
            box.getChildren().addAll(lbl, table);
        }
        return box;
    }

    // -------------------------------------------------------
    //  SCREEN 5 – STUDENT DASHBOARD
    // -------------------------------------------------------
    private BorderPane createStudentDashboard() {
        BorderPane bp = new BorderPane(); bp.setStyle("-fx-background-color: #f4f7fa;");

        HBox nav = new HBox(10); nav.setStyle("-fx-background-color: #1e1e2e;");
        nav.setPadding(new Insets(12, 20, 12, 20)); nav.setAlignment(Pos.CENTER_LEFT);

        Label appTitle = new Label("📖 Library BTAB");
        appTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        appTitle.setStyle("-fx-text-fill: white;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label userLabel = new Label("👤 " + currentUsername);
        userLabel.setStyle("-fx-text-fill: #aaa; -fx-padding: 0 10 0 0;");

        Button viewBooksBtn = navBtn("📚 Browse Books");
        Button myLoansBtn   = navBtn("📋 My Loans");
        Button myFinesBtn   = navBtn("💰 My Fines");
        Button logoutBtn    = navBtn("🚪 Logout");
        logoutBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 12; -fx-background-radius: 4;");

        nav.getChildren().addAll(appTitle, spacer, userLabel, viewBooksBtn, myLoansBtn, myFinesBtn, logoutBtn);
        bp.setTop(nav);

        StackPane content = new StackPane(); bp.setCenter(content);

        VBox browseTab = createStudentBrowseTab();
        VBox loansTab  = createStudentLoansTab();
        VBox finesTab  = createFinesScreen(false);
        content.getChildren().addAll(browseTab, loansTab, finesTab);

        viewBooksBtn.setOnAction(e -> { refreshBooks();        showTab(content, browseTab); });
        myLoansBtn.setOnAction(e ->   { refreshStudentLoans(); showTab(content, loansTab); });
        myFinesBtn.setOnAction(e ->   { refreshStudentFines(); showTab(content, finesTab); });
        logoutBtn.setOnAction(e -> {
            clearAuthFields(); currentBorrowerId = -1;
            mainPanel.getChildren().remove(studentDashboard); showScreen(selectionScreen);
        });

        refreshBooks(); showTab(content, browseTab);
        return bp;
    }

    private VBox createStudentBrowseTab() {
        VBox box = new VBox(15); box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #f4f7fa;");
        Label title = new Label("Book Catalogue"); title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

        TextField search = new TextField(); search.setPromptText("🔍  Search books…"); styleField(search);
        FilteredList<BookRow> filtered = new FilteredList<>(bookList, p -> true);
        search.textProperty().addListener((obs, old, val) -> {
            String lo = val.toLowerCase();
            filtered.setPredicate(r -> val.isEmpty()
                    || r.title.toLowerCase().contains(lo)
                    || r.author.toLowerCase().contains(lo)
                    || r.category.toLowerCase().contains(lo));
        });

        ScrollPane scroll = new ScrollPane(); scroll.setFitToWidth(true);
        TilePane grid = new TilePane(); grid.setPrefColumns(3); grid.setHgap(15); grid.setVgap(15);
        grid.setPadding(new Insets(10)); scroll.setContent(grid);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Runnable populateGrid = () -> {
            grid.getChildren().clear();
            for (BookRow b : filtered) {
                VBox card = new VBox(6); card.setPadding(new Insets(14));

                boolean available = b.availableCopies > 0;
                String baseBorder  = available ? "#d1fae5" : "#e2e8f0";
                String hoverBorder = available ? "#10b981" : "#cbd5e1";

                String baseStyle  = "-fx-border-color: " + baseBorder  + "; -fx-background-color: white; "
                        + "-fx-border-radius: 8; -fx-background-radius: 8;"
                        + (available ? " -fx-cursor: hand;" : "");
                String hoverStyle = "-fx-border-color: " + hoverBorder + "; -fx-background-color: "
                        + (available ? "#f0fdf4" : "#f8fafc") + "; "
                        + "-fx-border-radius: 8; -fx-background-radius: 8;"
                        + (available ? " -fx-cursor: hand;" : "");

                card.setStyle(baseStyle);

                String avText  = available ? "✅ Available: " + b.availableCopies : "❌ Not Available";
                String avColor = available ? "#dcfce7" : "#fee2e2";
                String avTextColor = available ? "#166534" : "#991b1b";

                Label t  = new Label("📖 " + b.title);
                t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13)); t.setWrapText(true);
                Label a  = new Label("✍  " + b.author); a.setStyle("-fx-text-fill: #666;");
                Label ca = new Label("🏷  " + b.category); ca.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");

                Label av = new Label(avText);
                av.setStyle("-fx-background-color: " + avColor + "; -fx-text-fill: " + avTextColor + "; "
                        + "-fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11; -fx-font-weight: bold;");

                Label hint = new Label("Click to Borrow →");
                hint.setStyle("-fx-text-fill: #10b981; -fx-font-size: 10; -fx-font-style: italic;");
                hint.setVisible(available);

                card.getChildren().addAll(t, a, ca, av, hint);

                if (available) {
                    card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
                    card.setOnMouseExited(e  -> card.setStyle(baseStyle));
                    card.setOnMouseClicked(e -> showBorrowDialog(b));
                }

                grid.getChildren().add(card);
            }
        };

        filtered.addListener((javafx.collections.ListChangeListener<BookRow>) c -> populateGrid.run());
        bookList.addListener((javafx.collections.ListChangeListener<BookRow>) c -> populateGrid.run());

        box.getChildren().addAll(title, search, scroll);
        return box;
    }

    // -------------------------------------------------------
//  BORROW DIALOG
// -------------------------------------------------------
    private void showBorrowDialog(BookRow book) {
        // ── Fetch one available copy_id for this book ──────────────────
        String availableCopyId = null;
        String fetchCopy = "SELECT copy_id FROM copy WHERE book_id = ? AND availability = 'Available' LIMIT 1";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(fetchCopy)) {
            ps.setInt(1, book.id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) availableCopyId = rs.getString("copy_id");
        } catch (SQLException ex) {
            showAlert("Database error: " + ex.getMessage()); return;
        }
        if (availableCopyId == null) {
            showAlert("Sorry, no copies are available right now."); return;
        }
        final String copyId = availableCopyId;

        // ── Check: member already borrowing this book? ─────────────────
        String dupCheck = "SELECT COUNT(*) FROM loan l JOIN copy c ON l.copy_id = c.copy_id "
                + "WHERE c.book_id = ? AND l.borrower_id = ? AND l.loan_status IN ('Active','Overdue')";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(dupCheck)) {
            ps.setInt(1, book.id); ps.setInt(2, currentBorrowerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                showAlert("⚠️ You are already borrowing a copy of \"" + book.title + "\"."); return;
            }
        } catch (SQLException ex) {
            showAlert("Database error: " + ex.getMessage()); return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Borrow Book");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: white; -fx-border-radius: 10; -fx-background-radius: 10;");
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        javafx.scene.Node okBtn = pane.lookupButton(ButtonType.OK);
        okBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 6;");
        ((Button) okBtn).setText("✅ Confirm Borrow");

        javafx.scene.Node cancelBtn = pane.lookupButton(ButtonType.CANCEL);
        cancelBtn.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 6;");

        // Content
        VBox content = new VBox(14);
        content.setPadding(new Insets(20, 25, 10, 25));

        // Book info header
        VBox bookInfo = new VBox(4);
        bookInfo.setStyle("-fx-background-color: #f0fdf4; -fx-border-color: #d1fae5; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 14;");

        Label bookTitle = new Label("📖 " + book.title);
        bookTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        bookTitle.setWrapText(true);

        Label bookAuthor = new Label("✍  " + book.author);
        bookAuthor.setStyle("-fx-text-fill: #555;");

        Label bookCat = new Label("🏷  " + book.category + "   |   Copy: " + copyId);
        bookCat.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");

        Label availLabel = new Label("✅ " + book.availableCopies + " cop" + (book.availableCopies == 1 ? "y" : "ies") + " available");
        availLabel.setStyle("-fx-text-fill: #166534; -fx-font-weight: bold; -fx-font-size: 11;");

        bookInfo.getChildren().addAll(bookTitle, bookAuthor, bookCat, availLabel);

        // Due date picker
        VBox dateBox = new VBox(6);
        Label dateLabel = bold("Select Return Due Date:");
        DatePicker duePicker = new DatePicker(LocalDate.now().plusDays(14));
        duePicker.setMaxWidth(Double.MAX_VALUE);
        duePicker.setStyle("-fx-border-color: #d1d5db; -fx-border-radius: 5; -fx-background-radius: 5;");

        // Quick-select buttons
        HBox quickDates = new HBox(8);
        quickDates.setAlignment(Pos.CENTER_LEFT);
        Label quickLabel = new Label("Quick pick:"); quickLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        Button w1 = quickBtn("1 week",  duePicker, 7);
        Button w2 = quickBtn("2 weeks", duePicker, 14);
        Button w3 = quickBtn("3 weeks", duePicker, 21);
        Button w4 = quickBtn("1 month", duePicker, 30);
        quickDates.getChildren().addAll(quickLabel, w1, w2, w3, w4);

        dateBox.getChildren().addAll(dateLabel, duePicker, quickDates);

        // Policy note
        Label policy = new Label("ℹ️  A fine of PHP 10.00 per day will be charged for late returns.");
        policy.setStyle("-fx-text-fill: #b45309; -fx-font-size: 11; -fx-background-color: #fef3c7; "
                + "-fx-padding: 8 10; -fx-background-radius: 6;");
        policy.setWrapText(true);

        content.getChildren().addAll(bookInfo, dateBox, policy);
        pane.setContent(content);

        // ── Handle confirmation ────────────────────────────────────────
        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            LocalDate dueDate = duePicker.getValue();
            if (dueDate == null || !dueDate.isAfter(LocalDate.now())) {
                showAlert("⚠️ Due date must be a future date."); return;
            }

            String issueSql = "INSERT INTO loan(copy_id, borrower_id, issue_date, due_date, loan_status) "
                    + "VALUES(?, ?, ?, ?, 'Active')";
            String updateCopy = "UPDATE copy SET availability = 'Borrowed' WHERE copy_id = ?";

            try (Connection con = getConnection()) {
                con.setAutoCommit(false);
                try (PreparedStatement ps1 = con.prepareStatement(issueSql);
                     PreparedStatement ps2 = con.prepareStatement(updateCopy)) {

                    ps1.setString(1, copyId);
                    ps1.setInt(2, currentBorrowerId);
                    ps1.setString(3, LocalDate.now().toString());
                    ps1.setString(4, dueDate.toString());
                    ps1.executeUpdate();

                    ps2.setString(1, copyId);
                    ps2.executeUpdate();

                    con.commit();
                } catch (SQLException ex) {
                    con.rollback();
                    showAlert("Failed to borrow book: " + ex.getMessage()); return;
                }

                showAlert("✅ Success! You have borrowed:\n\n"
                        + "📖 " + book.title + "\n"
                        + "📅 Due date: " + dueDate + "\n"
                        + "🔖 Copy ID: " + copyId + "\n\n"
                        + "Please return it on time to avoid fines.");

                refreshBooks();
                refreshStudentLoans();

            } catch (SQLException ex) {
                showAlert("Database error: " + ex.getMessage());
            }
        });
    }

    private Button quickBtn(String label, DatePicker picker, int days) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; "
                + "-fx-font-size: 10; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 3 8;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #bae6fd; -fx-text-fill: #0369a1; "
                + "-fx-font-size: 10; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 3 8;"));
        b.setOnMouseExited(e  -> b.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; "
                + "-fx-font-size: 10; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 3 8;"));
        b.setOnAction(e -> picker.setValue(LocalDate.now().plusDays(days)));
        return b;
    }

    private VBox createStudentLoansTab() {
        VBox box = new VBox(10); box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #f4f7fa;");
        Label lbl = new Label("My Loan History"); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TableView<LoanRow> table = new TableView<>(loanList);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getColumns().addAll(
                col("Book",        r -> new SimpleStringProperty(r.bookTitle)),
                col("Copy ID",     r -> new SimpleStringProperty(r.copyId)),
                col("Issue Date",  r -> new SimpleStringProperty(r.issueDate)),
                col("Due Date",    r -> new SimpleStringProperty(r.dueDate)),
                col("Return Date", r -> new SimpleStringProperty(r.returnDate)),
                col("Status",      r -> new SimpleStringProperty(r.status)),
                col("Days Overdue",r -> new SimpleStringProperty(r.daysOverdue > 0 ? r.daysOverdue + " days ⚠️" : "—"))
        );
        colorOverdueRows(table);
        box.getChildren().addAll(lbl, table);
        return box;
    }

    // -------------------------------------------------------
    //  REFRESH METHODS
    // -------------------------------------------------------
    private void refreshMembers() {
        memberList.clear();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT borrower_id,full_name,username,email,contact_number,account_status,join_date "
                             + "FROM member WHERE role='Student/Member' ORDER BY borrower_id")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                memberList.add(new UserRow(rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)));
        } catch (SQLException ex) { showAlert("Error loading members: " + ex.getMessage()); }
    }

    private void refreshBooks() {
        bookList.clear();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT b.book_id,b.title,b.author,b.isbn,b.category,"
                             + "COUNT(c.copy_id) AS total_copies,"
                             + "COALESCE(SUM(c.availability='Available'),0) AS available_copies "
                             + "FROM book b LEFT JOIN copy c ON b.book_id=c.book_id "
                             + "GROUP BY b.book_id ORDER BY b.title")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                bookList.add(new BookRow(rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getInt(7), rs.getInt(6)));
        } catch (SQLException ex) { showAlert("Error loading books: " + ex.getMessage()); }
    }

    private void refreshActiveLoans() {
        loanList.clear();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT l.loan_id,m.full_name,m.borrower_id,b.title,l.copy_id,"
                             + "l.issue_date,l.due_date,l.return_date,l.loan_status,"
                             + "GREATEST(0,DATEDIFF(CURRENT_DATE,l.due_date)) AS days_overdue "
                             + "FROM loan l JOIN member m ON l.borrower_id=m.borrower_id "
                             + "JOIN copy c ON l.copy_id=c.copy_id JOIN book b ON c.book_id=b.book_id "
                             + "WHERE l.loan_status IN ('Active','Overdue') ORDER BY l.due_date")) {
            loadLoanRows(ps);
        } catch (SQLException ex) { showAlert("Error loading loans: " + ex.getMessage()); }
    }

    private void refreshAllLoans() {
        loanList.clear();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT l.loan_id,m.full_name,m.borrower_id,b.title,l.copy_id,"
                             + "l.issue_date,l.due_date,l.return_date,l.loan_status,"
                             + "GREATEST(0,DATEDIFF(CURRENT_DATE,l.due_date)) AS days_overdue "
                             + "FROM loan l JOIN member m ON l.borrower_id=m.borrower_id "
                             + "JOIN copy c ON l.copy_id=c.copy_id JOIN book b ON c.book_id=b.book_id "
                             + "ORDER BY l.loan_id DESC")) {
            loadLoanRows(ps);
        } catch (SQLException ex) { showAlert("Error loading loan history: " + ex.getMessage()); }
    }

    private void refreshStudentLoans() {
        loanList.clear();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT l.loan_id,m.full_name,m.borrower_id,b.title,l.copy_id,"
                             + "l.issue_date,l.due_date,l.return_date,l.loan_status,"
                             + "GREATEST(0,DATEDIFF(CURRENT_DATE,l.due_date)) AS days_overdue "
                             + "FROM loan l JOIN member m ON l.borrower_id=m.borrower_id "
                             + "JOIN copy c ON l.copy_id=c.copy_id JOIN book b ON c.book_id=b.book_id "
                             + "WHERE l.borrower_id=? ORDER BY l.issue_date DESC")) {
            ps.setInt(1, currentBorrowerId); loadLoanRows(ps);
        } catch (SQLException ex) { showAlert("Error: " + ex.getMessage()); }
    }

    private void loadLoanRows(PreparedStatement ps) throws SQLException {
        ResultSet rs = ps.executeQuery();
        while (rs.next())
            loanList.add(new LoanRow(rs.getInt(1), rs.getString(2), rs.getInt(3),
                    rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7),
                    rs.getString(8) == null ? "—" : rs.getString(8),
                    rs.getString(9), rs.getInt(10)));
    }

    private void refreshFines() {
        fineList.clear();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT f.fine_id,m.full_name,m.borrower_id,b.title,f.amount,f.reason,f.payment_status "
                             + "FROM fine f JOIN member m ON f.borrower_id=m.borrower_id "
                             + "JOIN loan l ON f.loan_id=l.loan_id JOIN copy c ON l.copy_id=c.copy_id "
                             + "JOIN book b ON c.book_id=b.book_id ORDER BY f.fine_id DESC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                fineList.add(new FineRow(rs.getInt(1), rs.getString(2), rs.getInt(3),
                        rs.getString(4), rs.getDouble(5), rs.getString(6), rs.getString(7)));
        } catch (SQLException ex) { showAlert("Error loading fines: " + ex.getMessage()); }
    }

    private void refreshStudentFines() {
        fineList.clear();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT f.fine_id,m.full_name,m.borrower_id,b.title,f.amount,f.reason,f.payment_status "
                             + "FROM fine f JOIN member m ON f.borrower_id=m.borrower_id "
                             + "JOIN loan l ON f.loan_id=l.loan_id JOIN copy c ON l.copy_id=c.copy_id "
                             + "JOIN book b ON c.book_id=b.book_id WHERE f.borrower_id=? ORDER BY f.fine_id DESC")) {
            ps.setInt(1, currentBorrowerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                fineList.add(new FineRow(rs.getInt(1), rs.getString(2), rs.getInt(3),
                        rs.getString(4), rs.getDouble(5), rs.getString(6), rs.getString(7)));
        } catch (SQLException ex) { showAlert("Error: " + ex.getMessage()); }
    }

    // -------------------------------------------------------
    //  PROFILE DIALOG
    // -------------------------------------------------------
    private void showProfileDialog() {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT full_name,username,email,contact_number,role,join_date FROM member WHERE borrower_id=?")) {
            ps.setInt(1, currentBorrowerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) showAlert("👤 Profile Information\n\n"
                    + "Name:    " + rs.getString(1) + "\n"
                    + "User:    " + rs.getString(2) + "\n"
                    + "Email:   " + rs.getString(3) + "\n"
                    + "Contact: " + rs.getString(4) + "\n"
                    + "Role:    " + rs.getString(5) + "\n"
                    + "Joined:  " + rs.getString(6));
        } catch (SQLException ex) { showAlert("Error: " + ex.getMessage()); }
    }

    // -------------------------------------------------------
    //  UTILITIES
    // -------------------------------------------------------
    private <T> TableColumn<T, String> col(String title,
                                           java.util.function.Function<T, javafx.beans.value.ObservableValue<String>> factory) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(data -> factory.apply(data.getValue()));
        return c;
    }

    private void colorOverdueRows(TableView<LoanRow> table) {
        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(LoanRow item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    if (item.daysOverdue > 7)      setStyle("-fx-background-color: #fee2e2;");
                    else if (item.daysOverdue > 0) setStyle("-fx-background-color: #fef3c7;");
                    else                           setStyle("");
                } else setStyle("");
            }
        });
    }

    private Button navBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; -fx-cursor: hand; -fx-font-size: 12;");
        b.setOnMouseEntered(e -> b.setStyle(b.getStyle().replace("#ccc", "white")));
        b.setOnMouseExited (e -> b.setStyle(b.getStyle().replace("white", "#ccc")));
        return b;
    }

    private Label bold(String text) {
        Label l = new Label(text); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); return l;
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    // -------------------------------------------------------
    //  DATA MODEL CLASSES
    // -------------------------------------------------------
    public static class UserRow {
        int id; String fullName, username, email, contact, status, joinDate;
        UserRow(int id, String fn, String u, String e, String c, String s, String j) {
            this.id=id; fullName=fn; username=u; email=e; contact=c==null?"":c; status=s; joinDate=j;
        }
    }
    public static class BookRow {
        int id, availableCopies, totalCopies; String title, author, isbn, category;
        BookRow(int id, String t, String a, String i, String c, int av, int tot) {
            this.id=id; title=t; author=a; isbn=i; category=c==null?"":c; availableCopies=av; totalCopies=tot;
        }
    }
    public static class LoanRow {
        int loanId, borrowerId, daysOverdue;
        String borrowerName, bookTitle, copyId, issueDate, dueDate, returnDate, status;
        LoanRow(int li, String bn, int bi, String bt, String ci, String id, String dd, String rd, String st, int dov) {
            loanId=li; borrowerName=bn; borrowerId=bi; bookTitle=bt; copyId=ci;
            issueDate=id; dueDate=dd; returnDate=rd; status=st; daysOverdue=dov;
        }
    }
    public static class FineRow {
        int fineId, borrowerId; double amount; String borrowerName, bookTitle, reason, paymentStatus;
        FineRow(int fi, String bn, int bi, String bt, double am, String r, String ps) {
            fineId=fi; borrowerName=bn; borrowerId=bi; bookTitle=bt; amount=am; reason=r; paymentStatus=ps;
        }
    }
}