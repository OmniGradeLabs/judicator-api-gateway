package com.judicator.gateway.modules.exam.dto.response;

import java.util.UUID;

/**
 * Summary returned immediately after a batch grading job is triggered.
 *
 * @param examId the exam that was targeted
 * @param submissionsQueued number of student submissions extracted from the ZIP and queued
 * @param message human-readable status message
 */
public record BatchGradeResponse(UUID examId, int submissionsQueued, String message) {}
