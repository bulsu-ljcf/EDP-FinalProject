// =============================================================
//  VIEW LAYER  –  View.java
//  Contains: LibraryAppFX (Application entry point + navigation),
//  and all screen-building classes:
//    SelectionView, LoginView, RegisterView,
//    AdminDashboardView, StudentDashboardView
//  No SQL, no business logic — only JavaFX layout and wiring.
// =============================================================


import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.Optional;

// =============================================================
//  APPLICATION ENTRY POINT + NAVIGATION
// =============================================================
public class View extends Application {

    // Controllers (one per domain, shared across views)
    private final AuthController      auth       = new AuthController();
    private final BookController      bookCtrl   = new BookController();
    private final MemberController    memberCtrl = new MemberController();
    private final LoanController      loanCtrl   = new LoanController();
    private final FineController      fineCtrl   = new FineController();
    private final DashboardController dashCtrl   = new DashboardController();

    // Root panel — all screens live as children; only one visible at a time
    private StackPane mainPanel;

    // Screens that persist for the lifetime of the app
    private VBox       selectionScreen;
    private VBox       loginScreen;
    private VBox       registerScreen;

    // Dashboards are rebuilt on each login so stale data is never shown
    private BorderPane adminDashboard;
    private BorderPane studentDashboard;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        mainPanel = new StackPane();

        selectionScreen = new SelectionView(auth, this::showLogin).build();
        loginScreen     = new LoginView(auth, this::onLoginSuccess, this::showSelection).build();
        registerScreen  = new RegisterView(auth, this::showLogin).build();

        mainPanel.getChildren().addAll(selectionScreen, loginScreen, registerScreen);
        showScreen(selectionScreen);

        stage.setScene(new Scene(mainPanel, 1200, 750));
        stage.setTitle("Library BTAB");
        stage.show();
    }

    // -------------------------------------------------------
    //  NAVIGATION HELPERS
    // -------------------------------------------------------
    void showScreen(Pane target) {
        mainPanel.getChildren().forEach(n -> n.setVisible(false));
        target.setVisible(true);
    }

    private void showSelection() {
        // Remove any previously built dashboards to avoid stale state
        if (adminDashboard   != null) mainPanel.getChildren().remove(adminDashboard);
        if (studentDashboard != null) mainPanel.getChildren().remove(studentDashboard);
        adminDashboard   = null;
        studentDashboard = null;
        showScreen(selectionScreen);
    }

    private void showLogin() { showScreen(loginScreen); }

    /** Called by LoginView after a successful credential check. */
    private void onLoginSuccess() {
        try {
            if ("Admin/Librarian".equals(auth.selectedRole)) {
                adminDashboard = new AdminDashboardView(
                    auth, bookCtrl, memberCtrl, loanCtrl, fineCtrl, dashCtrl,
                    this::showSelection).build();
                mainPanel.getChildren().add(adminDashboard);
                showScreen(adminDashboard);
            } else {
                studentDashboard = new StudentDashboardView(
                    auth, bookCtrl, loanCtrl, fineCtrl,
                    this::showSelection).build();
                mainPanel.getChildren().add(studentDashboard);
                showScreen(studentDashboard);
            }
        } catch (Exception e) {
            ViewUtils.alert("Error building dashboard: " + e.getMessage());
        }
    }
}

// =============================================================
//  SELECTION SCREEN  –  Role picker
// =============================================================
class SelectionView {
    private final AuthController auth;
    private final Runnable       onRoleSelected;   // → show login

    SelectionView(AuthController auth, Runnable onRoleSelected) {
        this.auth           = auth;
        this.onRoleSelected = onRoleSelected;
    }

    VBox build() {
        VBox outer = new VBox();
        outer.setAlignment(Pos.CENTER);
        outer.setStyle("-fx-background-color: #e6ebf5;");

        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(520, 320);
        card.setStyle(ViewUtils.CARD_STYLE);
        card.setPadding(new Insets(30, 40, 30, 40));

        Label icon  = new Label("📖");
        icon.setFont(Font.font("Segoe UI", 44));
        Label title = new Label("Library BTAB");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        Label sub   = new Label("Select your role to continue");
        sub.setStyle("-fx-text-fill: #888;");

        HBox roles = new HBox(20);
        roles.setAlignment(Pos.CENTER);
        roles.setPadding(new Insets(15, 0, 0, 0));
        roles.getChildren().addAll(
            roleBox("\uD83D\uDD11", "Librarian", "Admin/Librarian"),
            roleBox("🎓",          "Member",    "Student/Member")
        );

        card.getChildren().addAll(icon, title, sub, roles);
        outer.getChildren().add(card);
        return outer;
    }

    private VBox roleBox(String icon, String label, String roleType) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(190, 130);
        box.setPadding(new Insets(15));
        String base  = "-fx-background-color: white; -fx-border-color: #e6ebf5; "
                     + "-fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;";
        String hover = "-fx-background-color: #f0f4ff; -fx-border-color: #6450f0; "
                     + "-fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; "
                     + "-fx-cursor: hand;";
        box.setStyle(base);

        Label ic = new Label(icon);  ic.setFont(Font.font("Segoe UI", 34));
        Label nm = new Label(label); nm.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        box.getChildren().addAll(ic, nm);

        box.setOnMouseEntered(e -> box.setStyle(hover));
        box.setOnMouseExited (e -> box.setStyle(base));
        box.setOnMouseClicked(e -> {
            auth.selectedRole = roleType;
            onRoleSelected.run();
        });
        return box;
    }
}

// =============================================================
//  LOGIN SCREEN
// =============================================================
class LoginView {
    private final AuthController auth;
    private final Runnable       onLoginSuccess;
    private final Runnable       onBack;

    private TextField     userField;
    private PasswordField passField;
    private Label         createAccLabel;

    LoginView(AuthController auth, Runnable onLoginSuccess, Runnable onBack) {
        this.auth           = auth;
        this.onLoginSuccess = onLoginSuccess;
        this.onBack         = onBack;
    }

