package com.example.internship.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.internship.common.BusinessException;
import com.example.internship.common.PageResult;
import com.example.internship.common.SecurityUtils;
import com.example.internship.dto.ApplicationRequest;
import com.example.internship.dto.ApplicationVO;
import com.example.internship.dto.HandleApplicationRequest;
import com.example.internship.entity.*;
import com.example.internship.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 投递服务
 */
@Service
public class ApplicationService {

    private final ApplicationMapper applicationMapper;
    private final JobMapper jobMapper;
    private final StudentInfoMapper studentInfoMapper;
    private final CompanyInfoMapper companyInfoMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public ApplicationService(ApplicationMapper applicationMapper, JobMapper jobMapper,
                              StudentInfoMapper studentInfoMapper, CompanyInfoMapper companyInfoMapper,
                              UserMapper userMapper, NotificationService notificationService) {
        this.applicationMapper = applicationMapper;
        this.jobMapper = jobMapper;
        this.studentInfoMapper = studentInfoMapper;
        this.companyInfoMapper = companyInfoMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    /**
     * 学生投递岗位
     */
    @Transactional
    public void apply(ApplicationRequest req) {
        Long studentId = SecurityUtils.getCurrentUserId();

        // 检查岗位存在且状态为 published
        Job job = jobMapper.selectById(req.getJobId());
        if (job == null) {
            throw new BusinessException(404, "岗位不存在");
        }
        if (!"published".equals(job.getStatus())) {
            throw new BusinessException(4002, "岗位已下架或不可投递");
        }

        // 检查是否已有投递记录
        // pending -> 不可重复投递
        var pendingList = applicationMapper.selectList(new LambdaQueryWrapper<Application>()
                .eq(Application::getStudentId, studentId)
                .eq(Application::getJobId, req.getJobId())
                .eq(Application::getStatus, "pending"));
        if (!pendingList.isEmpty()) {
            throw new BusinessException(4001, "请勿重复投递，您已有待处理的投递");
        }
        // accepted/rejected -> 不可再次投递
        var finalList = applicationMapper.selectList(new LambdaQueryWrapper<Application>()
                .eq(Application::getStudentId, studentId)
                .eq(Application::getJobId, req.getJobId())
                .in(Application::getStatus, "accepted", "rejected"));
        if (!finalList.isEmpty()) {
            throw new BusinessException("您已投递过该岗位，无法再次投递");
        }

        // 获取学生当前简历路径
        StudentInfo studentInfo = studentInfoMapper.selectOne(new LambdaQueryWrapper<StudentInfo>()
                .eq(StudentInfo::getUserId, studentId));
        if (studentInfo == null || studentInfo.getResumePath() == null) {
            throw new BusinessException("请先在个人中心上传简历");
        }

        // 创建投递记录
        Application application = new Application();
        application.setStudentId(studentId);
        application.setJobId(req.getJobId());
        application.setStatus("pending");
        application.setResumePath(studentInfo.getResumePath());
        application.setApplyComment(req.getApplyComment());
        application.setApplyTime(LocalDateTime.now());
        applicationMapper.insert(application);

        // 通知企业
        CompanyInfo companyInfo = companyInfoMapper.selectOne(new LambdaQueryWrapper<CompanyInfo>()
                .eq(CompanyInfo::getUserId, job.getCompanyId()));
        notificationService.createNotification(
                job.getCompanyId(),
                studentId,
                "学生「" + studentInfo.getName() + "」投递了您发布的岗位「" + job.getTitle() + "」",
                "application"
        );
    }

    /**
     * 查询投递列表 - 根据角色返回不同视角的数据
     */
    public PageResult<ApplicationVO> listApplications(int page, int size, String status) {
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();
        User user = userMapper.selectById(userId);

        var query = new LambdaQueryWrapper<Application>();
        if (status != null && !status.isEmpty()) {
            query.eq(Application::getStatus, status);
        }

        Page<Application> pageObj = new Page<>(page, size);
        if ("student".equals(role)) {
            query.eq(Application::getStudentId, userId);
            query.orderByDesc(Application::getApplyTime);
        } else if ("company".equals(role)) {
            // 企业只能看到投递给自有岗位的申请
            query.inSql(Application::getJobId,
                    "SELECT id FROM job WHERE company_id = " + userId);
            query.orderByDesc(Application::getApplyTime);
        } else if ("admin".equals(role)) {
            query.orderByDesc(Application::getApplyTime);
        }

        Page<Application> result = applicationMapper.selectPage(pageObj, query);

        var vos = result.getRecords().stream().map(a -> {
            ApplicationVO vo = new ApplicationVO();
            vo.setId(a.getId());
            vo.setStudentId(a.getStudentId());
            vo.setJobId(a.getJobId());
            vo.setStatus(a.getStatus());
            vo.setResumePath(a.getResumePath());
            vo.setApplyComment(a.getApplyComment());
            vo.setHandleComment(a.getHandleComment());
            vo.setApplyTime(a.getApplyTime());
            vo.setHandleTime(a.getHandleTime());

            // 关联岗位信息
            Job job = jobMapper.selectById(a.getJobId());
            if (job != null) {
                vo.setJobTitle(job.getTitle());
                CompanyInfo ci = companyInfoMapper.selectOne(new LambdaQueryWrapper<CompanyInfo>()
                        .eq(CompanyInfo::getUserId, job.getCompanyId()));
                vo.setCompanyName(ci != null ? ci.getCompanyName() : "");
            }

            // 关联学生信息
            StudentInfo si = studentInfoMapper.selectOne(new LambdaQueryWrapper<StudentInfo>()
                    .eq(StudentInfo::getUserId, a.getStudentId()));
            if (si != null) {
                vo.setStudentName(si.getName());
                vo.setStudentIdNum(si.getStudentId());
                vo.setMajor(si.getMajor());
            }

            return vo;
        }).toList();

        return new PageResult<>(vos, result.getTotal(), page, size);
    }

    /**
     * 企业处理投递（接受/拒绝）
     */
    @Transactional
    public void handleApplication(Long applicationId, HandleApplicationRequest req) {
        Long companyUserId = SecurityUtils.getCurrentUserId();

        Application application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(404, "投递记录不存在");
        }
        if (!"pending".equals(application.getStatus())) {
            throw new BusinessException("该投递已处理，无法重复操作");
        }

        // 验证该投递属于本企业的岗位
        Job job = jobMapper.selectById(application.getJobId());
        if (job == null || !job.getCompanyId().equals(companyUserId)) {
            throw new BusinessException(403, "无权处理该投递");
        }

        String action = req.getAction();
        if ("accept".equals(action)) {
            application.setStatus("accepted");
        } else if ("reject".equals(action)) {
            application.setStatus("rejected");
        } else {
            throw new BusinessException("操作无效");
        }

        application.setHandleComment(req.getHandleComment());
        application.setHandleTime(LocalDateTime.now());
        application.setHandlerId(companyUserId);
        applicationMapper.updateById(application);

        // 通知学生
        CompanyInfo ci = companyInfoMapper.selectOne(new LambdaQueryWrapper<CompanyInfo>()
                .eq(CompanyInfo::getUserId, companyUserId));
        String actionText = "accept".equals(action) ? "接受了" : "拒绝了";
        notificationService.createNotification(
                application.getStudentId(),
                companyUserId,
                "企业「" + (ci != null ? ci.getCompanyName() : "") + "」" + actionText + "您对「" + job.getTitle() + "」的投递",
                "application"
        );
    }

    /**
     * 学生撤销投递（仅 pending 状态）
     */
    @Transactional
    public void cancelApplication(Long applicationId) {
        Long studentId = SecurityUtils.getCurrentUserId();

        Application application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(404, "投递记录不存在");
        }
        if (!application.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权操作");
        }
        if (!"pending".equals(application.getStatus())) {
            throw new BusinessException("只能撤销待处理的投递");
        }

        application.setStatus("cancelled");
        applicationMapper.updateById(application);

        // 通知企业
        Job job = jobMapper.selectById(application.getJobId());
        StudentInfo si = studentInfoMapper.selectOne(new LambdaQueryWrapper<StudentInfo>()
                .eq(StudentInfo::getUserId, studentId));
        notificationService.createNotification(
                job.getCompanyId(),
                studentId,
                "学生「" + (si != null ? si.getName() : "") + "」撤销了对「" + job.getTitle() + "」的投递",
                "application"
        );
    }
}
