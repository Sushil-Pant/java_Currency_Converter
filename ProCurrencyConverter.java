import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.*;



public class ProCurrencyConverter extends JFrame {

    private final HashMap<String, Double> rates;

    private JComboBox<String> fromCurrency, toCurrency;
    private JTextField amountField;
    private JLabel resultLabel;
    private JButton convertButton, clearButton;

    public ProCurrencyConverter() {
        rates = DatabaseHelper.loadRates();
        setTitle("Currency Converter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(560, 530);
        setLocationRelativeTo(null);
        setResizable(false);

       	
        JPanel panel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(54, 0, 179),
                    getWidth(), getHeight(), new Color(0, 204, 255)
                );
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setLayout(null);

        JLabel title = new JLabel("Currency Converter", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        title.setBounds(60, 25, 400, 40);
        panel.add(title);

        JLabel amountLbl = new JLabel("Enter Amount:");
        amountLbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        amountLbl.setForeground(Color.WHITE);
        amountLbl.setBounds(60, 100, 130, 30);
        panel.add(amountLbl);

        amountField = new JTextField();
        amountField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        amountField.setBounds(200, 100, 220, 35);
        panel.add(amountField);

        JLabel fromLbl = new JLabel("From Currency:");
        fromLbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        fromLbl.setForeground(Color.WHITE);
        fromLbl.setBounds(60, 160, 130, 30);
        panel.add(fromLbl);

        String[] currencies = {
            "USD", "INR", "EUR", "GBP", "JPY", "CAD", "AUD",
            "CHF", "CNY", "AED", "SAR", "PKR", "NPR", "KRW", "ZAR"
        };

        fromCurrency = new JComboBox<>(currencies);
        fromCurrency.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        fromCurrency.setBounds(200, 160, 220, 35);
        panel.add(fromCurrency);

        JLabel toLbl = new JLabel("To Currency:");
        toLbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        toLbl.setForeground(Color.WHITE);
        toLbl.setBounds(60, 220, 130, 30);
        panel.add(toLbl);

        toCurrency = new JComboBox<>(currencies);
        toCurrency.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        toCurrency.setBounds(200, 220, 220, 35);
        panel.add(toCurrency);

        convertButton = new JButton("Convert");
        styleButton(convertButton, new Color(0, 255, 200), new Color(0, 130, 255));
        convertButton.setBounds(120, 300, 120, 45);
        panel.add(convertButton);

        clearButton = new JButton("Clear");
        styleButton(clearButton, new Color(255, 80, 80), new Color(255, 0, 0));
        clearButton.setBounds(260, 300, 120, 45);
        panel.add(clearButton);

        JButton databaseButton = new JButton("Database Dashboard");
        styleButton(databaseButton, new Color(255, 220, 100), new Color(120, 80, 0));
        databaseButton.setBounds(180, 365, 180, 40);
        panel.add(databaseButton);

        resultLabel = new JLabel("", JLabel.CENTER);
        resultLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 18));
        resultLabel.setForeground(Color.WHITE);
        resultLabel.setBounds(60, 420, 440, 35);
        panel.add(resultLabel);

        convertButton.addActionListener(e -> convertCurrency());
        clearButton.addActionListener(e -> clearFields());
        databaseButton.addActionListener(e -> showDatabaseViewer());

        add(panel);
    }

    private void styleButton(JButton button, Color hover, Color base) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setBackground(Color.WHITE);
        button.setForeground(base);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(base, 2, true));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
                button.setForeground(Color.WHITE);
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.WHITE);
                button.setForeground(base);
            }
        });
    }

    private void convertCurrency() {
        try {
            double amount = Double.parseDouble(amountField.getText());
            String from = (String) fromCurrency.getSelectedItem();
            String to = (String) toCurrency.getSelectedItem();

            if (from.equals(to)) {
                resultLabel.setText("Same currency selected!");
                return;
            }

            String key = from + "_" + to;
            if (rates.containsKey(key)) {
                double converted = amount * rates.get(key);
                resultLabel.setText(String.format("%.2f %s = %.2f %s", amount, from, converted, to));
                
                // Save the conversion to the database
                DatabaseHelper.saveConversionHistory(amount, from, converted, to);
            } else {
                resultLabel.setText(" Conversion rate not available.");
            }
        } catch (NumberFormatException ex) {
            resultLabel.setText(" Please enter a valid amount!");
        }
    }

    private void clearFields() {
        amountField.setText("");
        fromCurrency.setSelectedIndex(0);
        toCurrency.setSelectedIndex(0);
        resultLabel.setText("");
    }

    private DatabaseDashboardFrame dashboardFrame;

    public void reloadRates() {
        rates.clear();
        rates.putAll(DatabaseHelper.loadRates());
        System.out.println("Rates refreshed in ProCurrencyConverter.");
    }

    private void showDatabaseViewer() {
        if (dashboardFrame == null || !dashboardFrame.isDisplayable()) {
            dashboardFrame = new DatabaseDashboardFrame(this);
        }
        dashboardFrame.refreshAllData();
        dashboardFrame.setVisible(true);
        dashboardFrame.toFront();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ProCurrencyConverter frame = new ProCurrencyConverter();
            frame.setVisible(true);
        });
    }
}
