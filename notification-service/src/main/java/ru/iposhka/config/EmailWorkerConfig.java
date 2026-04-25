package ru.iposhka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class EmailWorkerConfig {

    @Bean(name = "emailWorkerExecutor")
    public ThreadPoolTaskExecutor emailWorkerExecutor(EmailJobProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("email-worker-");
        executor.setCorePoolSize(properties.worker().poolSize());
        executor.setMaxPoolSize(properties.worker().poolSize());
        executor.setQueueCapacity(properties.worker().queueCapacity());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}