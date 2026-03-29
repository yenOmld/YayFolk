-- 添加 is_super_admin 列
ALTER TABLE users 
  ADD COLUMN IF NOT EXISTS is_super_admin TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否超级管理员：1-是，0-否';

-- 更新 admin 用户为超级管理员
UPDATE users 
SET 
  role = 'admin', 
  status = 1, 
  is_super_admin = 1, 
  email = COALESCE(email, 'admin@yayfolk.com'), 
  location = COALESCE(location, 'YayFolk Admin Center'), 
  language = COALESCE(language, 'zh'), 
  shop_status = COALESCE(shop_status, 'none'), 
  lang_code = COALESCE(lang_code, 'zh'), 
  region_code = COALESCE(region_code, 'CN'), 
  nickname = COALESCE(nickname, '系统管理员'), 
  avatar = COALESCE(avatar, 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin'), 
  update_time = NOW() 
WHERE username = 'admin';

-- 帖子举报记录（用于举报优先人工审核）
CREATE TABLE IF NOT EXISTS post_reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  reporter_id BIGINT NOT NULL,
  reason VARCHAR(500) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  handler_id BIGINT NULL,
  handle_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_post_reports_post_status (post_id, status),
  INDEX idx_post_reports_reporter_status (reporter_id, status)
);

-- User moderation fields and unban application workflow
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS ban_reason VARCHAR(500) NULL COMMENT 'Ban reason';

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS ban_admin_id BIGINT NULL COMMENT 'Administrator who banned the user';

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS ban_time DATETIME NULL COMMENT 'Ban timestamp';

CREATE TABLE IF NOT EXISTS user_unban_applications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  apply_reason VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  admin_id BIGINT NULL,
  admin_remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  handle_time DATETIME NULL,
  update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_unban_user_status (user_id, status),
  INDEX idx_unban_status_create_time (status, create_time)
);
