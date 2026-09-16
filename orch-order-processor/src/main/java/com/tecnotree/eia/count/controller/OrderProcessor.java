package com.tecnotree.eia.count.controller;

import com.tecnotree.eia.processed.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Service
public class OrderProcessor {

    @Autowired
    private OrderService orderService;

    //   private volatile boolean running = true;

    private static final Logger log = LoggerFactory.getLogger(OrderProcessor.class);

    //  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Scheduled(fixedRateString = "${scheduler.rate}")
    public void run() {
        log.info("Starting scheduled process.");
        orderService.pollAndProcessOrders();

    }

    
  /*  @PostConstruct
    public void startProcessing() {
        log.info("Initializing background order processor...");

        executorService.submit(() -> {
            while (running) {
                try {
                    log.debug("Polling and processing orders...");
                    boolean hasWork = orderService.pollAndProcessOrders();
                    
                    if (!hasWork) {
                        log.debug("No orders found. Sleeping for 15 seconds.");
                        Thread.sleep(15000);
                    }
                } catch (InterruptedException ie) {
                    log.warn("Processor thread interrupted, likely due to shutdown.", ie);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("Error while processing orders", e);
                }
            }
        });
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Shutting down background order processor...");
        running = false;
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in time. Forcing shutdown...");
                executorService.shutdownNow();
            } else {
                log.info("Background processor shut down cleanly.");
            }
        } catch (InterruptedException e) {
            log.error("Shutdown interrupted, forcing shutdown now.", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    */
}
