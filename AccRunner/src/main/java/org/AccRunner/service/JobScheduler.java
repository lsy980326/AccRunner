package org.AccRunner.service;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.AccRunner.domain.ScheduledJob;
import org.AccRunner.repository.ScheduledJobRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobScheduler {

    private final ScheduledJobRepository scheduledJobRepository;
    private final BatchJobService batchJobService;
    private final CronParser cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING));

    @Scheduled(cron = "0 * * * * *")
    public void runScheduledJobs() {
        log.debug("Checking for scheduled jobs to run...");
        List<ScheduledJob> activeJobs = scheduledJobRepository.findByActive(true);

        ZonedDateTime now = ZonedDateTime.now();

        for (ScheduledJob job : activeJobs) {
            try {
                Cron cron = cronParser.parse(job.getCronExpression());
                ExecutionTime executionTime = ExecutionTime.forCron(cron);

                if (executionTime.isMatch(now)) {
                    log.info("Cron matched! Triggering job: {}", job.getJobName());
                    batchJobService.runJob(job);
                }
            } catch (Exception e) {
                log.error("Failed to schedule job: {}", job.getJobName(), e);
            }
        }
    }
}