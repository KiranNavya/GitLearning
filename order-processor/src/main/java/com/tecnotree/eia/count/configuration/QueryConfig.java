package com.tecnotree.eia.count.configuration;

import com.tecnotree.eia.count.beans.ActionCountProjection;
import com.tecnotree.eia.count.beans.ActionCountProjectionImpl;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Configuration
@ConfigurationProperties(prefix = "query")
public class QueryConfig {

    private static final Logger log = LoggerFactory.getLogger(QueryConfig.class);

    private QueryMetaData queryMetaData;
    private String countQuery;

    // Extracted column names and aliases, will be populated during validation
    private String extractedTableName;
    private String extractedActionCodeColumn;
    private String extractedStatusColumn;
    private String extractedWhereClause;
    private String extractedActionCodeAlias;
    private String extractedRecordCountAlias;

    /**
     * Returns the count query string as configured in the application properties.
     * This allows the user to define complex WHERE conditions directly in the configuration.
     *
     * @return The configured count query string.
     */
    public String getCountQuery() {
        return countQuery;
    }

    /**
     * Sets the count query string from the application properties.
     * This method is required by Spring's @ConfigurationProperties for binding.
     *
     * @param countQuery The count query string.
     */
    public void setCountQuery(String countQuery) {
        this.countQuery = countQuery;
    }

    /**
     * Provides a RowMapper to map the results of the count query to ActionCountProjectionImpl objects.
     * It uses the dynamically extracted actionCodeAlias and recordCountAlias to fetch values
     * from the ResultSet, making the mapping dynamic based on user-defined aliases in the count query.
     *
     * @return A RowMapper for ActionCountProjection.
     */
    public RowMapper<ActionCountProjection> getCountRowMapper() {
        return (rs, rowNum) -> new ActionCountProjectionImpl(rs.getString(this.extractedActionCodeAlias), rs.getInt(this.extractedRecordCountAlias));
    }

    /**
     * Constructs the fetch query dynamically based on configured metadata and computed quotas.
     * This method uses the table name, action code column, status column, and WHERE clause
     * extracted from the configured countQuery.
     *
     * @param finalMap A map containing action codes and their respective record quotas.
     * @return The constructed SQL query string for fetching records.
     * @throws IllegalStateException if essential query metadata properties are missing or cannot be extracted.
     */
    public String getFetchQuery(Map<String, Integer> finalMap) {
        if (queryMetaData == null || queryMetaData.getActionDateColumn() == null || queryMetaData.getPrimaryKeyColumn() == null || queryMetaData.getColumnNamesToFetch() == null || queryMetaData.getColumnNamesToFetch().trim().isEmpty() || queryMetaData.getRequestPayloadColumn() == null) {
            throw new IllegalStateException("Missing one or more required fields in 'query.queryMetaData' (actionDateColumn, primaryKeyColumn, requestXmlColumn, processNoColumn, columnNamesToFetch) required to build the fetch query.");
        }

        // Use the extracted values
        String tableName = this.extractedTableName;
        String actionCodeColumn = this.extractedActionCodeColumn;
        String statusColumn = this.extractedStatusColumn;
        String whereClauseFromCountQuery = this.extractedWhereClause;

        String actionDateColumn = queryMetaData.getActionDateColumn();

        // Determine columns to select for the outer and inner queries
        String outerSelectColumns;
        String innerSelectColumns;

        if ("*".equals(queryMetaData.getColumnNamesToFetch().trim())) {
            outerSelectColumns = "*"; // Select all from the derived table
            innerSelectColumns = "a.*"; // Select all from the actual table with its alias
        } else {
            outerSelectColumns = queryMetaData.getColumnNamesToFetch();
            // For the inner query, prefix each column with 'a.'
            List<String> innerCols = new java.util.ArrayList<>(Arrays.asList(queryMetaData.getColumnNamesToFetch().split(",")));
            // Ensure actionCodeColumn is always included in the inner SELECT if not already present
            // It's needed for PARTITION BY and the outer WHERE clause.
            if (!innerCols.contains(actionCodeColumn) && !innerCols.contains(actionCodeColumn.toUpperCase())) {
                innerCols.add(actionCodeColumn);
            }
            innerSelectColumns = innerCols.stream().map(String::trim).map(col -> "a." + col).collect(Collectors.joining(","));
        }

        // Construct the WHERE condition based on the computed quotas (actionCode and row number limit)
        String whereConditionForQuota = finalMap.entrySet().stream().map(e -> String.format("(%s = '%s' AND rn <= %d)", actionCodeColumn, e.getKey(), e.getValue())).collect(Collectors.joining(" OR "));

        // Combine the extracted WHERE clause with the common status filter and the quota-based conditions.
        String combinedInitialWhereClause = String.format("a.%s IN (%s) AND a.%s = 'Q'", actionCodeColumn, String.join(",", finalMap.keySet().stream().map(k -> "'" + k + "'").toList()), statusColumn);

        if (whereClauseFromCountQuery != null && !whereClauseFromCountQuery.trim().isEmpty()) {
            // Ensure the extracted clause is properly wrapped if it contains AND/OR logic
            combinedInitialWhereClause += " AND (" + whereClauseFromCountQuery + ")";
        }

        // The final SQL query structure, using ROW_NUMBER for limiting records per action code
        String sql = """
                    SELECT %s FROM (
                        SELECT %s, ROW_NUMBER() OVER (PARTITION BY a.%s ORDER BY a.%s DESC) AS rn
                        FROM %s a
                        WHERE %s
                    ) WHERE %s
                """.formatted(outerSelectColumns, // Columns for the outer SELECT
                innerSelectColumns, // Columns for the inner SELECT (what columns to partition by)
                actionCodeColumn, actionDateColumn, tableName, // Use extracted table name
                combinedInitialWhereClause, // Combined WHERE clause for the inner query
                whereConditionForQuota // WHERE clause for the outer query to apply row number limits
        );
        return sql;
    }