    VBox build() {
        VBox outer = new VBox();
        outer.setAlignment(Pos.CENTER);
        outer.setStyle("-fx-background-color: #e6ebf5;");

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxSize(400, 480);
        card.setStyle(ViewUtils.CARD_STYLE);
        card.setPadding(new Insets(35, 40, 35, 40));

        Label back = new Label("← Back to role selection");
        back.setStyle("-fx-cursor: hand; -fx-text-fill: #6450f0;");
        back.setOnMouseClicked(e -> { clear(); onBack.run(); });

        Label title = new Label("Welcome Back");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        Label sub = new Label("Sign in to Library BTAB");
        sub.setStyle("-fx-text-fill: #888;");

        userField = new TextField();
        userField.setPromptText("Enter your username");
        userField.setPrefHeight(38);
        ViewUtils.styleField(userField);

        passField = new PasswordField();
        passField.setPromptText("Enter your password");
        passField.setPrefHeight(38);
        ViewUtils.styleField(passField);
        passField.setOnAction(e -> handleLogin());

        Button loginBtn = new Button("Login →");
        loginBtn.setPrefHeight(42);
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle(ViewUtils.BTN_PRIMARY);
        loginBtn.setOnAction(e -> handleLogin());

        createAccLabel = new Label("Don't have an account? Create one");
        createAccLabel.setStyle("-fx-text-fill: #6450f0; -fx-cursor: hand;");

        card.getChildren().addAll(
            back, title, sub,
            ViewUtils.bold("Username"), userField,
            ViewUtils.bold("Password"), passField,
            loginBtn, createAccLabel);
        outer.getChildren().add(card);

        // The SelectionView controls which role is set; the LoginView
        // updates createAccLabel visibility each time it is shown.
        outer.visibleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) createAccLabel.setVisible("Student/Member".equals(auth.selectedRole));
        });

        return outer;
    }

    /** Wire the "Create account" label externally (called from RegisterView setup). */
    void setOnCreateAccount(Runnable action) {
        createAccLabel.setOnMouseClicked(e -> { clear(); action.run(); });
    }

    private void handleLogin() {
        String u = userField.getText().trim();
        String p = passField.getText();
        if (u.isEmpty() || p.isEmpty()) {
            ViewUtils.alert("Please enter username and password.");
            return;
        }
        try {
            AuthModel.LoginResult result = auth.login(u, p);
            if (result == null) {
                ViewUtils.alert("Invalid username or password, or account is suspended.");
                return;
            }
            if (!result.role.equals(auth.selectedRole)) {
                ViewUtils.alert("Access Denied: Your account is \"" + result.role + "\".\n"
                              + "Please go back and choose the correct role.");
                auth.logout();   // undo partial session state
                return;
            }
            clear();
            onLoginSuccess.run();
        } catch (Exception ex) {
            ViewUtils.alert("Database error: " + ex.getMessage());
        }
    }

    private void clear() {
        if (userField != null) userField.clear();
        if (passField != null) passField.clear();
    }
}

// =============================================================
//  REGISTER SCREEN
// =============================================================
class RegisterView {
    private final AuthController auth;
    private final Runnable       onBack;   // → show login

    RegisterView(AuthController auth, Runnable onBack) {
        this.auth   = auth;
        this.onBack = onBack;
    }

    VBox build() {
        VBox outer = new VBox();
        outer.setAlignment(Pos.CENTER);
        outer.setStyle("-fx-background-color: #e6ebf5;");

        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxSize(420, 560);
        card.setStyle(ViewUtils.CARD_STYLE);
        card.setPadding(new Insets(35, 40, 35, 40));

        Label back = new Label("← Back to Login");
        back.setStyle("-fx-cursor: hand; -fx-text-fill: #6450f0;");
        back.setOnMouseClicked(e -> onBack.run());

        Label title = new Label("Create Member Account");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        TextField     nameF  = field("Full Name");
        TextField     emailF = field("Email");
        TextField     userF  = field("Username");
        PasswordField passF  = new PasswordField();
        passF.setPromptText("Password");
        ViewUtils.styleField(passF);

        Button signUpBtn = new Button("Create Account");
        signUpBtn.setPrefHeight(42);
        signUpBtn.setMaxWidth(Double.MAX_VALUE);
        signUpBtn.setStyle(ViewUtils.BTN_PRIMARY);
        signUpBtn.setOnAction(e -> {
            if (nameF.getText().isBlank() || emailF.getText().isBlank()
                    || userF.getText().isBlank() || passF.getText().isBlank()) {
                ViewUtils.alert("All fields are required.");
                return;
            }
            try {
                auth.register(userF.getText().trim(), passF.getText(),
                              nameF.getText().trim(), emailF.getText().trim());
                ViewUtils.alert("Account created! You can now log in.");
                nameF.clear(); emailF.clear(); userF.clear(); passF.clear();
                onBack.run();
            } catch (SQLIntegrityConstraintViolationException ex) {
                ViewUtils.alert("Username or email already exists!");
            } catch (Exception ex) {
                ViewUtils.alert("Database error: " + ex.getMessage());
            }
        });

        card.getChildren().addAll(
            back, title,
            ViewUtils.bold("Full Name"), nameF,
            ViewUtils.bold("Email"),     emailF,
            ViewUtils.bold("Username"),  userF,
            ViewUtils.bold("Password"),  passF,
            signUpBtn);
        outer.getChildren().add(card);
        return outer;
    }

    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        ViewUtils.styleField(tf);
        return tf;
    }
}

// =============================================================
//  ADMIN DASHBOARD VIEW
// =============================================================
class AdminDashboardView {
    private final AuthController      auth;
    private final BookController      bookCtrl;
    private final MemberController    memberCtrl;
    private final LoanController      loanCtrl;
    private final FineController      fineCtrl;
    private final DashboardController dashCtrl;
    private final Runnable            onLogout;

