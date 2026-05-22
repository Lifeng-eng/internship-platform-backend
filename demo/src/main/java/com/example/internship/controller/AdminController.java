package com.example.internship.controller;

import com.example.internship.common.Result;
import com.example.internship.dto.ReviewRequest;
import com.example.internship.service.ReviewService;
import com.example.internship.service.UserManageService;
import com.example.internship.service.StatsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员控制器 - 审核、用户管理、统计
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ReviewService reviewService;
    private final UserManageService userManageService;
    private final StatsService statsService;

    public AdminController(ReviewService reviewService, UserManageService userManageService,
                           StatsService statsService) {
        this.reviewService = reviewService;
        this.userManageService = userManageService;
        this.statsService = statsService;
    }

    /**
     * 待审核岗位列表
     */
    @GetMapping("/jobs/pending")
    public Result<?> listPendingJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(reviewService.listPendingJobs(page, size));
    }

    /**
     * 审核岗位
     */
    @PostMapping("/jobs/{id}/review")
    public Result<?> reviewJob(@PathVariable Long id, @Valid @RequestBody ReviewRequest req) {
        reviewService.reviewJob(id, req);
        return Result.success();
    }

    /**
     * 用户列表
     */
    @GetMapping("/users")
    public Result<?> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role) {
        return Result.success(userManageService.listUsers(page, size, role));
    }

    /**
     * 禁用用户
     */
    @PutMapping("/users/{id}/disable")
    public Result<?> disableUser(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        userManageService.disableUser(id, reason);
        return Result.success();
    }

    /**
     * 启用用户
     */
    @PutMapping("/users/{id}/enable")
    public Result<?> enableUser(@PathVariable Long id) {
        userManageService.enableUser(id);
        return Result.success();
    }

    /**
     * 管理员统计
     */
    @GetMapping("/stats")
    public Result<?> getAdminStats() {
        return Result.success(statsService.getAdminStats());
    }
}
