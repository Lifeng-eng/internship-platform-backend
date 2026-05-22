# 大学生实训实习岗位投递与校企对接系统 — 产品需求说明书（PRD）

> 组号：14 | 成员：马光涵, 崔振豪, 刘恩赐, 海学涛

---

## 1. 项目概述

### 1.1 项目背景

当前，我国高等教育每年有超过1000万高校毕业生进入就业市场，实训实习经历已成为企业招聘的重要考量因素。传统模式下存在以下痛点：

- **信息不对称**：企业难以触达目标学生群体，学生难以获取符合专业的实习机会
- **渠道分散**：实习信息分散在招聘网站、学校就业中心、企业官网等平台
- **匹配精准度低**：难以实现学生专业能力与企业岗位需求的精准匹配
- **校企合作机制不完善**：缺乏有效的对接平台
- **管理效率低下**：招聘流程繁琐，投递后缺乏及时反馈

### 1.2 项目目标

构建集岗位发布、智能匹配、投递管理、数据统计于一体的综合性校企对接平台，消除信息孤岛，实现精准匹配，优化投递流程，加强校企合作。

### 1.3 用户角色

| 角色 | 说明 |
|------|------|
| **学生（Student）** | 浏览岗位、投递简历、管理个人信息和简历 |
| **企业（Company）** | 发布/管理岗位、处理投递申请、查看统计 |
| **管理员（Administrator）** | 用户管理、岗位审核、数据统计 |
| **访客（Visitor）** | 未登录用户，可浏览已发布岗位列表和详情 |

### 1.4 技术栈

| 层 | 技术选型 |
|----|----------|
| **前端** | TypeScript + React + Ant Design + Axios + Zustand + React Router v6 |
| **后端** | Spring Boot 3.x + MyBatis-Plus + Spring Security + JWT |
| **数据库** | MySQL 8.0（InnoDB） |
| **缓存** | Redis（用于 JWT refresh token、密码重置验证码） |
| **文件存储** | 开发期：后端本地文件夹；生产环境：云存储（OSS） |
| **架构** | 前后端分离，单体架构 |

---

## 2. 功能需求

### 2.1 认证模块

#### 2.1.1 学生注册

- **必填字段**：username（邮箱或手机号）、password、name、student_id（学号）、major（专业）、phone（手机号）
- **选填字段**：email、preferred_jobs（期望岗位类型，逗号分隔）、preferred_locations（期望地区，逗号分隔）
- **校验规则**：
  - username 唯一（匹配已有 email 或 phone）
  - phone 全局唯一
  - email 若填写则全局唯一
  - student_id 唯一
  - 密码长度 ≥ 6
- **注册后**：自动登录或跳转登录页

#### 2.1.2 企业注册

- **必填字段**：username（邮箱或手机号）、password、company_name、contact_person、phone（手机号）
- **选填字段**：email、company_intro（企业简介）、company_scale（企业规模）
- **校验规则**：
  - username 唯一
  - phone 全局唯一
  - email 若填写则全局唯一
  - company_name 唯一
  - 密码长度 ≥ 6
- **注册后**：自动登录或跳转登录页，可直接发布岗位（岗位需管理员审核）

#### 2.1.3 登录

- 支持 username / email / phone 任一作为登录凭证
- 密码 BCrypt 加密校验
- 登录成功返回：
  - access token（JWT，TTL 2 小时）
  - refresh token（存 Redis，TTL 7 天）
  - 用户基本信息（id、role、name）

#### 2.1.4 登出

- 前端清除 localStorage 中的 access token
- 后端从 Redis 删除对应的 refresh token

#### 2.1.5 Token 刷新

- access token 过期后，前端调用 `POST /api/auth/refresh`，携带 refresh token
- 后端验证 Redis 中 refresh token 存在且有效，返回新的 access token
- 若 refresh token 无效或已过期，返回 401，前端跳转登录页

#### 2.1.6 密码重置

1. 用户输入注册手机号，调用 `POST /api/auth/reset-password/request`
2. 后端验证手机号存在，生成 6 位数字验证码，存入 Redis（key: `reset_code:{phone}`，TTL 5 分钟）
3. 开发期：验证码直接在 API 响应中返回（模拟短信发送）
4. 前端用户输入验证码，调用 `POST /api/auth/reset-password/verify`
5. 后端校验验证码，通过后允许设置新密码（BCrypt 加密）
6. 密码重置成功后，删除 Redis 中的验证码

