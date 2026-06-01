package com.counterstrike.app.repository;

/**
 * A preset, read-only analytical query. {@code sql} may contain {@code ?}
 * placeholders; when {@code paramLabel} is non-null the UI prompts once for a
 * value and binds the same (lower-cased, %-wrapped) pattern to every placeholder.
 */
public record AnalyticsQuery(
        String title,
        String description,
        String sql,
        String paramLabel
) {
    public boolean hasParameter() {
        return paramLabel != null && !paramLabel.isBlank();
    }

    public int placeholderCount() {
        int count = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return title;
    }
}
