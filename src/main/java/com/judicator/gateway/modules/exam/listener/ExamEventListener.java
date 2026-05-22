package com.judicator.gateway.modules.exam.listener;

import com.judicator.gateway.common.events.TenantSuspendedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Exam module listener for cross-module domain events originating from the {@code identity} module.
 *
 * <p>Uses {@link TransactionalEventListener} with {@link TransactionPhase#AFTER_COMMIT} to
 * guarantee that the event is only processed after the identity module's transaction has
 * successfully committed to the database. This prevents reacting to a suspension that was later
 * rolled back.
 *
 * <p><strong>Dependency rule:</strong> This class imports ONLY from {@code common.events} — never
 * from the {@code identity} module's internals.
 */
@Slf4j
@Component
public class ExamEventListener {

  /**
   * Reacts to a tenant being suspended or deleted in the identity module.
   *
   * <p>Triggered AFTER the identity transaction commits, ensuring data consistency.
   *
   * <p><strong>TODO (implementation):</strong> Inject {@code ExamRepository} (or an {@code
   * ExamService}) and execute:
   *
   * <pre>{@code
   * examRepository.closeAllActiveExamsByTenantId(event.tenantId(), ExamStatus.CLOSED);
   * }</pre>
   *
   * This will mark all exams in {@code DRAFT} or {@code PUBLISHED} state for this tenant as {@code
   * CLOSED}, preventing students from submitting to a suspended tenant's exams.
   *
   * @param event the {@link TenantSuspendedEvent} published by {@code TenantServiceImpl}
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTenantSuspended(TenantSuspendedEvent event) {
    log.warn(
        "[ExamEventListener] Received TenantSuspendedEvent — tenantId: {}, suspendedAt: {}. "
            + "Initiating closure of all active exams for this tenant.",
        event.tenantId(),
        event.suspendedAt());

    // TODO: Inject ExamRepository and close all active exams for this tenant.
    // Example logic to be implemented:
    //
    //   int closedCount = examRepository.closeAllActiveExamsByTenantId(
    //       event.tenantId(), ExamStatus.CLOSED);
    //
    //   log.info("[ExamEventListener] Closed {} active exam(s) for suspended Tenant ID: {}",
    //       closedCount, event.tenantId());
    //
    // NOTE: This runs in its own transaction (outside the identity module's TX, which has already
    // committed). Wrap in @Transactional on this method if write operations are needed.
  }
}
