package ru.iposhka.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.email-jobs")
public record EmailJobProperties(
        @Min(100) long pollDelayMs,
        @Min(1) int batchSize,
        @Min(1) int maxAttempts,
        @NotEmpty List<@Min(1) Long> retryDelaysSeconds,
        @Min(30) long processingTimeoutSeconds,
        @Valid Worker worker
) {
    public record Worker(
            @Min(1) int poolSize,
            @Min(1) int queueCapacity
    ) {
    }
}