package com.example.internship.controller;

import com.example.internship.common.Result;
import com.example.internship.dto.ApplicationRequest;
import com.example.internship.dto.HandleApplicationRequest;
import com.example.internship.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 投递控制器
 */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * 投递岗位（学生）
     */
    @PostMapping
    public Result<?> apply(@Valid @RequestBody ApplicationRequest req) {
        applicationService.apply(req);
        return Result.success();
    }

    /**
     * 投递列表（学生端/企业端根据 JWT role 自动区分）
     */
    @GetMapping
    public Result<?> listApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return Result.success(applicationService.listApplications(page, size, status));
    }

    /**
     * 处理投递（企业）
     */
    @PutMapping("/{id}")
    public Result<?> handleApplication(@PathVariable Long id, @Valid @RequestBody HandleApplicationRequest req) {
        applicationService.handleApplication(id, req);
        return Result.success();
    }

    /**
     * 撤销投递（学生）
     */
    @PostMapping("/{id}/cancel")
    public Result<?> cancelApplication(@PathVariable Long id) {
        applicationService.cancelApplication(id);
        return Result.success();
    }
}
