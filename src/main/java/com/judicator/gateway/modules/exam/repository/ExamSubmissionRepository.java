package com.judicator.gateway.modules.exam.repository;

import com.judicator.gateway.modules.exam.entity.ExamSubmission;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamSubmissionRepository extends JpaRepository<ExamSubmission, UUID> {}
