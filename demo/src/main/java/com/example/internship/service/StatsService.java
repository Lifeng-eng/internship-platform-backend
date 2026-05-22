package com.example.internship.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.internship.common.SecurityUtils;
import com.example.internship.entity.Application;
import com.example.internship.entity.Job;
import com.example.internship.entity.User;
import com.example.internship.mapper.ApplicationMapper;
import com.example.internship.mapper.JobMapper;
import com.example.internship.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 统计服务
 */
@Service
public class StatsService {

    private final JobMapper jobMapper;
    private final ApplicationMapper applicationMapper;
    private final UserMapper userMapper;

    public StatsService(JobMapper jobMapper, ApplicationMapper applicationMapper,
                        UserMapper userMapper) {
        this.jobMapper = jobMapper;
        this.applicationMapper = applicationMapper;
        this.userMapper = userMapper;
    }

    /**
     * 管理员统计
     */
    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 总数
        Long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());
        Long totalJobs = jobMapper.selectCount(new LambdaQueryWrapper<>());
        Long totalApplications = applicationMapper.selectCount(new LambdaQueryWrapper<>());

        stats.put("totalUsers", totalUsers);
        stats.put("totalJobs", totalJobs);
        stats.put("totalApplications", totalApplications);

        // 岗位状态分布 → [{status, count}, ...]
        List<Map<String, Object>> jobStatusRaw = jobMapper.countByStatus();
        List<Map<String, Object>> jobStatusDistribution = new ArrayList<>();
        for (Map<String, Object> row : jobStatusRaw) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("status", row.get("status"));
            item.put("count", row.get("count"));
            jobStatusDistribution.add(item);
        }
        stats.put("jobStatusDistribution", jobStatusDistribution);

        // 投递状态分布 → [{status, count}, ...]
        List<Map<String, Object>> appStatusRaw = applicationMapper.countByStatus();
        List<Map<String, Object>> appStatusDistribution = new ArrayList<>();
        long acceptedCount = 0;
        for (Map<String, Object> row : appStatusRaw) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("status", row.get("status"));
            item.put("count", row.get("count"));
            appStatusDistribution.add(item);
            if ("accepted".equals(row.get("status"))) {
                acceptedCount = ((Number) row.get("count")).longValue();
            }
        }
        stats.put("applicationStatusDistribution", appStatusDistribution);

        // 通过率
        double acceptanceRate = totalApplications > 0
                ? Math.round(acceptedCount * 1000.0 / totalApplications) / 10.0
                : 0.0;
        stats.put("acceptanceRate", acceptanceRate);

        // 投递趋势（近30天）
        stats.put("applicationTrend", applicationMapper.countByApplyDate());

        return stats;
    }

    /**
     * 企业统计
     */
    public Map<String, Object> getCompanyStats() {
        Long companyId = SecurityUtils.getCurrentUserId();

        Map<String, Object> stats = new LinkedHashMap<>();

        Long totalJobs = jobMapper.selectCount(new LambdaQueryWrapper<Job>()
                .eq(Job::getCompanyId, companyId));
        stats.put("totalJobs", totalJobs);

        stats.put("applicationStatusDistribution", applicationMapper.countByStatusForCompany(companyId));

        long totalApplications = applicationMapper.selectCount(
                new LambdaQueryWrapper<Application>()
                        .inSql(Application::getJobId,
                                "SELECT id FROM job WHERE company_id = " + companyId));
        stats.put("totalApplications", totalApplications);

        stats.put("applicationTrend", applicationMapper.countByDateForCompany(companyId));

        return stats;
    }
}
