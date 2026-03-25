-- ============================================================
-- 非遗社群项目数据库扩展脚本
-- 基于现有 travelate 数据库
-- 创建时间：2026-03-25
-- ============================================================

USE `travelate`;

-- ============================================================
-- 一、用户与角色相关（4张）
-- ============================================================

-- 1.1 修改 users 表，添加角色字段（支持多角色扩展）
ALTER TABLE `users` 
ADD COLUMN `role` ENUM('user', 'merchant', 'admin') DEFAULT 'user' COMMENT '用户角色：user-普通用户，merchant-商家，admin-管理员' AFTER `avatar`,
ADD COLUMN `bio` VARCHAR(200) DEFAULT NULL COMMENT '个人简介' AFTER `nickname`,
ADD COLUMN `follower_count` INT DEFAULT 0 COMMENT '粉丝数' AFTER `bio`,
ADD COLUMN `following_count` INT DEFAULT 0 COMMENT '关注数' AFTER `follower_count`,
ADD COLUMN `location` VARCHAR(100) DEFAULT NULL COMMENT '定位城市' AFTER `country`,
ADD COLUMN `language` VARCHAR(20) DEFAULT 'zh' COMMENT '多语言偏好：zh-中文，en-英文，ja-日文' AFTER `location`,
ADD COLUMN `shop_status` ENUM('none', 'applied', 'approved', 'rejected') DEFAULT 'none' COMMENT '店铺状态：none-无店铺，applied-已申请，approved-已通过，rejected-已拒绝' AFTER `language`,
ADD COLUMN `shop_name` VARCHAR(100) DEFAULT NULL COMMENT '店铺名称' AFTER `shop_status`,
ADD COLUMN `shop_cover` VARCHAR(255) DEFAULT NULL COMMENT '店铺封面图' AFTER `shop_name`,
ADD COLUMN `shop_intro` VARCHAR(500) DEFAULT NULL COMMENT '店铺简介' AFTER `shop_cover`;

-- 1.2 角色表（支持多角色扩展）
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `role_name` VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称：user/merchant/admin',
  `role_description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
  `permissions` JSON DEFAULT NULL COMMENT '权限配置 JSON',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 初始化角色数据
INSERT INTO `roles` (`role_name`, `role_description`, `permissions`) VALUES
('user', '普通用户', '{"can_post": true, "can_comment": true, "can_buy": true, "can_activity": true, "can_follow": true}'),
('merchant', '商家', '{"can_post": true, "can_comment": true, "can_buy": true, "can_activity": true, "can_sell": true, "can_publish_activity": true, "can_message": true, "can_follow": true}'),
('admin', '管理员', '{"can_post": true, "can_comment": true, "can_buy": true, "can_activity": true, "can_sell": true, "can_publish_activity": true, "can_message": true, "can_follow": true, "can_audit": true, "can_manage": true}');

-- 1.3 用户角色关联表（支持多角色扩展）
DROP TABLE IF EXISTS `user_roles`;
CREATE TABLE `user_roles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `role_id` INT NOT NULL COMMENT '角色 ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 初始化用户角色（默认普通用户）
INSERT INTO `user_roles` (`user_id`, `role_id`) 
SELECT u.id, r.id FROM users u, roles r WHERE r.role_name = 'user';

