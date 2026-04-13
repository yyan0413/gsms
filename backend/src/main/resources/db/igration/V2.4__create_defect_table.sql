-- 创建缺陷表
CREATE TABLE IF NOT EXISTS gsms_defect (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '缺陷ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    parent_task_id BIGINT NOT NULL COMMENT '父任务ID（需求）',
    description TEXT NOT NULL COMMENT '缺陷描述',
    assignee_id BIGINT COMMENT '责任人ID',
    severity VARCHAR(20) NOT NULL COMMENT '缺陷等级：FATAL(致命)、SERIOUS(严重)、NORMAL(一般)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING(未整改)、FIXED(已整改)、CLOSED(已办结)',
    screenshot_url VARCHAR(500) COMMENT '截图URL',
    creator_id BIGINT NOT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    update_user_id BIGINT COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除：0-有效，1-已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_parent_task_id (parent_task_id),
    INDEX idx_assignee_id (assignee_id),
    INDEX idx_severity (severity),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time),
    INDEX idx_creator_id (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷表';

-- 插入权限数据
INSERT INTO sys_permission (name, code, description, permission_type, create_time, update_time, create_user_id, update_user_id, is_deleted)
VALUES
('创建缺陷', 'defect:create', '创建缺陷权限', 1, NOW(), NOW(), 1, 1, 0),
('查看缺陷', 'defect:view', '查看缺陷权限', 1, NOW(), NOW(), 1, 1, 0),
('编辑缺陷', 'defect:edit', '编辑缺陷权限', 1, NOW(), NOW(), 1, 1, 0),
('删除缺陷', 'defect:delete', '删除缺陷权限', 1, NOW(), NOW(), 1, 1, 0),
('办结缺陷', 'defect:close', '办结缺陷权限', 1, NOW(), NOW(), 1, 1, 0),
('导出缺陷', 'defect:export', '导出缺陷权限', 1, NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE update_time = NOW();
