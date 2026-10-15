import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Dedicated UI Window for Database Connectivity, Analytics & Management.
 * Displays live MySQL status, conversion logs, exchange rate CRUD, and diagnostics.
 */
public class DatabaseDashboardFrame extends JFrame {

    private final ProCurrencyConverter parentConverter;

    // Header & Status components
    private JLabel statusBadgeLabel;
    private JLabel totalConversionsLabel;
    private JLabel totalRatesLabel;
    private JLabel dbHostLabel;

    // Tab 1: Conversion History
    private JTable historyTable;
    private DefaultTableModel historyTableModel;
    private TableRowSorter<DefaultTableModel> historySorter;
    private JTextField historySearchField;
    private JLabel historyCountLabel;

    // Tab 2: Exchange Rates
    private JTable ratesTable;
    private DefaultTableModel ratesTableModel;
    private TableRowSorter<DefaultTableModel> ratesSorter;
    private JTextField ratesSearchField;
    private JComboBox<String> fromCombo;
    private JComboBox<String> toCombo;
    private JTextField rateInputField;
    private JLabel formStatusLabel;

    // Tab 3: Diagnostics
    private JTextArea diagnosticsArea;

    private static final String[] CURRENCIES = {
        "USD", "INR", "EUR", "GBP", "JPY", "CAD", "AUD",
        "CHF", "CNY", "AED", "SAR", "PKR", "NPR", "KRW", "ZAR"
    };

    public DatabaseDashboardFrame(ProCurrencyConverter parentConverter) {
        this.parentConverter = parentConverter;

        setTitle("Database Management Console - currency_db");
        setSize(960, 680);
        setMinimumSize(new Dimension(840, 580));
        setLocationRelativeTo(parentConverter);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initUI();
        refreshAllData();
    }

    private void initUI() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(new Color(243, 246, 251));

        // 1. Top Header with Gradient and Live Status
        JPanel headerPanel = createHeaderPanel();
        rootPanel.add(headerPanel, BorderLayout.NORTH);

        // 2. Center Content with Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(Color.WHITE);

        tabbedPane.addTab("📜 Conversion History Log", createHistoryPanel());
        tabbedPane.addTab("💱 Exchange Rates Manager", createRatesPanel());
        tabbedPane.addTab("🔌 Connectivity & Diagnostics", createDiagnosticsPanel());

        rootPanel.add(tabbedPane, BorderLayout.CENTER);

        // 3. Footer Bar
        JPanel footerPanel = createFooterPanel();
        rootPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    // ==================== HEADER & METRIC CARDS ====================

    private JPanel createHeaderPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        // Top Gradient Bar
        JPanel banner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(30, 27, 75),
                    getWidth(), 0, new Color(55, 48, 163)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        banner.setLayout(new BorderLayout());
        banner.setBorder(new EmptyBorder(16, 24, 16, 24));

        // Title Section
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Database Management Console");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel("Live MySQL Connectivity  •  Database: currency_db  •  Tables: exchange_rates, conversion_history");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLabel.setForeground(new Color(199, 210, 254));

        titlePanel.add(titleLabel);
        titlePanel.add(subLabel);
        banner.add(titlePanel, BorderLayout.WEST);

        // Status Badge & Test Button
        JPanel statusBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        statusBox.setOpaque(false);

