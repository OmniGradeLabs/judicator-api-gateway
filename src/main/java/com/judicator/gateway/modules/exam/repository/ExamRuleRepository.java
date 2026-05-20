package com.judicator.gateway.modules.exam.repository;

import com.judicator.gateway.modules.exam.entity.ExamRule;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamRuleRepository extends JpaRepository<ExamRule, UUID> {

  Optional<ExamRule> findByExam_IdAndExam_TenantId(UUID examId, UUID tenantId);

  @Query(
      """
      select r
      from ExamRule r
      join fetch r.exam e
      where e.id in :examIds
        and e.tenantId = :tenantId
      """)
  List<ExamRule> findAllByExamIdsAndTenantId(
      @Param("examIds") Collection<UUID> examIds, @Param("tenantId") UUID tenantId);
}
