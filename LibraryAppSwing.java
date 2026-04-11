
package libraryappswing;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class LibraryAppSwing extends JFrame {

    private JPanel mainPanel;
    private CardLayout cardLayout;
    
    // In-memory DB Simulations
    private final Map<String, User> userDatabase = new HashMap<>();
    private final List<Book> bookDatabase = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();
    
    // Global UI State
    private String selectedRole = "";
    private User currentUser = null;

    // Table Models for Real-Time UI Updates
    private DefaultTableModel memberTableModel;
    private DefaultTableModel bookTableModel;
    private DefaultTableModel historyTableModel;
    private DefaultTableModel currentBorrowedTableModel;
    
    // Student Specific Table Models
    private DefaultTableModel studentIssuedTableModel;
    private DefaultTableModel studentReturnedTableModel;

    // TextFields to clear on exit or screen transitions
    private JTextField loginUserField;
    private JPasswordField loginPassField;
    private JLabel createAccLabel;

    public LibraryAppSwing() {
        setTitle("Library BTAB");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);

        seedInitialData();

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Core App Routing Screens
        mainPanel.add(createSelectionScreen(), "SELECTION");
        mainPanel.add(createLoginScreen(), "LOGIN");
        mainPanel.add(createRegisterScreen(), "REGISTER");
        mainPanel.add(new JPanel(), "ADMIN_DASH");   // Placeholder for dynamic load
        mainPanel.add(new JPanel(), "STUDENT_DASH"); // Placeholder for dynamic load

        add(mainPanel);
    }

    private void seedInitialData() {
        // Initial core users
        userDatabase.put("admin", new User("admin", "admin123", "Admin/Librarian", "Admin User", "admin@email.com", "2024-01-01"));
        userDatabase.put("student", new User("student", "student123", "Student/Member", "John Doe", "john@email.com", "2024-02-15"));

        // Sample Books
        bookDatabase.add(new Book(1, "To Kill a Mockingbird", "Harper Lee", "978-0061120084", "Fiction", 5));
        bookDatabase.add(new Book(2, "The Great Gatsby", "F. Scott Fitzgerald", "978-0743273565", "Fiction", 3));
        bookDatabase.add(new Book(3, "1984", "George Orwell", "978-0451524935", "Dystopian", 4));
        bookDatabase.add(new Book(4, "Clean Code", "Robert C. Martin", "978-0132350884", "Technology", 2));

        // Sample Active Transactions
        transactions.add(new Transaction(1, 1, "student", "2026-03-15", "2026-03-29", "Borrowed"));
        transactions.add(new Transaction(2, 4, "student", "2026-03-20", "2026-04-03", "Borrowed"));
    }

    private void clearAuthenticationFields() {
        if (loginUserField != null) loginUserField.setText("");
        if (loginPassField != null) loginPassField.setText("");
    }

    // SCREEN 1: Role Selection
    private JPanel createSelectionScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(230, 235, 245));

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225), 1),
                new EmptyBorder(25, 35, 25, 35)
        ));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel iconLabel = new JLabel("📖");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 40));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("Library BTAB");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(iconLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        headerPanel.add(titleLabel);
        card.add(headerPanel, BorderLayout.NORTH);

        JPanel rolesPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        rolesPanel.setBackground(Color.WHITE);
        rolesPanel.setBorder(new EmptyBorder(30, 0, 10, 0));

        rolesPanel.add(createRoleBox("🛡️", "Admin / Librarian", "Admin/Librarian"));
        rolesPanel.add(createRoleBox("🎓", "Student / Member", "Student/Member"));

        card.add(rolesPanel, BorderLayout.CENTER);
        panel.add(card);
        return panel;
    }

    private JPanel createRoleBox(String icon, String roleName, String roleType) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 235, 245), 2),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI", Font.PLAIN, 32));
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLbl = new JLabel(roleName);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(Box.createVerticalGlue());
        box.add(iconLbl);
        box.add(Box.createRigidArea(new Dimension(0, 15)));
        box.add(nameLbl);
        box.add(Box.createVerticalGlue());

        box.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                box.setBackground(new Color(245, 248, 255));
                box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                box.setBackground(Color.WHITE);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedRole = roleType;
                clearAuthenticationFields();
                
                if (selectedRole.equals("Student/Member")) {
                    createAccLabel.setVisible(false);
                } else {
                    createAccLabel.setVisible(true);
                }
                
                cardLayout.show(mainPanel, "LOGIN");
            }
        });

        return box;
    }

    // SCREEN 2: Login Screen
    private JPanel createLoginScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(230, 235, 245));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(30, 35, 30, 35));
        card.setPreferredSize(new Dimension(400, 500));

        JLabel backLabel = new JLabel("← Back to role selection");
        backLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { 
                clearAuthenticationFields();
                cardLayout.show(mainPanel, "SELECTION"); 
            }
        });
        card.add(backLabel);
        card.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel title = new JLabel("Library BTAB Login");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 25)));

        card.add(new JLabel("Username"));
        loginUserField = new JTextField();
        loginUserField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        card.add(loginUserField);
        card.add(Box.createRigidArea(new Dimension(0, 15)));

        card.add(new JLabel("Password"));
        loginPassField = new JPasswordField();
        loginPassField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        card.add(loginPassField);
        card.add(Box.createRigidArea(new Dimension(0, 20)));

        JButton loginBtn = new JButton("→ Login");
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginBtn.setBackground(new Color(100, 80, 240));
        loginBtn.setForeground(Color.WHITE);

        loginBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String u = loginUserField.getText();
                String p = new String(loginPassField.getPassword());

                if (userDatabase.containsKey(u) && userDatabase.get(u).password.equals(p)) {
                    User user = userDatabase.get(u);
                    if (user.role.equals(selectedRole)) {
                        currentUser = user;
                        if (selectedRole.equals("Admin/Librarian")) {
                            mainPanel.add(createAdminDashboard(), "ADMIN_DASH");
                            cardLayout.show(mainPanel, "ADMIN_DASH");
                        } else {
                            mainPanel.add(createStudentDashboard(), "STUDENT_DASH");
                            cardLayout.show(mainPanel, "STUDENT_DASH");
                        }
                    } else {
                        JOptionPane.showMessageDialog(LibraryAppSwing.this, "Access Denied: Wrong role!");
                    }
                } else {
                    JOptionPane.showMessageDialog(LibraryAppSwing.this, "Invalid Username or Password!");
                }
            }
        });

        card.add(loginBtn);
        card.add(Box.createRigidArea(new Dimension(0, 15)));

        createAccLabel = new JLabel("Don't have an account? Create one");
        createAccLabel.setForeground(new Color(100, 80, 240));
        createAccLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createAccLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        createAccLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { 
                clearAuthenticationFields();
                cardLayout.show(mainPanel, "REGISTER"); 
            }
        });
        card.add(createAccLabel);

        panel.add(card);
        return panel;
    }

    // SCREEN 3: Admin Self Sign-Up
    private JPanel createRegisterScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(230, 235, 245));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(30, 35, 30, 35));
        card.setPreferredSize(new Dimension(400, 600));

        JLabel backLabel = new JLabel("← Back to Login");
        backLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { 
                clearAuthenticationFields();
                cardLayout.show(mainPanel, "LOGIN"); 
            }
        });
        card.add(backLabel);
        card.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel title = new JLabel("Create Admin Account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 25)));

        card.add(new JLabel("Full Name"));
        JTextField regNameField = new JTextField();
        regNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        card.add(regNameField);

        card.add(new JLabel("Email"));
        JTextField regEmailField = new JTextField();
        regEmailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        card.add(regEmailField);

        card.add(new JLabel("Username"));
        JTextField regUserField = new JTextField();
        regUserField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        card.add(regUserField);

        card.add(new JLabel("Password"));
        JPasswordField regPassField = new JPasswordField();
        regPassField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        card.add(regPassField);
        card.add(Box.createRigidArea(new Dimension(0, 20)));

        JButton registerBtn = new JButton("Sign Up");
        registerBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        registerBtn.setBackground(new Color(100, 80, 240));
        registerBtn.setForeground(Color.WHITE);

        registerBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String u = regUserField.getText();
                if (userDatabase.containsKey(u)) {
                    JOptionPane.showMessageDialog(LibraryAppSwing.this, "Username already exists!");
                    return;
                }
                userDatabase.put(u, new User(u, new String(regPassField.getPassword()), selectedRole, regNameField.getText(), regEmailField.getText(), LocalDate.now().toString()));
                JOptionPane.showMessageDialog(LibraryAppSwing.this, "Account created successfully!");
                clearAuthenticationFields();
                cardLayout.show(mainPanel, "LOGIN");
            }
        });

        card.add(registerBtn);
        panel.add(card);
        return panel;
    }

    // SCREEN 4: MASTER LIBRARIAN DASHBOARD
    private JPanel createAdminDashboard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        // TOP NAVIGATION BAR
        JPanel topNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topNav.setBackground(new Color(245, 247, 250));
        topNav.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel appTitle = new JLabel("Library BTAB");
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        topNav.add(appTitle);
        topNav.add(new JSeparator(SwingConstants.VERTICAL));

        JButton addMemberBtn = new JButton("Add Members");
        JButton viewMemberBtn = new JButton("View Members");
        JButton addBooksBtn = new JButton("Add Books");
        JButton viewBooksBtn = new JButton("View Books");
        JButton bookHistoryBtn = new JButton("Book History");
        JButton currentBorrowedBtn = new JButton("Current Borrowed");
        
        JButton profileBtn = new JButton("👤 Profile");
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(255, 200, 200));

        topNav.add(addMemberBtn); topNav.add(viewMemberBtn);
        topNav.add(addBooksBtn); topNav.add(viewBooksBtn);
        topNav.add(bookHistoryBtn); topNav.add(currentBorrowedBtn);
        topNav.add(new JSeparator(SwingConstants.VERTICAL));
        topNav.add(profileBtn); topNav.add(logoutBtn);

        // Center dynamic content
        JPanel contentArea = new JPanel(new CardLayout());
        CardLayout contentLayout = (CardLayout) contentArea.getLayout();

        // [TAB 1] Add Members
        JPanel addMemberScreen = new JPanel(new GridBagLayout());
        addMemberScreen.setBackground(Color.WHITE);
        JPanel mForm = new JPanel();
        mForm.setLayout(new BoxLayout(mForm, BoxLayout.Y_AXIS));
        mForm.setBackground(Color.WHITE);
        mForm.setPreferredSize(new Dimension(400, 450));
        mForm.setBorder(BorderFactory.createTitledBorder("Add New Student Account"));
        
        JTextField mName = new JTextField(); JTextField mUser = new JTextField();
        JTextField mEmail = new JTextField(); JPasswordField mPass = new JPasswordField();
        JButton submitMember = new JButton("Register Student");
        
        mForm.add(new JLabel("Full Name")); mForm.add(mName);
        mForm.add(new JLabel("Username")); mForm.add(mUser);
        mForm.add(new JLabel("Email")); mForm.add(mEmail);
        mForm.add(new JLabel("Password")); mForm.add(mPass);
        mForm.add(Box.createRigidArea(new Dimension(0, 15))); mForm.add(submitMember);
        addMemberScreen.add(mForm);

        submitMember.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String u = mUser.getText();
                if(userDatabase.containsKey(u)) {
                    JOptionPane.showMessageDialog(LibraryAppSwing.this, "Student username already exists!");
                    return;
                }
                userDatabase.put(u, new User(u, new String(mPass.getPassword()), "Student/Member", mName.getText(), mEmail.getText(), LocalDate.now().toString()));
                JOptionPane.showMessageDialog(LibraryAppSwing.this, "Student account created successfully!");
                mName.setText(""); mUser.setText(""); mEmail.setText(""); mPass.setText("");
            }
        });

        // [TAB 2] View Members
        JPanel viewMemberScreen = new JPanel(new BorderLayout());
        viewMemberScreen.setBorder(new EmptyBorder(20,20,20,20));
        memberTableModel = new DefaultTableModel(new String[]{"ID", "Name", "Username", "Email", "Join Date"}, 0);
        JTable memberTable = new JTable(memberTableModel);
        viewMemberScreen.add(new JScrollPane(memberTable), BorderLayout.CENTER);

        // [TAB 3] Add Books
        JPanel addBooksScreen = new JPanel(new GridBagLayout());
        addBooksScreen.setBackground(Color.WHITE);
        JPanel bForm = new JPanel();
        bForm.setLayout(new BoxLayout(bForm, BoxLayout.Y_AXIS));
        bForm.setBackground(Color.WHITE);
        bForm.setPreferredSize(new Dimension(400, 400));
        
        JTextField titleF = new JTextField(); JTextField authorF = new JTextField();
        JTextField isbnF = new JTextField(); JTextField catF = new JTextField();
        JTextField qtyF = new JTextField(); JButton submitBook = new JButton("Add Book");
        
        bForm.add(new JLabel("Book Title")); bForm.add(titleF);
        bForm.add(new JLabel("Author")); bForm.add(authorF);
        bForm.add(new JLabel("ISBN")); bForm.add(isbnF);
        bForm.add(new JLabel("Category")); bForm.add(catF);
        bForm.add(new JLabel("Quantity")); bForm.add(qtyF);
        bForm.add(Box.createRigidArea(new Dimension(0, 15))); bForm.add(submitBook);
        addBooksScreen.add(bForm);

        // [TAB 4] View Books
        JPanel viewBooksScreen = new JPanel(new BorderLayout());
        viewBooksScreen.setBorder(new EmptyBorder(20,20,20,20));
        bookTableModel = new DefaultTableModel(new String[]{"ID", "Title", "Author", "ISBN", "Category", "Qty"}, 0);
        JTable bookTable = new JTable(bookTableModel);
        
        JPanel bookControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton deleteBookBtn = new JButton("Delete Selected Book");
        bookControls.add(deleteBookBtn);
        
        viewBooksScreen.add(new JScrollPane(bookTable), BorderLayout.CENTER);
        viewBooksScreen.add(bookControls, BorderLayout.SOUTH);

        // [TAB 5] Book History
        JPanel bookHistoryScreen = new JPanel(new BorderLayout());
        bookHistoryScreen.setBorder(new EmptyBorder(20,20,20,20));
        historyTableModel = new DefaultTableModel(new String[]{"Trans ID", "Book ID", "Student", "Borrow Date", "Status"}, 0);
        JTable historyTable = new JTable(historyTableModel);
        bookHistoryScreen.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        // [TAB 6] Current Borrowed
        JPanel currentBorrowedScreen = new JPanel(new BorderLayout());
        currentBorrowedScreen.setBorder(new EmptyBorder(20,20,20,20));
        currentBorrowedTableModel = new DefaultTableModel(new String[]{"Trans ID", "Book", "Student", "Borrow Date", "Due Date"}, 0);
        JTable curTable = new JTable(currentBorrowedTableModel);
        currentBorrowedScreen.add(new JScrollPane(curTable), BorderLayout.CENTER);

        contentArea.add(addMemberScreen, "ADD_MEMBER");
        contentArea.add(viewMemberScreen, "VIEW_MEMBER");
        contentArea.add(addBooksScreen, "ADD_BOOK");
        contentArea.add(viewBooksScreen, "VIEW_BOOK");
        contentArea.add(bookHistoryScreen, "HISTORY");
        contentArea.add(currentBorrowedScreen, "BORROWED");

        // Top Nav Listeners
        addMemberBtn.addActionListener(e -> contentLayout.show(contentArea, "ADD_MEMBER"));
        viewMemberBtn.addActionListener(e -> { refreshMembers(); contentLayout.show(contentArea, "VIEW_MEMBER"); });
        addBooksBtn.addActionListener(e -> contentLayout.show(contentArea, "ADD_BOOK"));
        viewBooksBtn.addActionListener(e -> { refreshBooks(); contentLayout.show(contentArea, "VIEW_BOOK"); });
        bookHistoryBtn.addActionListener(e -> { refreshHistory(); contentLayout.show(contentArea, "HISTORY"); });
        currentBorrowedBtn.addActionListener(e -> { refreshCurrentBorrowed(); contentLayout.show(contentArea, "BORROWED"); });
        
        profileBtn.addActionListener(e -> showProfileDialog());
        logoutBtn.addActionListener(e -> {
            clearAuthenticationFields();
            cardLayout.show(mainPanel, "SELECTION");
        });

        submitBook.addActionListener(e -> {
            try {
                int id = bookDatabase.size() + 1;
                int qty = Integer.parseInt(qtyF.getText());
                bookDatabase.add(new Book(id, titleF.getText(), authorF.getText(), isbnF.getText(), catF.getText(), qty));
                JOptionPane.showMessageDialog(LibraryAppSwing.this, "Book Added Successfully!");
                titleF.setText(""); authorF.setText(""); isbnF.setText(""); catF.setText(""); qtyF.setText("");
            } catch (Exception ex) { JOptionPane.showMessageDialog(LibraryAppSwing.this, "Invalid quantity field!"); }
        });

        deleteBookBtn.addActionListener(e -> {
            int row = bookTable.getSelectedRow();
            if(row >= 0) {
                bookDatabase.remove(row);
                refreshBooks();
                JOptionPane.showMessageDialog(LibraryAppSwing.this, "Book Deleted!");
            } else { JOptionPane.showMessageDialog(LibraryAppSwing.this, "Please select a row first!"); }
        });

        panel.add(topNav, BorderLayout.NORTH);
        panel.add(contentArea, BorderLayout.CENTER);
        return panel;
    }

    // SCREEN 5: STUDENT DASHBOARD (NOW ALIGNED WITH ADMIN STYLE AND 3 OPTIONS)
    private JPanel createStudentDashboard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        // TOP NAVIGATION BAR (Mirrors Admin layout)
        JPanel topNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topNav.setBackground(new Color(245, 247, 250));
        topNav.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel appTitle = new JLabel("Library BTAB");
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        topNav.add(appTitle);
        topNav.add(new JSeparator(SwingConstants.VERTICAL));

        // 3 Dedicated Options for Student
        JButton viewBooksBtn = new JButton("View Books");
        JButton issuedBooksBtn = new JButton("Issued Books");
        JButton returnedBooksBtn = new JButton("Returned Books");
        
        JButton profileBtn = new JButton("👤 Profile");
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(255, 200, 200));

        topNav.add(viewBooksBtn); 
        topNav.add(issuedBooksBtn); 
        topNav.add(returnedBooksBtn);
        topNav.add(new JSeparator(SwingConstants.VERTICAL));
        topNav.add(profileBtn); 
        topNav.add(logoutBtn);

        panel.add(topNav, BorderLayout.NORTH);

        // Center dynamic content area for Student
        JPanel contentArea = new JPanel(new CardLayout());
        CardLayout contentLayout = (CardLayout) contentArea.getLayout();

        // [TAB 1] View Books (The original custom grid layout)
        JPanel viewBooksTab = new JPanel(new BorderLayout());
        viewBooksTab.setBackground(Color.WHITE);

        // Metric Dashboard Boxes
        JPanel metricPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        metricPanel.setBackground(Color.WHITE);
        metricPanel.setBorder(new EmptyBorder(20, 20, 10, 20));

        int totalBorrowed = 0;
        for(Transaction t : transactions) {
            if(t.studentUser.equals(currentUser.username)) totalBorrowed++;
        }

        metricPanel.add(createMetricBox("View Books", String.valueOf(bookDatabase.size()), new Color(240, 244, 255)));
        metricPanel.add(createMetricBox("Issued Books", String.valueOf(totalBorrowed), new Color(255, 248, 240)));
        metricPanel.add(createMetricBox("Returned Books", "0", new Color(240, 250, 245)));
        viewBooksTab.add(metricPanel, BorderLayout.NORTH);

        // Dynamically generating book elements in grid
        JPanel bookGrid = new JPanel(new GridLayout(0, 2, 20, 20));
        bookGrid.setBackground(Color.WHITE);
        bookGrid.setBorder(new EmptyBorder(10, 20, 20, 20));

        for (Book b : bookDatabase) {
            JPanel bCard = new JPanel();
            bCard.setLayout(new BoxLayout(bCard, BoxLayout.Y_AXIS));
            bCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
                    new EmptyBorder(15, 15, 15, 15)
            ));
            bCard.setBackground(Color.WHITE);
            
            JLabel titleL = new JLabel("📖 " + b.title);
            titleL.setFont(new Font("Segoe UI", Font.BOLD, 14));
            bCard.add(titleL);
            bCard.add(new JLabel("By: " + b.author));
            bCard.add(new JLabel("ISBN: " + b.isbn));
            bCard.add(new JLabel("Category: " + b.category));
            
            Transaction activeT = null;
            for(Transaction t : transactions){
                if(t.bookId == b.id && t.studentUser.equals(currentUser.username)) activeT = t;
            }

            if (activeT != null) {
                JLabel borrowedLbl = new JLabel("Status: Borrowed (Due: " + activeT.dueDate + ")");
                borrowedLbl.setForeground(Color.BLUE);
                bCard.add(borrowedLbl);
                
                if (LocalDate.now().isAfter(LocalDate.parse(activeT.dueDate))) {
                    JLabel overLbl = new JLabel("⚠️ This book is OVERDUE!");
                    overLbl.setForeground(Color.RED);
                    bCard.add(overLbl);
                }
            } else if (b.availableQty > 0) {
                JLabel availLbl = new JLabel("Available: " + b.availableQty);
                availLbl.setForeground(new Color(40, 160, 80));
                bCard.add(availLbl);
            } else {
                JLabel outLbl = new JLabel("Status: Out of Stock");
                outLbl.setForeground(Color.GRAY);
                bCard.add(outLbl);
            }
            bookGrid.add(bCard);
        }
        viewBooksTab.add(new JScrollPane(bookGrid), BorderLayout.CENTER);

        // [TAB 2] Issued Books Tab
        JPanel issuedBooksTab = new JPanel(new BorderLayout());
        issuedBooksTab.setBorder(new EmptyBorder(20,20,20,20));
        studentIssuedTableModel = new DefaultTableModel(new String[]{"Trans ID", "Book", "Borrow Date", "Due Date"}, 0);
        JTable issuedTable = new JTable(studentIssuedTableModel);
        issuedBooksTab.add(new JScrollPane(issuedTable), BorderLayout.CENTER);

        // [TAB 3] Returned Books Tab
        JPanel returnedBooksTab = new JPanel(new BorderLayout());
        returnedBooksTab.setBorder(new EmptyBorder(20,20,20,20));
        studentReturnedTableModel = new DefaultTableModel(new String[]{"Trans ID", "Book", "Return Date", "Status"}, 0);
        JTable returnedTable = new JTable(studentReturnedTableModel);
        returnedBooksTab.add(new JScrollPane(returnedTable), BorderLayout.CENTER);

        contentArea.add(viewBooksTab, "STUDENT_VIEW_BOOKS");
        contentArea.add(issuedBooksTab, "STUDENT_ISSUED");
        contentArea.add(returnedBooksTab, "STUDENT_RETURNED");

        // Top Nav Listeners for Student
        viewBooksBtn.addActionListener(e -> contentLayout.show(contentArea, "STUDENT_VIEW_BOOKS"));
        issuedBooksBtn.addActionListener(e -> { refreshStudentIssued(); contentLayout.show(contentArea, "STUDENT_ISSUED"); });
        returnedBooksBtn.addActionListener(e -> { refreshStudentReturned(); contentLayout.show(contentArea, "STUDENT_RETURNED"); });
        
        profileBtn.addActionListener(e -> showProfileDialog());
        logoutBtn.addActionListener(e -> {
            clearAuthenticationFields();
            cardLayout.show(mainPanel, "SELECTION");
        });

        panel.add(contentArea, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMetricBox(String title, String value, Color bgColor) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(bgColor);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(Color.DARK_GRAY);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 30));

        box.add(titleLbl);
        box.add(Box.createRigidArea(new Dimension(0, 5)));
        box.add(valLbl);

        return box;
    }

    private void showProfileDialog() {
        if (currentUser == null) return;
        
        StringBuilder profile = new StringBuilder();
        profile.append("Full Name: ").append(currentUser.fullName).append("\n");
        profile.append("Username: ").append(currentUser.username).append("\n");
        profile.append("Email: ").append(currentUser.email).append("\n");
        profile.append("Account Type: ").append(currentUser.role).append("\n");
        profile.append("Member Since: ").append(currentUser.joinDate);
        
        JOptionPane.showMessageDialog(this, profile.toString(), "Profile Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshMembers() {
        memberTableModel.setRowCount(0);
        int id = 1;
        for (User u : userDatabase.values()) {
            if (u.role.equals("Student/Member")) {
                memberTableModel.addRow(new Object[]{id++, u.fullName, u.username, u.email, u.joinDate});
            }
        }
    }

    private void refreshBooks() {
        bookTableModel.setRowCount(0);
        for (Book b : bookDatabase) {
            bookTableModel.addRow(new Object[]{b.id, b.title, b.author, b.isbn, b.category, b.availableQty});
        }
    }

    private void refreshHistory() {
        historyTableModel.setRowCount(0);
        for (Transaction t : transactions) {
            historyTableModel.addRow(new Object[]{t.id, t.bookId, t.studentUser, t.borrowDate, t.status});
        }
    }

    private void refreshCurrentBorrowed() {
        currentBorrowedTableModel.setRowCount(0);
        for (Transaction t : transactions) {
            if(t.status.equals("Borrowed")) {
                Book b = bookDatabase.get(t.bookId - 1);
                currentBorrowedTableModel.addRow(new Object[]{t.id, b.title, t.studentUser, t.borrowDate, t.dueDate});
            }
        }
    }
    
    // Dynamic Student Refresher methods
    private void refreshStudentIssued() {
        studentIssuedTableModel.setRowCount(0);
        for (Transaction t : transactions) {
            if(t.studentUser.equals(currentUser.username) && t.status.equals("Borrowed")) {
                Book b = bookDatabase.get(t.bookId - 1);
                studentIssuedTableModel.addRow(new Object[]{t.id, b.title, t.borrowDate, t.dueDate});
            }
        }
    }
    
    private void refreshStudentReturned() {
        studentReturnedTableModel.setRowCount(0);
        for (Transaction t : transactions) {
            if(t.studentUser.equals(currentUser.username) && t.status.equals("Returned")) {
                Book b = bookDatabase.get(t.bookId - 1);
                studentReturnedTableModel.addRow(new Object[]{t.id, b.title, t.borrowDate, t.status});
            }
        }
    }

    // Object Blueprints
    class User {
        String username, password, role, fullName, email, joinDate;
        User(String u, String p, String r, String fn, String e, String jd) {
            this.username = u; this.password = p; this.role = r; this.fullName = fn; this.email = e; this.joinDate = jd;
        }
    }
    class Book {
        int id, availableQty; String title, author, isbn, category;
        Book(int id, String t, String a, String i, String c, int qty) {
            this.id = id; this.title = t; this.author = a; this.isbn = i; this.category = c; this.availableQty = qty;
        }
    }
    class Transaction {
        int id, bookId; String studentUser, borrowDate, dueDate, status;
        Transaction(int id, int bid, String su, String bd, String dd, String status) {
            this.id = id; this.bookId = bid; this.studentUser = su; this.borrowDate = bd; this.dueDate = dd; this.status = status;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LibraryAppSwing().setVisible(true);
            }
        });
    }
}
