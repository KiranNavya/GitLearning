package com.tecnotree.eia.processed.service;

import com.tecnotree.eia.count.beans.ActionCountProjection;
import com.tecnotree.eia.commons.configuration.ActionCodeConfig;
import com.tecnotree.eia.count.configuration.QueryConfig;
import com.tecnotree.eia.fatch.repository.FetchQRecordsRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final FetchQRecordsRepository fetchRepo;
    private final ActionCodeConfig actionCodeConfig;
    private final QueryConfig queryConfig;
    private final SendToProcess sendToProcess;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public OrderService(FetchQRecordsRepository fetchRepo, ActionCodeConfig actionCodeConfig, QueryConfig queryConfig, SendToProcess sendToProcess, JdbcTemplate jdbcTemplate) {
        this.fetchRepo = fetchRepo;
        this.actionCodeConfig = actionCodeConfig;
        this.queryConfig = queryConfig;
        this.sendToProcess = sendToProcess;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void pollAndProcessOrders() {
        log.debug("Starting count operation.");
        long batchStart = System.currentTimeMillis();
        List<Map<String, Object>> fetchedRecords = List.of(); // Changed type
        try {
            String countQuerySql = queryConfig.getCountQuery();
            log.debug("Executing count query: '{}'", countQuerySql);

            List<ActionCountProjection> actionCounts = jdbcTemplate.query(countQuerySql, queryConfig.getCountRowMapper());

            Map<String, Integer> dbCountMap = actionCounts.stream().collect(Collectors.toMap(ActionCountProjection::actionCode, ActionCountProjection::recordCount));

            Map<String, Integer> finalQuotaMap = computeFinalQuotas(actionCodeConfig.getActionCodes(), dbCountMap);
            log.info("Final Quota Map computed: {}", finalQuotaMap);

            fetchedRecords = fetchRecords(finalQuotaMap);

            if (!fetchedRecords.isEmpty()) {
                log.info("Sending {} records for processing", fetchedRecords.size());
                fetchedRecords.forEach(this::processOrderAsync);
            } else {
                log.info("No records fetched for processing.");
            }
        } catch (Exception e) {
            log.error("Failed to process orders batch", e);
        } finally {
            long batchEnd = System.currentTimeMillis();
            log.info("Processed batch of {} records in {} ms", fetchedRecords.size(), (batchEnd - batchStart));
        }
    }

    /**
     * Computes the final quotas for each action code based on configured quotas and available counts.
     * It attempts to fulfill desired quotas first, then distributes any leftover capacity.
     *
     * @param configuredQuotas A map of action codes to their desired processing limits.
     * @param availableCounts  A map of action codes to their currently available record counts in the database.
     * @return A map indicating the final number of records to process for each action code.
     */
    private Map<String, Integer> computeFinalQuotas(Map<String, Integer> configuredQuotas, Map<String, Integer> availableCounts) {
        Map<String, Integer> result = new LinkedHashMap<>();
        int leftover = 0;

        // First pass: fulfill desired quotas up to available counts
        for (String action : configuredQuotas.keySet()) {
            int desired = configuredQuotas.get(action);
            int available = availableCounts.getOrDefault(action, 0);
            int toUse = Math.min(desired, available);
            if (available > 0) result.put(action, toUse);
            leftover += desired - toUse;
        }

        // Second pass: distribute leftover quota if there's extra room in available counts
        for (String action : result.keySet()) {
            if (leftover == 0) break;
            int available = availableCounts.getOrDefault(action, 0);
            int current = result.get(action);
            int extraRoom = available - current;
            if (extraRoom > 0) {
                int toGive = Math.min(leftover, extraRoom);
                result.put(action, current + toGive);
                leftover -= toGive;
            }
        }

        return result;
    }

    /**
     * Fetches records from the database based on the final quota map.
     * It constructs a dynamic SQL query using QueryConfig and updates the status of fetched records to 'I' (In-Progress).
     *
     * @param finalQuotaMap A map indicating the number of records to fetch for each action code.
     * @return A list of maps, where each map represents a fetched record.
     */
    @Transactional
    List<Map<String, Object>> fetchRecords(Map<String, Integer> finalQuotaMap) { // Changed return type
        try {
            AtomicInteger total = new AtomicInteger(0);

            Map<String, Integer> finalMap = finalQuotaMap.entrySet().stream().map(entry -> {
                int remaining = actionCodeConfig.getLimitValue() - total.get();
                if (remaining <= 0) return null;
                int allowed = Math.min(entry.getValue(), remaining);
                total.addAndGet(allowed);
                return new AbstractMap.SimpleEntry<>(entry.getKey(), allowed);
            }).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

            if (finalMap.isEmpty()) {
                log.warn("No quota available after applying limits.");
                return List.of();
            }

            String sql = queryConfig.getFetchQuery(finalMap);
            log.debug("Generated Fetch SQL Query: {}", sql);

            List<Map<String, Object>> records = fetchRepo.fetchByActionCodes(sql);

            if (!records.isEmpty()) {
                updateRecordsStatusInDb(records, "I");
                log.info("Marked {} records as In-Progress ('I').", records.size());
            }

            return records;
        } catch (Exception e) {
            log.error("Error while fetching records", e);
            return List.of();
        }
    }

    /**
     * Updates the status of a list of records in the database to a new status.
     * It uses batch update for efficiency.
     *
     * @param records   The list of records (maps) to update.
     * @param newStatus The new status to set for the records.
     */
    private void updateRecordsStatusInDb(List<Map<String, Object>> records, String newStatus) {
        if (records.isEmpty()) {
            return;
        }

        String tableName = queryConfig.getExtractedTableName(); // Extracted from countQuery
        String statusColumn = queryConfig.getExtractedStatusColumn(); // Extracted from countQuery
        String primaryKeyColumn = queryConfig.getQueryMetaData().getPrimaryKeyColumn(); // From config
        String primaryKeyColumn2 = queryConfig.getQueryMetaData().getPrimaryKeyColumn2(); // From config

        String updateSql;
        List<Object[]> batchArgs = new ArrayList<>();

        // Conditionally build the UPDATE SQL
        if (primaryKeyColumn2 != null && !primaryKeyColumn2.trim().isEmpty()) {
            // If primaryKeyColumn2 is present, use both primary keys in WHERE clause
            updateSql = String.format("UPDATE %s SET %s = ? WHERE %s = ? AND %s = ?", tableName, statusColumn, primaryKeyColumn, primaryKeyColumn2);
        } else {
            // If primaryKeyColumn2 is null or empty, use the existing single WHERE clause
            updateSql = String.format("UPDATE %s SET %s = ? WHERE %s = ?", tableName, statusColumn, primaryKeyColumn);
        }

        for (Map<String, Object> record : records) {
            Object primaryKeyValue = record.get(primaryKeyColumn.toUpperCase()); // Assuming JDBC driver upper-cases column names
            if (primaryKeyValue == null) {
                log.warn("Record missing primary key column '{}' for update: {}", primaryKeyColumn, record);
                continue;
            }

            Object[] currentBatchArgs;
            String previewSql = updateSql;

            if (primaryKeyColumn2 != null && !primaryKeyColumn2.trim().isEmpty()) {
                Object primaryKey2Value = record.get(primaryKeyColumn2.toUpperCase()); // <--- NEW: Get primaryKeyColumn2 value
                if (primaryKey2Value == null) {
                    log.warn("Record missing secondary primary key column '{}' for update: {}", primaryKeyColumn2, record);
                    continue;
                }
                currentBatchArgs = new Object[]{newStatus, primaryKeyValue, primaryKey2Value};
                previewSql = String.format("UPDATE %s SET %s = '%s' WHERE %s = '%s' AND %s = '%s'", tableName, statusColumn, newStatus, primaryKeyColumn, primaryKeyValue, primaryKeyColumn2, primaryKey2Value);
            } else {
                currentBatchArgs = new Object[]{newStatus, primaryKeyValue};
                previewSql = String.format("UPDATE %s SET %s = '%s' WHERE %s = '%s'", tableName, statusColumn, newStatus, primaryKeyColumn, primaryKeyValue);
            }
            batchArgs.add(currentBatchArgs);
            log.info("Preview SQL for record update: {}", previewSql);
        }

        if (!batchArgs.isEmpty()) {
            int[] updateCounts = jdbcTemplate.batchUpdate(updateSql, batchArgs);
            log.info("Batch update for status completed. {} records updated.", Arrays.stream(updateCounts).sum());
        }
    }

    /**
     * Processes a single order asynchronously.
     * It sends the order to EIA and handles potential errors.
     *
     * @param order The map representing the order to be processed.
     */
    @Async("orderProcessingExecutor")
    protected void processOrderAsync(Map<String, Object> order) {
        String primaryKeyColumn = queryConfig.getQueryMetaData().getPrimaryKeyColumn();
        String actionCodeColumn = queryConfig.getExtractedActionCodeColumn();

        // Access dynamically using upper-cased column names (common with JDBC)
        Object orderId = order.get(primaryKeyColumn.toUpperCase());
        String actionCode = (String) order.get(actionCodeColumn.toUpperCase());

        log.debug("Starting to process order ID {} (Action: {}) in thread {}", orderId, actionCode, Thread.currentThread().getName());
        long start = System.currentTimeMillis();
        try {
            sendToProcess.sendOrderToEia(order);
        } catch (Exception e) {
            log.error("Failed processing order {} (Action: {}): {}", orderId, actionCode, e.getMessage(), e);
        } finally {
            long end = System.currentTimeMillis();
            log.debug("Order ID {} (Action: {}) processed in {} ms", orderId, actionCode, (end - start));
        }
    }

    @PostConstruct
    public void init() {
        log.info("OrderService initialized.");
    }

    @PreDestroy
    public void shutdown() {
        log.info("OrderService shutting down.");
    }
}