#### 2.1.7 管理员首次登录强制改密

- `admin_info` 表添加 `must_change_password`（BOOLEAN，默认 true）
- 管理员登录后，后端检查此字段，为 true 则返回 code=4003，前端跳转强制改密页
- 改密成功后设为 false

---

### 2.2 岗位模块

#### 2.2.1 岗位浏览（访客/学生/企业）

- 仅展示 `status = 'published'` 的岗位
- 支持筛选：
  - 关键词搜索（匹配 title、description）
  - 地区（location，精确匹配）
  - 薪资范围（salary_min ~ salary_max）
- 支持排序：按发布时间倒序（默认）、按薪资高低
- 分页：每页 10 条

#### 2.2.2 岗位详情

- 展示：title、company_name、salary_min、salary_max、location、description、requirements、publish_time
- **投递按钮状态**（学生端）：
  - 从未投递 → "投递简历"按钮
  - 已投递（pending） → "已投递，待处理" + 可点"撤销"
  - 已投递（accepted） → "已接受"
  - 已投递（rejected） → "已被拒绝"，不可再投
  - 已投递（cancelled） → 回到"投递简历"
- 学生/访客：可点击"投递"（学生需登录，访客跳转登录页）
- 企业：可操作"编辑"、"下架"、"删除"（仅自有岗位）

#### 2.2.3 发布岗位（企业）

- **必填字段**：title、location、description、requirements
- **选填字段**：salary_min、salary_max（都为空时显示"面议"）
- 提交后：
  - 状态设为 `pending`
  - 通知管理员审核
  - 提示"岗位提交成功，等待审核"

#### 2.2.4 编辑岗位（企业，自有岗位）

- 编辑后状态重置为 `pending`，需重新审核
- 已投递记录保留

- 下架岗位（企业，自有岗位）

- 状态设为 `closed`
- 不再对学生/访客可见
- 已投递记录保留
- 企业仍可处理 pending 的投递

#### 2.2.7 企业查询自有岗位

- 企业端"我发布的岗位"列表返回所有状态的岗位（pending/published/rejected/closed）
- 支持按状态筛选

#### 2.2.6 删除岗位（企业，自有岗位）

- 仅允许 `pending` 或 `rejected` 状态的岗位
- 物理删除（同时删除关联的审核记录、投递记录、通知）
- 若岗位已有投递记录，不允许删除

---

### 2.3 投递模块

#### 2.3.1 投递岗位（学生）

- **前置条件**：学生已登录、岗位状态为 `published`、学生未投递过该岗位（或投递已撤销/被拒后可重投）
- **投递数据**：
  - student_id、job_id
  - resume_path（当前简历文件路径）
  - apply_comment（可选，VARCHAR(200)）
- **投递后**：
  - Application 记录状态为 `pending`
  - 站内通知企业

#### 2.3.2 撤销投递（学生）

- 仅允许 `pending` 状态的投递
- 撤销后状态变为 `cancelled`
- 学生可再次投递同一岗位

#### 2.3.3 处理投递（企业）

- 仅允许 `pending` 状态的投递
- 操作：接受（→ `accepted`）或 拒绝（→ `rejected`）
- handle_comment 可选
- 操作后站内通知学生
- **状态约束**：
  - 学生被拒绝后不可再次投递同一岗位
  - 学生被接受后不可再次投递同一岗位

#### 2.3.4 投递列表

- **学生端**：我的投递（application_id、job_title、company_name、status、apply_time、handle_time）
- **企业端**：收到的投递（student_name、job_title、status、apply_time、resume 下载链接、apply_comment）
- **API 设计**：`GET /api/applications`，后端根据 JWT 中的 role 自动返回对应视角的数据

---

### 2.4 审核模块（管理员）

#### 2.4.1 待审核岗位列表

- 仅展示 `status = 'pending'` 的岗位
- 分页

#### 2.4.2 审核岗位

