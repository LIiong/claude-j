-- V9: Add roles field to t_user table for RBAC authorization

ALTER TABLE t_user ADD COLUMN roles VARCHAR(100) NOT NULL DEFAULT 'USER' COMMENT '用户角色（逗号分隔，如 USER,ADMIN）';

-- Create index for roles query (optional, for future role-based filtering)
-- INDEX idx_roles (roles);