package org.AccRunner.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Slf4j
public class DynamicQueryTasklet implements Tasklet {

    private final String sqlQuery;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DynamicQueryTasklet(String sqlQuery, DataSource dataSource) {
        this.sqlQuery = sqlQuery;
        this.dataSource = dataSource;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Executing query: [{}] on datasource: {}", sqlQuery, dataSource.toString());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        if (isSelectQuery(sqlQuery)) {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery);
            log.info("Query result ({} rows):", results.size());

            for (int i = 0; i < Math.min(results.size(), 5); i++) {
                String rowAsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results.get(i));
                log.info("Row {}:\n{}", i + 1, rowAsJson);
            }
            if (results.size() > 5) {
                log.info("... and {} more rows.", results.size() - 5);
            }

            contribution.getStepExecution().getJobExecution().getExecutionContext()
                    .put("selectResultCount", results.size());

        } else {
            jdbcTemplate.execute(sqlQuery);
            log.info("Non-SELECT query executed successfully.");
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * 입력된 쿼리가 SELECT 문인지 간단하게 확인하는 헬퍼 메소드
     */
    private boolean isSelectQuery(String query) {
        // 공백, 줄바꿈 등을 제거하고 소문자로 변환하여 "select"로 시작하는지 확인
        return query.trim().toLowerCase().startsWith("select");
    }
}