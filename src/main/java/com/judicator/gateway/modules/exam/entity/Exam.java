package com.judicator.gateway.modules.exam.entity;

import com.judicator.gateway.infrastructure.persistence.BaseEntity;
import com.judicator.gateway.modules.exam.enumType.ExamStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "exams")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class Exam extends BaseEntity {

  // Cross-module reference: stored as plain UUID to avoid coupling with identity module
  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String markdown;

  @Column(name = "time_limit_minutes")
  private Integer timeLimitMinutes;

  @Column(name = "pace_limits", length = 255)
  private String paceLimits;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private ExamStatus status = ExamStatus.DRAFT;

  // Cross-module reference: stored as plain UUID to avoid coupling with identity module
  @Column(name = "created_by_teacher_id", nullable = false)
  private UUID createdByTeacherId;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private boolean isDeleted = false;
}
