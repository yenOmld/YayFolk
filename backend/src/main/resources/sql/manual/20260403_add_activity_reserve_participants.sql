-- 创建活动预订参与者信息表
CREATE TABLE IF NOT EXISTS `activity_reserve_participants` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reserve_id` BIGINT NOT NULL COMMENT '预订ID，关联activity_reserves表',
  `name` VARCHAR(50) NOT NULL COMMENT '参与者姓名',
  `phone` VARCHAR(20) NOT NULL COMMENT '参与者电话',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_reserve_id` (`reserve_id`),
  CONSTRAINT `fk_reserve_participants_reserve` FOREIGN KEY (`reserve_id`) REFERENCES `activity_reserves` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动预订参与者信息表';