    AdminDashboardView(AuthController auth, BookController bookCtrl,
                       MemberController memberCtrl, LoanController loanCtrl,
                       FineController fineCtrl, DashboardController dashCtrl,
                       Runnable onLogout) {
        this.auth       = auth;
        this.bookCtrl   = bookCtrl;
        this.memberCtrl = memberCtrl;
        this.loanCtrl   = loanCtrl;
        this.fineCtrl   = fineCtrl;
        this.dashCtrl   = dashCtrl;
        this.onLogout   = onLogout;
    }

    BorderPane build() {
        BorderPane bp = new BorderPane();
        bp.setStyle("-fx-background-color: #f4f7fa;");

        // --- Nav bar ---
        HBox nav = new HBox(8);
        nav.setStyle("-fx-background-color: #1e1e2e;");
        nav.setPadding(new Insets(12, 20, 12, 20));
        nav.setAlignment(Pos.CENTER_LEFT);

        Label appTitle = new Label("📖 Library BTAB");
        appTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        appTitle.setStyle("-fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button homeBtn        = ViewUtils.navBtn("🏠 Home");
        Button addMemberBtn   = ViewUtils.navBtn("➕ Add Member");
        Button viewMemberBtn  = ViewUtils.navBtn("👥 Members");
        Button addBooksBtn    = ViewUtils.navBtn("📚 Add Book");
        Button viewBooksBtn   = ViewUtils.navBtn("🔍 Books");
        Button activeLoansBtn = ViewUtils.navBtn("📋 Active Loans");
        Button allLoansBtn    = ViewUtils.navBtn("📜 History");
        Button finesBtn       = ViewUtils.navBtn("💰 Fines");
        Button profileBtn     = ViewUtils.navBtn("👤 Profile");
        Button logoutBtn      = ViewUtils.navBtn("🚪 Logout");
        logoutBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; "
                         + "-fx-cursor: hand; -fx-font-size: 12; -fx-background-radius: 4;");

        nav.getChildren().addAll(appTitle, spacer,
            homeBtn, addMemberBtn, viewMemberBtn, addBooksBtn,
            viewBooksBtn, activeLoansBtn, allLoansBtn, finesBtn, profileBtn, logoutBtn);
        bp.setTop(nav);

        // --- Tab container ---
        StackPane content = new StackPane();
        bp.setCenter(content);

        VBox homeTab        = buildHomeTab();
        VBox addMemberTab   = buildAddMemberTab();
        VBox viewMemberTab  = buildViewMemberTab();
        VBox addBookTab     = buildAddBookTab();
        VBox viewBookTab    = buildViewBookTab();
        VBox activeLoansTab = buildActiveLoansTab();
        VBox allLoansTab    = buildAllLoansTab();
        VBox finesTab       = buildFinesTab();

        content.getChildren().addAll(
            homeTab, addMemberTab, viewMemberTab, addBookTab,
            viewBookTab, activeLoansTab, allLoansTab, finesTab);

        // --- Nav wiring ---
        homeBtn.setOnAction(e -> {
            refreshHome(homeTab);
            ViewUtils.showTab(content, homeTab);
        });
        addMemberBtn.setOnAction(e -> ViewUtils.showTab(content, addMemberTab));
        viewMemberBtn.setOnAction(e -> {
            runSafe(memberCtrl::refresh);
            ViewUtils.showTab(content, viewMemberTab);
        });
        addBooksBtn.setOnAction(e -> ViewUtils.showTab(content, addBookTab));
        viewBooksBtn.setOnAction(e -> {
            runSafe(bookCtrl::refresh);
            ViewUtils.showTab(content, viewBookTab);
        });
        activeLoansBtn.setOnAction(e -> {
            runSafe(loanCtrl::refreshActive);
            ViewUtils.showTab(content, activeLoansTab);
        });
        allLoansBtn.setOnAction(e -> {
            runSafe(loanCtrl::refreshAll);
            ViewUtils.showTab(content, allLoansTab);
        });
        finesBtn.setOnAction(e -> {
            runSafe(fineCtrl::refreshAll);
            ViewUtils.showTab(content, finesTab);
        });
        profileBtn.setOnAction(e -> showProfile());
        logoutBtn.setOnAction(e -> { auth.logout(); onLogout.run(); });

        // Initial load
        refreshHome(homeTab);
        ViewUtils.showTab(content, homeTab);
        return bp;
    }

    // -------------------------------------------------------
    //  HOME TAB
    // -------------------------------------------------------
    private VBox buildHomeTab() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: #f4f7fa;");

        Label title = new Label("Dashboard Overview");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        HBox metrics = new HBox(15);
        metrics.getChildren().addAll(
            ViewUtils.statCard("📚", "Total Books",   "—", "#6450f0", "#ede9ff"),
            ViewUtils.statCard("👥", "Total Members", "—", "#0891b2", "#e0f7fa"),
            ViewUtils.statCard("📋", "Active Loans",  "—", "#d97706", "#fef3c7"),
            ViewUtils.statCard("⚠️", "Overdue Loans", "—", "#dc2626", "#fee2e2"),
            ViewUtils.statCard("💰", "Unpaid Fines",  "—", "#16a34a", "#dcfce7")
        );

        Label recentTitle = new Label("Top Overdue Loans");
        recentTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        TableView<LoanRow> overdueTable = new TableView<>(loanCtrl.loans);
        overdueTable.setMaxHeight(260);
        overdueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        overdueTable.getColumns().addAll(
            ViewUtils.col("Borrower",     r -> new SimpleStringProperty(r.borrowerName)),
            ViewUtils.col("Book",         r -> new SimpleStringProperty(r.bookTitle)),
            ViewUtils.col("Due Date",     r -> new SimpleStringProperty(r.dueDate)),
            ViewUtils.col("Days Overdue", r -> new SimpleStringProperty(r.daysOverdue + " days ⚠️"))
        );
        ViewUtils.colorOverdueRows(overdueTable);

