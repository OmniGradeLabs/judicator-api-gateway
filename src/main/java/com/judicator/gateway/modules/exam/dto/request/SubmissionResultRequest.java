package com.judicator.gateway.modules.exam.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Webhook payload sent by the grading runner to report a submission result.
 *
 * @param autoScore the raw automated score produced by the runner
 * @param finalScore the final score after any adjustments (may equal autoScore initially)
 */
public record SubmissionResultRequest(
    @NotNull(message = "autoScore không được để trống")
        @DecimalMin(value = "0.0", message = "autoScore phải >= 0")
        @DecimalMax(value = "10.0", message = "autoScore phải <= 10")
        Double autoScore,
    @NotNull(message = "finalScore không được để trống")
        @DecimalMin(value = "0.0", message = "finalScore phải >= 0")
        @DecimalMax(value = "10.0", message = "finalScore phải <= 10")
        Double finalScore) {}
