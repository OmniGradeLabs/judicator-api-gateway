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

@Document(collection = "submission_grading_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionGradingLogDoc {

  @Id private String id;

  // Logical reference to exam_submissions.id in PostgreSQL — no physical constraint
  @Indexed
  @Field("submission_id")
  private UUID submissionId;

  /** Stream source: "stdout", "stderr", or "ai" */
  @Field("stream")
  private String stream;

  @Field("content")
  private String content;

  @Field("created_at")
  private Instant createdAt;
}