        box.getChildren().addAll(title, metrics, recentTitle, overdueTable);
        return box;
    }

    private void refreshHome(VBox homeTab) {
        // Update stat cards
        try {
            DashboardController.Stats s = dashCtrl.fetchStats();
            HBox metrics = (HBox) homeTab.getChildren().get(1);
            int[] vals = { s.totalBooks, s.totalMembers, s.activeLoans, s.overdueLoans, s.unpaidFines };
            for (int i = 0; i < vals.length; i++) {
                VBox card = (VBox) metrics.getChildren().get(i);
                ((Label) card.getChildren().get(2)).setText(String.valueOf(vals[i]));
            }
        } catch (Exception ex) { /* silently skip on error */ }

        // Update overdue table
        try {
            loanCtrl.loans.setAll(dashCtrl.fetchOverdueTop10());
        } catch (Exception ex) { /* silently skip */ }
    }

    // -------------------------------------------------------
    //  ADD MEMBER TAB
    // -------------------------------------------------------
    private VBox buildAddMemberTab() {
        VBox outer = new VBox();
        outer.setAlignment(Pos.CENTER);
        outer.setStyle("-fx-background-color: #f4f7fa;");

        VBox form = new VBox(10);
        form.setMaxWidth(430);
        form.setPadding(new Insets(30));
        form.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; "
                    + "-fx-border-radius: 8; -fx-background-radius: 8;");

        Label lbl = new Label("Register New Student");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TextField     mName  = field("Full Name");
        TextField     mUser  = field("Username");
        TextField     mEmail = field("Email");
        TextField     mPhone = field("Contact Number");
        PasswordField mPass  = new PasswordField();
        mPass.setPromptText("Password");
        ViewUtils.styleField(mPass);

        Button submit = new Button("Register Student");
        submit.setPrefHeight(40);
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setStyle(ViewUtils.BTN_PRIMARY);
        submit.setOnAction(e -> {
            if (mName.getText().isBlank() || mUser.getText().isBlank()
                    || mEmail.getText().isBlank() || mPass.getText().isBlank()) {
                ViewUtils.alert("Name, username, email, and password are required.");
                return;
            }
            try {
                memberCtrl.addStudent(mUser.getText().trim(), mPass.getText(),
                    mName.getText().trim(), mEmail.getText().trim(), mPhone.getText().trim());
                ViewUtils.alert("✅ Student account created!");
                mName.clear(); mUser.clear(); mEmail.clear(); mPhone.clear(); mPass.clear();
            } catch (SQLIntegrityConstraintViolationException ex) {
                ViewUtils.alert("Username or email already exists!");
            } catch (Exception ex) {
                ViewUtils.alert("Database error: " + ex.getMessage());
            }
        });

        form.getChildren().addAll(lbl,
            ViewUtils.bold("Full Name"), mName,
            ViewUtils.bold("Username"),  mUser,
            ViewUtils.bold("Email"),     mEmail,
            ViewUtils.bold("Contact"),   mPhone,
            ViewUtils.bold("Password"),  mPass, submit);
        outer.getChildren().add(form);
        return outer;
    }

    // -------------------------------------------------------
    //  VIEW MEMBERS TAB
    // -------------------------------------------------------
    private VBox buildViewMemberTab() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #f4f7fa;");

        Label title = new Label("Student Members");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TextField search = new TextField();
        search.setPromptText("🔍  Search by name, username, or email…");
        search.setPrefHeight(38);
        ViewUtils.styleField(search);

        FilteredList<UserRow> filtered = new FilteredList<>(memberCtrl.members, p -> true);
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
            ViewUtils.col("ID",        r -> new SimpleStringProperty(String.valueOf(r.id))),
            ViewUtils.col("Full Name", r -> new SimpleStringProperty(r.fullName)),
            ViewUtils.col("Username",  r -> new SimpleStringProperty(r.username)),
            ViewUtils.col("Email",     r -> new SimpleStringProperty(r.email)),
            ViewUtils.col("Contact",   r -> new SimpleStringProperty(r.contact)),
            ViewUtils.col("Status",    r -> new SimpleStringProperty(r.status)),
            ViewUtils.col("Join Date", r -> new SimpleStringProperty(r.joinDate))
        );
        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(UserRow item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(!empty && item != null && "Suspended".equals(item.status)
                    ? "-fx-background-color: #fee2e2;" : "");
            }
        });

        Button toggleBtn = new Button("🔄 Toggle Suspend/Active");
        Button deleteBtn = new Button("🗑 Delete Member");
        deleteBtn.setStyle("-fx-background-color: #fee2e2;");

        toggleBtn.setOnAction(e -> {
            UserRow sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { ViewUtils.alert("Select a member first."); return; }
            String nextStatus = "Active".equals(sel.status) ? "Suspended" : "Active";
            if (!ViewUtils.confirm("Change \"" + sel.fullName + "\" status to " + nextStatus + "?")) return;
            try {
                String updated = memberCtrl.toggleStatus(sel);
                ViewUtils.alert("Status updated to " + updated + ".");
            } catch (Exception ex) { ViewUtils.alert("Error: " + ex.getMessage()); }
        });

        deleteBtn.setOnAction(e -> {
            UserRow sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { ViewUtils.alert("Select a member first."); return; }
            if (!ViewUtils.confirm("Delete member \"" + sel.fullName + "\"? This cannot be undone.")) return;
            try {
                memberCtrl.deleteMember(sel.id);
                ViewUtils.alert("Member deleted.");
            } catch (Exception ex) { ViewUtils.alert("Error: " + ex.getMessage()); }
        });

        box.getChildren().addAll(title, search, table, new HBox(10, toggleBtn, deleteBtn));
        return box;
    }

    // -------------------------------------------------------
    //  ADD BOOK TAB
    // -------------------------------------------------------
    private VBox buildAddBookTab() {
        VBox outer = new VBox();
        outer.setAlignment(Pos.CENTER);
        outer.setStyle("-fx-background-color: #f4f7fa;");

        VBox form = new VBox(10);
        form.setMaxWidth(450);
        form.setPadding(new Insets(30));
        form.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; "
                    + "-fx-border-radius: 8; -fx-background-radius: 8;");

        Label lbl = new Label("Add New Book");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TextField titleF  = field("Book Title");
        TextField authorF = field("Author");
        TextField isbnF   = field("ISBN (13 digits)");
        TextField catF    = field("Category");
        TextField pubF    = field("Publisher");
        TextField yearF   = field("Year Published");
        TextField copiesF = field("Number of Copies");

        Button submit = new Button("Add Book");
        submit.setPrefHeight(40);
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setStyle(ViewUtils.BTN_PRIMARY);
        submit.setOnAction(e -> {
            if (titleF.getText().isBlank() || authorF.getText().isBlank() || isbnF.getText().isBlank()) {
                ViewUtils.alert("Title, author, and ISBN are required.");
                return;
            }
            try {
                int copies = Integer.parseInt(copiesF.getText().trim());
                bookCtrl.addBook(titleF.getText().trim(), authorF.getText().trim(),
                    isbnF.getText().trim(), catF.getText().trim(),
                    pubF.getText().trim(),  yearF.getText().trim(), copies);
                ViewUtils.alert("✅ Book added with " + copies + " copy/copies!");
                titleF.clear(); authorF.clear(); isbnF.clear();
                catF.clear(); pubF.clear(); yearF.clear(); copiesF.clear();
            } catch (NumberFormatException ex) {
                ViewUtils.alert("Please enter a valid number of copies.");
            } catch (Exception ex) {
                ViewUtils.alert("Error: " + ex.getMessage());
            }
        });

        form.getChildren().addAll(lbl,
            ViewUtils.bold("Title"),             titleF,
            ViewUtils.bold("Author"),            authorF,
            ViewUtils.bold("ISBN"),              isbnF,
            ViewUtils.bold("Category"),          catF,
            ViewUtils.bold("Publisher"),         pubF,
            ViewUtils.bold("Year"),              yearF,
            ViewUtils.bold("Number of Copies"),  copiesF, submit);
        outer.getChildren().add(form);
        return outer;
    }

    // -------------------------------------------------------
    //  VIEW BOOKS TAB
    // -------------------------------------------------------
    private VBox buildViewBookTab() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #f4f7fa;");

        Label title = new Label("Book Catalogue");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TextField search = new TextField();
        search.setPromptText("🔍  Search by title, author, ISBN, or category…");
        search.setPrefHeight(38);
        ViewUtils.styleField(search);

        FilteredList<BookRow> filtered = new FilteredList<>(bookCtrl.books, p -> true);
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
            ViewUtils.col("ID",        r -> new SimpleStringProperty(String.valueOf(r.id))),
            ViewUtils.col("Title",     r -> new SimpleStringProperty(r.title)),
            ViewUtils.col("Author",    r -> new SimpleStringProperty(r.author)),
            ViewUtils.col("ISBN",      r -> new SimpleStringProperty(r.isbn)),
            ViewUtils.col("Category",  r -> new SimpleStringProperty(r.category)),
            ViewUtils.col("Available", r -> new SimpleStringProperty(r.availableCopies + " / " + r.totalCopies))
        );
        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(BookRow item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(!empty && item != null && item.availableCopies == 0
                    ? "-fx-background-color: #fff3cd;" : "");
            }
        });

        Button deleteBtn = new Button("🗑 Delete Selected Book");
        deleteBtn.setStyle("-fx-background-color: #fee2e2; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            BookRow sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { ViewUtils.alert("Select a book first."); return; }
            if (!ViewUtils.confirm("Delete \"" + sel.title + "\"? All copies will also be deleted.")) return;
            try {
                bookCtrl.deleteBook(sel.id);
                ViewUtils.alert("Book deleted.");
            } catch (Exception ex) { ViewUtils.alert("Error: " + ex.getMessage()); }
        });

        box.getChildren().addAll(title, search, table, deleteBtn);
        return box;
    }

    // -------------------------------------------------------
    //  ACTIVE LOANS TAB
    // -------------------------------------------------------
    private VBox buildActiveLoansTab() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #f4f7fa;");

        Label lbl = new Label("Active & Overdue Loans");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TableView<LoanRow> table = new TableView<>(loanCtrl.loans);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getColumns().addAll(
            ViewUtils.col("Loan ID",      r -> new SimpleStringProperty(String.valueOf(r.loanId))),
            ViewUtils.col("Borrower",     r -> new SimpleStringProperty(r.borrowerName)),
            ViewUtils.col("Book",         r -> new SimpleStringProperty(r.bookTitle)),
            ViewUtils.col("Copy ID",      r -> new SimpleStringProperty(r.copyId)),
            ViewUtils.col("Due Date",     r -> new SimpleStringProperty(r.dueDate)),
            ViewUtils.col("Days Overdue", r -> new SimpleStringProperty(
                r.daysOverdue > 0 ? r.daysOverdue + " days ⚠️" : "On time")),
            ViewUtils.col("Status",       r -> new SimpleStringProperty(r.status))
        );
        ViewUtils.colorOverdueRows(table);

        Button issueBtn  = new Button("📤 Issue Loan");
        issueBtn.setStyle("-fx-background-color: #6450f0; -fx-text-fill: white; -fx-cursor: hand;");
        Button returnBtn = new Button("✅ Mark as Returned");
        returnBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-cursor: hand;");

        issueBtn.setOnAction(e -> showIssueLoanDialog());
        returnBtn.setOnAction(e -> {
            LoanRow sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { ViewUtils.alert("Select a loan first."); return; }
            String msg = sel.daysOverdue > 0
                ? "Return this book?\nIt is " + sel.daysOverdue + " days overdue.\n"
                + "A fine of PHP " + (sel.daysOverdue * 10.0) + " will be recorded."
                : "Mark this loan as returned? No fine will be charged.";
            if (!ViewUtils.confirm(msg)) return;
            try {
                double fine = loanCtrl.returnLoan(sel, auth.currentBorrowerId);
                if (fine > 0) ViewUtils.alert("Book returned. Fine of PHP " + fine + " recorded.");
                else          ViewUtils.alert("Book returned on time. No fine.");
                runSafe(loanCtrl::refreshActive);
            } catch (Exception ex) { ViewUtils.alert("Error: " + ex.getMessage()); }
        });

        box.getChildren().addAll(lbl, table, new HBox(10, issueBtn, returnBtn));
        return box;
    }

    private void showIssueLoanDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Issue New Loan");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(25));

        TextField copyField = new TextField(); copyField.setPromptText("e.g. copy_3_2"); ViewUtils.styleField(copyField);
        TextField userField = new TextField(); userField.setPromptText("Student username"); ViewUtils.styleField(userField);
        DatePicker duePicker = new DatePicker(LocalDate.now().plusDays(14));

        grid.add(new Label("Copy ID:"),  0, 0); grid.add(copyField, 1, 0);
        grid.add(new Label("Username:"), 0, 1); grid.add(userField, 1, 1);
        grid.add(new Label("Due Date:"), 0, 2); grid.add(duePicker, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            if (duePicker.getValue() == null || duePicker.getValue().isBefore(LocalDate.now())) {
                ViewUtils.alert("Due date must be today or in the future."); return;
            }
            try {
                loanCtrl.issueLoan(copyField.getText().trim(),
                                   userField.getText().trim(),
                                   duePicker.getValue().toString());
                ViewUtils.alert("✅ Loan issued successfully!");
            } catch (Exception ex) { ViewUtils.alert("Error: " + ex.getMessage()); }
        });
    }

    // -------------------------------------------------------
    //  ALL LOANS TAB
    // -------------------------------------------------------
    private VBox buildAllLoansTab() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #f4f7fa;");

        Label lbl = new Label("Full Loan History");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TableView<LoanRow> table = new TableView<>(loanCtrl.loans);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getColumns().addAll(
            ViewUtils.col("Loan ID",     r -> new SimpleStringProperty(String.valueOf(r.loanId))),
            ViewUtils.col("Borrower",    r -> new SimpleStringProperty(r.borrowerName)),
            ViewUtils.col("Book",        r -> new SimpleStringProperty(r.bookTitle)),
            ViewUtils.col("Issue Date",  r -> new SimpleStringProperty(r.issueDate)),
            ViewUtils.col("Due Date",    r -> new SimpleStringProperty(r.dueDate)),
            ViewUtils.col("Return Date", r -> new SimpleStringProperty(r.returnDate)),
            ViewUtils.col("Status",      r -> new SimpleStringProperty(r.status))
        );
        ViewUtils.colorOverdueRows(table);

        box.getChildren().addAll(lbl, table);
        return box;
    }

    // -------------------------------------------------------
    //  FINES TAB (admin — with Mark Paid button)
    // -------------------------------------------------------
    private VBox buildFinesTab() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #f4f7fa;");

        Label lbl = new Label("Fines & Penalties");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TableView<FineRow> table = ViewUtils.buildFineTable(fineCtrl.fines);
        VBox.setVgrow(table, Priority.ALWAYS);

        Button markPaidBtn = new Button("✅ Mark as Paid (Cash)");
        markPaidBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-cursor: hand;");
        markPaidBtn.setOnAction(e -> {
            FineRow sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { ViewUtils.alert("Select a fine first."); return; }
            if (!ViewUtils.confirm("Mark PHP " + String.format("%.2f", sel.amount) + " fine as paid (Cash)?")) return;
            try {
                fineCtrl.markPaid(sel, auth.currentBorrowerId);
                ViewUtils.alert("Fine marked as paid.");
            } catch (IllegalStateException ex) {
                ViewUtils.alert(ex.getMessage());
            } catch (Exception ex) {
                ViewUtils.alert("Error: " + ex.getMessage());
            }
        });

        box.getChildren().addAll(lbl, table, new HBox(10, markPaidBtn));
        return box;
    }

    private void showProfile() {
        try {
            String[] p = auth.fetchProfile();
            if (p != null) ViewUtils.alert(
                "👤 Profile Information\n\n"
              + "Name:    " + p[0] + "\nUser:    " + p[1]
              + "\nEmail:   " + p[2] + "\nContact: " + p[3]
              + "\nRole:    " + p[4] + "\nJoined:  " + p[5]);
        } catch (Exception ex) { ViewUtils.alert("Error: " + ex.getMessage()); }
    }

    // Helpers
    private TextField field(String prompt) {
        TextField tf = new TextField(); tf.setPromptText(prompt); ViewUtils.styleField(tf); return tf;
    }
    private void runSafe(ThrowingRunnable r) {
        try { r.run(); } catch (Exception ex) { ViewUtils.alert("Error: " + ex.getMessage()); }
    }
}

