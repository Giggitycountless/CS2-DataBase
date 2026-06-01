package com.counterstrike.app.repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Metadata for one table: its display name, real table name, ordered columns,
 * and an ORDER BY clause for browsing. Drives the generic CRUD repository + UI.
 */
public record TableSpec(
        String displayName,
        String tableName,
        List<ColumnSpec> columns,
        String orderBy
) {
    public List<ColumnSpec> primaryKey() {
        List<ColumnSpec> keys = new ArrayList<>();
        for (ColumnSpec column : columns) {
            if (column.primaryKey()) {
                keys.add(column);
            }
        }
        return keys;
    }

    /** Columns the user edits when inserting (everything). */
    public List<ColumnSpec> insertableColumns() {
        return columns;
    }

    /** Columns the user edits when updating (non-primary-key columns). */
    public List<ColumnSpec> editableColumns() {
        List<ColumnSpec> editable = new ArrayList<>();
        for (ColumnSpec column : columns) {
            if (!column.primaryKey()) {
                editable.add(column);
            }
        }
        return editable;
    }

    public ColumnSpec column(String name) {
        for (ColumnSpec column : columns) {
            if (column.name().equalsIgnoreCase(name)) {
                return column;
            }
        }
        throw new IllegalArgumentException("Unknown column " + name + " in " + tableName);
    }

    public String[] columnLabels() {
        String[] labels = new String[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            labels[i] = columns.get(i).label();
        }
        return labels;
    }
}