- 操作：通过（→ `published`）或 拒绝（→ `rejected`）
- 拒绝时审核意见（comment）必填
- 审核记录写入 Review 表
- 审核结果站内通知企业
- 逐条审核，不支持批量操作

---

### 2.5 通知模块

#### 2.5.1 通知列表

- 站内通知，不引入邮件/SMS
- 展示：content、type（application/review）、status（unread/read）、send_time
- 分页

#### 2.5.2 标记已读

- 单条标记已读或全部标记已读

#### 2.5.3 通知触发场景

| 场景 | 接收方 | 类型 |
|------|--------|------|
| 学生投递岗位 | 企业 | application |
| 企业处理投递（接受/拒绝） | 学生 | application |
| 企业发布岗位（通知管理员审核） | 管理员 | review |
| 管理员审核岗位（通过/拒绝） | 企业 | review |
| 学生撤销投递 | 企业 | application |

---

### 2.6 个人信息模块

#### 2.6.1 学生个人中心

- 查看/编辑：name、student_id、major、phone、email、preferred_jobs、preferred_locations
- 简历管理：上传/替换 PDF 简历（最大 5MB，仅 PDF）
- 上传新简历时，自动删除旧简历文件，只保留最新一份
- 修改密码

#### 2.6.4 简历下载权限控制

- 企业只能下载投递给**自己发布岗位**的简历
- 学生只能下载**自己**的简历
- 管理员不能下载简历
- 下载接口校验权限，不暴露文件系统真实路径

#### 2.6.2 企业个人中心

- 查看/编辑：company_name、contact_person、phone、email、company_intro、company_scale
- 修改密码

#### 2.6.3 管理员个人中心

- 查看：admin_id
- 修改密码

---

### 2.7 用户管理（管理员）

- 用户列表（分页、按角色筛选）
- 禁用/启用用户（被禁用用户无法登录）
- 禁用时可选填写原因

---

### 2.8 数据统计

#### 2.8.1 管理员统计

- 岗位统计：
  - 按状态分布的饼图（pending/published/rejected/closed）
  - 按发布时间的折线图
- 投递统计：
  - 按日/周的投递量折线图
  - 按岗位的投递量柱状图
  - 按处理结果的饼图（accepted/rejected/pending/cancelled）

#### 2.8.2 企业统计

- 每个岗位的投递数量
- 按状态分布（pending/accepted/rejected/cancelled）
- 投递时间趋势（近 7 天折线图）

---

### 2.9 智能推荐（学生端）

- 首页或岗位列表页展示"为你推荐"
- 匹配逻辑：
  - 学生 `major` 与岗位 `title` + `requirements` 关键词匹配
  - 学生 `preferred_jobs` 与岗位 `title` + `description` 匹配
  - 学生 `preferred_locations` 与岗位 `location` 匹配
- 按匹配程度排序，最多展示 10 条
- 简单字符串包含匹配，不引入机器学习算法

---

## 3. 数据模型

### 3.1 ER 图概要

```
user (1) ── (1) student_info
user (1) ── (1) company_info
user (1) ── (1) admin_info
user (1) ── (N) job              [company_id → user.id, role='company']
job (1) ── (N) application       [job_id → job.id]
user (1) ── (N) application      [student_id → user.id, role='student']
job (1) ── (N) review
user (1) ── (N) review           [admin_id → user.id, role='admin']
user (1) ── (N) notification     [receiver_id, sender_id]
```

### 3.2 表结构

#### 3.2.1 user 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 用户唯一标识 |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 登录名 |
| password | VARCHAR(100) | NOT NULL | BCrypt 加密密码 |
| email | VARCHAR(100) | UNIQUE | 邮箱（选填） |
| phone | VARCHAR(20) | UNIQUE, NOT NULL | 手机号 |
| role | ENUM('student','company','admin') | NOT NULL | 用户角色 |
| status | ENUM('normal','disabled') | DEFAULT 'normal' | 用户状态 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

