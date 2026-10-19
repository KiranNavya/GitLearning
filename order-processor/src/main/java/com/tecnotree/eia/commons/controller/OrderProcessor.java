package com.tecnotree.eia.commons.controller;

import com.tecnotree.eia.processed.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Service
public class OrderProcessor {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ApplicationContext appContext;

    private static final Logger log = LoggerFactory.getLogger(OrderProcessor.class);

    @Scheduled(fixedRateString = "${scheduler.rate}")
    public void run() {
        log.info("Starting scheduled process.");
        orderService.pollAndProcessOrders();
    }

    @Scheduled(cron = "${scheduler.shutdown-cron}", zone = "${scheduler.shutdown-timezone:Asia/Kolkata}")
    public void initiateShutdown() {
        log.warn("Scheduled application shutdown initiated by configured cron expression.");
        log.info("Performing graceful application context shutdown...");
        ((org.springframework.context.ConfigurableApplicationContext) appContext).close();
    }
}