// =============================================================
//  STUDENT DASHBOARD VIEW
// =============================================================
class StudentDashboardView {
    private final AuthController  auth;
    private final BookController  bookCtrl;
    private final LoanController  loanCtrl;
    private final FineController  fineCtrl;
    private final Runnable        onLogout;

    StudentDashboardView(AuthController auth, BookController bookCtrl,
                         LoanController loanCtrl, FineController fineCtrl,
                         Runnable onLogout) {
        this.auth     = auth;
        this.bookCtrl = bookCtrl;
        this.loanCtrl = loanCtrl;
        this.fineCtrl = fineCtrl;
        this.onLogout = onLogout;
    }

    BorderPane build() throws Exception {
        BorderPane bp = new BorderPane();
        bp.setStyle("-fx-background-color: #f4f7fa;");

        HBox nav = new HBox(10);
        nav.setStyle("-fx-background-color: #1e1e2e;");
        nav.setPadding(new Insets(12, 20, 12, 20));
        nav.setAlignment(Pos.CENTER_LEFT);

        Label appTitle = new Label("📖 Library BTAB");
        appTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        appTitle.setStyle("-fx-text-fill: white;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label userLabel = new Label("👤 " + auth.currentUsername);
        userLabel.setStyle("-fx-text-fill: #aaa; -fx-padding: 0 10 0 0;");

        Button viewBooksBtn = ViewUtils.navBtn("📚 Browse Books");
        Button myLoansBtn   = ViewUtils.navBtn("📋 My Loans");
        Button myFinesBtn   = ViewUtils.navBtn("💰 My Fines");
        Button logoutBtn    = ViewUtils.navBtn("🚪 Logout");
        logoutBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; "
                         + "-fx-cursor: hand; -fx-font-size: 12; -fx-background-radius: 4;");

        nav.getChildren().addAll(appTitle, spacer, userLabel, viewBooksBtn, myLoansBtn, myFinesBtn, logoutBtn);
        bp.setTop(nav);

        StackPane content = new StackPane();
        bp.setCenter(content);

        VBox browseTab = buildBrowseTab();
        VBox loansTab  = buildLoansTab();
        VBox finesTab  = buildStudentFinesTab();
        content.getChildren().addAll(browseTab, loansTab, finesTab);

        viewBooksBtn.setOnAction(e -> { runSafe(bookCtrl::refresh); ViewUtils.showTab(content, browseTab); });
        myLoansBtn.setOnAction(e -> {
            runSafe(() -> loanCtrl.refreshByBorrower(auth.currentBorrowerId));
            ViewUtils.showTab(content, loansTab);
        });
        myFinesBtn.setOnAction(e -> {
            runSafe(() -> fineCtrl.refreshByBorrower(auth.currentBorrowerId));
            ViewUtils.showTab(content, finesTab);
        });
        logoutBtn.setOnAction(e -> { auth.logout(); onLogout.run(); });

        bookCtrl.refresh();
        ViewUtils.showTab(content, browseTab);
        return bp;
    }

