package com.tecnotree.eia.processed.configuration;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Value("${threadpool.cbs.maxSize}")
	private int cbsThreadPoolMaxSize;
	
	@Value("${threadpool.cbs.coreSize}")
	private int cbsThreadCoreSize;
	
	@Value("${threadpool.cbs.queueCapacity}")
	private int cbsThreadqueueCapacity;
	
	@Value("${threadpool.prc.maxSize}")
	private int prcThreadPoolMaxSize;

	@Value("${threadpool.prc.coreSize}")
	private int prcThreadCoreSize;

	@Value("${threadpool.prc.queueCapacity}")
	private int prcThreadqueueCapacity;

    @Bean(name = "orderProcessingExecutor")
    Executor cbsExecutor() {
        log.info("Configuring orderProcessingExecutor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cbsThreadCoreSize);
        executor.setMaxPoolSize(cbsThreadPoolMaxSize);
        executor.setQueueCapacity(cbsThreadqueueCapacity);
        executor.setThreadNamePrefix("OrderProcess-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Bean(name = "prcExecutor")
    Executor prcExecutor() {
        log.info("Configuring prcExecutor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(prcThreadCoreSize);
        executor.setMaxPoolSize(prcThreadPoolMaxSize);
        executor.setQueueCapacity(prcThreadqueueCapacity);
        executor.setThreadNamePrefix("PrcExecutor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
