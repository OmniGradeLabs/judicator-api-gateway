package com.judicator.gateway.modules.exam.document;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "exam_anti_cheating_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AntiCheatingEventDoc {

  @Id private String id;

  // Logical reference to exams.id in PostgreSQL — no physical constraint
  @Indexed
  @Field("exam_id")
  private UUID examId;

  // Logical reference to identity users.id in PostgreSQL — no physical constraint
  @Indexed
  @Field("student_id")
  private UUID studentId;

  /** Category of the detected event, e.g. "FOCUS_LOST", "COPY_DETECTED" */
  @Field("event_type")
  private String eventType;

  @Field("details")
  private String details;

  /** Risk classification: "LOW", "MEDIUM", "HIGH", "CRITICAL" */
  @Field("risk_level")
  private String riskLevel;

  @Field("occurred_at")
  private Instant occurredAt;
}