    // -------------------------------------------------------
    //  BROWSE TAB  – card grid
    // -------------------------------------------------------
    private VBox buildBrowseTab() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #f4f7fa;");

        Label title = new Label("Book Catalogue");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

        TextField search = new TextField();
        search.setPromptText("🔍  Search books…");
        ViewUtils.styleField(search);

        FilteredList<BookRow> filtered = new FilteredList<>(bookCtrl.books, p -> true);
        search.textProperty().addListener((obs, old, val) -> {
            String lo = val.toLowerCase();
            filtered.setPredicate(r -> val.isEmpty()
                || r.title.toLowerCase().contains(lo)
                || r.author.toLowerCase().contains(lo)
                || r.category.toLowerCase().contains(lo));
        });

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        TilePane grid = new TilePane();
        grid.setPrefColumns(3); grid.setHgap(15); grid.setVgap(15);
        grid.setPadding(new Insets(10));
        scroll.setContent(grid);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Runnable populateGrid = () -> {
            grid.getChildren().clear();
            for (BookRow b : filtered) grid.getChildren().add(buildBookCard(b));
        };

        filtered.addListener((javafx.collections.ListChangeListener<BookRow>) c -> populateGrid.run());
        bookCtrl.books.addListener((javafx.collections.ListChangeListener<BookRow>) c -> populateGrid.run());