#### 3.2.2 student_info 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK(user.id), UNIQUE, NOT NULL | 关联用户 |
| student_id | VARCHAR(20) | UNIQUE, NOT NULL | 学号 |
| name | VARCHAR(50) | NOT NULL | 姓名 |
| major | VARCHAR(50) | NOT NULL | 专业 |
| preferred_jobs | VARCHAR(200) | | 期望岗位类型（逗号分隔） |
| preferred_locations | VARCHAR(100) | | 期望地区（逗号分隔） |
| resume_path | VARCHAR(255) | | 当前简历文件路径 |

#### 3.2.3 company_info 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK(user.id), UNIQUE, NOT NULL | 关联用户 |
| company_name | VARCHAR(100) | UNIQUE, NOT NULL | 企业名称 |
| contact_person | VARCHAR(50) | NOT NULL | 联系人 |
| company_intro | TEXT | | 企业简介 |
| company_scale | VARCHAR(20) | | 企业规模 |

#### 3.2.4 admin_info 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK(user.id), UNIQUE, NOT NULL | 关联用户 |
| admin_id | VARCHAR(20) | UNIQUE, NOT NULL | 管理员编号 |

#### 3.2.5 job 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 岗位 ID |
| title | VARCHAR(100) | NOT NULL | 岗位标题 |
| company_id | BIGINT | FK(user.id), NOT NULL | 发布企业 ID |
| salary_min | DECIMAL(10,2) | | 最低薪资（为空=面议） |
| salary_max | DECIMAL(10,2) | | 最高薪资 |
| location | VARCHAR(100) | NOT NULL | 工作地点 |
| description | TEXT | NOT NULL | 岗位描述 |
| requirements | TEXT | NOT NULL | 岗位要求 |
| status | ENUM('pending','published','rejected','closed') | DEFAULT 'pending' | 岗位状态 |
| publish_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 发布时间 |
| review_time | DATETIME | | 审核时间 |
| reviewer_id | BIGINT | FK(user.id) | 审核管理员 ID |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | |

#### 3.2.6 application 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 投递 ID |
| student_id | BIGINT | FK(user.id), NOT NULL | 投递学生 ID |
| job_id | BIGINT | FK(job.id), NOT NULL | 投递岗位 ID |
| status | ENUM('pending','accepted','rejected','cancelled') | DEFAULT 'pending' | 投递状态 |
| resume_path | VARCHAR(255) | NOT NULL | 投递时简历路径（快照） |
| apply_comment | VARCHAR(200) | | 投递附言 |
| handle_comment | VARCHAR(200) | | 企业处理附言 |
| apply_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 投递时间 |
| handle_time | DATETIME | | 企业处理时间 |
| handler_id | BIGINT | FK(user.id) | 处理企业 ID |
| UNIQUE(student_id, job_id) | | | 同一学生同一岗位唯一（撤销后允许重投，通过删除旧记录或更新状态实现） |

> **关于唯一约束的说明**：UNIQUE(student_id, job_id) 会阻止同一学生多次投递。实际实现中，建议不设数据库级唯一约束，改由业务层判断：
> - `pending`：不可重复投递
> - `accepted`/`rejected`：不可再次投递
> - `cancelled`：可再次投递（插入新记录）

#### 3.2.7 review 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 审核记录 ID |
| job_id | BIGINT | FK(job.id), NOT NULL | 被审核岗位 ID |
| admin_id | BIGINT | FK(user.id), NOT NULL | 审核管理员 ID |
| review_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 审核时间 |
| result | ENUM('pass','reject') | NOT NULL | 审核结果 |
| comment | VARCHAR(200) | | 审核意见（拒绝时必填） |

#### 3.2.8 notification 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 通知 ID |
| receiver_id | BIGINT | FK(user.id), NOT NULL | 接收人 ID |
| sender_id | BIGINT | FK(user.id), NULLABLE | 发送人 ID（系统通知可为 NULL） |
| content | TEXT | NOT NULL | 通知内容 |
| type | ENUM('application','review') | NOT NULL | 通知类型 |
| status | ENUM('unread','read') | DEFAULT 'unread' | 阅读状态 |
| send_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 发送时间 |

### 3.3 索引设计

