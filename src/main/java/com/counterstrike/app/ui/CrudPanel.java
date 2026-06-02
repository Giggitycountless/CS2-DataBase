package com.counterstrike.app.ui;

import com.counterstrike.app.repository.AppRepository;
import com.counterstrike.app.repository.ColumnSpec;
import com.counterstrike.app.repository.Schema;
import com.counterstrike.app.repository.TableData;
import com.counterstrike.app.repository.TableSpec;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic, metadata-driven management page. A table selector chooses which of the
 * schema's tables to edit; Add / Edit / Delete operate through forms generated
 * from each table's {@link TableSpec}.
 */
final class CrudPanel extends JPanel {

    private final AppRepository repository;
    private final JComboBox<TableSpec> tableSelect = new JComboBox<>();
    private final JTextField searchField = new JTextField();
    private final JTable table = Styles.styledTable();
    private final JLabel statusLabel = new JLabel("Ready");

    private TableSpec currentSpec;

    CrudPanel(AppRepository repository) {
        this.repository = repository;
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(16, 24, 16, 24));
        setBackground(MainFrame.BG_DARKER);

        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        for (TableSpec spec : Schema.ALL) {
            tableSelect.addItem(spec);
        }
        tableSelect.setRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value == null ? "" : value.displayName());
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(4, 8, 4, 8));
            label.setBackground(selected ? MainFrame.ACCENT_DIM : MainFrame.BG_CARD);
            label.setForeground(MainFrame.TEXT_PRIMARY);
            return label;
        });
        tableSelect.addActionListener(e -> {
            currentSpec = (TableSpec) tableSelect.getSelectedItem();
            reload();
        });

        currentSpec = Schema.ALL.get(0);
        tableSelect.setSelectedIndex(0);
        reload();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 10));
        header.setOpaque(false);

        JLabel title = new JLabel("Manage Data");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(MainFrame.TEXT_PRIMARY);
        header.add(title, BorderLayout.NORTH);

        JPanel controls = new JPanel(new BorderLayout(8, 0));
        controls.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel tableLabel = new JLabel("Table");
        tableLabel.setForeground(MainFrame.TEXT_MUTED);
        tableSelect.setPreferredSize(new Dimension(220, 30));
        left.add(tableLabel);
        left.add(tableSelect);

        JPanel search = new JPanel(new BorderLayout(8, 0));
        search.setOpaque(false);
        JLabel searchLabel = new JLabel("Search");
        searchLabel.setForeground(MainFrame.TEXT_MUTED);
        searchLabel.setBorder(new EmptyBorder(0, 12, 0, 6));
        search.add(searchLabel, BorderLayout.WEST);
        search.add(searchField, BorderLayout.CENTER);
        searchField.addActionListener(e -> reload());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton addButton = Styles.accentButton("Add");
        JButton editButton = Styles.accentButton("Edit");
        JButton deleteButton = Styles.accentButton("Delete");
        JButton refreshButton = Styles.accentButton("Refresh");
        addButton.addActionListener(e -> openForm(false));
        editButton.addActionListener(e -> openForm(true));
        deleteButton.addActionListener(e -> deleteSelected());
        refreshButton.addActionListener(e -> reload());
        actions.add(addButton);
        actions.add(editButton);
        actions.add(deleteButton);
        actions.add(refreshButton);

        controls.add(left, BorderLayout.WEST);
        controls.add(search, BorderLayout.CENTER);
        controls.add(actions, BorderLayout.EAST);
        header.add(controls, BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        statusLabel.setForeground(MainFrame.TEXT_MUTED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
        footer.add(statusLabel, BorderLayout.WEST);
        return footer;
    }

    // ── Data loading ──
    private void reload() {
        if (currentSpec == null) {
            return;
        }
        TableSpec spec = currentSpec;
        String search = searchField.getText();
        statusLabel.setText("Loading " + spec.displayName() + "...");
        new SwingWorker<TableData, Void>() {
            @Override
            protected TableData doInBackground() throws Exception {
                return repository.browse(spec, search);
            }

            @Override
            protected void done() {
                try {
                    TableData data = get();
                    ((ReadOnlyTableModel) table.getModel()).setData(data);
                    statusLabel.setText(spec.displayName() + " | rows: " + data.rowCount());
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        }.execute();
    }

    // ── Add / Edit ──
    private void openForm(boolean edit) {
        TableSpec spec = currentSpec;
        Map<String, String> existing = new LinkedHashMap<>();
        Map<String, String> primaryKey = new LinkedHashMap<>();

        if (edit) {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                JOptionPane.showMessageDialog(this, "Select a row to edit first.",
                        "No Row Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = table.convertRowIndexToModel(viewRow);
            List<ColumnSpec> columns = spec.columns();
            for (int i = 0; i < columns.size(); i++) {
                String value = Styles.asEditText(table.getModel().getValueAt(modelRow, i));
                existing.put(columns.get(i).name(), value);
                if (columns.get(i).primaryKey()) {
                    primaryKey.put(columns.get(i).name(), value);
                }
            }
        }

        // Foreign-key option lists are loaded off the EDT, then the dialog is shown.
        statusLabel.setText("Preparing form...");
        new SwingWorker<Map<String, List<String>>, Void>() {
            @Override
            protected Map<String, List<String>> doInBackground() throws Exception {
                Map<String, List<String>> options = new LinkedHashMap<>();
                for (ColumnSpec column : spec.columns()) {
                    if (column.foreignKey() != null) {
                        options.put(column.name(), repository.distinctValues(
                                column.foreignKey().table(), column.foreignKey().column()));
                    }
                }
                return options;
            }

            @Override
            protected void done() {
                try {
                    showForm(spec, edit, existing, primaryKey, get());
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        }.execute();
    }

    private void showForm(TableSpec spec, boolean edit, Map<String, String> existing,
                          Map<String, String> primaryKey, Map<String, List<String>> fkOptions) {
        statusLabel.setText("Ready");
        JPanel form = new JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gc = new java.awt.GridBagConstraints();
        gc.insets = new java.awt.Insets(4, 4, 4, 4);
        gc.anchor = java.awt.GridBagConstraints.WEST;

        Map<String, JComponent> fields = new LinkedHashMap<>();
        int row = 0;
        for (ColumnSpec column : spec.columns()) {
            gc.gridx = 0;
            gc.gridy = row;
            gc.weightx = 0;
            gc.fill = java.awt.GridBagConstraints.NONE;
            String labelText = column.label()
                    + (column.required() ? " *" : "")
                    + (column.isDate() ? " (YYYY-MM-DD)" : "");
            form.add(new JLabel(labelText), gc);

            gc.gridx = 1;
            gc.weightx = 1;
            gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
            String initial = existing.getOrDefault(column.name(), "");
            boolean locked = edit && column.primaryKey();

            JComponent field;
            if (column.foreignKey() != null && !locked) {
                JComboBox<String> combo = new JComboBox<>();
                combo.setEditable(true);
                if (!column.required()) {
                    combo.addItem("");
                }
                for (String option : fkOptions.getOrDefault(column.name(), List.of())) {
                    combo.addItem(option);
                }
                combo.setSelectedItem(initial);
                field = combo;
            } else {
                JTextField text = new JTextField(initial, 22);
                text.setEnabled(!locked);
                field = text;
            }
            field.setPreferredSize(new Dimension(260, 28));
            form.add(field, gc);
            fields.put(column.name(), field);
            row++;
        }

        String title = (edit ? "Edit " : "Add ") + spec.displayName();
        int choice = JOptionPane.showConfirmDialog(this, form, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        Map<String, String> values = new LinkedHashMap<>();
        for (ColumnSpec column : spec.columns()) {
            values.put(column.name(), readComponent(fields.get(column.name())));
        }
        save(spec, edit, values, primaryKey);
    }

    private void save(TableSpec spec, boolean edit, Map<String, String> values, Map<String, String> primaryKey) {
        statusLabel.setText(edit ? "Updating..." : "Inserting...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (edit) {
                    repository.updateRow(spec, primaryKey, values);
                } else {
                    repository.insertRow(spec, values);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText(edit ? "Row updated." : "Row added.");
                    reload();
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        }.execute();
    }

    // ── Delete ──
    private void deleteSelected() {
        TableSpec spec = currentSpec;
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to delete first.",
                    "No Row Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        List<ColumnSpec> columns = spec.columns();
        Map<String, String> primaryKey = new LinkedHashMap<>();
        StringBuilder describe = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).primaryKey()) {
                String value = Styles.asEditText(table.getModel().getValueAt(modelRow, i));
                primaryKey.put(columns.get(i).name(), value);
                if (describe.length() > 0) {
                    describe.append(", ");
                }
                describe.append(columns.get(i).label()).append(" = ").append(value);
            }
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this " + spec.displayName() + " row?\n" + describe + "\n\nThis cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        statusLabel.setText("Deleting...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                repository.deleteRow(spec, primaryKey);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("Row deleted.");
                    reload();
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        }.execute();
    }

    // ── Helpers ──
    private static String readComponent(JComponent component) {
        if (component instanceof JComboBox<?> combo) {
            Object item = combo.getSelectedItem();
            return item == null ? "" : item.toString().trim();
        }
        if (component instanceof JTextField text) {
            return text.getText().trim();
        }
        return "";
    }

    private void showError(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        statusLabel.setText("Error: " + cause.getMessage());
        JOptionPane.showMessageDialog(this, cause.getMessage(), "Operation Failed", JOptionPane.ERROR_MESSAGE);
    }
}
