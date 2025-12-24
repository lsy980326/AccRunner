package org.AccRunner.controller;

import lombok.RequiredArgsConstructor;
import org.AccRunner.domain.ScheduledJob;
import org.AccRunner.repository.ExecutionHistoryRepository;
import org.AccRunner.service.BatchJobService;
import org.AccRunner.service.JobScheduleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class JobScheduleController {

    private final JobScheduleService jobScheduleService;
    private final BatchJobService batchJobService;
    private final Map<String, DataSource> dataSources;
    private final ExecutionHistoryRepository executionHistoryRepository;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("jobs", jobScheduleService.findAllJobs());
        model.addAttribute("newJob", new ScheduledJob());
        model.addAttribute("dbNames", getAvailableDbNames());
        model.addAttribute("recentHistories", executionHistoryRepository.findTop50ByOrderByStartTimeDesc());

        return "dashboard";
    }

    @PostMapping("/schedules")
    public String createSchedule(@ModelAttribute ScheduledJob job) {
        jobScheduleService.createJob(job);
        return "redirect:/";
    }

    @PostMapping("/schedules/{id}/run")
    public String runSchedule(@PathVariable Long id) {
        batchJobService.runJobById(id);
        return "redirect:/";
    }

    @PostMapping("/schedules/{id}/toggle")
    public String toggleSchedule(@PathVariable Long id) {
        jobScheduleService.toggleActive(id);
        return "redirect:/";
    }

    private Set<String> getAvailableDbNames() {
        return dataSources.keySet().stream()
                .filter(name -> !name.equals("dataSource"))
                .map(name -> name.replace("DataSource", ""))
                .map(name -> name.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase())
                .collect(Collectors.toSet());
    }
}