| 表 | 索引字段 | 说明 |
|----|----------|------|
| user | username, phone, email | 登录查询 |
| student_info | student_id | 学号查询 |
| company_info | company_name | 企业名称查询 |
| job | company_id, status | 企业查询自有岗位、状态筛选 |
| job | title, location | 搜索筛选 |
| job | publish_time | 排序 |
| application | student_id, job_id | 查询投递记录 |
| application | status | 状态筛选 |
| review | job_id | 查询岗位审核历史 |
| notification | receiver_id, status | 查询未读通知 |

---

## 4. API 接口设计

### 4.1 认证

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/register/student` | 学生注册 | 访客 |
| POST | `/api/auth/register/company` | 企业注册 | 访客 |
| POST | `/api/auth/login` | 登录 | 访客 |
| POST | `/api/auth/logout` | 登出 | 已登录 |
| POST | `/api/auth/refresh` | 刷新 access token | 访客 |
| POST | `/api/auth/reset-password/request` | 申请密码重置验证码 | 访客 |
| POST | `/api/auth/reset-password/verify` | 校验验证码并重置密码 | 访客 |

### 4.2 岗位

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/jobs` | 岗位列表（分页+筛选） | 所有人 |
| GET | `/api/jobs/{id}` | 岗位详情 | 所有人 |
| POST | `/api/jobs` | 发布岗位 | 企业 |
| PUT | `/api/jobs/{id}` | 编辑岗位 | 企业（自有） |
| DELETE | `/api/jobs/{id}` | 删除岗位 | 企业（自有，pending/rejected） |
| POST | `/api/jobs/{id}/close` | 下架岗位 | 企业（自有） |
| GET | `/api/jobs/recommended` | 推荐岗位 | 学生 |

### 4.3 投递

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/applications` | 投递岗位 | 学生 |
| GET | `/api/applications` | 我的投递（学生端）/ 收到的投递（企业端） | 学生/企业 |
| PUT | `/api/applications/{id}` | 处理投递（接受/拒绝） | 企业 |
| POST | `/api/applications/{id}/cancel` | 撤销投递 | 学生（pending） |

### 4.4 审核

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/admin/jobs/pending` | 待审核岗位列表 | 管理员 |
| POST | `/api/admin/jobs/{id}/review` | 审核岗位 | 管理员 |

### 4.5 通知

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/notifications` | 我的通知列表 | 已登录 |
| PUT | `/api/notifications/{id}/read` | 标记已读 | 已登录 |
| PUT | `/api/notifications/read-all` | 全部标记已读 | 已登录 |

### 4.6 个人中心

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/profile` | 获取个人信息 | 已登录 |
| PUT | `/api/profile` | 更新个人信息 | 已登录 |
| POST | `/api/profile/resume` | 上传简历 | 学生 |
| GET | `/api/profile/resume` | 获取简历信息 | 学生 |
| DELETE | `/api/profile/resume` | 删除简历 | 学生 |
| PUT | `/api/profile/password` | 修改密码 | 已登录 |

### 4.7 用户管理（管理员）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/admin/users` | 用户列表（分页+角色筛选） | 管理员 |
| PUT | `/api/admin/users/{id}/disable` | 禁用用户 | 管理员 |
| PUT | `/api/admin/users/{id}/enable` | 启用用户 | 管理员 |

### 4.8 统计

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/admin/stats` | 管理员统计 | 管理员 |
| GET | `/api/company/stats` | 企业统计 | 企业 |

---

## 5. 前端页面结构

### 5.1 路由

```
/                      → 首页（岗位列表 + 推荐）
/login                 → 登录
/register/student      → 学生注册
/register/company      → 企业注册
/jobs/:id              → 岗位详情
/student/profile       → 学生个人中心
/student/applications  → 我的投递
/company/profile       → 企业个人中心
/company/jobs          → 我发布的岗位
/company/jobs/new      → 发布岗位
/company/jobs/:id/edit → 编辑岗位
/company/applications  → 收到的投递
/company/stats         → 企业统计
/admin/jobs            → 岗位审核
/admin/users           → 用户管理
/admin/stats           → 数据统计
/admin/profile         → 管理员个人中心
/notifications         → 通知中心
```

### 5.2 权限路由

- 访客可访问：首页、岗位列表、岗位详情、登录、注册
- 学生可访问：+ 个人中心、我的投递、通知、投递操作
- 企业可访问：+ 个人中心、发布/管理岗位、投递处理、统计、通知
- 管理员可访问：+ 岗位审核、用户管理、数据统计

---

## 6. 非功能需求

### 6.1 API 统一响应格式

所有 API 返回统一 JSON 格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1716307200000
}
```

