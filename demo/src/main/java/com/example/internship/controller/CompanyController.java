package com.example.internship.controller;

import com.example.internship.common.Result;
import com.example.internship.service.JobService;
import com.example.internship.service.StatsService;
import org.springframework.web.bind.annotation.*;

/**
 * 企业控制器 - 统计、自有岗位管理
 */
@RestController
@RequestMapping("/api/company")
public class CompanyController {

    private final StatsService statsService;
    private final JobService jobService;

    public CompanyController(StatsService statsService, JobService jobService) {
        this.statsService = statsService;
        this.jobService = jobService;
    }

    /**
     * 企业统计
     */
    @GetMapping("/stats")
    public Result<?> getCompanyStats() {
        return Result.success(statsService.getCompanyStats());
    }

    /**
     * 我发布的岗位
     */
    @GetMapping("/jobs")
    public Result<?> listCompanyJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return Result.success(jobService.listCompanyJobs(page, size, status));
    }
}
