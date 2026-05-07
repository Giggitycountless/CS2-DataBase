package com.counterstrike.app.repository;

import java.util.List;

public record TableData(String[] columns, List<Object[]> rows) {
    public int rowCount() {
        return rows.size();
    }
}
