package com.judicator.gateway.modules.exam.entity;

import com.judicator.gateway.infrastructure.persistence.BaseEntity;
import com.judicator.gateway.modules.exam.enumType.SubmissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "exam_submissions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSubmission extends BaseEntity {

  // Intra-module relation: physical FK to exams table is valid here
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exam_id", nullable = false)
  private Exam exam;

  // Cross-module reference: stored as plain UUID to avoid coupling with identity module
  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "source_file_url", length = 1000)
  private String sourceFileUrl;

  @Column(name = "auto_score")
  private Double autoScore;

  @Column(name = "final_score")
  private Double finalScore;

  @Column(name = "teacher_comment", columnDefinition = "TEXT")
  private String teacherComment;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private SubmissionStatus status = SubmissionStatus.PENDING;

  // Cross-module reference: stored as plain UUID to avoid coupling with identity module
  @Column(name = "reviewed_by_teacher_id")
  private UUID reviewedByTeacherId;

  @Column(name = "submitted_at")
  private Instant submittedAt;
}
