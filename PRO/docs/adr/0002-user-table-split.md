# ADR-0002: 数据库表结构拆分（user + 角色扩展表）

**日期**: 2026-05-21
**状态**: 已接受

## 背景

原文档将所有角色字段（student_id、company_name、admin_id 等）放在一张 user 表中，通过 role 字段区分。这导致大量 NULL 字段，且外键语义不清晰（如 Job.company_id 关联 user.id 但只应关联 role='company' 的用户）。

## 决策

拆分为：
- `user` 表：公共字段（id、username、password、email、phone、role、status）
- `student_info` 表：学生专属字段（user_id FK、student_id、name、major、preferred_*、resume_path）
- `company_info` 表：企业专属字段（user_id FK、company_name、contact_person、company_intro、company_scale）
- `admin_info` 表：管理员专属字段（user_id FK、admin_id）

外键仍关联 user.id，但业务层校验 role 类型。

## 后果

- **正面**：消除 NULL 字段，类型安全，外键语义清晰，易于扩展新角色
- **负面**：查询用户信息时需 JOIN，MyBatis-Plus 映射稍复杂
- **风险**：业务层必须校验 role，否则可能出现学生 ID 关联企业岗位的情况
