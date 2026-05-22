package com.example.internship.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.internship.common.BusinessException;
import com.example.internship.common.PageResult;
import com.example.internship.common.SecurityUtils;
import com.example.internship.dto.JobVO;
import com.example.internship.dto.ReviewRequest;
import com.example.internship.entity.CompanyInfo;
import com.example.internship.entity.Job;
import com.example.internship.entity.Review;
import com.example.internship.mapper.CompanyInfoMapper;
import com.example.internship.mapper.JobMapper;
import com.example.internship.mapper.ReviewMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 审核服务
 */
@Service
public class ReviewService {

    private final JobMapper jobMapper;
    private final ReviewMapper reviewMapper;
    private final CompanyInfoMapper companyInfoMapper;
    private final NotificationService notificationService;

    public ReviewService(JobMapper jobMapper, ReviewMapper reviewMapper,
                         CompanyInfoMapper companyInfoMapper, NotificationService notificationService) {
        this.jobMapper = jobMapper;
        this.reviewMapper = reviewMapper;
        this.companyInfoMapper = companyInfoMapper;
        this.notificationService = notificationService;
    }

    /**
     * 待审核岗位列表
     */
    public PageResult<JobVO> listPendingJobs(int page, int size) {
        var query = new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, "pending")
                .orderByAsc(Job::getPublishTime);

        Page<Job> pageObj = new Page<>(page, size);
        Page<Job> result = jobMapper.selectPage(pageObj, query);

        var vos = result.getRecords().stream().map(this::toJobVO).toList();
        return new PageResult<>(vos, result.getTotal(), page, size);
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

        CompanyInfo ci = companyInfoMapper.selectOne(new LambdaQueryWrapper<CompanyInfo>()
                .eq(CompanyInfo::getUserId, job.getCompanyId()));
        if (ci != null) {
            vo.setCompanyName(ci.getCompanyName());
            vo.setCompanyIntro(ci.getCompanyIntro());
            vo.setCompanyScale(ci.getCompanyScale());
        }
        return vo;
    }

    /**
     * 审核岗位
     */
    @Transactional
    public void reviewJob(Long jobId, ReviewRequest req) {
        Long adminId = SecurityUtils.getCurrentUserId();

        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(404, "岗位不存在");
        }
        if (!"pending".equals(job.getStatus())) {
            throw new BusinessException("该岗位已审核");
        }
        if ("reject".equals(req.getResult()) && (req.getComment() == null || req.getComment().isEmpty())) {
            throw new BusinessException("拒绝时必须填写审核意见");
        }

        // 更新岗位状态
        job.setStatus("pass".equals(req.getResult()) ? "published" : "rejected");
        job.setReviewTime(LocalDateTime.now());
        job.setReviewerId(adminId);
        jobMapper.updateById(job);

        // 记录审核
        Review review = new Review();
        review.setJobId(jobId);
        review.setAdminId(adminId);
        review.setReviewTime(LocalDateTime.now());
        review.setResult(req.getResult());
        review.setComment(req.getComment());
        reviewMapper.insert(review);

        // 通知企业
        String resultText = "pass".equals(req.getResult()) ? "通过" : "拒绝";
        notificationService.createNotification(
                job.getCompanyId(),
                adminId,
                "您发布的岗位「" + job.getTitle() + "」审核" + resultText +
                        (req.getComment() != null ? "，意见：" + req.getComment() : ""),
                "review"
        );
    }
}