        box.getChildren().addAll(title, search, scroll);
        return box;
    }

    private VBox buildBookCard(BookRow b) {
        VBox card = new VBox(6); card.setPadding(new Insets(14));
        boolean avail = b.availableCopies > 0;

        String baseStyle  = "-fx-border-color: " + (avail ? "#d1fae5" : "#e2e8f0") + "; "
            + "-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8;"
            + (avail ? " -fx-cursor: hand;" : "");
        String hoverStyle = "-fx-border-color: " + (avail ? "#10b981" : "#cbd5e1") + "; "
            + "-fx-background-color: " + (avail ? "#f0fdf4" : "#f8fafc") + "; "
            + "-fx-border-radius: 8; -fx-background-radius: 8;" + (avail ? " -fx-cursor: hand;" : "");

        card.setStyle(baseStyle);
        Label t  = new Label("📖 " + b.title);
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13)); t.setWrapText(true);
        Label a  = new Label("✍  " + b.author); a.setStyle("-fx-text-fill: #666;");
        Label ca = new Label("🏷  " + b.category); ca.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");

        String avText  = avail ? "✅ Available: " + b.availableCopies : "❌ Not Available";
        Label av = new Label(avText);
        av.setStyle("-fx-background-color: " + (avail ? "#dcfce7" : "#fee2e2") + "; "
            + "-fx-text-fill: " + (avail ? "#166534" : "#991b1b") + "; "
            + "-fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11; -fx-font-weight: bold;");

        Label hint = new Label("Click to Borrow →");
        hint.setStyle("-fx-text-fill: #10b981; -fx-font-size: 10; -fx-font-style: italic;");
        hint.setVisible(avail);

        card.getChildren().addAll(t, a, ca, av, hint);
        if (avail) {
            card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
            card.setOnMouseExited(e  -> card.setStyle(baseStyle));
            card.setOnMouseClicked(e -> showBorrowDialog(b));
        }
        return card;
    }

    private void showBorrowDialog(BookRow book) {
        String copyId;
        try {
            copyId = bookCtrl.prepareForBorrow(book.id, auth.currentBorrowerId);
        } catch (IllegalStateException ex) {
            ViewUtils.alert("⚠️ " + ex.getMessage()); return;
        } catch (Exception ex) {
            ViewUtils.alert("Database error: " + ex.getMessage()); return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Borrow Book");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        javafx.scene.Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; "
                     + "-fx-font-weight: bold; -fx-background-radius: 6;");
        ((Button) okBtn).setText("✅ Confirm Borrow");

        VBox content = new VBox(14);
        content.setPadding(new Insets(20, 25, 10, 25));

        VBox bookInfo = new VBox(4);
        bookInfo.setStyle("-fx-background-color: #f0fdf4; -fx-border-color: #d1fae5; "
                        + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 14;");
        Label bookTitle = new Label("📖 " + book.title);
        bookTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16)); bookTitle.setWrapText(true);
        Label bookAuthor = new Label("✍  " + book.author); bookAuthor.setStyle("-fx-text-fill: #555;");
        Label bookCat    = new Label("🏷  " + book.category + "   |   Copy: " + copyId);
        bookCat.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        bookInfo.getChildren().addAll(bookTitle, bookAuthor, bookCat);

        DatePicker duePicker = new DatePicker(LocalDate.now().plusDays(14));
        duePicker.setMaxWidth(Double.MAX_VALUE);

        HBox quickDates = new HBox(8); quickDates.setAlignment(Pos.CENTER_LEFT);
        Label ql = new Label("Quick pick:"); ql.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        quickDates.getChildren().addAll(ql,
            quickBtn("1 week",  duePicker, 7),  quickBtn("2 weeks", duePicker, 14),
            quickBtn("3 weeks", duePicker, 21), quickBtn("1 month", duePicker, 30));

        Label policy = new Label("ℹ️  A fine of PHP 10.00 per day will be charged for late returns.");
        policy.setStyle("-fx-text-fill: #b45309; -fx-font-size: 11; -fx-background-color: #fef3c7; "
                      + "-fx-padding: 8 10; -fx-background-radius: 6;");
        policy.setWrapText(true);

        content.getChildren().addAll(bookInfo, ViewUtils.bold("Select Return Due Date:"),
                                     duePicker, quickDates, policy);
        dialog.getDialogPane().setContent(content);

        final String finalCopyId = copyId;
        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            LocalDate dueDate = duePicker.getValue();
            if (dueDate == null || !dueDate.isAfter(LocalDate.now())) {
                ViewUtils.alert("⚠️ Due date must be a future date."); return;
            }
            try {
                loanCtrl.borrowBook(finalCopyId, auth.currentBorrowerId, dueDate.toString());
                bookCtrl.refresh();
                loanCtrl.refreshByBorrower(auth.currentBorrowerId);
                ViewUtils.alert("✅ Success! You have borrowed:\n\n📖 " + book.title
                    + "\n📅 Due date: " + dueDate
                    + "\n🔖 Copy ID: " + finalCopyId
                    + "\n\nPlease return it on time to avoid fines.");
            } catch (Exception ex) {
                ViewUtils.alert("Failed to borrow: " + ex.getMessage());
            }
        });
    }

    private Button quickBtn(String label, DatePicker picker, int days) {
        Button b = new Button(label);
        String style = "-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; "
                     + "-fx-font-size: 10; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 3 8;";
        b.setStyle(style);
        b.setOnAction(e -> picker.setValue(LocalDate.now().plusDays(days)));
        return b;
    }

    // -------------------------------------------------------
    //  MY LOANS TAB
    // -------------------------------------------------------
    private VBox buildLoansTab() {
        VBox box = new VBox(10); box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #f4f7fa;");
        Label lbl = new Label("My Loan History");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TableView<LoanRow> table = new TableView<>(loanCtrl.loans);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getColumns().addAll(
            ViewUtils.col("Book",         r -> new SimpleStringProperty(r.bookTitle)),
            ViewUtils.col("Copy ID",      r -> new SimpleStringProperty(r.copyId)),
            ViewUtils.col("Issue Date",   r -> new SimpleStringProperty(r.issueDate)),
            ViewUtils.col("Due Date",     r -> new SimpleStringProperty(r.dueDate)),
            ViewUtils.col("Return Date",  r -> new SimpleStringProperty(r.returnDate)),
            ViewUtils.col("Status",       r -> new SimpleStringProperty(r.status)),
            ViewUtils.col("Days Overdue", r -> new SimpleStringProperty(
                r.daysOverdue > 0 ? r.daysOverdue + " days ⚠️" : "—"))
        );
        ViewUtils.colorOverdueRows(table);
        box.getChildren().addAll(lbl, table);
        return box;
    }

    // -------------------------------------------------------
    //  MY FINES TAB (read-only for student)
    // -------------------------------------------------------
    private VBox buildStudentFinesTab() {
        VBox box = new VBox(10); box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #f4f7fa;");
        Label lbl = new Label("My Fines");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        TableView<FineRow> table = ViewUtils.buildFineTable(fineCtrl.fines);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.getChildren().addAll(lbl, table);
        return box;
    }

    private void runSafe(ThrowingRunnable r) {
        try { r.run(); } catch (Exception ex) { ViewUtils.alert("Error: " + ex.getMessage()); }
    }
}

