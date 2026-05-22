package com.example.internship.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.internship.common.BusinessException;
import com.example.internship.common.PageResult;
import com.example.internship.common.SecurityUtils;
import com.example.internship.dto.JobRequest;
import com.example.internship.dto.JobVO;
import com.example.internship.entity.*;
import com.example.internship.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 岗位服务
 */
@Service
public class JobService {

    private final JobMapper jobMapper;
    private final CompanyInfoMapper companyInfoMapper;
    private final StudentInfoMapper studentInfoMapper;
    private final ApplicationMapper applicationMapper;
    private final NotificationService notificationService;

    public JobService(JobMapper jobMapper, CompanyInfoMapper companyInfoMapper,
                      StudentInfoMapper studentInfoMapper, ApplicationMapper applicationMapper,
                      NotificationService notificationService) {
        this.jobMapper = jobMapper;
        this.companyInfoMapper = companyInfoMapper;
        this.studentInfoMapper = studentInfoMapper;
        this.applicationMapper = applicationMapper;
        this.notificationService = notificationService;
    }

    /**
     * 岗位列表（分页+筛选+排序）
     */
    public PageResult<JobVO> listJobs(int page, int size, String keyword, String location,
                                       BigDecimal salaryMin, BigDecimal salaryMax, String sort) {
        var query = new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, "published");

        if (keyword != null && !keyword.isEmpty()) {
            query.and(w -> w.like(Job::getTitle, keyword).or().like(Job::getDescription, keyword));
        }
        if (location != null && !location.isEmpty()) {
            query.eq(Job::getLocation, location);
        }
        if (salaryMin != null) {
            query.ge(Job::getSalaryMax, salaryMin);
        }
        if (salaryMax != null) {
            query.le(Job::getSalaryMin, salaryMax);
        }

        if ("salary".equals(sort)) {
            query.orderByDesc(Job::getSalaryMax);
        } else {
            query.orderByDesc(Job::getPublishTime);
        }

        Page<Job> pageObj = new Page<>(page, size);
        Page<Job> result = jobMapper.selectPage(pageObj, query);

        var vos = result.getRecords().stream().map(job -> {
            JobVO vo = toJobVO(job);
            // 附加当前学生投递状态
            try {
                Long userId = SecurityUtils.getCurrentUserId();
                User user = null;
                try {
                    user = new User();
                    user.setId(userId);
                    // Check role by attempting student lookup
                } catch (Exception ignored) {}
                if (user != null) {
                    setApplyStatus(vo, userId);
                }
            } catch (Exception e) {
                // 未登录用户不附加投递状态
            }
            return vo;
        }).toList();