-- 1.4 商家认证申请记录
DROP TABLE IF EXISTS `merchant_applications`;
CREATE TABLE `merchant_applications` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '申请用户 ID',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `id_card` VARCHAR(18) NOT NULL COMMENT '身份证号',
  `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `heritage_type` VARCHAR(50) DEFAULT NULL COMMENT '非遗品类',
  `heritage_description` TEXT COMMENT '非遗项目描述',
  `proof_images` JSON DEFAULT NULL COMMENT '证明材料图片 URL 数组',
  `shop_name` VARCHAR(100) DEFAULT NULL COMMENT '店铺名称',
  `shop_address` VARCHAR(200) DEFAULT NULL COMMENT '店铺地址',
  `application_status` ENUM('pending', 'approved', 'rejected') DEFAULT 'pending' COMMENT '申请状态：pending-待审核，approved-已通过，rejected-已拒绝',
  `audit_admin_id` BIGINT DEFAULT NULL COMMENT '审核管理员 ID',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注/驳回原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_application` (`user_id`),
  KEY `idx_status` (`application_status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_merchant_app_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家认证申请记录';

-- ============================================================
-- 二、内容体系（5张）
-- ============================================================

-- 2.1 修改 travel_posts 表，扩展帖子类型
ALTER TABLE `travel_posts`
ADD COLUMN `post_type` ENUM('normal', 'merchant', 'group', 'official') DEFAULT 'normal' COMMENT '帖子类型：normal-普通帖子，merchant-商家帖子，group-组团搭子帖，official-官方帖子' AFTER `category`,
ADD COLUMN `heritage_tags` JSON DEFAULT NULL COMMENT '非遗标签数组' AFTER `post_type`,
ADD COLUMN `related_product_id` BIGINT DEFAULT NULL COMMENT '关联商品 ID' AFTER `heritage_tags`,
ADD COLUMN `related_activity_id` BIGINT DEFAULT NULL COMMENT '关联活动 ID' AFTER `related_product_id`,
ADD COLUMN `group_info` JSON DEFAULT NULL COMMENT '组团信息' AFTER `related_activity_id`,
ADD COLUMN `is_official` TINYINT(1) DEFAULT 0 COMMENT '是否官方' AFTER `group_info`,
ADD COLUMN `audit_status` ENUM('pending', 'passed', 'rejected') DEFAULT 'passed' COMMENT '审核状态：pending-待审核，passed-已通过，rejected-已拒绝' AFTER `is_official`,
ADD COLUMN `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间' AFTER `audit_status`,
ADD COLUMN `audit_admin_id` BIGINT DEFAULT NULL COMMENT '审核管理员 ID' AFTER `audit_time`,
ADD COLUMN `share_count` INT DEFAULT 0 COMMENT '分享数' AFTER `collect_count`,
ADD COLUMN `view_count` INT DEFAULT 0 COMMENT '浏览次数' AFTER `share_count`,
ADD COLUMN `collection_count` INT DEFAULT 0 COMMENT '收藏数' AFTER `view_count`,
ADD COLUMN `like_count` INT DEFAULT 0 COMMENT '点赞数' AFTER `collection_count`;

-- 2.2 帖子标签表（非遗品类、朝代、地域等）
DROP TABLE IF EXISTS `post_tags`;
CREATE TABLE `post_tags` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
  `tag_type` ENUM('heritage_type', 'dynasty', 'region', 'other') DEFAULT 'other' COMMENT '标签类型：heritage_type-非遗品类，dynasty-朝代，region-地域，other-其他',
  `usage_count` INT DEFAULT 0 COMMENT '使用次数',
  `is_hot` TINYINT(1) DEFAULT 0 COMMENT '是否热门',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tag_type` (`tag_type`),
  KEY `idx_usage` (`usage_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子标签表';

-- 初始化帖子标签数据
INSERT INTO `post_tags` (`tag_name`, `tag_type`, `usage_count`, `is_hot`) VALUES
('刺绣', 'heritage_type', 0, 1),
('剪纸', 'heritage_type', 0, 1),
('陶瓷', 'heritage_type', 0, 1),
('漆器', 'heritage_type', 0, 0),
('雕刻', 'heritage_type', 0, 0),
('唐代', 'dynasty', 0, 1),
('宋代', 'dynasty', 0, 1),
('明代', 'dynasty', 0, 0),
('清代', 'dynasty', 0, 0),
('北京', 'region', 0, 1),
('苏州', 'region', 0, 1),
('杭州', 'region', 0, 1),
('成都', 'region', 0, 0);

-- 2.3 评论表（支持多级评论）
-- 注意：已有 travel_post_comments 表，无需创建
-- 如需统一评论表，可参考以下设计：
-- DROP TABLE IF EXISTS `comments`;
-- CREATE TABLE `comments` (
--   `id` BIGINT NOT NULL AUTO_INCREMENT,
--   `post_id` BIGINT NOT NULL COMMENT '帖子ID',
--   `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
--   `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID，用于回复功能',
--   `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '回复目标用户ID',
--   `content` TEXT NOT NULL COMMENT '评论内容',
--   `like_count` INT DEFAULT 0 COMMENT '评论点赞数',
--   `status` TINYINT(1) DEFAULT 1 COMMENT '状态：1-正常，0-删除',
--   `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
--   PRIMARY KEY (`id`),
--   KEY `idx_post_id` (`post_id`),
--   KEY `idx_user_id` (`user_id`),
--   KEY `idx_parent_id` (`parent_id`),
--   CONSTRAINT `fk_comments_post` FOREIGN KEY (`post_id`) REFERENCES `travel_posts` (`id`) ON DELETE CASCADE,
--   CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 2.4 点赞/收藏记录（统一处理点赞与收藏行为）
-- 注意：已有 travel_post_collections 表用于收藏
-- 如需统一，可参考以下设计：
-- DROP TABLE IF EXISTS `likes`;
-- CREATE TABLE `likes` (
--   `id` BIGINT NOT NULL AUTO_INCREMENT,
--   `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
--   `target_type` ENUM('post', 'comment', 'product', 'activity') NOT NULL COMMENT '目标类型',
--   `target_id` BIGINT NOT NULL COMMENT '目标ID',
--   `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
--   PRIMARY KEY (`id`),
--   UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
--   KEY `idx_target` (`target_type`, `target_id`),
--   CONSTRAINT `fk_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞/收藏记录表';

-- 2.5 帖子商品关联表
DROP TABLE IF EXISTS `post_product_links`;
CREATE TABLE `post_product_links` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `post_id` BIGINT NOT NULL COMMENT '帖子 ID',
  `product_id` BIGINT NOT NULL COMMENT '商品 ID',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_product_id` (`product_id`),
  CONSTRAINT `fk_post_product_link_post` FOREIGN KEY (`post_id`) REFERENCES `travel_posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_post_product_link_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子商品关联表';

-- 2.6 帖子活动关联表
DROP TABLE IF EXISTS `post_activity_links`;
CREATE TABLE `post_activity_links` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `post_id` BIGINT NOT NULL COMMENT '帖子 ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动 ID',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_activity_id` (`activity_id`),
  CONSTRAINT `fk_post_activity_link_post` FOREIGN KEY (`post_id`) REFERENCES `travel_posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_post_activity_link_activity` FOREIGN KEY (`activity_id`) REFERENCES `activities` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子活动关联表';

-- ============================================================
-- 三、非遗库与官方内容（3张）
-- ============================================================

-- 3.1 非遗项目库
DROP TABLE IF EXISTS `intangible_cultural_heritage`;
CREATE TABLE `intangible_cultural_heritage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(200) NOT NULL COMMENT '非遗项目名称',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '类别：传统美术/传统技艺/传统戏剧等',
  `subcategory` VARCHAR(50) DEFAULT NULL COMMENT '子类别',
  `dynasty` VARCHAR(50) DEFAULT NULL COMMENT '相关朝代',
  `region` VARCHAR(100) DEFAULT NULL COMMENT '地域',
  `level` ENUM('national', 'provincial', 'municipal') DEFAULT 'national' COMMENT '级别：national-国家级，provincial-省级，municipal-市级',
  `introduction` TEXT COMMENT '介绍',
  `history` TEXT COMMENT '历史故事',
  `inheritance_value` TEXT COMMENT '传承价值',
  `representative_inheritor` VARCHAR(100) DEFAULT NULL COMMENT '代表性传承人',
  `images` JSON DEFAULT NULL COMMENT '图片数组',
  `video_url` VARCHAR(255) DEFAULT NULL COMMENT '视频 URL',
  `related_poems` JSON DEFAULT NULL COMMENT '相关诗词',
  `related_solar_terms` JSON DEFAULT NULL COMMENT '相关节气',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `longitude` DECIMAL(11,6) DEFAULT NULL COMMENT '经度',
  `is_featured` TINYINT(1) DEFAULT 0 COMMENT '是否推荐',
  `view_count` INT DEFAULT 0 COMMENT '浏览数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_dynasty` (`dynasty`),
  KEY `idx_region` (`region`),
  KEY `idx_level` (`level`),
  KEY `idx_featured` (`is_featured`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='非遗项目库';

-- 3.2 官方发布内容
DROP TABLE IF EXISTS `official_contents`;
CREATE TABLE `official_contents` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` TEXT NOT NULL COMMENT '内容',
  `category` VARCHAR(50) DEFAULT 'introduction' COMMENT '分类：introduction-非遗介绍，news-文化资讯，announcement-平台公告',
  `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图',
  `images` JSON DEFAULT NULL COMMENT '图片数组',
  `tags` JSON DEFAULT NULL COMMENT '标签数组',
  `is_public` TINYINT(1) DEFAULT 1 COMMENT '是否公开',
  `view_count` INT DEFAULT 0 COMMENT '浏览数',
  `admin_id` BIGINT NOT NULL COMMENT '发布管理员 ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_public` (`is_public`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_official_admin` FOREIGN KEY (`admin_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='官方发布内容';

-- 3.3 非遗标签字典（供AI打标签用）
DROP TABLE IF EXISTS `heritage_tags`;
CREATE TABLE `heritage_tags` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tag_name` VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
  `tag_type` ENUM('heritage_type', 'dynasty', 'region', 'solar_term', 'poem') DEFAULT 'heritage_type' COMMENT '标签类型：heritage_type-非遗品类，dynasty-朝代，region-地域，solar_term-节气，poem-诗词',
  `usage_count` INT DEFAULT 0 COMMENT '使用次数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tag_type` (`tag_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='非遗标签字典';

-- 初始化非遗标签字典
INSERT INTO `heritage_tags` (`tag_name`, `tag_type`, `usage_count`) VALUES
('刺绣', 'heritage_type', 0),
('剪纸', 'heritage_type', 0),
('陶瓷', 'heritage_type', 0),
('漆器', 'heritage_type', 0),
('雕刻', 'heritage_type', 0),
('京剧', 'heritage_type', 0),
('昆曲', 'heritage_type', 0),
('古琴', 'heritage_type', 0),
('太极', 'heritage_type', 0),
('中医', 'heritage_type', 0),
('唐代', 'dynasty', 0),
('宋代', 'dynasty', 0),
('明代', 'dynasty', 0),
('清代', 'dynasty', 0),
('北京', 'region', 0),
('苏州', 'region', 0),
('杭州', 'region', 0),
('成都', 'region', 0),
('广州', 'region', 0),
('西安', 'region', 0),
('立春', 'solar_term', 0),
('清明', 'solar_term', 0),
('端午', 'solar_term', 0),
('中秋', 'solar_term', 0),
('春节', 'solar_term', 0),
('将进酒', 'poem', 0),
('静夜思', 'poem', 0),
('春江花月夜', 'poem', 0);

-- ============================================================
-- 四、商品与订单体系（5张）
-- ============================================================

-- 4.1 商品分类表
DROP TABLE IF EXISTS `product_categories`;
CREATE TABLE `product_categories` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `parent_id` INT DEFAULT 0 COMMENT '父分类 ID',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `level` INT DEFAULT 1 COMMENT '层级',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- 初始化商品分类数据
INSERT INTO `product_categories` (`name`, `parent_id`, `icon`, `sort_order`, `level`) VALUES
('传统美术', 0, 'icon-art', 1, 1),
('传统技艺', 0, 'icon-craft', 2, 1),
('传统美食', 0, 'icon-food', 3, 1),
('传统医药', 0, 'icon-medicine', 4, 1),
('传统服饰', 0, 'icon-clothing', 5, 1),
('刺绣', 1, NULL, 1, 2),
('剪纸', 1, NULL, 2, 2),
('年画', 1, NULL, 3, 2),
('陶瓷', 2, NULL, 1, 2),
('漆器', 2, NULL, 2, 2),
('雕刻', 2, NULL, 3, 2);

-- 4.2 商品表
DROP TABLE IF EXISTS `products`;
CREATE TABLE `products` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` BIGINT NOT NULL COMMENT '商家 ID',
  `category_id` INT DEFAULT NULL COMMENT '分类 ID',
  `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `subtitle` VARCHAR(100) DEFAULT NULL COMMENT '副标题',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '商品描述',
  `detail` TEXT COMMENT '详细介绍 - 富文本',
  `price` INT NOT NULL COMMENT '价格（单位：分）',
  `original_price` INT DEFAULT NULL COMMENT '原价（单位：分）',
  `stock` INT DEFAULT 0 COMMENT '库存',
  `sales` INT DEFAULT 0 COMMENT '销量',
  `main_image` VARCHAR(255) DEFAULT NULL COMMENT '主图 URL',
  `images` JSON DEFAULT NULL COMMENT '图片数组 JSON',
  `video_url` VARCHAR(255) DEFAULT NULL COMMENT '视频 URL',
  `heritage_type` VARCHAR(50) DEFAULT NULL COMMENT '关联非遗类型',
  `heritage_item` VARCHAR(100) DEFAULT NULL COMMENT '关联非遗项目',
  `status` ENUM('on_sale', 'off_sale', 'sold_out') DEFAULT 'on_sale' COMMENT '状态：on_sale-上架，off_sale-下架，sold_out-售罄',
  `is_recommend` TINYINT(1) DEFAULT 0 COMMENT '是否推荐',
  `view_count` INT DEFAULT 0 COMMENT '浏览数',
  `sales_count` INT DEFAULT 0 COMMENT '成交数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_heritage_type` (`heritage_type`),
  CONSTRAINT `fk_products_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_products_category` FOREIGN KEY (`category_id`) REFERENCES `product_categories` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 4.3 订单表
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `merchant_id` BIGINT NOT NULL COMMENT '商家 ID',
  `total_amount` INT NOT NULL COMMENT '订单总金额（分）',
  `pay_amount` INT NOT NULL COMMENT '实付金额（分）',
  `freight_amount` INT DEFAULT 0 COMMENT '运费（分）',
  `coupon_amount` INT DEFAULT 0 COMMENT '优惠金额（分）',
  `status` ENUM('pending_payment', 'paid', 'pending_shipment', 'shipped', 'pending_receipt', 'completed', 'cancelled', 'refunded') DEFAULT 'pending_payment' COMMENT '订单状态',
  `payment_type` VARCHAR(20) DEFAULT NULL COMMENT '支付方式',
  `payment_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `delivery_time` DATETIME DEFAULT NULL COMMENT '发货时间',
  `receive_time` DATETIME DEFAULT NULL COMMENT '签收时间',
  `comment_time` DATETIME DEFAULT NULL COMMENT '评价时间',
  `receiver_name` VARCHAR(50) DEFAULT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT '收货人电话',
  `receiver_province` VARCHAR(50) DEFAULT NULL COMMENT '省',
  `receiver_city` VARCHAR(50) DEFAULT NULL COMMENT '市',
  `receiver_district` VARCHAR(50) DEFAULT NULL COMMENT '区',
  `receiver_address` VARCHAR(200) DEFAULT NULL COMMENT '详细地址',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '用户备注',
  `logistics_company` VARCHAR(50) DEFAULT NULL COMMENT '物流公司',
  `logistics_no` VARCHAR(50) DEFAULT NULL COMMENT '物流单号',
  `delete_status` TINYINT(1) DEFAULT 0 COMMENT '删除状态',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_orders_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 4.4 订单状态变更日志
DROP TABLE IF EXISTS `order_logs`;
CREATE TABLE `order_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL COMMENT '订单 ID',
  `old_status` VARCHAR(50) DEFAULT NULL COMMENT '原状态',
  `new_status` VARCHAR(50) NOT NULL COMMENT '新状态',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人 ID',
  `operator_type` ENUM('user', 'merchant', 'system') DEFAULT 'system' COMMENT '操作人类型：user-用户，merchant-商家，system-系统',
  `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_order_logs_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态变更日志';

-- 4.5 购物车表
DROP TABLE IF EXISTS `carts`;
CREATE TABLE `carts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `product_id` BIGINT NOT NULL COMMENT '商品 ID',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`),
  CONSTRAINT `fk_carts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_carts_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- 4.6 收货地址表
DROP TABLE IF EXISTS `delivery_addresses`;
CREATE TABLE `delivery_addresses` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人电话',
  `province` VARCHAR(50) NOT NULL COMMENT '省',
  `city` VARCHAR(50) NOT NULL COMMENT '市',
  `district` VARCHAR(50) NOT NULL COMMENT '区',
  `detail_address` VARCHAR(200) NOT NULL COMMENT '详细地址',
  `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否默认地址',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_addresses_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收货地址表';

-- ============================================================
-- 五、活动与预约体系（4张）
-- ============================================================

-- 5.1 活动分类表
DROP TABLE IF EXISTS `activity_categories`;
CREATE TABLE `activity_categories` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动分类表';

-- 初始化活动分类数据
INSERT INTO `activity_categories` (`name`, `icon`, `sort_order`) VALUES
('非遗体验', 'icon-experience', 1),
('技艺教学', 'icon-teaching', 2),
('文化展览', 'icon-exhibition', 3),
('节庆活动', 'icon-festival', 4),
('亲子活动', 'icon-family', 5);

-- 5.2 活动表
DROP TABLE IF EXISTS `activities`;
CREATE TABLE `activities` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` BIGINT NOT NULL COMMENT '商家 ID',
  `category_id` INT DEFAULT NULL COMMENT '分类 ID',
  `title` VARCHAR(200) NOT NULL COMMENT '活动标题',
  `subtitle` VARCHAR(100) DEFAULT NULL COMMENT '副标题',
  `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图',
  `images` JSON DEFAULT NULL COMMENT '图片数组 JSON',
  `content` TEXT COMMENT '活动内容 - 富文本',
  `activity_type` ENUM('offline', 'online', 'exhibition') DEFAULT 'offline' COMMENT '活动类型：offline-线下体验，online-线上课程，exhibition-展览',
  `heritage_type` VARCHAR(50) DEFAULT NULL COMMENT '非遗类型',
  `heritage_item` VARCHAR(100) DEFAULT NULL COMMENT '非遗项目',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `signup_start_time` DATETIME DEFAULT NULL COMMENT '报名开始时间',
  `signup_end_time` DATETIME DEFAULT NULL COMMENT '报名截止时间',
  `max_participants` INT DEFAULT NULL COMMENT '最大人数',
  `current_participants` INT DEFAULT 0 COMMENT '当前报名人数',
  `price` INT DEFAULT 0 COMMENT '活动价格（分）',
  `original_price` INT DEFAULT NULL COMMENT '原价（分）',
  `location_province` VARCHAR(50) DEFAULT NULL COMMENT '省',
  `location_city` VARCHAR(50) DEFAULT NULL COMMENT '市',
  `location_district` VARCHAR(50) DEFAULT NULL COMMENT '区',
  `location_detail` VARCHAR(200) DEFAULT NULL COMMENT '详细地址',
  `location_lat` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `location_lng` DECIMAL(11,6) DEFAULT NULL COMMENT '经度',
  `status` ENUM('signup', 'full', 'ongoing', 'ended') DEFAULT 'signup' COMMENT '活动状态：signup-报名中，full-已满员，ongoing-进行中，ended-已结束',
  `is_recommend` TINYINT(1) DEFAULT 0 COMMENT '是否推荐',
  `view_count` INT DEFAULT 0 COMMENT '浏览数',
  `signup_count` INT DEFAULT 0 COMMENT '报名人数',
  `collect_count` INT DEFAULT 0 COMMENT '收藏数',
  `share_count` INT DEFAULT 0 COMMENT '分享数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_heritage_type` (`heritage_type`),
  CONSTRAINT `fk_activities_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_activities_category` FOREIGN KEY (`category_id`) REFERENCES `activity_categories` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动表';

-- 5.3 活动预约表
DROP TABLE IF EXISTS `activity_bookings`;
CREATE TABLE `activity_bookings` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `activity_id` BIGINT NOT NULL COMMENT '活动 ID',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `signup_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `status` ENUM('registered', 'confirmed', 'cancelled', 'completed') DEFAULT 'registered' COMMENT '预约状态：registered-已报名，confirmed-已确认，cancelled-已取消，completed-已完成',
  `payment_status` ENUM('unpaid', 'paid', 'refunded') DEFAULT 'unpaid' COMMENT '支付状态：unpaid-未支付，paid-已支付，refunded-已退款',
  `payment_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `participant_name` VARCHAR(50) DEFAULT NULL COMMENT '参与者姓名',
  `participant_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `participant_count` INT DEFAULT 1 COMMENT '参与人数',
  `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `qr_code` VARCHAR(100) DEFAULT NULL COMMENT '核销二维码',
  `verification_time` DATETIME DEFAULT NULL COMMENT '核销时间',
  `verified_by` BIGINT DEFAULT NULL COMMENT '核销人 ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_activity_bookings_activity` FOREIGN KEY (`activity_id`) REFERENCES `activities` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_activity_bookings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动预约表';

-- 5.4 核销记录表
DROP TABLE IF EXISTS `check_in_records`;
CREATE TABLE `check_in_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `activity_booking_id` BIGINT NOT NULL COMMENT '活动预约 ID',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `merchant_id` BIGINT NOT NULL COMMENT '商家 ID',
  `check_in_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '核销时间',
  `check_in_type` ENUM('qr_code', 'manual', 'location') DEFAULT 'qr_code' COMMENT '核销方式：qr_code-二维码，manual-手动，location-定位',
  `location_lat` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `location_lng` DECIMAL(11,6) DEFAULT NULL COMMENT '经度',
  `address` VARCHAR(200) DEFAULT NULL COMMENT '地址',
  `verified_by` BIGINT DEFAULT NULL COMMENT '核销员 ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_activity_booking_id` (`activity_booking_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_check_in_time` (`check_in_time`),
  CONSTRAINT `fk_checkin_activity_booking` FOREIGN KEY (`activity_booking_id`) REFERENCES `activity_bookings` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_checkin_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_checkin_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核销记录表';

-- ============================================================
-- 六、打卡成就体系（3张）
-- ============================================================

-- 6.1 成就定义表
DROP TABLE IF EXISTS `achievements`;
CREATE TABLE `achievements` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '成就名称',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '成就描述',
  `icon` VARCHAR(255) DEFAULT NULL COMMENT '图标 URL',
  `background_color` VARCHAR(20) DEFAULT '#FFE4B5' COMMENT '背景色',
  `achievement_type` ENUM('check_in', 'learning', 'collection', 'share', 'consumption') DEFAULT 'check_in' COMMENT '成就类型：check_in-打卡，learning-学习，collection-收藏，share-分享，consumption-消费',
  `requirement_type` VARCHAR(50) DEFAULT 'count' COMMENT '达成条件类型',
  `requirement_value` INT DEFAULT 1 COMMENT '达成条件值',
  `requirement_description` VARCHAR(200) DEFAULT NULL COMMENT '达成条件描述',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `is_hidden` TINYINT(1) DEFAULT 0 COMMENT '是否隐藏成就',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`achievement_type`),
  KEY `idx_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成就定义表';

-- 初始化成就数据
INSERT INTO `achievements` (`name`, `description`, `icon`, `background_color`, `achievement_type`, `requirement_type`, `requirement_value`, `requirement_description`, `sort_order`) VALUES
('初来乍到', '完成首次打卡', 'icon-newbie', '#FFE4B5', 'check_in', 'count', 1, '打卡 1 次', 1),
('非遗探索者', '打卡 10 个不同非遗项目', 'icon-explorer', '#87CEEB', 'check_in', 'count', 10, '打卡 10 次', 2),
('学习达人', '参加 5 次教学活动', 'icon-learner', '#98FB98', 'learning', 'count', 5, '参加 5 次活动', 3),
('收藏家', '收藏 20 个帖子', 'icon-collector', '#DDA0DD', 'collection', 'count', 20, '收藏 20 次', 4),
('分享使者', '分享 10 次内容', 'icon-ambassador', '#FFB6C1', 'share', 'count', 10, '分享 10 次', 5),
('消费达人', '完成 3 笔订单', 'icon-buyer', '#FFD700', 'consumption', 'count', 3, '完成 3 笔订单', 6);

-- 6.2 用户获得成就记录表
DROP TABLE IF EXISTS `user_achievements`;
CREATE TABLE `user_achievements` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `achievement_id` BIGINT NOT NULL COMMENT '成就 ID',
  `achieved_time` DATETIME DEFAULT NULL COMMENT '达成时间',
  `progress` INT DEFAULT 0 COMMENT '当前进度',
  `status` ENUM('in_progress', 'achieved') DEFAULT 'in_progress' COMMENT '状态：in_progress-进行中，achieved-已完成',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_achievement` (`user_id`, `achievement_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_achievement_id` (`achievement_id`),
  CONSTRAINT `fk_user_achievements_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_achievements_achievement` FOREIGN KEY (`achievement_id`) REFERENCES `achievements` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户获得成就记录表';

-- 6.3 自动打卡记录表
DROP TABLE IF EXISTS `check_in_logs`;
CREATE TABLE `check_in_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `target_type` ENUM('activity', 'merchant', 'product', 'heritage') NOT NULL COMMENT '服务/活动/商品/非遗类型',
  `target_id` BIGINT NOT NULL COMMENT '关联 ID',
  `check_in_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
  `achievement_id` BIGINT DEFAULT NULL COMMENT '关联成就 ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_check_in_time` (`check_in_time`),
  KEY `idx_achievement_id` (`achievement_id`),
  CONSTRAINT `fk_checkin_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_checkin_logs_achievement` FOREIGN KEY (`achievement_id`) REFERENCES `achievements` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自动打卡记录表';

-- ============================================================
-- 七、AI 路线规划（3张）
-- ============================================================

-- 7.1 AI路线模板表
DROP TABLE IF EXISTS `ai_route_templates`;
CREATE TABLE `ai_route_templates` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `template_type` ENUM('dynasty', 'region', 'heritage_type', 'poem', 'solar_term') NOT NULL COMMENT '模板类型：dynasty-按朝代，region-按地域，heritage_type-按非遗品类，poem-按诗词，solar_term-按节气',
  `template_config` JSON DEFAULT NULL COMMENT '模板配置（AI 提示词、筛选条件等）',
  `default_values` JSON DEFAULT NULL COMMENT '默认参数值',
  `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_template_type` (`template_type`),
  KEY `idx_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI路线模板表';

-- 初始化路线模板数据
INSERT INTO `ai_route_templates` (`template_name`, `template_type`, `template_config`, `default_values`, `is_active`) VALUES
('朝代探秘之旅', 'dynasty', '{"prompt": "根据用户选择的朝代，规划一条非遗文化体验路线", "filters": {"dynasty": "required"}}', '{"duration": 3, "budget": 2000}', 1),
('地域文化之旅', 'region', '{"prompt": "根据用户选择的地域，规划一条非遗文化体验路线", "filters": {"region": "required"}}', '{"duration": 5, "budget": 5000}', 1),
('非遗品类之旅', 'heritage_type', '{"prompt": "根据用户选择的非遗品类，规划一条非遗文化体验路线", "filters": {"heritage_type": "required"}}', '{"duration": 4, "budget": 3000}', 1),
('诗词里的非遗', 'poem', '{"prompt": "根据用户输入的诗词，规划一条非遗文化体验路线", "filters": {"poem": "required"}}', '{"duration": 3, "budget": 2500}', 1),
('节气文化之旅', 'solar_term', '{"prompt": "根据用户选择的节气，规划一条非遗文化体验路线", "filters": {"solar_term": "required"}}', '{"duration": 2, "budget": 1500}', 1);

-- 7.2 用户生成的AI路线记录表
DROP TABLE IF EXISTS `user_routes`;
CREATE TABLE `user_routes` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `template_id` BIGINT DEFAULT NULL COMMENT '使用的模板 ID',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '路线标题',
  `description` TEXT COMMENT '路线描述',
  `requirements` JSON DEFAULT NULL COMMENT '用户需求（用户填写的参数）',
  `route_data` JSON DEFAULT NULL COMMENT '路线数据（AI 生成的详细路线）',
  `heritage_list` JSON DEFAULT NULL COMMENT '非遗项目列表',
  `merchant_recommend` JSON DEFAULT NULL COMMENT '商家推荐',
  `activity_recommend` JSON DEFAULT NULL COMMENT '活动推荐',
  `total_duration` INT DEFAULT NULL COMMENT '总时长（天）',
  `estimated_budget` INT DEFAULT NULL COMMENT '预估预算（分）',
  `is_public` TINYINT(1) DEFAULT 0 COMMENT '是否公开',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `collect_count` INT DEFAULT 0 COMMENT '收藏数',
  `share_count` INT DEFAULT 0 COMMENT '分享数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_public` (`is_public`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_user_routes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_routes_template` FOREIGN KEY (`template_id`) REFERENCES `ai_route_templates` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户生成的AI路线记录表';

-- 7.3 路线收藏与分享记录表
DROP TABLE IF EXISTS `route_collections`;
CREATE TABLE `route_collections` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `route_id` BIGINT NOT NULL COMMENT '路线 ID',
  `collection_type` ENUM('collection', 'like', 'share') NOT NULL COMMENT '收藏类型：collection-收藏，like-点赞，share-分享',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_route_type` (`user_id`, `route_id`, `collection_type`),
  KEY `idx_route_id` (`route_id`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_route_collections_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_route_collections_route` FOREIGN KEY (`route_id`) REFERENCES `user_routes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='路线收藏与分享记录表';

-- ============================================================
-- 八、消息与私信（2张）
-- ============================================================

-- 8.1 私信表
-- 注意：已有 conversations 和 messages 表用于私信
-- 如需统一，可参考以下设计：
-- DROP TABLE IF EXISTS `private_messages`;
-- CREATE TABLE `private_messages` (
--   `id` BIGINT NOT NULL AUTO_INCREMENT,
--   `conversation_id` BIGINT NOT NULL COMMENT '会话 ID',
--   `sender_id` BIGINT NOT NULL COMMENT '发送方 ID',
--   `receiver_id` BIGINT NOT NULL COMMENT '接收方 ID',
--   `content` TEXT NOT NULL COMMENT '内容',
--   `is_read` TINYINT(1) DEFAULT 0 COMMENT '已读状态',
--   `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
--   `deleted_by_sender` TINYINT(1) DEFAULT 0 COMMENT '发送方删除标记',
--   `deleted_by_receiver` TINYINT(1) DEFAULT 0 COMMENT '接收方删除标记',
--   `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
--   PRIMARY KEY (`id`),
--   KEY `idx_conversation_id` (`conversation_id`),
--   KEY `idx_sender_id` (`sender_id`),
--   KEY `idx_receiver_id` (`receiver_id`),
--   CONSTRAINT `fk_private_messages_conv` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE,
--   CONSTRAINT `fk_private_messages_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
--   CONSTRAINT `fk_private_messages_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信表';

-- 8.2 关注/粉丝关系表
DROP TABLE IF EXISTS `follow_relations`;
CREATE TABLE `follow_relations` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `follower_id` BIGINT NOT NULL COMMENT '关注者 ID',
  `following_id` BIGINT NOT NULL COMMENT '被关注者 ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_following` (`follower_id`, `following_id`),
  KEY `idx_following_id` (`following_id`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_follow_relations_follower` FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_follow_relations_following` FOREIGN KEY (`following_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注/粉丝关系表';

-- ============================================================
-- 九、管理员后台（5张）
-- ============================================================

-- 9.1 审核记录表
DROP TABLE IF EXISTS `audit_records`;
CREATE TABLE `audit_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `target_type` ENUM('merchant', 'post', 'comment', 'product', 'activity') NOT NULL COMMENT '审核目标类型：merchant-商家，post-帖子，comment-评论，product-商品，activity-活动',
  `target_id` BIGINT NOT NULL COMMENT '目标 ID',
  `audit_type` ENUM('merchant_application', 'content', 'other') DEFAULT 'other' COMMENT '审核类型：merchant_application-商家申请，content-内容审核，other-其他',
  `ai_result` JSON DEFAULT NULL COMMENT 'AI 审核结果',
  `manual_result` ENUM('pending', 'passed', 'rejected') DEFAULT 'pending' COMMENT '人工审核结果：pending-待审核，passed-通过，rejected-拒绝',
  `audit_admin_id` BIGINT DEFAULT NULL COMMENT '审核人 ID',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_manual_result` (`manual_result`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_audit_records_admin` FOREIGN KEY (`audit_admin_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核记录表';

-- 9.2 AI审核详细日志表
DROP TABLE IF EXISTS `ai_audit_logs`;
CREATE TABLE `ai_audit_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `target_type` ENUM('post', 'comment', 'image', 'product', 'activity') NOT NULL COMMENT '目标类型',
  `target_id` BIGINT NOT NULL COMMENT '目标 ID',
  `content` TEXT COMMENT '待审核内容',
  `audit_result` ENUM('pass', 'reject', 'review') DEFAULT 'pass' COMMENT '审核结果：pass-通过，reject-拒绝，review-需人工复核',
  `risk_score` INT DEFAULT 0 COMMENT '风险评分',
  `risk_labels` JSON DEFAULT NULL COMMENT '风险标签',
  `ai_model` VARCHAR(50) DEFAULT NULL COMMENT 'AI 模型',
  `confidence` DECIMAL(5,4) DEFAULT NULL COMMENT '置信度',
  `trigger_reason` VARCHAR(200) DEFAULT NULL COMMENT '触发人工原因',
  `manual_review` TINYINT(1) DEFAULT 0 COMMENT '是否人工复核',
  `manual_reviewer_id` BIGINT DEFAULT NULL COMMENT '复核人 ID',
  `manual_result` ENUM('pass', 'reject') DEFAULT NULL COMMENT '复核结果',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_result` (`audit_result`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_ai_audit_logs_reviewer` FOREIGN KEY (`manual_reviewer_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI审核详细日志表';

-- 9.3 投诉举报表
DROP TABLE IF EXISTS `complaints`;
CREATE TABLE `complaints` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '举报用户 ID',
  `complaint_type` ENUM('post', 'comment', 'product', 'activity', 'merchant', 'user') NOT NULL COMMENT '投诉举报类型',
  `complaint_target_id` BIGINT NOT NULL COMMENT '投诉目标 ID',
  `content` TEXT NOT NULL COMMENT '举报原因',
  `images` JSON DEFAULT NULL COMMENT '证据图片',
  `status` ENUM('pending', 'processing', 'resolved', 'rejected') DEFAULT 'pending' COMMENT '处理状态：pending-待处理，processing-处理中，resolved-已解决，rejected-已拒绝',
  `admin_id` BIGINT DEFAULT NULL COMMENT '处理管理员 ID',
  `admin_remark` VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
  `resolve_time` DATETIME DEFAULT NULL COMMENT '解决时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_complaint_type` (`complaint_type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_complaints_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_complaints_admin` FOREIGN KEY (`admin_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投诉举报表';

-- 9.4 客服对话记录表
DROP TABLE IF EXISTS `customer_service_records`;
CREATE TABLE `customer_service_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `admin_id` BIGINT NOT NULL COMMENT '客服管理员 ID',
  `session_id` VARCHAR(50) DEFAULT NULL COMMENT '会话 ID',
  `content` TEXT NOT NULL COMMENT '对话内容',
  `is_admin_reply` TINYINT(1) DEFAULT 0 COMMENT '是否管理员回复：0-用户，1-管理员',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_cs_records_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cs_records_admin` FOREIGN KEY (`admin_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客服对话记录表';

-- 9.5 客服会话表
DROP TABLE IF EXISTS `customer_service_sessions`;
CREATE TABLE `customer_service_sessions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `admin_id` BIGINT DEFAULT NULL COMMENT '客服管理员 ID',
  `last_message` VARCHAR(500) DEFAULT NULL COMMENT '最后一条消息',
  `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
  `status` ENUM('active', 'closed') DEFAULT 'active' COMMENT '会话状态：active-进行中，closed-已关闭',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_admin` (`user_id`, `admin_id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_status` (`status`),
  KEY `idx_last_message_time` (`last_message_time`),
  CONSTRAINT `fk_cs_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cs_sessions_admin` FOREIGN KEY (`admin_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客服会话表';

-- ============================================================
-- 十、视图与索引优化
-- ============================================================

-- 10.1 创建商家信息视图
DROP VIEW IF EXISTS `v_merchant_info`;
CREATE VIEW `v_merchant_info` AS
SELECT 
    u.id AS user_id,
    u.username,
    u.nickname,
    u.avatar,
    u.bio,
    u.follower_count,
    u.following_count,
    u.location,
    u.create_time AS register_time,
    ma.id AS application_id,
    ma.application_status,
    ma.shop_name,
    ma.shop_address,
    ma.heritage_type,
    ma.heritage_description,
    ma.audit_time,
    ma.audit_remark
FROM users u
LEFT JOIN merchant_applications ma ON u.id = ma.user_id;

-- 10.2 创建热门活动视图
DROP VIEW IF EXISTS `v_hot_activities`;
CREATE VIEW `v_hot_activities` AS
SELECT 
    a.id,
    a.title,
    a.subtitle,
    a.cover_image,
    a.start_time,
    a.end_time,
    a.location_city,
    a.price,
    a.current_participants,
    a.max_participants,
    a.view_count,
    a.signup_count,
    a.is_recommend,
    u.nickname AS merchant_nickname,
    u.avatar AS merchant_avatar
FROM activities a
LEFT JOIN users u ON a.merchant_id = u.id
WHERE a.status = 'signup' 
  AND a.end_time > NOW()
ORDER BY a.view_count DESC, a.signup_count DESC
LIMIT 20;

-- 10.3 创建用户统计视图
DROP VIEW IF EXISTS `v_user_statistics`;
CREATE VIEW `v_user_statistics` AS
SELECT 
    u.id AS user_id,
    u.username,
    u.nickname,
    u.role,
    COUNT(DISTINCT tp.id) AS total_posts,
    COUNT(DISTINCT tpc.id) AS total_comments,
    COUNT(DISTINCT tpc2.id) AS total_comment_likes,
    COUNT(DISTINCT tpc3.id) AS total_collections,
    COUNT(DISTINCT cirl.id) AS total_checkins,
    COUNT(DISTINCT ab.id) AS total_activity_bookings,
    COUNT(DISTINCT o.id) AS total_orders,
    COALESCE(SUM(o.pay_amount), 0) AS total_consumption
FROM users u
LEFT JOIN travel_posts tp ON u.id = tp.user_id
LEFT JOIN travel_post_comments tpc ON u.id = tpc.user_id
LEFT JOIN travel_post_comment_likes tpc2 ON u.id = tpc2.user_id
LEFT JOIN travel_post_collections tpc3 ON u.id = tpc3.user_id
LEFT JOIN check_in_logs cirl ON u.id = cirl.user_id
LEFT JOIN activity_bookings ab ON u.id = ab.user_id
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id;

-- 10.4 创建索引优化
CREATE INDEX idx_posts_type_status ON travel_posts(post_type, audit_status);
CREATE INDEX idx_posts_create_time ON travel_posts(create_time DESC);
CREATE INDEX idx_products_merchant_status ON products(merchant_id, status);
CREATE INDEX idx_activities_merchant_status ON activities(merchant_id, status);
CREATE INDEX idx_orders_user_status ON orders(user_id, status);
CREATE INDEX idx_heritage_category_region ON intangible_cultural_heritage(category, region);
CREATE INDEX idx_audit_records_target ON audit_records(target_type, target_id);
CREATE INDEX idx_complaints_status ON complaints(status);

-- ============================================================
-- 结束
-- ============================================================
