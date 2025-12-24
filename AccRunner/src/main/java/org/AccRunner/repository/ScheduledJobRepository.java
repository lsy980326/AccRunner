package org.AccRunner.repository;

import org.AccRunner.domain.ScheduledJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {
    List<ScheduledJob> findByActive(boolean active);
}