package com.example.internship.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.internship.common.SecurityUtils;
import com.example.internship.entity.Job;
import com.example.internship.mapper.ApplicationMapper;
import com.example.internship.mapper.JobMapper;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 统计服务
 */
@Service
public class StatsService {

    private final JobMapper jobMapper;
    private final ApplicationMapper applicationMapper;

    public StatsService(JobMapper jobMapper, ApplicationMapper applicationMapper) {
        this.jobMapper = jobMapper;
        this.applicationMapper = applicationMapper;
    }

    /**
     * 管理员统计
     */
    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 岗位状态分布
        List<Map<String, Object>> jobStatusCounts = jobMapper.countByStatus();
        Map<String, Long> jobStatusMap = new LinkedHashMap<>();
        for (Map<String, Object> row : jobStatusCounts) {
            jobStatusMap.put((String) row.get("status"), (Long) row.get("count"));
        }
        stats.put("jobStatusDistribution", jobStatusMap);

        // 投递状态分布
        List<Map<String, Object>> appStatusCounts = applicationMapper.countByStatus();
        Map<String, Long> appStatusMap = new LinkedHashMap<>();
        for (Map<String, Object> row : appStatusCounts) {
            appStatusMap.put((String) row.get("status"), (Long) row.get("count"));
        }
        stats.put("applicationStatusDistribution", appStatusMap);

        // 岗位发布趋势（近30天）
        stats.put("jobPublishTrend", jobMapper.countByPublishDate());

        // 投递趋势（近30天）
        stats.put("applicationTrend", applicationMapper.countByApplyDate());

        // 按岗位投递量
        stats.put("applicationsByJob", applicationMapper.countByJob());

        // 总数
        stats.put("totalUsers", jobMapper.selectCount(new LambdaQueryWrapper<>())); // 此处实际需要 userMapper

        return stats;
    }

    /**
     * 企业统计
     */
    public Map<String, Object> getCompanyStats() {
        Long companyId = SecurityUtils.getCurrentUserId();

        Map<String, Object> stats = new LinkedHashMap<>();

        // 发布岗位总数
        Long totalJobs = jobMapper.selectCount(new LambdaQueryWrapper<Job>()
                .eq(Job::getCompanyId, companyId));
        stats.put("totalJobs", totalJobs);

        // 各状态投递数量
        stats.put("applicationStatusDistribution", applicationMapper.countByStatusForCompany(companyId));

        // 各岗位投递量
        long totalApplications = applicationMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.internship.entity.Application>()
                        .inSql(com.example.internship.entity.Application::getJobId,
                                "SELECT id FROM job WHERE company_id = " + companyId));
        stats.put("totalApplications", totalApplications);

        // 近7天投递趋势
        stats.put("applicationTrend", applicationMapper.countByDateForCompany(companyId));

        return stats;
    }
}
