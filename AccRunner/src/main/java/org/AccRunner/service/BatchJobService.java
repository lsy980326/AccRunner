package org.AccRunner.service;

import org.AccRunner.batch.DynamicQueryTasklet;
import org.AccRunner.domain.ExecutionHistory;
import org.AccRunner.domain.ExecutionStatus;
import org.AccRunner.domain.ScheduledJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.AccRunner.repository.ExecutionHistoryRepository;
import org.AccRunner.repository.ScheduledJobRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchJobService {

    private final JobLauncher jobLauncher;
    private final JobRepository jobRepository;
    private final Map<String, DataSource> dataSources;
    private final PlatformTransactionManager transactionManager;
    private final ScheduledJobRepository scheduledJobRepository;
    private final ExecutionHistoryRepository executionHistoryRepository;

    public void runJobById(Long id) {
        ScheduledJob jobToRun = scheduledJobRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Job not found with id: " + id));
        this.runJob(jobToRun);
    }
    public void runJob(ScheduledJob scheduledJob) {
        ExecutionHistory history = new ExecutionHistory();
        history.setScheduledJob(scheduledJob);
        history.setJobName(scheduledJob.getJobName());
        history.setStartTime(LocalDateTime.now());
        history.setStatus(ExecutionStatus.STARTED);
        history.setMessage("Job started");
        history = executionHistoryRepository.save(history);

        LocalDateTime endTime = null;
        try {
            // 1. 대상 DataSource 찾기
            String targetDbName = scheduledJob.getTargetDbName(); // "anasa-db"

            // 첫 글자는 그대로 두고, 하이픈(-) 뒤의 첫 글자를 대문자로 바꿈 (예: anasa-db -> anasaDb)
            String camelCaseName = toCamelCase(targetDbName);
            String targetDbBeanName = camelCaseName + "DataSource"; // "anasaDbDataSource"

            DataSource targetDataSource = dataSources.get(targetDbBeanName);

            if (targetDataSource == null) {
                log.error("DataSource를 찾을 수 없습니다. 찾으려는 Bean 이름: [{}], 사용 가능한 Bean: {}",
                          targetDbBeanName, dataSources.keySet());
                throw new NoSuchElementException("Cannot find datasource with name: " + scheduledJob.getTargetDbName());
            }

            // 2. Tasklet 생성
            DynamicQueryTasklet dynamicQueryTasklet = new DynamicQueryTasklet(scheduledJob.getSqlQuery(), targetDataSource);

            // 3. Step 생성 (Spring Batch 5 스타일)
            Step dynamicStep = new StepBuilder("dynamicStep-" + scheduledJob.getId(), jobRepository)
                    .tasklet(dynamicQueryTasklet, transactionManager) // Tasklet과 트랜잭션 매니저를 전달
                    .build();

            // 4. Job 생성 (Spring Batch 5 스타일)
            Job dynamicJob = new JobBuilder("dynamicJob-" + scheduledJob.getJobName(), jobRepository)
                    .incrementer(new RunIdIncrementer()) // 동일한 파라미터로도 Job을 재실행 가능하게 함
                    .start(dynamicStep)
                    .build();

            // 5. Job 실행
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("scheduleId", String.valueOf(scheduledJob.getId()))
                    .addString("jobName", scheduledJob.getJobName())
                    .toJobParameters();

            log.info("Job 실행 요청: {}, 파라미터: {}", dynamicJob.getName(), jobParameters);
            JobExecution jobExecution = jobLauncher.run(dynamicJob, jobParameters);
            log.info("Job 실행 완료: {}, 상태: {}", dynamicJob.getName(), jobExecution.getStatus());

            endTime = LocalDateTime.now();
            history.setEndTime(endTime);
            history.setStatus(ExecutionStatus.COMPLETED);
            history.setMessage("Job completed with status: " + jobExecution.getStatus());
            if (history.getStartTime() != null) {
                history.setDurationMs(Duration.between(history.getStartTime(), endTime).toMillis());
            }
            executionHistoryRepository.save(history);
        } catch (Exception e) {
            endTime = LocalDateTime.now();
            history.setEndTime(endTime);
            history.setStatus(ExecutionStatus.FAILED);
            String msg = e.getMessage();
            if (msg != null && msg.length() > 2000) {
                msg = msg.substring(0, 2000);
            }
            history.setMessage(msg);
            if (history.getStartTime() != null) {
                history.setDurationMs(Duration.between(history.getStartTime(), endTime).toMillis());
            }
            executionHistoryRepository.save(history);
            log.error("Job 실행 중 오류 발생 - Job ID: {}", scheduledJob.getId(), e);
        }
    }

    /**
     * kebab-case (예: anasa-db)를 camelCase (예: anasaDb)로 변환하는 헬퍼 메소드
     */
    private String toCamelCase(String kebabCase) {
        StringBuilder sb = new StringBuilder();
        boolean nextIsUpper = false;
        for (char c : kebabCase.toCharArray()) {
            if (c == '-') {
                nextIsUpper = true;
            } else {
                if (nextIsUpper) {
                    sb.append(Character.toUpperCase(c));
                    nextIsUpper = false;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}