        return new PageResult<>(vos, result.getTotal(), page, size);
    }

    /**
     * 岗位详情
     */
    public JobVO getJobDetail(Long jobId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(404, "岗位不存在");
        }
        // 访客只能看已发布的，企业和管理员可看全部
        if ("published".equals(job.getStatus())) {
            JobVO vo = toJobVO(job);
            try {
                Long userId = SecurityUtils.getCurrentUserId();
                setApplyStatus(vo, userId);
            } catch (Exception ignored) {}
            return vo;
        }

        // 非发布状态，检查权限
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            String role = SecurityUtils.getCurrentUserRole();
            if ("company".equals(role) && job.getCompanyId().equals(userId)) {
                return toJobVO(job);
            }
            if ("admin".equals(role)) {
                return toJobVO(job);
            }
        } catch (Exception e) {
            throw new BusinessException(404, "岗位不存在");
        }
        throw new BusinessException(404, "岗位不存在");
    }

    /**
     * 发布岗位
     */
    @Transactional
    public void createJob(JobRequest req) {
        Long companyId = SecurityUtils.getCurrentUserId();

        Job job = new Job();
        job.setTitle(req.getTitle());
        job.setCompanyId(companyId);
        job.setSalaryMin(req.getSalaryMin());
        job.setSalaryMax(req.getSalaryMax());
        job.setLocation(req.getLocation());
        job.setDescription(req.getDescription());
        job.setRequirements(req.getRequirements());
        job.setStatus("pending");
        job.setPublishTime(LocalDateTime.now());
        jobMapper.insert(job);

        // 通知管理员审核（发送给所有管理员）
        notificationService.createNotification(
                null, // 由 NotificationController 处理：所有管理员可见
                companyId,
                "新岗位「" + job.getTitle() + "」已提交，等待审核",
                "review"
        );
    }

    /**
     * 编辑岗位（状态重置为 pending）
     */
    @Transactional
    public void updateJob(Long jobId, JobRequest req) {
        Long companyId = SecurityUtils.getCurrentUserId();

        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(404, "岗位不存在");
        }
        if (!job.getCompanyId().equals(companyId)) {
            throw new BusinessException(403, "无权操作");
        }

        job.setTitle(req.getTitle());
        job.setSalaryMin(req.getSalaryMin());
        job.setSalaryMax(req.getSalaryMax());
        job.setLocation(req.getLocation());
        job.setDescription(req.getDescription());
        job.setRequirements(req.getRequirements());
        job.setStatus("pending"); // 重置为待审核
        jobMapper.updateById(job);
    }

    /**
     * 删除岗位（仅 pending/rejected，且无投递记录）
     */
    @Transactional
    public void deleteJob(Long jobId) {
        Long companyId = SecurityUtils.getCurrentUserId();

        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(404, "岗位不存在");
        }
        if (!job.getCompanyId().equals(companyId)) {
            throw new BusinessException(403, "无权操作");
        }
        if (!"pending".equals(job.getStatus()) && !"rejected".equals(job.getStatus())) {
            throw new BusinessException("只能删除待审核或被拒绝的岗位");
        }

        // 检查是否有投递记录
        var apps = applicationMapper.selectList(new LambdaQueryWrapper<Application>()
                .eq(Application::getJobId, jobId));
        if (!apps.isEmpty()) {
            throw new BusinessException("该岗位已有投递记录，无法删除");
        }

        jobMapper.deleteById(jobId);
    }

    /**
     * 下架岗位
     */
    @Transactional
    public void closeJob(Long jobId) {
        Long companyId = SecurityUtils.getCurrentUserId();

        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(404, "岗位不存在");
        }
        if (!job.getCompanyId().equals(companyId)) {
            throw new BusinessException(403, "无权操作");
        }

        job.setStatus("closed");
        jobMapper.updateById(job);
    }

    /**
     * 企业查询自有岗位
     */
    public PageResult<JobVO> listCompanyJobs(int page, int size, String status) {
        Long companyId = SecurityUtils.getCurrentUserId();

        var query = new LambdaQueryWrapper<Job>()
                .eq(Job::getCompanyId, companyId);
        if (status != null && !status.isEmpty()) {
            query.eq(Job::getStatus, status);
        }
        query.orderByDesc(Job::getCreateTime);

        Page<Job> pageObj = new Page<>(page, size);
        Page<Job> result = jobMapper.selectPage(pageObj, query);

        var vos = result.getRecords().stream().map(this::toJobVO).toList();
        return new PageResult<>(vos, result.getTotal(), page, size);
    }

    /**
     * 智能推荐（学生端）
     */
    public List<JobVO> getRecommendedJobs() {
        Long userId = SecurityUtils.getCurrentUserId();
        StudentInfo studentInfo = studentInfoMapper.selectOne(new LambdaQueryWrapper<StudentInfo>()
                .eq(StudentInfo::getUserId, userId));
        if (studentInfo == null) {
            return List.of();
        }

        List<Job> jobs = jobMapper.findRecommended(
                studentInfo.getMajor(),
                studentInfo.getPreferredJobs(),
                studentInfo.getPreferredLocations()
        );
        return jobs.stream().map(this::toJobVO).toList();
    }

    /**
     * 附加当前学生对岗位的投递状态
     */
    private void setApplyStatus(JobVO vo, Long userId) {
        var apps = applicationMapper.selectList(new LambdaQueryWrapper<Application>()
                .eq(Application::getStudentId, userId)
                .eq(Application::getJobId, vo.getId())
                .orderByDesc(Application::getId));
        if (!apps.isEmpty()) {
            vo.setApplyStatus(apps.get(0).getStatus());
            vo.setApplicationId(apps.get(0).getId());
        }
    }

    private JobVO toJobVO(Job job) {
        JobVO vo = new JobVO();
        vo.setId(job.getId());
        vo.setTitle(job.getTitle());
        vo.setCompanyId(job.getCompanyId());
        vo.setSalaryMin(job.getSalaryMin());
        vo.setSalaryMax(job.getSalaryMax());
        vo.setLocation(job.getLocation());
        vo.setDescription(job.getDescription());
        vo.setRequirements(job.getRequirements());
        vo.setStatus(job.getStatus());
        vo.setPublishTime(job.getPublishTime());
        vo.setReviewTime(job.getReviewTime());

        // 填充企业信息
        CompanyInfo ci = companyInfoMapper.selectOne(new LambdaQueryWrapper<CompanyInfo>()
                .eq(CompanyInfo::getUserId, job.getCompanyId()));
        if (ci != null) {
            vo.setCompanyName(ci.getCompanyName());
            vo.setCompanyIntro(ci.getCompanyIntro());
            vo.setCompanyScale(ci.getCompanyScale());
        }

        return vo;
    }
}
