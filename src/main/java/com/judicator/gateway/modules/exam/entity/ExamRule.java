package com.judicator.gateway.modules.exam.entity;

import com.judicator.gateway.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "exam_rules")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ExamRule extends BaseEntity {

  // Intra-module relation: physical FK to exams table is valid here
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exam_id", nullable = false, unique = true)
  private Exam exam;

  // JSONB column — stores structured rule configuration as a JSON blob
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "rule_payload", columnDefinition = "jsonb")
  private String rulePayload;

  @Column(name = "playwright_zip_url", length = 1000)
  private String playwrightZipUrl;
}
