package com.example.internship.controller;

import com.example.internship.common.Result;
import com.example.internship.dto.JobRequest;
import com.example.internship.dto.JobVO;
import com.example.internship.service.JobService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 岗位控制器
 */
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * 岗位列表（分页+筛选）
     */
    @GetMapping
    public Result<?> listJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false, defaultValue = "time") String sort) {
        return Result.success(jobService.listJobs(page, size, keyword, location, salaryMin, salaryMax, sort));
    }

    /**
     * 岗位详情
     */
    @GetMapping("/{id}")
    public Result<JobVO> getJobDetail(@PathVariable Long id) {
        return Result.success(jobService.getJobDetail(id));
    }

    /**
     * 发布岗位（企业）
     */
    @PostMapping
    public Result<?> createJob(@Valid @RequestBody JobRequest req) {
        jobService.createJob(req);
        return Result.success();
    }

    /**
     * 编辑岗位（企业，自有岗位）
     */
    @PutMapping("/{id}")
    public Result<?> updateJob(@PathVariable Long id, @Valid @RequestBody JobRequest req) {
        jobService.updateJob(id, req);
        return Result.success();
    }

    /**
     * 删除岗位（企业，pending/rejected）
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return Result.success();
    }

    /**
     * 下架岗位（企业）
     */
    @PostMapping("/{id}/close")
    public Result<?> closeJob(@PathVariable Long id) {
        jobService.closeJob(id);
        return Result.success();
    }

    /**
     * 推荐岗位（学生端）
     */
    @GetMapping("/recommended")
    public Result<List<JobVO>> getRecommendedJobs() {
        return Result.success(jobService.getRecommendedJobs());
    }
}
