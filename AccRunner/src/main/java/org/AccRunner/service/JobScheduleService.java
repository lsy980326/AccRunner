package org.AccRunner.service;

import lombok.RequiredArgsConstructor;
import org.AccRunner.domain.ScheduledJob;
import org.AccRunner.repository.ScheduledJobRepository;
import org.AccRunner.service.BatchJobService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobScheduleService {

    private final ScheduledJobRepository scheduledJobRepository;
    private final BatchJobService batchJobService;

    @Transactional
    public ScheduledJob createJob(ScheduledJob job) {
        job.setActive(false);
        return scheduledJobRepository.save(job);
    }

    public List<ScheduledJob> findAllJobs() {
        return scheduledJobRepository.findAll();
    }

    public ScheduledJob findJobById(Long id) {
        return scheduledJobRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Job not found with id: " + id));
    }

    @Transactional
    public ScheduledJob toggleActive(Long id) {
        ScheduledJob job = scheduledJobRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Job not found with id: " + id));
        job.setActive(!job.isActive());
        return scheduledJobRepository.save(job);
    }
}