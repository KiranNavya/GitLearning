package com.tecnotree.eia.processed.service;

import java.io.StringReader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.tecnotree.eia.count.configuration.QueryConfig;
import com.tecnotree.eia.processed.configuration.StoredProcedureConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.ssl.JksSslBundleProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import javax.management.Query;

@Service
public class PrcExecutorService {

    private static final Logger log = LoggerFactory.getLogger(PrcExecutorService.class);

    private final JdbcTemplate jdbcTemplate;
    private final StoredProcedureConfig storedProcedureConfig;
    private final QueryConfig queryConfig;

    public PrcExecutorService(JdbcTemplate jdbcTemplate, StoredProcedureConfig storedProcedureConfig, QueryConfig queryConfig) {
        this.jdbcTemplate = jdbcTemplate;
        this.storedProcedureConfig = storedProcedureConfig;
        this.queryConfig = queryConfig;
    }

    /**
     * Executes a stored procedure asynchronously.
     * The arguments for the stored procedure are dynamically configured via `StoredProcedureConfig`.
     *
     * @param order       The map containing the fetched record data, used to retrieve IN parameter values.
     * @param responseXml The XML response from HWCBS, used for 'EIARes' IN parameter.
     */
    @Async("prcExecutor")
    public void executeAsync(Map<String, Object> order, String responseXml) {
        long start = System.currentTimeMillis();
        String sql = storedProcedureConfig.getCallableStatementString();
        Connection conn = null;
        CallableStatement cs = null; // Declare CallableStatement outside try for finally block access
        List<String> debugArgs = new ArrayList<>();

        try {
            conn = jdbcTemplate.getDataSource().getConnection();
            cs = conn.prepareCall(sql);

            // Dynamically set parameters based on configuration
            int paramIndex = 1; // Stored procedure parameters are 1-indexed
            for (String argConfig : storedProcedureConfig.getArgs()) {
                String[] parts = argConfig.trim().split(",\\s*"); // Split by comma, trim whitespace

                if (parts[0].equalsIgnoreCase("IN")) {
                    if (parts.length < 2) {
                        log.error("Invalid IN argument configuration: {}. Expected 'IN, <COLUMN_NAME>' or 'IN, EIARes'.", argConfig);
                        throw new IllegalArgumentException("Invalid IN argument configuration: " + argConfig);
                    }
                    String argType = parts[1];
                    if (argType.equalsIgnoreCase("EIARes")) {
                        // For EIARes, send the responseXml as a CLOB
                        cs.setClob(paramIndex, new StringReader(responseXml));
                        debugArgs.add("EIARes (CLOB)");
                    } else {
                        // For other IN parameters, treat as column name from the order map
                        // Assume column names in Map are uppercase due to JDBC behavior
                        Object value = order.get(argType.toUpperCase());
                        if (value != null) {
                            cs.setString(paramIndex, value.toString());
                            debugArgs.add(argType + "='" + value + "'");
                        } else {
                            cs.setNull(paramIndex, Types.VARCHAR); // Set as NULL if value is not found
                            debugArgs.add(argType + "=NULL");
                            log.warn("IN parameter {}: Column '{}' not found in order map. Setting as NULL.", paramIndex, argType);
                        }
                    }
                } else if (parts[0].equalsIgnoreCase("OUT")) {
                    // For OUT parameters, register the type.
                    // Assuming INTEGER type as per previous code's setInt(3,0), setInt(4,0).
                    // If your stored procedure expects different OUT types, this needs to be configurable.
                    cs.registerOutParameter(paramIndex, Types.INTEGER);
                    debugArgs.add("OUT (INTEGER)");
                } else {
                    log.error("Unknown argument type in configuration: {}. Expected 'IN' or 'OUT'.", argConfig);
                    throw new IllegalArgumentException("Unknown argument type in configuration: " + argConfig);
                }
                paramIndex++;
            }

            log.debug("PRC: {} - Arguments being passed: {}", sql, String.join(", ", debugArgs));
            cs.execute();

            log.info("PRC: {} executed successfully for order ID: {}", storedProcedureConfig.getCallableStatementString(), order.get(queryConfig.getQueryMetaData().getPrimaryKeyColumn()));
        } catch (Exception e) {
            log.error("Error executing PRC: {} for order ID {}: {}", storedProcedureConfig.getCallableStatementString(), order.get(queryConfig.getQueryMetaData().getPrimaryKeyColumn()), e.getMessage(), e);
        } finally {
            if (cs != null) {
                try {
                    cs.close();
                } catch (java.sql.SQLException e) {
                    log.error("Error closing CallableStatement: {}", e.getMessage(), e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (java.sql.SQLException e) {
                    log.error("Error closing Connection: {}", e.getMessage(), e);
                }
            }
            long end = System.currentTimeMillis();
            log.debug("PRC: {} for order ID {} took {} ms", storedProcedureConfig.getCallableStatementString(), order.get(storedProcedureConfig.getCallableStatementString().toUpperCase()), (end - start)); // Placeholder for order ID
        }
    }

    @PostConstruct
    public void init() {
        log.info("PrcExecutorService initialized.");
    }

    @PreDestroy
    public void shutdown() {
        log.info("PrcExecutorService shutting down.");
    }
}