    /**
     * Extracts the WHERE clause from a given SQL query string.
     * This method is a basic parser and assumes a simple SQL structure.
     * It looks for the first "WHERE" keyword and extracts everything until "GROUP BY" or the end of the string.
     *
     * @param query The SQL query string from which to extract the WHERE clause.
     * @return The extracted WHERE clause, or an empty string if not found or if the query is complex.
     */
    private String extractWhereClause(String query) {
        Pattern wherePattern = Pattern.compile("WHERE\\s+(.*?)(?:\\s+GROUP\\s+BY|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = wherePattern.matcher(query);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return ""; // No WHERE clause found or could not be extracted simply
    }

    /**
     * Extracts the table name from a given SQL query string.
     * This method assumes the table name follows the "FROM" keyword and optionally an alias.
     * It's a basic parser and might not handle all complex SQL structures (e.g., subqueries in FROM).
     *
     * @param query The SQL query string from which to extract the table name.
     * @return The extracted table name, or null if not found.
     */
    private String extractTableName(String query) {
        Pattern fromPattern = Pattern.compile("FROM\\s+([a-zA-Z0-9_\\.]+)(?:\\s+AS\\s+[a-zA-Z0-9_]+|\\s+[a-zA-Z0-9_]+)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = fromPattern.matcher(query);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null; // Table name not found
    }

    /**
     * Extracts the action code column name from the GROUP BY clause of the count query.
     * This assumes the action code column is the one used in GROUP BY.
     *
     * @param query The count query string.
     * @return The extracted action code column name, or null if not found.
     */
    private String extractActionCodeColumn(String query) {
        // Look for GROUP BY followed by a column name (and optional alias prefix)
        Pattern groupPattern = Pattern.compile("GROUP\\s+BY\\s+([a-zA-Z0-9_\\.]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = groupPattern.matcher(query);
        if (matcher.find()) {
            String column = matcher.group(1).trim();
            // Remove table alias if present (e.g., "a.ACTION_CODE_COL" -> "ACTION_CODE_COL")
            int dotIndex = column.lastIndexOf('.');
            return dotIndex != -1 ? column.substring(dotIndex + 1) : column;
        }
        return null;
    }

    /**
     * Extracts the status column name from the WHERE clause of the count query.
     * This method specifically looks for a pattern like 'column_name = 'Q''.
     *
     * @param query The count query string.
     * @return The extracted status column name, or null if not found.
     */
    private String extractStatusColumn(String query) {
        // Look for 'column_name = 'Q'' within the WHERE clause.
        // This is a specific pattern based on the example and might need adjustment for other queries.
        Pattern statusPattern = Pattern.compile("([a-zA-Z0-9_\\.]+)\\s*=\\s*'Q'", Pattern.CASE_INSENSITIVE);
        Matcher matcher = statusPattern.matcher(query);
        if (matcher.find()) {
            String column = matcher.group(1).trim();
            // Remove table alias if present (e.g., "a.STATUS_COL" -> "STATUS_COL")
            int dotIndex = column.lastIndexOf('.');
            return dotIndex != -1 ? column.substring(dotIndex + 1) : column;
        }
        return null;
    }

    /**
     * Extracts the alias for the action code column from the SELECT clause of the count query.
     * Assumes the action code column is selected with an 'AS alias' pattern.
     *
     * @param query The count query string.
     * @return The extracted action code alias, or null if not found.
     */
    private String extractActionCodeAlias(String query) {
        // Look for 'column_name AS alias' or 'column_name alias' for the first selected column
        // This assumes the action code is the first aliased column in the SELECT statement.
        Pattern aliasPattern = Pattern.compile("SELECT\\s+.*?([a-zA-Z0-9_\\.]+)(?:\\s+AS)?\\s+([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = aliasPattern.matcher(query);
        if (matcher.find()) {
            // Group 1 is the original column name, Group 2 is the alias.
            // We need to ensure this is indeed the action code alias.
            // For now, we'll assume the first alias found is the action code alias.
            // A more robust solution might involve looking for the column name found in GROUP BY and then its alias.
            return matcher.group(2).trim();
        }
        return null;
    }

    /**
     * Extracts the alias for the record count column from the SELECT clause of the count query.
     * Assumes COUNT(*) is selected with an 'AS alias' pattern.
     *
     * @param query The count query string.
     * @return The extracted record count alias, or null if not found.
     */
    private String extractRecordCountAlias(String query) {
        // Look for 'COUNT(*)' followed by 'AS alias' or 'alias'
        Pattern countAliasPattern = Pattern.compile("COUNT\\s*\\(\\s*\\*\\s*\\)(?:\\s+AS)?\\s+([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = countAliasPattern.matcher(query);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    public QueryMetaData getQueryMetaData() {
        return queryMetaData;
    }

    public void setQueryMetaData(QueryMetaData queryMetaData) {
        this.queryMetaData = queryMetaData;
    }

    // Public getters for extracted fields
    public String getExtractedTableName() {
        return extractedTableName;
    }

    public String getExtractedActionCodeColumn() {
        return extractedActionCodeColumn;
    }

    public String getExtractedStatusColumn() {
        return extractedStatusColumn;
    }

    public String getExtractedWhereClause() {
        return extractedWhereClause;
    }

    public String getExtractedActionCodeAlias() {
        return extractedActionCodeAlias;
    }

    public String getExtractedRecordCountAlias() {
        return extractedRecordCountAlias;
    }


    /**
     * Nested class to hold metadata related to database queries.
     * Properties in this class are mapped from the `query.queryMetaData` section
     * in the application's configuration file.
     */
    public static class QueryMetaData {
        // tableName, actionCodeColumn, statusColumn, actionCodeAlias, recordCountAlias removed as they are extracted from countQuery
        private String actionDateColumn;
        private String primaryKeyColumn;
        private String primaryKeyColumn2;
        private String requestPayloadColumn;
        private String columnNamesToFetch; // Comma-separated list of columns to fetch


        public String getActionDateColumn() {
            return actionDateColumn;
        }

        public void setActionDateColumn(String actionDateColumn) {
            this.actionDateColumn = actionDateColumn;
        }

        public String getPrimaryKeyColumn() {
            return primaryKeyColumn;
        }

        public void setPrimaryKeyColumn(String primaryKeyColumn) {
            this.primaryKeyColumn = primaryKeyColumn;
        }

        public String getPrimaryKeyColumn2() {
            return primaryKeyColumn2;
        }

        public void setPrimaryKeyColumn2(String primaryKeyColumn2) {
            this.primaryKeyColumn2 = primaryKeyColumn2;
        }

        // FIX: Renamed from voidsetRequestXmlColumn to setRequestXmlColumn
        public void setRequestPayloadColumn(String requestPayloadColumn) {
            this.requestPayloadColumn = requestPayloadColumn;
        }

        public String getRequestPayloadColumn() {
            return requestPayloadColumn;
        }

        public String getColumnNamesToFetch() {
            return columnNamesToFetch;
        }

        public void setColumnNamesToFetch(String columnNamesToFetch) {
            this.columnNamesToFetch = columnNamesToFetch;
        }
    }

    /**
     * Initializes and validates the QueryConfig after properties have been loaded.
     * Ensures that all required metadata fields are present and not empty,
     * and that necessary columns can be extracted from the count query.
     * Logs the loaded configuration for debugging and verification.
     *
     * @throws IllegalStateException if any required configuration property is missing or empty,
     *                               or if essential column names cannot be extracted from the count query.
     */
    @PostConstruct
    public void validateConfiguration() {
        log.info("QueryConfig loaded.");

        if (queryMetaData == null) {
            throw new IllegalStateException("Missing 'query.queryMetaData' configuration. It is required for framing all queries.");
        }
        // Validate essential queryMetaData properties that are still in config
        if (queryMetaData.getActionDateColumn() == null || queryMetaData.getActionDateColumn().trim().isEmpty() || queryMetaData.getPrimaryKeyColumn() == null || queryMetaData.getPrimaryKeyColumn().trim().isEmpty() || queryMetaData.getRequestPayloadColumn() == null || queryMetaData.getRequestPayloadColumn().trim().isEmpty() ||
//                queryMetaData.getProcessNoColumn() == null || queryMetaData.getProcessNoColumn().trim().isEmpty() || // processNoColumn still validated here
                queryMetaData.getColumnNamesToFetch() == null || queryMetaData.getColumnNamesToFetch().trim().isEmpty()) {
            throw new IllegalStateException("Missing one or more required fields in 'query.queryMetaData' (actionDateColumn, primaryKeyColumn, requestXmlColumn, processNoColumn, columnNamesToFetch).");
        }
        // Validate countQuery
        if (countQuery == null || countQuery.trim().isEmpty()) {
            throw new IllegalStateException("Missing 'query.countQuery' configuration. It is required for counting records.");
        }

        // Extract and validate dynamic components from countQuery
        this.extractedTableName = extractTableName(countQuery);
        if (this.extractedTableName == null || this.extractedTableName.trim().isEmpty()) {
            throw new IllegalStateException("Could not extract table name from 'query.countQuery'. Please ensure the query contains a valid 'FROM <table>' clause.");
        }

        this.extractedActionCodeColumn = extractActionCodeColumn(countQuery);
        if (this.extractedActionCodeColumn == null || this.extractedActionCodeColumn.trim().isEmpty()) {
            throw new IllegalStateException("Could not extract action code column from 'query.countQuery'. Please ensure the query contains a valid 'GROUP BY <action_code_column>' clause.");
        }

        this.extractedStatusColumn = extractStatusColumn(countQuery);
        // Status column is critical for filtering, so it must be present
        if (this.extractedStatusColumn == null || this.extractedStatusColumn.trim().isEmpty()) {
            throw new IllegalStateException("Could not extract status column from 'query.countQuery'. Please ensure the query contains a 'WHERE <status_column> = 'Q'' or similar clause.");
        }

        this.extractedActionCodeAlias = extractActionCodeAlias(countQuery);
        if (this.extractedActionCodeAlias == null || this.extractedActionCodeAlias.trim().isEmpty()) {
            throw new IllegalStateException("Could not extract action code alias from 'query.countQuery'. Please ensure the action code column has an alias (e.g., 'SELECT col AS alias').");
        }

        this.extractedRecordCountAlias = extractRecordCountAlias(countQuery);
        if (this.extractedRecordCountAlias == null || this.extractedRecordCountAlias.trim().isEmpty()) {
            throw new IllegalStateException("Could not extract record count alias from 'query.countQuery'. Please ensure COUNT(*) has an alias (e.g., 'SELECT COUNT(*) AS alias').");
        }

        this.extractedWhereClause = extractWhereClause(countQuery); // Store for later use in fetch query

        log.info("Query Metadata Loaded: ActionDateColumn='{}', PrimaryKeyColumn='{}', RequestXmlColumn='{}', ColumnsToFetch='{}'", queryMetaData.getActionDateColumn(), queryMetaData.getPrimaryKeyColumn(), queryMetaData.getRequestPayloadColumn(), queryMetaData.getColumnNamesToFetch());
        log.info("Count Query Loaded: '{}'", countQuery);
        log.info("Extracted Table Name: '{}'", this.extractedTableName);
        log.info("Extracted Action Code Column: '{}'", this.extractedActionCodeColumn);
        log.info("Extracted Status Column: '{}'", this.extractedStatusColumn);
        log.info("Extracted Action Code Alias: '{}'", this.extractedActionCodeAlias);
        log.info("Extracted Record Count Alias: '{}'", this.extractedRecordCountAlias);
        log.info("Extracted WHERE Clause: '{}'", this.extractedWhereClause);
    }
}