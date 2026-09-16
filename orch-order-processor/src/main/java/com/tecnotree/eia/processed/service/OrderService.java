package com.tecnotree.eia.processed.service;

import com.tecnotree.eia.count.beans.ActionCountProjection;
import com.tecnotree.eia.count.configuration.CountProperties;
import com.tecnotree.eia.count.entity.CBSubsProvisioning;
import com.tecnotree.eia.count.repository.OrderRepository;
import com.tecnotree.eia.fatch.repository.FetchRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private FetchRepository fetchRepo;

    @Autowired
    private CountProperties countProperties;

    @Value("${threadpool.size:10}")
    private int threadPoolSize;

    private ExecutorService executor;

    @Autowired
    private SendToProcess sendToProcess;

    @PostConstruct
    public void init() {
        executor = Executors.newFixedThreadPool(threadPoolSize);
        log.info("Initialized thread pool with size: {}", threadPoolSize);
    }

    public void pollAndProcessOrders() {
        //  public Boolean pollAndProcessOrders() {
        log.info("Starting polling and order processing...");

        try {
            List<ActionCountProjection> actionCounts = orderRepo.getActionCounts();
            log.info("Fetched action count projections: {}", actionCounts);

            Map<String, Integer> dbCountMap = actionCounts.stream()
                    .collect(Collectors.toMap(ActionCountProjection::getActionCode, ActionCountProjection::getRecordCount));
            log.debug("Database action counts: {}", dbCountMap);

            Map<String, Integer> finalQuotaMap = computeFinalQuotas(countProperties.getActionCodes(), dbCountMap);
            log.info("Computed final quotas: {}", finalQuotaMap);

            List<CBSubsProvisioning> fetchedRecords = fetchRecords(finalQuotaMap);
            log.info("Fetched {} records from DB.", fetchedRecords.size());

            if (fetchedRecords.isEmpty()) {
                log.info("No records to process.");
                //   return false;
            }

            fetchedRecords.forEach(record -> executor.submit(() -> processOrder(record)));

            // return true;
        } catch (Exception e) {
            log.error("Exception occurred during polling and processing: {}", e.getMessage(), e);
            // return false;
        }
    }


    private Map<String, Integer> computeFinalQuotas(Map<String, Integer> configuredQuotas,
                                                    Map<String, Integer> availableCounts) {
        log.debug("Computing quotas from configured: {} and available: {}", configuredQuotas, availableCounts);

        Map<String, Integer> result = new LinkedHashMap<>();
        int leftover = 0;

        for (String action : configuredQuotas.keySet()) {
            int desired = configuredQuotas.get(action);
            int available = availableCounts.getOrDefault(action, 0);
            int toUse = Math.min(desired, available);
            if (available > 0) {
                result.put(action, toUse);
            }
            leftover += (desired - toUse);
        }

        log.debug("After initial assignment, quota result: {}, leftover: {}", result, leftover);

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

        log.info("Final distributed quotas: {}", result);
        return result;
    }

    public List<CBSubsProvisioning> fetchRecords(Map<String, Integer> finalQuotaMap) {
        log.info("Fetching records with finalQuotaMap: {}", finalQuotaMap);
        try {
            AtomicInteger total = new AtomicInteger(0);

            Map<String, Integer> finalMap = finalQuotaMap.entrySet().stream()
                    .map(entry -> {
                        int remaining = countProperties.getLimitValue() - total.get();
                        if (remaining <= 0) return null;

                        int allowed = Math.min(entry.getValue(), remaining);
                        total.addAndGet(allowed);
                        return new AbstractMap.SimpleEntry<>(entry.getKey(), allowed);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (existing, replacement) -> existing, LinkedHashMap::new));

            if (finalMap.isEmpty()) {
                log.warn("No records to fetch. Final quota map is empty after applying limits.");
                return List.of();
            }

            String actionCodesStr = finalMap.keySet().stream()
                    .map(code -> "'" + code + "'")
                    .collect(Collectors.joining(","));

            String whereCondition = finalMap.entrySet().stream()
                    .map(entry -> "(action_code_v = '" + entry.getKey() + "' AND rn <= " + entry.getValue() + ")")
                    .collect(Collectors.joining(" OR "));

            StringBuilder fetchSQLQuery = new StringBuilder(
                    "SELECT * FROM (SELECT a.*, ROW_NUMBER() OVER (PARTITION BY a.action_code_v ORDER BY a.TRANS_DATE_D DESC) AS rn "
                            + "FROM CB_ORCH_REQUEST_DTLS a WHERE a.action_code_v IN (" + actionCodesStr + ") "
                            + "AND a.status_code_v = 'Q' AND a.OPERATION_NAME_V = 'CLMD') WHERE " + whereCondition);

            log.debug("Constructed fetch SQL: {}", fetchSQLQuery);

            List<CBSubsProvisioning> finalResult = fetchRepo.fetchByActionCodes(fetchSQLQuery.toString());

            if (finalResult.isEmpty()) {
                log.info("Query returned no records.");
                return List.of();
            }

            finalResult.forEach(record -> record.setStatus("I"));
            orderRepo.saveAll(finalResult);

            log.info("Updated {} records to status 'I'.", finalResult.size());
            return finalResult;

        } catch (Exception e) {
            log.error("Exception in fetchRecords: {}", e.getMessage(), e);
            return List.of();
        }
    }

    void processOrder(CBSubsProvisioning order) {
        log.debug("Processing order: {}", order.getOrderId());
        try {
            sendToProcess.sendOrderToCLM(order);
        } catch (Exception e) {
            log.error("Failed to process order {}: {}", order.getOrderId(), e.getMessage(), e);
        }
    }
}