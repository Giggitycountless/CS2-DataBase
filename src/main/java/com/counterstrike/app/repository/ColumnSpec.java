package com.counterstrike.app.repository;

/**
 * Describes a single column of a database table: how it is typed, whether it is
 * part of the primary key, whether a value is required, and (optionally) which
 * table/column it references as a foreign key. The CRUD UI is generated entirely
 * from these specs.
 */
public record ColumnSpec(
        String name,
        String label,
        Type type,
        boolean primaryKey,
        boolean required,
        ForeignKey foreignKey
) {
    public enum Type {
        TEXT,
        INTEGER,
        DECIMAL,
        DATE
    }

    /** A foreign-key reference used to populate dropdowns and explain conflicts. */
    public record ForeignKey(String table, String column) {}

    public boolean isDate() {
        return type == Type.DATE;
    }

    // ── Concise factory helpers ──

    public static ColumnSpec pk(String name, String label, Type type) {
        return new ColumnSpec(name, label, type, true, true, null);
    }

    public static ColumnSpec pkRef(String name, String label, Type type, String refTable, String refColumn) {
        return new ColumnSpec(name, label, type, true, true, new ForeignKey(refTable, refColumn));
    }

    public static ColumnSpec col(String name, String label, Type type) {
        return new ColumnSpec(name, label, type, false, false, null);
    }

    public static ColumnSpec required(String name, String label, Type type) {
        return new ColumnSpec(name, label, type, false, true, null);
    }

    public static ColumnSpec ref(String name, String label, Type type, String refTable, String refColumn, boolean required) {
        return new ColumnSpec(name, label, type, false, required, new ForeignKey(refTable, refColumn));
    }
}
