package com.example.internship.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.internship.common.BusinessException;
import com.example.internship.common.SecurityUtils;
import com.example.internship.dto.ChangePasswordRequest;
import com.example.internship.dto.UpdateProfileRequest;
import com.example.internship.entity.*;
import com.example.internship.mapper.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 个人中心服务
 */
@Service
public class ProfileService {

    private final UserMapper userMapper;
    private final StudentInfoMapper studentInfoMapper;
    private final CompanyInfoMapper companyInfoMapper;
    private final AdminInfoMapper adminInfoMapper;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserMapper userMapper, StudentInfoMapper studentInfoMapper,
                          CompanyInfoMapper companyInfoMapper, AdminInfoMapper adminInfoMapper,
                          PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.studentInfoMapper = studentInfoMapper;
        this.companyInfoMapper = companyInfoMapper;
        this.adminInfoMapper = adminInfoMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 获取个人信息
     */
    public Map<String, Object> getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        String role = user.getRole();

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("role", role);
        profile.put("status", user.getStatus());
        profile.put("createTime", user.getCreateTime());

        switch (role) {
            case "student":
                StudentInfo si = studentInfoMapper.selectOne(new LambdaQueryWrapper<StudentInfo>()
                        .eq(StudentInfo::getUserId, userId));
                if (si != null) {
                    profile.put("studentId", si.getStudentId());
                    profile.put("name", si.getName());
                    profile.put("major", si.getMajor());
                    profile.put("preferredJobs", si.getPreferredJobs());
                    profile.put("preferredLocations", si.getPreferredLocations());
                    profile.put("resumePath", si.getResumePath());
                }
                break;
            case "company":
                CompanyInfo ci = companyInfoMapper.selectOne(new LambdaQueryWrapper<CompanyInfo>()
                        .eq(CompanyInfo::getUserId, userId));
                if (ci != null) {
                    profile.put("companyName", ci.getCompanyName());
                    profile.put("contactPerson", ci.getContactPerson());
                    profile.put("companyIntro", ci.getCompanyIntro());
                    profile.put("companyScale", ci.getCompanyScale());
                }
                break;
            case "admin":
                AdminInfo ai = adminInfoMapper.selectOne(new LambdaQueryWrapper<AdminInfo>()
                        .eq(AdminInfo::getUserId, userId));
                if (ai != null) {
                    profile.put("adminId", ai.getAdminId());
                    profile.put("mustChangePassword", ai.getMustChangePassword());
                }
                break;
        }

        return profile;
    }

    /**
     * 更新个人信息
     */
    public void updateProfile(UpdateProfileRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);

        // 更新公共字段
        if (req.getEmail() != null) {
            var existing = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, req.getEmail())
                    .ne(User::getId, userId));
            if (!existing.isEmpty()) {
                throw new BusinessException("邮箱已被使用");
            }
            user.setEmail(req.getEmail());
        }
        if (req.getPhone() != null) {
            var existing = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, req.getPhone())
                    .ne(User::getId, userId));
            if (!existing.isEmpty()) {
                throw new BusinessException("手机号已被使用");
            }
            user.setPhone(req.getPhone());
        }
        userMapper.updateById(user);

        // 更新角色专属字段
        switch (user.getRole()) {
            case "student":
                StudentInfo si = studentInfoMapper.selectOne(new LambdaQueryWrapper<StudentInfo>()
                        .eq(StudentInfo::getUserId, userId));
                if (si != null) {
                    if (req.getName() != null) si.setName(req.getName());
                    if (req.getMajor() != null) si.setMajor(req.getMajor());
                    if (req.getPreferredJobs() != null) si.setPreferredJobs(req.getPreferredJobs());
                    if (req.getPreferredLocations() != null) si.setPreferredLocations(req.getPreferredLocations());
                    studentInfoMapper.updateById(si);
                }
                break;
            case "company":
                CompanyInfo ci = companyInfoMapper.selectOne(new LambdaQueryWrapper<CompanyInfo>()
                        .eq(CompanyInfo::getUserId, userId));
                if (ci != null) {
                    if (req.getContactPerson() != null) ci.setContactPerson(req.getContactPerson());
                    if (req.getCompanyIntro() != null) ci.setCompanyIntro(req.getCompanyIntro());
                    if (req.getCompanyScale() != null) ci.setCompanyScale(req.getCompanyScale());
                    companyInfoMapper.updateById(ci);
                }
                break;
        }
    }

    /**
     * 修改密码
     */
    public void changePassword(ChangePasswordRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);

        // 管理员首次改密后清除标记
        if ("admin".equals(user.getRole())) {
            AdminInfo ai = adminInfoMapper.selectOne(new LambdaQueryWrapper<AdminInfo>()
                    .eq(AdminInfo::getUserId, userId));
            if (ai != null && Boolean.TRUE.equals(ai.getMustChangePassword())) {
                ai.setMustChangePassword(false);
                adminInfoMapper.updateById(ai);
            }
        }
    }

    /**
     * 更新简历路径
     */
    public void updateResumePath(Long userId, String resumePath) {
        StudentInfo si = studentInfoMapper.selectOne(new LambdaQueryWrapper<StudentInfo>()
                .eq(StudentInfo::getUserId, userId));
        if (si != null) {
            si.setResumePath(resumePath);
            studentInfoMapper.updateById(si);
        }
    }
}