状态码：

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

业务错误码（4xxx 范围）：

| code | 说明 |
|------|------|
| 4001 | 重复投递 |
| 4002 | 岗位已下架 |
| 4003 | 首次登录需修改密码 |
| 4004 | 验证码错误或已过期 |

### 6.2 分页响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 10,
    "totalPages": 10
  }
}
```

### 6.3 统一异常处理

- 全局 `@RestControllerAdvice` 统一捕获异常
- 自定义业务异常类：`ResourceNotFoundException`、`UnauthorizedException`、`ForbiddenException`、`BusinessException`
- 业务异常返回对应错误码和 message
- 500 异常只返回通用 message（"服务器内部错误"），不暴露堆栈信息

### 6.4 性能

- 核心 API 响应时间 ≤ 1s
- 复杂查询（多条件筛选）响应时间 ≤ 2s
- 支持 500 并发用户，峰值 1000

### 6.5 安全

- 密码 BCrypt 加密存储
- JWT 认证，HTTPS 传输（开发环境可 HTTP）
- 防 SQL 注入（MyBatis-Plus 参数化查询）
- 防 XSS（前端输入过滤，后端校验）
- 文件上传：仅允许 PDF，最大 5MB，文件名随机化（`{timestamp}_{uuid}.pdf`）
- CORS 配置：开发期允许 `localhost:3000`，生产期配置部署域名

### 6.6 兼容性

- 浏览器：Chrome、Firefox、Edge、Safari 最新版本
- 响应式设计：PC 端 + 移动端适配

### 6.7 可靠性

- 数据库每日全量备份
- 关键操作记录审计日志

---

## 7. 约束与限制

### 7.1 技术约束

- 后端：Spring Boot 3.x + MyBatis-Plus + Spring Security
- 前端：React + TypeScript
- 数据库：MySQL 8.0
- 缓存：Redis
- 单体架构

### 7.2 资源约束

- 服务器：2 核 CPU、4GB 内存、50GB 磁盘
- 数据库容量：50GB

### 7.3 时间约束

- 开发周期：16 周
- 里程碑：需求分析 2 周 → 设计 3 周 → 编码 8 周 → 测试 3 周

### 7.4 初始化数据

- 管理员账号通过 SQL 脚本初始化（`init_admin.sql`）
- 初始密码：`admin123`，首次登录后强制修改

---

## 8. 开发环境配置

### 8.1 后端

```yaml
# application-dev.yml 概要
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/internship?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root
  redis:
    host: localhost
    port: 6379
    password:
mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
jwt:
  access-secret: ${JWT_ACCESS_SECRET}
  access-expiration: 7200000      # 2 小时 (ms)
  refresh-secret: ${JWT_REFRESH_SECRET}
  refresh-expiration: 604800000   # 7 天 (ms)
file:
  upload-dir: ./uploads/resumes  # 简历存储路径
  max-size: 5242880              # 5MB (bytes)
```

### 8.2 前端

```env
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api
```

### 8.3 项目结构

```
backend/
├── src/main/java/com/internship/
│   ├── config/        # Spring/Security/MyBatis/Redis/CORS 配置
│   ├── controller/    # REST 接口
│   ├── service/       # 业务逻辑
│   ├── mapper/        # MyBatis-Plus Mapper
│   ├── entity/        # 数据库实体
│   ├── dto/           # 请求/响应 DTO
│   ├── security/      # JWT 过滤器、认证处理器
│   └── common/        # 统一响应、异常处理、工具类
└── src/main/resources/
    ├── application.yml
    ├── application-dev.yml
    ├── application-prod.yml
    └── mapper/

frontend/
├── src/
│   ├── api/           # API 调用封装
│   ├── components/    # 通用组件
│   ├── pages/         # 页面
│   ├── store/         # Zustand 状态管理
│   ├── router/        # 路由配置
│   ├── types/         # TypeScript 类型定义
│   └── utils/         # 工具函数
├── package.json
└── vite.config.ts
```
