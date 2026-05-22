package com.example.internship.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.internship.common.BusinessException;
import com.example.internship.common.PageResult;
import com.example.internship.dto.UserVO;
import com.example.internship.entity.CompanyInfo;
import com.example.internship.entity.StudentInfo;
import com.example.internship.entity.User;
import com.example.internship.mapper.CompanyInfoMapper;
import com.example.internship.mapper.StudentInfoMapper;
import com.example.internship.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 用户管理服务（管理员）
 */
@Service
public class UserManageService {

    private final UserMapper userMapper;
    private final StudentInfoMapper studentInfoMapper;
    private final CompanyInfoMapper companyInfoMapper;

    public UserManageService(UserMapper userMapper, StudentInfoMapper studentInfoMapper,
                             CompanyInfoMapper companyInfoMapper) {
        this.userMapper = userMapper;
        this.studentInfoMapper = studentInfoMapper;
        this.companyInfoMapper = companyInfoMapper;
    }

    /**
     * 用户列表（分页+角色筛选）
     */
    public PageResult<UserVO> listUsers(int page, int size, String role) {
        var query = new LambdaQueryWrapper<User>();
        if (role != null && !role.isEmpty()) {
            query.eq(User::getRole, role);
        }
        query.orderByDesc(User::getCreateTime);

        Page<User> pageObj = new Page<>(page, size);
        Page<User> result = userMapper.selectPage(pageObj, query);

        var vos = result.getRecords().stream().map(user -> {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setEmail(user.getEmail());
            vo.setPhone(user.getPhone());
            vo.setRole(user.getRole());
            vo.setStatus(user.getStatus());
            vo.setCreateTime(user.getCreateTime());

            switch (user.getRole()) {
                case "student":
                    StudentInfo si = studentInfoMapper.selectOne(new LambdaQueryWrapper<StudentInfo>()
                            .eq(StudentInfo::getUserId, user.getId()));
                    if (si != null) {
                        vo.setName(si.getName());
                    }
                    break;
                case "company":
                    CompanyInfo ci = companyInfoMapper.selectOne(new LambdaQueryWrapper<CompanyInfo>()
                            .eq(CompanyInfo::getUserId, user.getId()));
                    if (ci != null) {
                        vo.setName(ci.getContactPerson());
                        vo.setCompanyName(ci.getCompanyName());
                    }
                    break;
                case "admin":
                    vo.setName("管理员");
                    break;
            }
            return vo;
        }).toList();

        return new PageResult<>(vos, result.getTotal(), page, size);
    }

    /**
     * 禁用用户
     */
    public void disableUser(Long userId, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if ("admin".equals(user.getRole())) {
            throw new BusinessException("不能禁用管理员");
        }
        user.setStatus("disabled");
        userMapper.updateById(user);
    }

    /**
     * 启用用户
     */
    public void enableUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus("normal");
        userMapper.updateById(user);
    }
}
