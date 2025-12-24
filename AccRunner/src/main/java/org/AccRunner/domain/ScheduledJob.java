package org.AccRunner.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ScheduledJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String jobName;
    private String cronExpression;
    private String targetDbName;
    @Lob
    private String sqlQuery;
    private String description;
    private boolean active;
}