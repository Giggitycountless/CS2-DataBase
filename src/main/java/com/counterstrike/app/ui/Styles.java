package com.counterstrike.app.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Shared widget styling so the new pages match the main browser's dark theme. */
final class Styles {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    static JTable styledTable() {
        JTable table = new JTable(new ReadOnlyTableModel());
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(MainFrame.BG_CARD);
        table.setForeground(MainFrame.TEXT_PRIMARY);
        table.setSelectionBackground(MainFrame.ACCENT_DIM);
        table.setSelectionForeground(MainFrame.TEXT_PRIMARY);

        DefaultTableCellRenderer styledRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Object display = (value instanceof Date date) ? DATE_FORMAT.format(date) : value;
                Component c = super.getTableCellRendererComponent(tbl, display, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? MainFrame.BG_CARD : MainFrame.TABLE_ALT);
                }
                setHorizontalAlignment(LEFT);
                setBorder(new EmptyBorder(4, 12, 4, 12));
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, styledRenderer);
        table.setDefaultRenderer(Number.class, styledRenderer);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setBackground(MainFrame.TABLE_HEADER_BG);
        header.setForeground(MainFrame.TEXT_MUTED);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 12f));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, MainFrame.ACCENT_DIM));
        return table;
    }

    static JButton accentButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(MainFrame.ACCENT_DIM);
        button.setForeground(MainFrame.TEXT_PRIMARY);
        button.setFocusPainted(false);
        return button;
    }

    /** Format a raw cell value for prefilling a form field (dates as YYYY-MM-DD). */
    static String asEditText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Date date) {
            return DATE_FORMAT.format(date);
        }
        return String.valueOf(value);
    }

    private Styles() {
    }
}
