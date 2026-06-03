package com.counterstrike.app.ui;

import com.counterstrike.app.repository.TableData;

import javax.swing.table.AbstractTableModel;

public final class ReadOnlyTableModel extends AbstractTableModel {
    private String[] columns = new String[0];
    private Object[][] rows = new Object[0][0];

    public void setData(TableData data) {
        columns = data.columns();
        rows = data.rows().toArray(Object[][]::new);
        fireTableStructureChanged();
    }

    @Override
    public int getRowCount() {
        return rows.length;
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows[rowIndex][columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        for (Object[] row : rows) {
            if (row[columnIndex] != null) {
                return row[columnIndex].getClass();
            }
        }
        return Object.class;
    }
}
