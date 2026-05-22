-- 创建数据库
CREATE DATABASE IF NOT EXISTS internship DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE internship;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识',
    `username` VARCHAR(50) NOT NULL COMMENT '登录名',
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
    `email` VARCHAR(100) NULL COMMENT '邮箱',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `role` ENUM('student','company','admin') NOT NULL COMMENT '用户角色',
    `status` ENUM('normal','disabled') DEFAULT 'normal' COMMENT '用户状态',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 学生信息表
CREATE TABLE IF NOT EXISTS `student_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
    `student_id` VARCHAR(20) NOT NULL COMMENT '学号',
    `name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `major` VARCHAR(50) NOT NULL COMMENT '专业',
    `preferred_jobs` VARCHAR(200) NULL COMMENT '期望岗位类型',
    `preferred_locations` VARCHAR(100) NULL COMMENT '期望地区',
    `resume_path` VARCHAR(255) NULL COMMENT '当前简历文件路径',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_student_id` (`student_id`),
    CONSTRAINT `fk_student_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生信息表';

-- 企业信息表
CREATE TABLE IF NOT EXISTS `company_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
    `company_name` VARCHAR(100) NOT NULL COMMENT '企业名称',
    `contact_person` VARCHAR(50) NOT NULL COMMENT '联系人',
    `company_intro` TEXT NULL COMMENT '企业简介',
    `company_scale` VARCHAR(20) NULL COMMENT '企业规模',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_company_name` (`company_name`),
    CONSTRAINT `fk_company_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业信息表';

-- 管理员信息表
CREATE TABLE IF NOT EXISTS `admin_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
    `admin_id` VARCHAR(20) NOT NULL COMMENT '管理员编号',
    `must_change_password` BOOLEAN DEFAULT TRUE COMMENT '是否必须修改密码',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_admin_id` (`admin_id`),
    CONSTRAINT `fk_admin_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员信息表';

-- 岗位表
CREATE TABLE IF NOT EXISTS `job` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    `title` VARCHAR(100) NOT NULL COMMENT '岗位标题',
    `company_id` BIGINT NOT NULL COMMENT '发布企业ID',
    `salary_min` DECIMAL(10,2) NULL COMMENT '最低薪资',
    `salary_max` DECIMAL(10,2) NULL COMMENT '最高薪资',
    `location` VARCHAR(100) NOT NULL COMMENT '工作地点',
    `description` TEXT NOT NULL COMMENT '岗位描述',
    `requirements` TEXT NOT NULL COMMENT '岗位要求',
    `status` ENUM('pending','published','rejected','closed') DEFAULT 'pending' COMMENT '岗位状态',
    `publish_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `review_time` DATETIME NULL COMMENT '审核时间',
    `reviewer_id` BIGINT NULL COMMENT '审核管理员ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_company_id` (`company_id`),
    KEY `idx_status` (`status`),
    KEY `idx_title` (`title`),
    KEY `idx_location` (`location`),
    KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

-- 投递表
CREATE TABLE IF NOT EXISTS `application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '投递ID',
    `student_id` BIGINT NOT NULL COMMENT '投递学生ID',
    `job_id` BIGINT NOT NULL COMMENT '投递岗位ID',
    `status` ENUM('pending','accepted','rejected','cancelled') DEFAULT 'pending' COMMENT '投递状态',
    `resume_path` VARCHAR(255) NOT NULL COMMENT '投递时简历路径',
    `apply_comment` VARCHAR(200) NULL COMMENT '投递附言',
    `handle_comment` VARCHAR(200) NULL COMMENT '企业处理附言',
    `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间',
    `handle_time` DATETIME NULL COMMENT '企业处理时间',
    `handler_id` BIGINT NULL COMMENT '处理企业ID',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投递表';

-- 审核表
CREATE TABLE IF NOT EXISTS `review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审核记录ID',
    `job_id` BIGINT NOT NULL COMMENT '被审核岗位ID',
    `admin_id` BIGINT NOT NULL COMMENT '审核管理员ID',
    `review_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
    `result` ENUM('pass','reject') NOT NULL COMMENT '审核结果',
    `comment` VARCHAR(200) NULL COMMENT '审核意见',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核表';

-- 通知表
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `receiver_id` BIGINT NULL COMMENT '接收人ID（NULL=所有管理员可见）',
    `sender_id` BIGINT NULL COMMENT '发送人ID',
    `content` TEXT NOT NULL COMMENT '通知内容',
    `type` ENUM('application','review') NOT NULL COMMENT '通知类型',
    `status` ENUM('unread','read') DEFAULT 'unread' COMMENT '阅读状态',
    `send_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';
