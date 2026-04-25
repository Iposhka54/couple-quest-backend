package ru.iposhka.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import ru.iposhka.model.EmailJob;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailJobScheduler {

    private final EmailJobService emailJobService;
    private final ThreadPoolTaskExecutor emailWorkerExecutor;

    @Scheduled(fixedDelayString = "${app.email-jobs.poll-delay-ms}")
    public void processPendingEmails() {
        int availableSlots = calculateAvailableSlots();
        if (availableSlots <= 0) {
            return;
        }

        List<EmailJob> jobs = emailJobService.claimNextBatch(availableSlots);
        if (jobs.isEmpty()) {
            return;
        }

        log.debug("Submitting {} email jobs to worker pool", jobs.size());
        jobs.forEach(job -> emailWorkerExecutor.submit(() -> emailJobService.process(job)));
    }

    private int calculateAvailableSlots() {
        emailWorkerExecutor.getThreadPoolExecutor();

        int maxPoolSize = emailWorkerExecutor.getMaxPoolSize();
        int activeCount = emailWorkerExecutor.getActiveCount();
        int remainingQueueCapacity = emailWorkerExecutor.getThreadPoolExecutor().getQueue().remainingCapacity();
        return Math.max(0, (maxPoolSize - activeCount) + remainingQueueCapacity);
    }
}