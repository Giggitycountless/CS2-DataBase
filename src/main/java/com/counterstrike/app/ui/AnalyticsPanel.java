package com.counterstrike.app.ui;

import com.counterstrike.app.repository.AnalyticsCatalog;
import com.counterstrike.app.repository.AnalyticsQuery;
import com.counterstrike.app.repository.CounterStrikeRepository;
import com.counterstrike.app.repository.TableData;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/** Runs curated, read-only analytical queries (joins, aggregation, sub-queries). */
final class AnalyticsPanel extends JPanel {

    private final CounterStrikeRepository repository;
    private final JComboBox<AnalyticsQuery> querySelect = new JComboBox<>();
    private final JTextField paramField = new JTextField(18);
    private final JLabel paramLabel = new JLabel();
    private final JLabel descriptionLabel = new JLabel();
    private final JTable table = Styles.styledTable();
    private final JLabel statusLabel = new JLabel("Pick a query and press Run");

    AnalyticsPanel(CounterStrikeRepository repository) {
        this.repository = repository;
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(16, 24, 16, 24));
        setBackground(MainFrame.BG_DARKER);

        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        statusLabel.setForeground(MainFrame.TEXT_MUTED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
        footer.add(statusLabel, BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);

        for (AnalyticsQuery query : AnalyticsCatalog.QUERIES) {
            querySelect.addItem(query);
        }
        querySelect.addActionListener(e -> onQuerySelected());
        onQuerySelected();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 10));
        header.setOpaque(false);

        JLabel title = new JLabel("Advanced Queries");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(MainFrame.TEXT_PRIMARY);
        header.add(title, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        JLabel queryLabel = new JLabel("Query");
        queryLabel.setForeground(MainFrame.TEXT_MUTED);
        querySelect.setPreferredSize(new Dimension(320, 30));
        paramLabel.setForeground(MainFrame.TEXT_MUTED);
        paramLabel.setBorder(new EmptyBorder(0, 12, 0, 0));
        JButton runButton = Styles.accentButton("Run");
        runButton.addActionListener(e -> run());
        controls.add(queryLabel);
        controls.add(querySelect);
        controls.add(paramLabel);
        controls.add(paramField);
        controls.add(runButton);

        descriptionLabel.setForeground(MainFrame.TEXT_MUTED);
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(12f));
        descriptionLabel.setBorder(new EmptyBorder(8, 2, 0, 0));

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(controls, BorderLayout.NORTH);
        south.add(descriptionLabel, BorderLayout.SOUTH);
        header.add(south, BorderLayout.SOUTH);
        return header;
    }

    private void onQuerySelected() {
        AnalyticsQuery query = (AnalyticsQuery) querySelect.getSelectedItem();
        if (query == null) {
            return;
        }
        descriptionLabel.setText(query.description());
        boolean hasParam = query.hasParameter();
        paramLabel.setText(hasParam ? query.paramLabel() + ":" : "");
        paramLabel.setVisible(hasParam);
        paramField.setVisible(hasParam);
        paramField.setText("");
    }

    private void run() {
        AnalyticsQuery query = (AnalyticsQuery) querySelect.getSelectedItem();
        if (query == null) {
            return;
        }
        String parameter = query.hasParameter() ? paramField.getText() : "";
        statusLabel.setText("Running: " + query.title() + "...");
        new SwingWorker<TableData, Void>() {
            @Override
            protected TableData doInBackground() throws Exception {
                return repository.runAnalytics(query, parameter);
            }

            @Override
            protected void done() {
                try {
                    TableData data = get();
                    ((ReadOnlyTableModel) table.getModel()).setData(data);
                    statusLabel.setText(query.title() + " | rows: " + data.rowCount());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    statusLabel.setText("Error: " + cause.getMessage());
                    JOptionPane.showMessageDialog(AnalyticsPanel.this, cause.getMessage(),
                            "Query Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
