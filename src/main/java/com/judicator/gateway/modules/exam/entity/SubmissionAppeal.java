package com.judicator.gateway.modules.exam.entity;

import com.judicator.gateway.infrastructure.persistence.BaseEntity;
import com.judicator.gateway.modules.exam.enumType.AppealStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "submission_appeals")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionAppeal extends BaseEntity {

  // Intra-module relation: physical FK to exam_submissions table is valid here
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submission_id", nullable = false)
  private ExamSubmission submission;

  // Cross-module reference: stored as plain UUID to avoid coupling with identity module
  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(columnDefinition = "TEXT")
  private String complaint;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private AppealStatus status = AppealStatus.PENDING;

  @Column(name = "teacher_response", columnDefinition = "TEXT")
  private String teacherResponse;
}
