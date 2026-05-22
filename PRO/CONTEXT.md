---
name: glossary
description: Domain terms for the university internship job application and school-enterprise matching system
---

# Glossary

**企业（Company）**：在系统注册的企业用户，注册后即可直接发布岗位（岗位需管理员审核）。企业资质无需单独审核。

**学生登录凭证**：学生注册时自定义用户名（邮箱或手机号），登录时用该用户名+密码。`student_id`（学号）仅作为档案字段，不用于登录。

**投递状态**：`pending`（待处理）→ `accepted`（已接受）| `rejected`（已拒绝）| `cancelled`（已撤销）。企业只能接受或拒绝。学生可在`pending`状态时撤销投递，撤销后变为`cancelled`，可再次投递同一岗位。

**被拒岗位可重新提交**：岗位被管理员拒绝后，企业可编辑被拒岗位，修改后再次提交审核，状态重新变为`pending`。审核记录表保留完整历史。

**智能推荐匹配**：基于学生`major`（专业）与岗位`title`+`requirements`的关键词匹配，同时匹配学生`preferred_jobs`（期望岗位类型，VARCHAR(200)，逗号分隔）和`preferred_locations`（期望地区，VARCHAR(100)，逗号分隔）与岗位的对应字段。按匹配程度排序。

**通知方式**：仅站内通知。不引入邮件/SMS等第三方服务。用户登录后查看未读消息列表。

**岗位管理操作**：企业可对自有岗位执行：①编辑（修改后重新变为`pending`，需重新审核）；②下架/撤回（状态变为`closed`，不再对学生可见，已投递记录保留）；③删除（仅限`pending`或`rejected`状态）。岗位无自动过期机制，企业主动管理。

**企业处理投递附言**：企业接受或拒绝投递时，附言（`handle_comment`）均为可选，不强制必填。学生投递时也可附言（`apply_comment`，VARCHAR(200)，可选），企业在投递详情中可见。

**简历投递**：学生需上传PDF简历文件。投递时附带简历文件。开发期存入后端本地文件夹，生产环境可切换为云存储（如OSS）。简历文件与`Application`关联。学生可在个人中心管理（上传/替换）简历。仅PDF格式，最大5MB。

**企业信息**：纯文本字段，不支持上传附件/图片。

**分页**：所有列表页统一使用简单分页（上一页/下一页 + 当前页码），默认每页10条。使用 MyBatis-Plus 的分页插件实现。

**表结构拆分**：`user`公共表（id、username、password、email、phone、role、status、create_time）+ `student_info`（user_id FK、student_id、name、major、preferred_jobs、preferred_locations）+ `company_info`（user_id FK、company_name、contact_person、company_intro、company_scale）+ `admin_info`（user_id FK、admin_id）。登录时username可匹配email或phone。学生/企业注册时phone必填、email可选。

**前端架构**：前后端分离。后端 Spring Boot 提供 REST API（JSON），前端 React SPA 调用 API。认证使用 JWT（JSON Web Token），登录后前端存储 JWT 到 localStorage，后续请求通过 Authorization Header 携带。前端技术栈：TypeScript + React + Ant Design + Axios + Zustand + React Router v6。

**JWT 认证**：access token 有效期 2 小时，refresh token 有效期 7 天。refresh token 存储在 Redis 中（key: `refresh_token:{userId}`），登出时删除。前端通过 `/api/auth/refresh` 刷新 access token。

**密码重置**：通过手机号重置。用户输入注册手机号→后端生成6位验证码存Redis（TTL 5分钟）→开发期验证码直接通过API响应返回前端（模拟短信）→前端输入验证码→后端校验→校验通过后允许设置新密码。