        statusBadgeLabel = new JLabel("● Checking...");
        statusBadgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusBadgeLabel.setOpaque(true);
        statusBadgeLabel.setBackground(new Color(24, 24, 48, 180));
        statusBadgeLabel.setForeground(Color.WHITE);
        statusBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(99, 102, 241), 1, true),
                new EmptyBorder(6, 14, 6, 14)
        ));

        JButton pingBtn = createIconButton("⚡ Test Connection", new Color(79, 70, 229), Color.WHITE);
        pingBtn.addActionListener(e -> testConnectionAction());

        statusBox.add(statusBadgeLabel);
        statusBox.add(pingBtn);
        banner.add(statusBox, BorderLayout.EAST);

        container.add(banner, BorderLayout.NORTH);

        // KPI Summary Cards Bar
        JPanel cardsBar = new JPanel(new GridLayout(1, 3, 16, 0));
        cardsBar.setBackground(new Color(243, 246, 251));
        cardsBar.setBorder(new EmptyBorder(12, 24, 8, 24));

        totalConversionsLabel = new JLabel("0", JLabel.CENTER);
        totalRatesLabel = new JLabel("0", JLabel.CENTER);
        dbHostLabel = new JLabel("localhost:3306", JLabel.CENTER);

        cardsBar.add(createKpiCard("📊 Total Conversions Logged", totalConversionsLabel, new Color(59, 130, 246)));
        cardsBar.add(createKpiCard("💱 Exchange Rates Stored", totalRatesLabel, new Color(16, 185, 129)));
        cardsBar.add(createKpiCard("🗄️ MySQL Host & Port", dbHostLabel, new Color(139, 92, 246)));

        container.add(cardsBar, BorderLayout.SOUTH);
        return container;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(new Color(100, 116, 139));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    // ==================== TAB 1: CONVERSION HISTORY ====================

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(new Color(243, 246, 251));
        panel.setBorder(new EmptyBorder(14, 20, 14, 20));

        // History Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        // Search Filter
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBox.setOpaque(false);
        JLabel searchIcon = new JLabel("🔍 Search:");
        searchIcon.setFont(new Font("Segoe UI", Font.BOLD, 13));

        historySearchField = new JTextField(18);
        historySearchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        historySearchField.setToolTipText("Filter by currency (e.g. USD, INR) or amount");
        historySearchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyHistoryFilter();
            }
        });

        historyCountLabel = new JLabel("Loading records...");
        historyCountLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        historyCountLabel.setForeground(new Color(100, 116, 139));

        searchBox.add(searchIcon);
        searchBox.add(historySearchField);
        searchBox.add(historyCountLabel);
        toolbar.add(searchBox, BorderLayout.WEST);

        // Action Buttons
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionBox.setOpaque(false);

        JButton refreshBtn = createIconButton("🔄 Refresh", new Color(59, 130, 246), Color.WHITE);
        refreshBtn.addActionListener(e -> loadConversionHistory());

        JButton deleteSelectedBtn = createIconButton("🗑️ Delete Selected", new Color(239, 68, 68), Color.WHITE);
        deleteSelectedBtn.addActionListener(e -> deleteSelectedHistoryRow());

        JButton clearAllBtn = createIconButton("⚠️ Clear All", new Color(220, 38, 38), Color.WHITE);
        clearAllBtn.addActionListener(e -> clearAllHistory());

        actionBox.add(refreshBtn);
        actionBox.add(deleteSelectedBtn);
        actionBox.add(clearAllBtn);
        toolbar.add(actionBox, BorderLayout.EAST);

        panel.add(toolbar, BorderLayout.NORTH);

        // Table Model
        String[] columns = {"ID", "From", "To", "Original Amount", "Converted Amount", "Date & Time"};
        historyTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JTable(historyTableModel);
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        historyTable.setRowHeight(28);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.setShowVerticalLines(false);
        historyTable.setGridColor(new Color(241, 245, 249));

        historySorter = new TableRowSorter<>(historyTableModel);
        historyTable.setRowSorter(historySorter);

        styleTableHeader(historyTable.getTableHeader());
        applyTableAlignment(historyTable);

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void applyHistoryFilter() {
        String query = historySearchField.getText().trim();
        if (query.isEmpty()) {
            historySorter.setRowFilter(null);
        } else {
            historySorter.setRowFilter(RowFilter.regexFilter("(?i)" + query));
        }
        int count = historyTable.getRowCount();
        historyCountLabel.setText("Showing " + count + " entries");
    }

    private void loadConversionHistory() {
        historyTableModel.setRowCount(0);
        try {
            List<Object[]> rows = DatabaseHelper.getConversionHistoryRows();
            DecimalFormat df = new DecimalFormat("#,##0.00");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (Object[] row : rows) {
                Long id = (Long) row[0];
                String from = (String) row[1];
                String to = (String) row[2];
                Number orig = (Number) row[3];
                Number conv = (Number) row[4];
                Object timestamp = row[5];

                historyTableModel.addRow(new Object[]{
                    id,
                    from,
                    to,
                    df.format(orig),
                    df.format(conv),
                    timestamp != null ? sdf.format(timestamp) : ""
                });
            }
            historyCountLabel.setText("Showing " + rows.size() + " entries");
            totalConversionsLabel.setText(String.valueOf(rows.size()));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load conversion history: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedHistoryRow() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a conversion record to delete.",
                    "Select Record", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = historyTable.convertRowIndexToModel(selectedRow);
        Long id = (Long) historyTableModel.getValueAt(modelRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete conversion record #" + id + "?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DatabaseHelper.deleteConversionHistoryRow(id);
                loadConversionHistory();
                JOptionPane.showMessageDialog(this, "Record #" + id + " deleted successfully.",
                        "Deleted", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Failed to delete record: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearAllHistory() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Warning: This will permanently delete ALL conversion history records from MySQL!\nContinue?",
                "Confirm Clear All", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DatabaseHelper.clearConversionHistory();
                loadConversionHistory();
                JOptionPane.showMessageDialog(this, "All conversion history has been cleared.",
                        "Cleared", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Failed to clear history: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ==================== TAB 2: EXCHANGE RATES MANAGER ====================

    private JPanel createRatesPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setBackground(new Color(243, 246, 251));
        panel.setBorder(new EmptyBorder(14, 20, 14, 20));

        // Left Form Panel: Add / Update Rate
        JPanel formCard = new JPanel(new BorderLayout());
        formCard.setBackground(Color.WHITE);
        formCard.setPreferredSize(new Dimension(340, 0));
        formCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JLabel formTitle = new JLabel("➕ Add / Update Exchange Rate");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        formTitle.setForeground(new Color(30, 41, 59));
        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel formFields = new JPanel(new GridLayout(7, 1, 0, 8));
        formFields.setOpaque(false);
        formFields.setBorder(new EmptyBorder(14, 0, 14, 0));

        JLabel fromLbl = new JLabel("From Currency:");
        fromLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fromCombo = new JComboBox<>(CURRENCIES);
        fromCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel toLbl = new JLabel("To Currency:");
        toLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        toCombo = new JComboBox<>(CURRENCIES);
        toCombo.setSelectedItem("INR");
        toCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel rateLbl = new JLabel("Exchange Rate:");
        rateLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rateInputField = new JTextField();
        rateInputField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rateInputField.setToolTipText("e.g. 87.56");

        formFields.add(fromLbl);
        formFields.add(fromCombo);
        formFields.add(toLbl);
        formFields.add(toCombo);
        formFields.add(rateLbl);
        formFields.add(rateInputField);

        JButton saveRateBtn = createIconButton("💾 Save Rate to Database", new Color(16, 185, 129), Color.WHITE);
        saveRateBtn.setPreferredSize(new Dimension(0, 38));
        saveRateBtn.addActionListener(e -> saveOrUpdateRateAction());

        formStatusLabel = new JLabel("Enter a rate and click save.", JLabel.CENTER);
        formStatusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        formStatusLabel.setForeground(new Color(100, 116, 139));

        JPanel formBottom = new JPanel(new GridLayout(3, 1, 0, 8));
        formBottom.setOpaque(false);
        formBottom.add(saveRateBtn);
        formBottom.add(formStatusLabel);

        JButton resetDefaultsBtn = createIconButton("↺ Restore Default Rates", new Color(245, 158, 11), Color.WHITE);
        resetDefaultsBtn.addActionListener(e -> resetDefaultRatesAction());
        formBottom.add(resetDefaultsBtn);

        formCard.add(formFields, BorderLayout.CENTER);
        formCard.add(formBottom, BorderLayout.SOUTH);

        panel.add(formCard, BorderLayout.WEST);

        // Right Table Panel: Rates List
        JPanel tableContainer = new JPanel(new BorderLayout(0, 10));
        tableContainer.setOpaque(false);

        // Rates Toolbar
        JPanel ratesToolbar = new JPanel(new BorderLayout());
        ratesToolbar.setOpaque(false);

        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBox.setOpaque(false);
        JLabel searchLbl = new JLabel("🔍 Filter Rates:");
        searchLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));

        ratesSearchField = new JTextField(12);
        ratesSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ratesSearchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String q = ratesSearchField.getText().trim();
                if (q.isEmpty()) {
                    ratesSorter.setRowFilter(null);
                } else {
                    ratesSorter.setRowFilter(RowFilter.regexFilter("(?i)" + q));
                }
            }
        });

        searchBox.add(searchLbl);
        searchBox.add(ratesSearchField);
        ratesToolbar.add(searchBox, BorderLayout.WEST);

        JPanel rateActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rateActions.setOpaque(false);

        JButton refreshRatesBtn = createIconButton("🔄 Refresh Rates", new Color(59, 130, 246), Color.WHITE);
        refreshRatesBtn.addActionListener(e -> loadExchangeRates());

        JButton deleteRateBtn = createIconButton("🗑️ Delete Rate", new Color(239, 68, 68), Color.WHITE);
        deleteRateBtn.addActionListener(e -> deleteSelectedRate());

        rateActions.add(refreshRatesBtn);
        rateActions.add(deleteRateBtn);
        ratesToolbar.add(rateActions, BorderLayout.EAST);

        tableContainer.add(ratesToolbar, BorderLayout.NORTH);

        // Rates Table
        String[] columns = {"From Currency", "To Currency", "Exchange Rate"};
        ratesTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ratesTable = new JTable(ratesTableModel);
        ratesTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ratesTable.setRowHeight(26);
        ratesTable.setShowVerticalLines(false);
        ratesTable.setGridColor(new Color(241, 245, 249));

        ratesSorter = new TableRowSorter<>(ratesTableModel);
        ratesTable.setRowSorter(ratesSorter);

        styleTableHeader(ratesTable.getTableHeader());
        applyTableAlignment(ratesTable);

        // Click row to populate form
        ratesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = ratesTable.getSelectedRow();
                if (row != -1) {
                    int modelRow = ratesTable.convertRowIndexToModel(row);
                    fromCombo.setSelectedItem(ratesTableModel.getValueAt(modelRow, 0));
                    toCombo.setSelectedItem(ratesTableModel.getValueAt(modelRow, 1));
                    rateInputField.setText(String.valueOf(ratesTableModel.getValueAt(modelRow, 2)));
                }
            }
        });

        JScrollPane ratesScroll = new JScrollPane(ratesTable);
        ratesScroll.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));
        tableContainer.add(ratesScroll, BorderLayout.CENTER);

        panel.add(tableContainer, BorderLayout.CENTER);
        return panel;
    }

    private void saveOrUpdateRateAction() {
        String from = (String) fromCombo.getSelectedItem();
        String to = (String) toCombo.getSelectedItem();
        String rateText = rateInputField.getText().trim();

        if (from == null || to == null || from.equals(to)) {
            formStatusLabel.setForeground(Color.RED);
            formStatusLabel.setText("Please select two different currencies.");
            return;
        }

        double rate;
        try {
            rate = Double.parseDouble(rateText);
            if (rate <= 0) {
                formStatusLabel.setForeground(Color.RED);
                formStatusLabel.setText("Rate must be greater than 0.");
                return;
            }
        } catch (NumberFormatException ex) {
            formStatusLabel.setForeground(Color.RED);
            formStatusLabel.setText("Invalid rate number.");
            return;
        }

        try {
            DatabaseHelper.addOrUpdateRate(from, to, rate);
            formStatusLabel.setForeground(new Color(16, 185, 129));
            formStatusLabel.setText("Saved " + from + " -> " + to + " (" + rate + ") in MySQL!");

            // Refresh table
            loadExchangeRates();

            // Notify parent converter to refresh its rates immediately
            if (parentConverter != null) {
                parentConverter.reloadRates();
            }
        } catch (SQLException ex) {
            formStatusLabel.setForeground(Color.RED);
            formStatusLabel.setText("SQL Error: " + ex.getMessage());
        }
    }

    private void deleteSelectedRate() {
        int selected = ratesTable.getSelectedRow();
        if (selected == -1) {
            JOptionPane.showMessageDialog(this, "Please select an exchange rate row to delete.",
                    "Select Rate", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = ratesTable.convertRowIndexToModel(selected);
        String from = (String) ratesTableModel.getValueAt(modelRow, 0);
        String to = (String) ratesTableModel.getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete exchange rate for " + from + " <-> " + to + " from MySQL?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DatabaseHelper.deleteRate(from, to);
                loadExchangeRates();
                if (parentConverter != null) {
                    parentConverter.reloadRates();
                }
                JOptionPane.showMessageDialog(this, "Rate deleted successfully.",
                        "Deleted", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Failed to delete rate: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void resetDefaultRatesAction() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Reset all exchange rates in MySQL to standard default rates?",
                "Reset Rates", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DatabaseHelper.resetDefaultRates();
                loadExchangeRates();
                if (parentConverter != null) {
                    parentConverter.reloadRates();
                }
                JOptionPane.showMessageDialog(this, "Default rates restored successfully.",
                        "Restored", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Failed to restore defaults: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadExchangeRates() {
        ratesTableModel.setRowCount(0);
        try {
            List<Object[]> rows = DatabaseHelper.getExchangeRateRows();
            for (Object[] row : rows) {
                ratesTableModel.addRow(row);
            }
            totalRatesLabel.setText(String.valueOf(rows.size()));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load exchange rates: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== TAB 3: DIAGNOSTICS ====================

    private JPanel createDiagnosticsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(new Color(243, 246, 251));
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel topCard = new JPanel(new GridLayout(2, 2, 12, 12));
        topCard.setBackground(Color.WHITE);
        topCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        topCard.add(createDiagRow("JDBC URL", "jdbc:mysql://localhost:3306/currency_db"));
        topCard.add(createDiagRow("Database Name", "currency_db"));
        topCard.add(createDiagRow("Driver Class", "com.mysql.cj.jdbc.Driver"));
        topCard.add(createDiagRow("Database User", "root"));

        panel.add(topCard, BorderLayout.NORTH);

        diagnosticsArea = new JTextArea();
        diagnosticsArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        diagnosticsArea.setEditable(false);
        diagnosticsArea.setBackground(new Color(30, 41, 59));
        diagnosticsArea.setForeground(new Color(226, 232, 240));
        diagnosticsArea.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane logScroll = new JScrollPane(diagnosticsArea);
        logScroll.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));
        panel.add(logScroll, BorderLayout.CENTER);

        JPanel diagActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        diagActions.setOpaque(false);

        JButton runHealthBtn = createIconButton("🧪 Run Full Health Check", new Color(79, 70, 229), Color.WHITE);
        runHealthBtn.addActionListener(e -> runHealthCheck());

        diagActions.add(runHealthBtn);
        panel.add(diagActions, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createDiagRow(String key, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel k = new JLabel(key);
        k.setFont(new Font("Segoe UI", Font.BOLD, 12));
        k.setForeground(new Color(100, 116, 139));

        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        v.setForeground(new Color(30, 41, 59));

        p.add(k, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private void runHealthCheck() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DATABASE CONNECTIVITY HEALTH CHECK ===\n");
        sb.append("Timestamp: ").append(new java.util.Date()).append("\n\n");

        long start = System.currentTimeMillis();
        boolean ok = DatabaseHelper.testConnection();
        long ping = System.currentTimeMillis() - start;

        if (ok) {
            sb.append("✔ [PASS] Connection to MySQL Server: SUCCESSFUL\n");
            sb.append("✔ [PASS] Ping Latency: ").append(ping).append(" ms\n");
            sb.append("✔ [PASS] Target Database: currency_db (Accessible)\n");
            try {
                int convCount = DatabaseHelper.getConversionHistoryCount();
                int rateCount = DatabaseHelper.getExchangeRateCount();
                sb.append("✔ [PASS] Table `conversion_history`: FOUND (").append(convCount).append(" records)\n");
                sb.append("✔ [PASS] Table `exchange_rates`: FOUND (").append(rateCount).append(" pairs stored)\n");
                sb.append("\nSTATUS: ALL SYSTEMS OPERATIONAL AND CONNECTED.");
            } catch (SQLException ex) {
                sb.append("✖ [WARN] Table query error: ").append(ex.getMessage()).append("\n");
            }
        } else {
            sb.append("✖ [FAIL] Could not establish connection to MySQL server at localhost:3306.\n");
            sb.append("  Please check that the MySQL service is running and credentials are correct.\n");
        }

        diagnosticsArea.setText(sb.toString());
    }

    // ==================== FOOTER ====================

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, false),
                new EmptyBorder(10, 24, 10, 24)
        ));

        JLabel note = new JLabel("● Direct MySQL Synchronized Mode (JDBC mysql-connector-j-9.4.0)");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        note.setForeground(new Color(100, 116, 139));
        footer.add(note, BorderLayout.WEST);

        JButton closeBtn = createIconButton("Close Dashboard", new Color(100, 116, 139), Color.WHITE);
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn, BorderLayout.EAST);

        return footer;
    }

    // ==================== ACTIONS & HELPERS ====================

    public void refreshAllData() {
        testConnectionAction();
        loadConversionHistory();
        loadExchangeRates();
        runHealthCheck();
    }

    private void testConnectionAction() {
        boolean connected = DatabaseHelper.testConnection();
        if (connected) {
            statusBadgeLabel.setText("● Connected (MySQL 8.0)");
            statusBadgeLabel.setBackground(new Color(20, 83, 45));
            statusBadgeLabel.setForeground(new Color(134, 239, 172));
        } else {
            statusBadgeLabel.setText("● Disconnected");
            statusBadgeLabel.setBackground(new Color(127, 29, 29));
            statusBadgeLabel.setForeground(new Color(254, 202, 202));
        }
    }

    private JButton createIconButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(bg.darker(), 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bg.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    private void styleTableHeader(JTableHeader header) {
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(new Color(51, 65, 85));
        header.setPreferredSize(new Dimension(0, 32));
    }

    private void applyTableAlignment(JTable table) {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    public DatabaseDashboardFrame() {
        this(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DatabaseDashboardFrame frame = new DatabaseDashboardFrame();
            frame.setVisible(true);
        });
    }
}
