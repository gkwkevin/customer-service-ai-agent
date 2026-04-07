CREATE DATABASE IF NOT EXISTS ai_customer_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_customer_service;

CREATE TABLE IF NOT EXISTS `conversation` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话标识',
    `user_id` BIGINT COMMENT '用户ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1-进行中 2-已结束 3-转人工',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话标识',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `sender_type` TINYINT NOT NULL COMMENT '发送者类型: 1-用户 2-AI 3-客服',
    `sender_id` BIGINT COMMENT '发送者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

CREATE TABLE IF NOT EXISTS `ticket` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工单ID',
    `ticket_no` VARCHAR(32) NOT NULL COMMENT '工单编号',
    `session_id` VARCHAR(64) COMMENT '关联会话ID',
    `user_id` BIGINT COMMENT '用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '工单标题',
    `content` TEXT COMMENT '工单内容',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1-待处理 2-处理中 3-已解决 4-已关闭',
    `priority` TINYINT DEFAULT 2 COMMENT '优先级: 1-高 2-中 3-低',
    `assignee_id` BIGINT COMMENT '处理人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_ticket_no` (`ticket_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) COMMENT '密码',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `role` TINYINT DEFAULT 1 COMMENT '角色: 1-用户 2-客服 3-管理员',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1-正常 2-禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
