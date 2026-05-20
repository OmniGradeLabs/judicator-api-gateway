package com.judicator.gateway.modules.exam.repository;

import com.judicator.gateway.modules.exam.entity.Exam;
import com.judicator.gateway.modules.exam.enumType.ExamStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamRepository extends JpaRepository<Exam, UUID> {

  Optional<Exam> findByIdAndTenantId(UUID id, UUID tenantId);

  List<Exam> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update Exam e
      set e.status = :closedStatus
      where e.tenantId = :tenantId
        and e.status in :activeStatuses
        and e.isDeleted = false
      """)
  int closeAllActiveExamsByTenantId(
      @Param("tenantId") UUID tenantId,
      @Param("closedStatus") ExamStatus closedStatus,
      @Param("activeStatuses") List<ExamStatus> activeStatuses);
}
