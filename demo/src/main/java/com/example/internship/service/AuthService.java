package com.example.internship.service;

import com.example.internship.common.BusinessException;
import com.example.internship.dto.*;
import com.example.internship.entity.*;
import com.example.internship.mapper.*;
import com.example.internship.security.JwtTokenProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 认证服务 - 注册、登录、登出、Token刷新、密码重置
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final StudentInfoMapper studentInfoMapper;
    private final CompanyInfoMapper companyInfoMapper;
    private final AdminInfoMapper adminInfoMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    public AuthService(UserMapper userMapper, StudentInfoMapper studentInfoMapper,
                       CompanyInfoMapper companyInfoMapper, AdminInfoMapper adminInfoMapper,
                       PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider,
                       RedisTemplate<String, String> redisTemplate) {
        this.userMapper = userMapper;
        this.studentInfoMapper = studentInfoMapper;
        this.companyInfoMapper = companyInfoMapper;
        this.adminInfoMapper = adminInfoMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 学生注册
     */
    @Transactional
    public void registerStudent(RegisterStudentRequest req) {
        // 校验唯一性
        if (userMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername())).size() > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (userMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getPhone, req.getPhone())).size() > 0) {
            throw new BusinessException("手机号已被注册");
        }
        if (req.getEmail() != null && !req.getEmail().isEmpty()
                && userMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getEmail, req.getEmail())).size() > 0) {
            throw new BusinessException("邮箱已被注册");
        }
        if (studentInfoMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentInfo>()
                .eq(StudentInfo::getStudentId, req.getStudentId())).size() > 0) {
            throw new BusinessException("学号已被注册");
        }

        // 创建用户
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setRole("student");
        user.setStatus("normal");
        userMapper.insert(user);

        // 创建学生信息
        StudentInfo studentInfo = new StudentInfo();
        studentInfo.setUserId(user.getId());
        studentInfo.setStudentId(req.getStudentId());
        studentInfo.setName(req.getName());
        studentInfo.setMajor(req.getMajor());
        studentInfo.setPreferredJobs(req.getPreferredJobs());
        studentInfo.setPreferredLocations(req.getPreferredLocations());
        studentInfoMapper.insert(studentInfo);
    }

    /**
     * 企业注册
     */
    @Transactional
    public void registerCompany(RegisterCompanyRequest req) {
        if (userMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername())).size() > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (userMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getPhone, req.getPhone())).size() > 0) {
            throw new BusinessException("手机号已被注册");
        }
        if (req.getEmail() != null && !req.getEmail().isEmpty()
                && userMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getEmail, req.getEmail())).size() > 0) {
            throw new BusinessException("邮箱已被注册");
        }
        if (companyInfoMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CompanyInfo>()
                .eq(CompanyInfo::getCompanyName, req.getCompanyName())).size() > 0) {
            throw new BusinessException("企业名称已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setRole("company");
        user.setStatus("normal");
        userMapper.insert(user);

        CompanyInfo companyInfo = new CompanyInfo();
        companyInfo.setUserId(user.getId());
        companyInfo.setCompanyName(req.getCompanyName());
        companyInfo.setContactPerson(req.getContactPerson());
        companyInfo.setCompanyIntro(req.getCompanyIntro());
        companyInfo.setCompanyScale(req.getCompanyScale());
        companyInfoMapper.insert(companyInfo);
    }

    /**
     * 登录 - 支持 username/email/phone
     */
    public LoginResponse login(LoginRequest req) {
        // 查找用户（支持用户名、邮箱、手机号登录）
        var users = userMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername())
                .or()
                .eq(User::getEmail, req.getUsername())
                .or()
                .eq(User::getPhone, req.getUsername()));
        if (users.isEmpty()) {
            throw new BusinessException(401, "账号或密码错误");
        }
        User user = users.get(0);

        if ("disabled".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "账号或密码错误");
        }

        // 生成 tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // refresh token 存 Redis
        redisTemplate.opsForValue().set("refresh_token:" + user.getId(), refreshToken,
                jwtTokenProvider.getRefreshTokenRemainingTTL(refreshToken), TimeUnit.MILLISECONDS);

        // 获取显示名称
        String name = getUserDisplayName(user);

        LoginResponse response = new LoginResponse(accessToken, refreshToken, user.getId(), user.getRole(), name);

        // 管理员首次登录强制改密
        if ("admin".equals(user.getRole())) {
            var adminInfo = adminInfoMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AdminInfo>()
                    .eq(AdminInfo::getUserId, user.getId()));
            if (!adminInfo.isEmpty() && Boolean.TRUE.equals(adminInfo.get(0).getMustChangePassword())) {
                response.setMustChangePassword(1);
                return response;
            }
        }
        response.setMustChangePassword(0);
        return response;
    }

    /**
     * 登出 - 删除 Redis 中的 refresh token
     */
    public void logout(Long userId) {
        redisTemplate.delete("refresh_token:" + userId);
    }

    /**
     * 刷新 access token
     */
    public String refreshAccessToken(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new BusinessException(401, "refresh token 无效或已过期");
        }
        Long userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
        String storedToken = redisTemplate.opsForValue().get("refresh_token:" + userId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(401, "refresh token 已失效");
        }
        User user = userMapper.selectById(userId);
        return jwtTokenProvider.generateAccessToken(userId, user.getRole());
    }

    /**
     * 申请密码重置验证码
     */
    public String requestPasswordReset(String phone) {
        var users = userMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));
        if (users.isEmpty()) {
            throw new BusinessException("该手机号未注册");
        }
        // 生成6位验证码
        String code = String.format("%06d", (int) (Math.random() * 1000000));
        redisTemplate.opsForValue().set("reset_code:" + phone, code, 5, TimeUnit.MINUTES);
        return code; // 开发期直接返回
    }

    /**
     * 校验验证码并重置密码
     */
    public void verifyAndResetPassword(ResetPasswordVerifyRequest req) {
        String storedCode = redisTemplate.opsForValue().get("reset_code:" + req.getPhone());
        if (storedCode == null || !storedCode.equals(req.getCode())) {
            throw new BusinessException(4004, "验证码错误或已过期");
        }
        var users = userMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getPhone, req.getPhone()));
        if (users.isEmpty()) {
            throw new BusinessException("用户不存在");
        }
        User user = users.get(0);
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);

        // 删除验证码
        redisTemplate.delete("reset_code:" + req.getPhone());
    }

    private String getUserDisplayName(User user) {
        switch (user.getRole()) {
            case "student":
                var studentInfo = studentInfoMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentInfo>()
                                .eq(StudentInfo::getUserId, user.getId()));
                return studentInfo != null ? studentInfo.getName() : user.getUsername();
            case "company":
                var companyInfo = companyInfoMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CompanyInfo>()
                                .eq(CompanyInfo::getUserId, user.getId()));
                return companyInfo != null ? companyInfo.getCompanyName() : user.getUsername();
            case "admin":
                return "管理员";
            default:
                return user.getUsername();
        }
    }
}
