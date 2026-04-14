CREATE DATABASE IF NOT EXISTS BATB_Library;
USE BATB_Library;

-- ============================================================
--  SECTION 1: TABLES
-- ============================================================

CREATE TABLE IF NOT EXISTS MEMBER (
    borrower_id     INT             AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(100)    NOT NULL UNIQUE,
    contact_number  VARCHAR(20),
    address         TEXT,
    role            ENUM('Admin/Librarian','Student/Member') NOT NULL DEFAULT 'Student/Member',
    account_status  ENUM('Active','Suspended','Pending')     NOT NULL DEFAULT 'Active',
    join_date       DATE            NOT NULL DEFAULT (CURRENT_DATE),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS BOOK (
    book_id        INT          AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(255) NOT NULL,
    author         VARCHAR(150) NOT NULL,
    isbn           CHAR(13)     NOT NULL UNIQUE,
    category       VARCHAR(80),
    publisher      VARCHAR(150),
    year_published YEAR,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS COPY (
    copy_id       VARCHAR(30)  PRIMARY KEY,
    book_id       INT          NOT NULL,
    availability  ENUM('Available','Borrowed','Damaged','Lost') NOT NULL DEFAULT 'Available',
    date_acquired DATE,
    CONSTRAINT fk_copy_book FOREIGN KEY (book_id) REFERENCES BOOK(book_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS LOAN (
    loan_id     INT         AUTO_INCREMENT PRIMARY KEY,
    copy_id     VARCHAR(30) NOT NULL,
    borrower_id INT         NOT NULL,
    issue_date  DATE        NOT NULL,
    due_date    DATE        NOT NULL,
    return_date DATE,
    loan_status ENUM('Active','Returned','Overdue','Lost') NOT NULL DEFAULT 'Active',
    CONSTRAINT fk_loan_copy     FOREIGN KEY (copy_id)     REFERENCES COPY(copy_id),
    CONSTRAINT fk_loan_borrower FOREIGN KEY (borrower_id) REFERENCES MEMBER(borrower_id)
);

CREATE TABLE IF NOT EXISTS FINE (
    fine_id        INT            AUTO_INCREMENT PRIMARY KEY,
    loan_id        INT            NOT NULL,
    borrower_id    INT            NOT NULL,
    amount         DECIMAL(8,2)   NOT NULL,
    reason         VARCHAR(255),
    payment_status ENUM('Unpaid','Paid','Waived') NOT NULL DEFAULT 'Unpaid',
    created_at     TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fine_loan     FOREIGN KEY (loan_id)     REFERENCES LOAN(loan_id),
    CONSTRAINT fk_fine_borrower FOREIGN KEY (borrower_id) REFERENCES MEMBER(borrower_id)
);

CREATE TABLE IF NOT EXISTS PAYMENT (
    payment_id     INT           AUTO_INCREMENT PRIMARY KEY,
    fine_id        INT           NOT NULL,
    borrower_id    INT           NOT NULL,
    paid_amount    DECIMAL(8,2)  NOT NULL,
    payment_date   DATE          NOT NULL,
    payment_method ENUM('Cash','GCash','Maya','Card','Other') NOT NULL DEFAULT 'Cash',
    received_by    INT,
    notes          TEXT,
    CONSTRAINT fk_payment_fine     FOREIGN KEY (fine_id)     REFERENCES FINE(fine_id),
    CONSTRAINT fk_payment_borrower FOREIGN KEY (borrower_id) REFERENCES MEMBER(borrower_id),
    CONSTRAINT fk_payment_receiver FOREIGN KEY (received_by) REFERENCES MEMBER(borrower_id)
);

-- Audit log table used by trigger (Section 6)
CREATE TABLE IF NOT EXISTS copy_audit_log (
    log_id      INT AUTO_INCREMENT PRIMARY KEY,
    copy_id     VARCHAR(30) NOT NULL,
    old_status  VARCHAR(20),
    new_status  VARCHAR(20),
    changed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
--  SECTION 2: VALIDATION RULES (CHECK Constraints)
-- ============================================================

ALTER TABLE BOOK
    ADD CONSTRAINT chk_isbn_length
    CHECK (CHAR_LENGTH(isbn) = 13 AND isbn REGEXP '^[0-9]+$');

ALTER TABLE LOAN
    ADD CONSTRAINT chk_loan_dates
    CHECK (due_date > issue_date);

ALTER TABLE LOAN
    ADD CONSTRAINT chk_return_date
    CHECK (return_date IS NULL OR return_date >= issue_date);

ALTER TABLE FINE
    ADD CONSTRAINT chk_fine_amount
    CHECK (amount > 0);

ALTER TABLE PAYMENT
    ADD CONSTRAINT chk_payment_amount
    CHECK (paid_amount > 0);

ALTER TABLE MEMBER
    ADD CONSTRAINT chk_contact_length
    CHECK (contact_number IS NULL OR CHAR_LENGTH(contact_number) >= 7);

-- ============================================================
--  SECTION 3: CLEAR OLD DATA (re-seed)
-- ============================================================
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE PAYMENT;
TRUNCATE TABLE FINE;
TRUNCATE TABLE copy_audit_log;
TRUNCATE TABLE LOAN;
TRUNCATE TABLE COPY;
TRUNCATE TABLE BOOK;
TRUNCATE TABLE MEMBER;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
--  SECTION 4: SEED DATA
-- ============================================================

-- ---- MEMBERS ----
-- Admins: borrower_id 1–6
-- Students: borrower_id 7–18
INSERT INTO MEMBER (username, password_hash, full_name, email, contact_number, address, role, account_status, join_date) VALUES
('admin',       'admin123',    'Admin User',         'admin@library.edu',       '09171234567', 'Library Office, Main Building', 'Admin/Librarian', 'Active',    '2024-01-01'),
('fabian',      '679854132',   'Lyzander Fabian',    'fabian@library.edu',      '09189876543', 'Library Office, Main Building', 'Admin/Librarian', 'Active',    '2024-01-15'),
('mendoza',     'mendoza789',  'Benedict Mendoza',   'mendoza@library.edu',     '09175550001', 'Library Office, Main Building', 'Admin/Librarian', 'Active',    '2024-02-01'),
('sumilang',    'sumilang456', 'Ivan Sumilang',      'sumilang@library.edu',    '09175345001', 'Library Office, Main Building', 'Admin/Librarian', 'Active',    '2024-02-03'),
('maigue',      'maigue678',   'John Maigue',        'maigue@library.edu',      '09234765027', 'Library Office, Main Building', 'Admin/Librarian', 'Active',    '2024-02-10'),
('quizon',      'quizon789',   'Kyle Quizon',        'quizon@library.edu',      '09836829466', 'Library Office, Main Building', 'Admin/Librarian', 'Suspended', '2024-02-15'),
-- Students
('jdoe',        'student123',  'John Doe',           'jdoe@student.edu',        '09201111111', '123 Mabini St, Manila',               'Student/Member', 'Active',    '2024-02-15'),
('acruzan',     'pass2024',    'Anna Cruz',          'acruz@student.edu',       '09202222222', '45 Rizal Ave, Quezon City',           'Student/Member', 'Active',    '2024-03-01'),
('bkagawa',     'bk9900',      'Ben Kagawa',         'bkag@student.edu',        '09203333333', '78 Aurora Blvd, Pasig',               'Student/Member', 'Active',    '2024-03-10'),
('clopez',      'clopez1',     'Carlos Lopez',       'clopez@student.edu',      '09204444444', '9 Shaw Blvd, Mandaluyong',            'Student/Member', 'Suspended', '2024-04-05'),
('dnavarro',    'dnavarro2',   'Diana Navarro',      'dnavarro@student.edu',    '09205555555', '20 EDSA, Makati',                     'Student/Member', 'Active',    '2024-05-20'),
('emendoza',    'emen2024',    'Eduardo Mendoza',    'emendoza@student.edu',    '09206666666', '33 Katipunan Ave, Quezon City',       'Student/Member', 'Active',    '2024-06-01'),
('ftorres',     'ftorres99',   'Fatima Torres',      'ftorres@student.edu',     '09207777777', '88 Commonwealth Ave, Quezon City',   'Student/Member', 'Active',    '2024-06-15'),
('gvillanueva', 'gv2024',      'Gabriel Villanueva', 'gvillanueva@student.edu', '09208888888', '12 C.P. Garcia Ave, Diliman',         'Student/Member', 'Active',    '2024-07-01'),
('hbautista',   'hb1234',      'Hannah Bautista',    'hbautista@student.edu',   '09209999999', '55 España Blvd, Sampaloc',            'Student/Member', 'Active',    '2024-07-10'),
('iramos',      'iramos55',    'Ivan Ramos',         'iramos@student.edu',      '09210000001', '7 Taft Ave, Malate',                  'Student/Member', 'Suspended', '2024-08-01'),
('jaquino',     'jaq2025',     'Jasmine Aquino',     'jaquino@student.edu',     '09210000002', '101 Roxas Blvd, Pasay',               'Student/Member', 'Active',    '2024-08-15'),
('kcabrera',    'kc9876',      'Kenneth Cabrera',    'kcabrera@student.edu',    '09210000003', '22 Ortigas Ave, Pasig',               'Student/Member', 'Active',    '2024-09-01');

-- ---- BOOKS ----
INSERT INTO BOOK (title, author, isbn, category, publisher, year_published) VALUES
('To Kill a Mockingbird',                   'Harper Lee',            '9780061120084', 'Fiction',      'HarperCollins',    1960),
('The Great Gatsby',                        'F. Scott Fitzgerald',   '9780743273565', 'Fiction',      'Scribner',         1925),
('1984',                                    'George Orwell',         '9780451524935', 'Dystopian',    'Signet Classic',   1949),
('Clean Code',                              'Robert C. Martin',      '9780132350884', 'Technology',   'Prentice Hall',    2008),
('Sapiens',                                 'Yuval Noah Harari',     '9780062316097', 'Non-Fiction',  'HarperCollins',    2011),
('The Pragmatic Programmer',                'Andrew Hunt',           '9780135957059', 'Technology',   'Addison-Wesley',   2019),
('Design Patterns',                         'Gang of Four',          '9780201633610', 'Technology',   'Addison-Wesley',   1994),
('Harry Potter and the Philosopher''s Stone','J.K. Rowling',         '9780747532743', 'Fantasy',      'Bloomsbury',       1997),
('Atomic Habits',                           'James Clear',           '9780735211292', 'Self-Help',    'Avery',            2018),
('The Alchemist',                           'Paulo Coelho',          '9780062315007', 'Fiction',      'HarperOne',        1988),
('Introduction to Algorithms',              'Cormen et al.',         '9780262033848', 'Technology',   'MIT Press',        2009),
('Thinking, Fast and Slow',                 'Daniel Kahneman',       '9780374533557', 'Psychology',   'Farrar Straus',    2011),
('The 7 Habits of Highly Effective People', 'Stephen Covey',         '9781982137274', 'Self-Help',    'Simon & Schuster', 1989),
('Rich Dad Poor Dad',                       'Robert Kiyosaki',       '9781612680194', 'Finance',      'Plata Publishing', 1997),
('Dune',                                    'Frank Herbert',         '9780441013593', 'Sci-Fi',       'Ace Books',        1965),
('The Hunger Games',                        'Suzanne Collins',       '9780439023481', 'Dystopian',    'Scholastic',       2008),
('Animal Farm',                             'George Orwell',         '9780451526342', 'Dystopian',    'Signet Classic',   1945),
('Principles of Economics',                 'N. Gregory Mankiw',     '9781305585126', 'Economics',    'Cengage Learning', 2014),
('Calculus: Early Transcendentals',         'James Stewart',         '9781285741550', 'Mathematics',  'Cengage Learning', 2015),
('Data Structures and Algorithms',          'Michael T. Goodrich',   '9781118771334', 'Technology',   'Wiley',            2014);

-- ---- COPIES ----
INSERT INTO COPY (copy_id, book_id, availability, date_acquired) VALUES
('copy_1_1',  1,  'Available', '2023-06-01'), ('copy_1_2',  1,  'Borrowed',  '2023-06-01'), ('copy_1_3',  1,  'Available', '2023-06-01'),
('copy_2_1',  2,  'Available', '2023-06-01'), ('copy_2_2',  2,  'Borrowed',  '2023-06-01'),
('copy_3_1',  3,  'Borrowed',  '2023-06-01'), ('copy_3_2',  3,  'Available', '2023-06-01'), ('copy_3_3',  3,  'Damaged',   '2023-06-01'),
('copy_4_1',  4,  'Available', '2023-07-01'), ('copy_4_2',  4,  'Borrowed',  '2023-07-01'),
('copy_5_1',  5,  'Available', '2024-01-10'), ('copy_5_2',  5,  'Borrowed',  '2024-01-10'), ('copy_5_3',  5,  'Available', '2024-01-10'),
('copy_6_1',  6,  'Available', '2024-01-10'), ('copy_6_2',  6,  'Borrowed',  '2024-01-10'),
('copy_7_1',  7,  'Borrowed',  '2024-01-10'), ('copy_7_2',  7,  'Available', '2024-01-10'),
('copy_8_1',  8,  'Available', '2024-02-01'), ('copy_8_2',  8,  'Borrowed',  '2024-02-01'), ('copy_8_3',  8,  'Lost',      '2024-02-01'),
('copy_9_1',  9,  'Available', '2024-03-01'), ('copy_9_2',  9,  'Borrowed',  '2024-03-01'), ('copy_9_3',  9,  'Available', '2024-03-01'),
('copy_10_1', 10, 'Available', '2024-03-01'), ('copy_10_2', 10, 'Borrowed',  '2024-03-01'),
('copy_11_1', 11, 'Available', '2024-04-01'), ('copy_11_2', 11, 'Borrowed',  '2024-04-01'),
('copy_12_1', 12, 'Available', '2024-04-01'), ('copy_12_2', 12, 'Available', '2024-04-01'),
('copy_13_1', 13, 'Borrowed',  '2024-05-01'), ('copy_13_2', 13, 'Available', '2024-05-01'), ('copy_13_3', 13, 'Available', '2024-05-01'),
('copy_14_1', 14, 'Available', '2024-05-01'), ('copy_14_2', 14, 'Borrowed',  '2024-05-01'),
('copy_15_1', 15, 'Available', '2024-06-01'), ('copy_15_2', 15, 'Available', '2024-06-01'),
('copy_16_1', 16, 'Borrowed',  '2024-06-01'), ('copy_16_2', 16, 'Available', '2024-06-01'), ('copy_16_3', 16, 'Damaged',   '2024-06-01'),
('copy_17_1', 17, 'Available', '2024-07-01'), ('copy_17_2', 17, 'Available', '2024-07-01'),
('copy_18_1', 18, 'Available', '2024-07-01'), ('copy_18_2', 18, 'Borrowed',  '2024-07-01'), ('copy_18_3', 18, 'Available', '2024-07-01'),
('copy_19_1', 19, 'Borrowed',  '2024-08-01'), ('copy_19_2', 19, 'Available', '2024-08-01'),
('copy_20_1', 20, 'Available', '2024-08-01'), ('copy_20_2', 20, 'Borrowed',  '2024-08-01');

-- ---- LOANS ----
-- Students: jdoe=7, acruzan=8, bkagawa=9, clopez=10, dnavarro=11,
--           emendoza=12, ftorres=13, gvillanueva=14, hbautista=15,
--           iramos=16, jaquino=17, kcabrera=18
INSERT INTO LOAN (copy_id, borrower_id, issue_date, due_date, return_date, loan_status) VALUES
-- Active / Overdue (loan_id 1–16)
('copy_1_2',   7, '2026-03-20', '2026-04-03', NULL, 'Active'),
('copy_4_2',   7, '2026-03-22', '2026-04-05', NULL, 'Active'),
('copy_3_1',   8, '2026-03-25', '2026-04-08', NULL, 'Active'),
('copy_9_2',   8, '2026-03-28', '2026-04-11', NULL, 'Active'),
('copy_8_2',   9, '2026-03-15', '2026-03-29', NULL, 'Overdue'),
('copy_6_2',   9, '2026-03-10', '2026-03-24', NULL, 'Overdue'),
('copy_7_1',  11, '2026-03-18', '2026-04-01', NULL, 'Active'),
('copy_10_2', 12, '2026-03-20', '2026-04-03', NULL, 'Active'),
('copy_11_2', 12, '2026-03-22', '2026-04-05', NULL, 'Active'),
('copy_13_1', 13, '2026-03-05', '2026-03-19', NULL, 'Overdue'),
('copy_14_2', 14, '2026-03-28', '2026-04-11', NULL, 'Active'),
('copy_16_1', 15, '2026-03-15', '2026-03-29', NULL, 'Overdue'),
('copy_18_2', 17, '2026-03-30', '2026-04-13', NULL, 'Active'),
('copy_19_1', 18, '2026-03-28', '2026-04-11', NULL, 'Active'),
('copy_20_2', 18, '2026-03-25', '2026-04-08', NULL, 'Active'),
('copy_2_2',  10, '2026-02-01', '2026-02-15', NULL, 'Overdue'),
-- Returned (loan_id 17–31)
('copy_5_2',   7, '2026-01-05', '2026-01-19', '2026-01-18', 'Returned'),
('copy_2_1',   8, '2026-01-10', '2026-01-24', '2026-01-23', 'Returned'),
('copy_12_1',  9, '2026-01-15', '2026-01-29', '2026-02-03', 'Returned'),
('copy_15_1', 11, '2026-01-20', '2026-02-03', '2026-02-03', 'Returned'),
('copy_17_1', 12, '2026-01-25', '2026-02-08', '2026-02-15', 'Returned'),
('copy_1_1',  13, '2026-02-01', '2026-02-15', '2026-02-14', 'Returned'),
('copy_9_1',  14, '2026-02-05', '2026-02-19', '2026-02-19', 'Returned'),
('copy_3_2',  15, '2026-02-10', '2026-02-24', '2026-03-02', 'Returned'),
('copy_13_2', 17, '2026-02-12', '2026-02-26', '2026-02-25', 'Returned'),
('copy_7_2',  18, '2026-02-15', '2026-03-01', '2026-03-01', 'Returned'),
('copy_4_1',   7, '2025-12-01', '2025-12-15', '2025-12-20', 'Returned'),
('copy_5_1',   8, '2025-12-10', '2025-12-24', '2025-12-23', 'Returned'),
('copy_10_1',  9, '2025-12-15', '2025-12-29', '2025-12-29', 'Returned'),
('copy_11_1', 11, '2026-01-02', '2026-01-16', '2026-01-30', 'Returned'),
('copy_6_1',  10, '2025-11-01', '2025-11-15', '2025-11-25', 'Returned'),
-- Lost (loan_id 32)
('copy_8_3',  10, '2025-12-01', '2025-12-15', NULL, 'Lost');

-- ---- FINES ----
INSERT INTO FINE (loan_id, borrower_id, amount, reason, payment_status) VALUES
(5,   9,  60.00,  'Overdue – Harry Potter (6 days past due)',          'Unpaid'),
(6,   9, 110.00,  'Overdue – Pragmatic Programmer (11 days past due)', 'Unpaid'),
(10, 13, 160.00,  'Overdue – The 7 Habits (16 days past due)',         'Unpaid'),
(12, 15,  60.00,  'Overdue – Hunger Games (6 days past due)',          'Unpaid'),
(16, 10, 480.00,  'Overdue – Great Gatsby (48 days past due)',         'Unpaid'),
(19,  9,  50.00,  'Late return – Thinking Fast & Slow (5 days)',       'Paid'),
(21, 12,  70.00,  'Late return – Animal Farm (7 days)',                'Paid'),
(24, 15,  60.00,  'Late return – 1984 (6 days)',                       'Unpaid'),
(27,  7,  50.00,  'Late return – Clean Code (5 days)',                 'Paid'),
(30, 11, 140.00,  'Late return – Intro to Algorithms (14 days)',       'Unpaid'),
(31, 10, 100.00,  'Late return – Pragmatic Programmer (10 days)',      'Unpaid'),
(32, 10, 800.00,  'Lost book – Harry Potter replacement cost',         'Unpaid');

-- ---- PAYMENTS ----
INSERT INTO PAYMENT (fine_id, borrower_id, paid_amount, payment_date, payment_method, received_by, notes) VALUES
(6,  9,  50.00, '2026-02-04', 'Cash',  1, 'Full payment – Thinking Fast & Slow late return. Receipt #001'),
(7,  12, 70.00, '2026-02-16', 'GCash', 1, 'Full payment – Animal Farm late return. GCash ref #GP20260216'),
(9,  7,  50.00, '2025-12-21', 'Cash',  2, 'Full payment – Clean Code late return. Receipt #002');

-- ============================================================
--  SECTION 5: VIEWS
-- ============================================================

-- Active loans with full details
CREATE OR REPLACE VIEW v_active_loans AS
SELECT
    l.loan_id,
    m.full_name                        AS borrower_name,
    m.username,
    b.title                            AS book_title,
    b.author,
    l.copy_id,
    l.issue_date,
    l.due_date,
    DATEDIFF(CURRENT_DATE, l.due_date) AS days_overdue,
    l.loan_status
FROM LOAN l
JOIN MEMBER m ON l.borrower_id = m.borrower_id
JOIN COPY   c ON l.copy_id     = c.copy_id
JOIN BOOK   b ON c.book_id     = b.book_id
WHERE l.loan_status IN ('Active','Overdue');

-- All unpaid fines with borrower and book info
CREATE OR REPLACE VIEW v_unpaid_fines AS
SELECT
    f.fine_id,
    m.full_name      AS borrower_name,
    m.username,
    b.title          AS book_title,
    f.amount,
    f.reason,
    f.payment_status,
    f.created_at
FROM FINE f
JOIN MEMBER m ON f.borrower_id = m.borrower_id
JOIN LOAN   l ON f.loan_id     = l.loan_id
JOIN COPY   c ON l.copy_id     = c.copy_id
JOIN BOOK   b ON c.book_id     = b.book_id
WHERE f.payment_status = 'Unpaid';

-- Book availability summary per title
CREATE OR REPLACE VIEW v_book_availability AS
SELECT
    b.book_id,
    b.title,
    b.author,
    b.isbn,
    b.category,
    COUNT(c.copy_id)                          AS total_copies,
    SUM(c.availability = 'Available')         AS available_copies,
    SUM(c.availability = 'Borrowed')          AS borrowed_copies,
    SUM(c.availability IN ('Damaged','Lost')) AS unavailable_copies
FROM BOOK b
LEFT JOIN COPY c ON b.book_id = c.book_id
GROUP BY b.book_id, b.title, b.author, b.isbn, b.category;

-- Monthly loan and fine summary (used in reports)
CREATE OR REPLACE VIEW v_monthly_summary AS
SELECT
    DATE_FORMAT(l.issue_date, '%Y-%m')    AS month,
    COUNT(l.loan_id)                       AS loans_issued,
    SUM(l.loan_status = 'Returned')        AS books_returned,
    SUM(l.loan_status IN ('Active','Overdue')) AS books_still_out,
    COALESCE(SUM(f.amount), 0)             AS fines_generated,
    COALESCE(SUM(CASE WHEN f.payment_status = 'Paid' THEN f.amount ELSE 0 END), 0) AS fines_collected
FROM LOAN l
LEFT JOIN FINE f ON l.loan_id = f.loan_id
GROUP BY DATE_FORMAT(l.issue_date, '%Y-%m')
ORDER BY month DESC;

-- Top 10 most borrowed books
CREATE OR REPLACE VIEW v_most_borrowed_books AS
SELECT
    b.title,
    b.author,
    b.category,
    COUNT(l.loan_id) AS times_borrowed
FROM BOOK b
JOIN COPY c ON b.book_id = c.book_id
JOIN LOAN l ON c.copy_id = l.copy_id
GROUP BY b.book_id, b.title, b.author, b.category
ORDER BY times_borrowed DESC
LIMIT 10;

-- Fine payment status summary
CREATE OR REPLACE VIEW v_fine_summary AS
SELECT
    payment_status,
    COUNT(*)    AS count,
    SUM(amount) AS total_amount
FROM FINE
GROUP BY payment_status;

-- ============================================================
--  SECTION 6: INDEXES
-- ============================================================

-- Login query: WHERE username = ?
CREATE INDEX idx_member_username   ON MEMBER (username);

-- View Members tab: WHERE role = 'Student/Member'
CREATE INDEX idx_member_role       ON MEMBER (role);

-- Book search: WHERE title/author/category LIKE ?
CREATE INDEX idx_book_title        ON BOOK (title);
CREATE INDEX idx_book_author       ON BOOK (author);
CREATE INDEX idx_book_category     ON BOOK (category);

-- Copy availability check when browsing books
CREATE INDEX idx_copy_availability ON COPY (availability);

-- Active Loans tab: WHERE loan_status IN ('Active','Overdue')
CREATE INDEX idx_loan_status       ON LOAN (loan_status);

-- Student My Loans tab: WHERE borrower_id = ?
CREATE INDEX idx_loan_borrower     ON LOAN (borrower_id);

-- Fines tab: WHERE payment_status = 'Unpaid'
CREATE INDEX idx_fine_payment      ON FINE (payment_status);

-- Student My Fines tab: WHERE borrower_id = ?
CREATE INDEX idx_fine_borrower     ON FINE (borrower_id);

-- ============================================================
--  VERIFICATION — Run after script to confirm everything is loaded
--  Expected: Members=18, Books=20, Copies=48, Loans=32,
--            Fines=12, Payments=3
-- ============================================================
SELECT 'Members'  AS entity, COUNT(*) AS total FROM MEMBER  UNION ALL
SELECT 'Books',              COUNT(*)           FROM BOOK    UNION ALL
SELECT 'Copies',             COUNT(*)           FROM COPY    UNION ALL
SELECT 'Loans',              COUNT(*)           FROM LOAN    UNION ALL
SELECT 'Fines',              COUNT(*)           FROM FINE    UNION ALL
SELECT 'Payments',           COUNT(*)           FROM PAYMENT;