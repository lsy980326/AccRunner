package org.AccRunner.repository;

import org.AccRunner.domain.ExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutionHistoryRepository extends JpaRepository<ExecutionHistory, Long> {
    List<ExecutionHistory> findTop50ByOrderByStartTimeDesc();
    List<ExecutionHistory> findByScheduledJobIdOrderByStartTimeDesc(Long scheduledJobId);
}