// =============================================================
//  SHARED VIEW UTILITIES
// =============================================================
class ViewUtils {
    static final String CARD_STYLE =
        "-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8; "
      + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 3);";

    static final String BTN_PRIMARY =
        "-fx-background-color: #6450f0; -fx-text-fill: white; "
      + "-fx-cursor: hand; -fx-font-size: 14; -fx-background-radius: 6;";

    static void styleField(Control field) {
        field.setStyle("-fx-border-color: #d2d7e1; -fx-border-radius: 5; "
                     + "-fx-background-radius: 5; -fx-padding: 5;");
    }

    static Label bold(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        return l;
    }

    static Button navBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; "
                 + "-fx-cursor: hand; -fx-font-size: 12;");
        b.setOnMouseEntered(e -> b.setStyle(b.getStyle().replace("#ccc", "white")));
        b.setOnMouseExited (e -> b.setStyle(b.getStyle().replace("white", "#ccc")));
        return b;
    }

    static VBox statCard(String icon, String label, String value, String accent, String bg) {
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

    static <T> TableColumn<T, String> col(String title,
            java.util.function.Function<T, javafx.beans.value.ObservableValue<String>> factory) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(data -> factory.apply(data.getValue()));
        return c;
    }

    static void colorOverdueRows(TableView<LoanRow> table) {
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

    static TableView<FineRow> buildFineTable(javafx.collections.ObservableList<FineRow> list) {
        TableView<FineRow> table = new TableView<>(list);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
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
                    else                                        setStyle("-fx-background-color: #fef3c7;");
                } else setStyle("");
            }
        });
        return table;
    }

    static void showTab(StackPane container, Pane tab) {
        container.getChildren().forEach(n -> n.setVisible(false));
        tab.setVisible(true);
    }

    static void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}

// =============================================================
//  FUNCTIONAL INTERFACE  –  lets lambdas throw checked exceptions
// =============================================================
@FunctionalInterface
interface ThrowingRunnable {
    void run() throws Exception;
}
