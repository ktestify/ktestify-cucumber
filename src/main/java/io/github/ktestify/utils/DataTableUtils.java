/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.ktestify.utils;

import io.cucumber.datatable.DataTable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class for reading values from a Cucumber {@link DataTable}.
 *
 * <p>Step definitions receive DataTables as a list of maps (header row → value row). This helper provides null-safe
 * accessors and type conversions so step code stays concise.
 *
 * <p>All methods operate on the <em>first data row</em> ({@code row 0}) of the table unless stated otherwise.
 */
public final class DataTableUtils {

    private DataTableUtils() {}

    /**
     * Converts a {@link DataTable} to a list of row maps (header → value).
     *
     * @param dataTable the Cucumber DataTable
     * @return list of row maps; never {@code null}
     */
    public static List<Map<String, String>> toListOfMaps(DataTable dataTable) {
        return dataTable.asMaps(String.class, String.class);
    }

    /**
     * Returns the first row of the DataTable as a map (header → value).
     *
     * @param dataTable the Cucumber DataTable
     * @return first row map
     * @throws IllegalArgumentException if the table has no data rows
     */
    public static Map<String, String> firstRow(DataTable dataTable) {
        List<Map<String, String>> rows = toListOfMaps(dataTable);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("DataTable has no data rows.");
        }
        return rows.get(0);
    }

    /**
     * Reads a String value from the first row by column name. Returns {@code null} if the column is absent or blank.
     */
    public static String getString(DataTable dataTable, String column) {
        return getString(firstRow(dataTable), column);
    }

    /** Reads a String value from a row map. Returns {@code null} if the column is absent or blank. */
    public static String getString(Map<String, String> row, String column) {
        String value = row.get(column);
        return (value != null && !value.isBlank()) ? value : null;
    }

    /** Reads a Long value from the first row by column name. Returns {@code null} if the column is absent or blank. */
    public static Long getLong(DataTable dataTable, String column) {
        return getLong(firstRow(dataTable), column);
    }

    /** Reads a Long value from a row map. Returns {@code null} if the column is absent or blank. */
    public static Long getLong(Map<String, String> row, String column) {
        String value = getString(row, column);
        if (value == null) return null;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Column '" + column + "' is not a valid long: '" + value + "'", e);
        }
    }

    /**
     * Reads an Integer value from the first row by column name. Returns {@code null} if the column is absent or blank.
     */
    public static Integer getInt(DataTable dataTable, String column) {
        return getInt(firstRow(dataTable), column);
    }

    /** Reads an Integer from a row map. Returns {@code null} if the column is absent or blank. */
    public static Integer getInt(Map<String, String> row, String column) {
        String value = getString(row, column);
        if (value == null) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Column '" + column + "' is not a valid integer: '" + value + "'", e);
        }
    }

    /**
     * Reads a comma-separated column value from the first row and splits it into a trimmed list. Returns an empty list
     * if the column is absent or blank.
     *
     * <p>Example: {@code "f1.json,f2.json, f3.json"} → {@code ["f1.json", "f2.json", "f3.json"]}
     */
    public static List<String> getList(DataTable dataTable, String column) {
        return getList(firstRow(dataTable), column);
    }

    /**
     * Reads a comma-separated value from a row map and splits into a trimmed list. Returns an empty list if the column
     * is absent or blank.
     */
    public static List<String> getList(Map<String, String> row, String column) {
        String value = getString(row, column);
        if (value == null) return Collections.emptyList();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    /** Returns an {@link Optional} wrapping a String value from the first row. Useful for optional columns. */
    public static Optional<String> getOptional(DataTable dataTable, String column) {
        return Optional.ofNullable(getString(dataTable, column));
    }
}
