-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: yayfolk
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `activities`
--

DROP TABLE IF EXISTS `activities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint NOT NULL,
  `category_id` int DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `subtitle` varchar(100) DEFAULT NULL,
  `cover_image` varchar(255) DEFAULT NULL,
  `images` json DEFAULT NULL,
  `video_url` varchar(500) DEFAULT NULL,
  `video_cover_url` varchar(500) DEFAULT NULL,
  `content` text,
  `activity_type` varchar(20) DEFAULT NULL,
  `heritage_type` varchar(50) DEFAULT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `signup_start_time` datetime DEFAULT NULL,
  `signup_end_time` datetime DEFAULT NULL,
  `max_participants` int DEFAULT NULL,
  `current_participants` int DEFAULT '0',
  `price` int DEFAULT '0',
  `original_price` int DEFAULT NULL,
  `location_province` varchar(50) DEFAULT NULL,
  `location_city` varchar(50) DEFAULT NULL,
  `location_district` varchar(50) DEFAULT NULL,
  `location_detail` varchar(200) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'signup',
  `audit_status` varchar(20) DEFAULT 'pending',
  `audit_remark` varchar(500) DEFAULT NULL,
  `is_recommend` tinyint(1) DEFAULT '0',
  `view_count` int DEFAULT '0',
  `signup_count` int DEFAULT '0',
  `collect_count` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `merchant_profile_id` bigint DEFAULT NULL COMMENT 'normalized merchant id, merchant_profiles.id',
  PRIMARY KEY (`id`),
  KEY `fk_activities_merchant` (`merchant_id`),
  KEY `idx_activities_merchant_profile` (`merchant_profile_id`),
  CONSTRAINT `fk_activities_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_activities_merchant_profile` FOREIGN KEY (`merchant_profile_id`) REFERENCES `merchant_profiles` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=77 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_activities_bi_fill_merchant_profile` BEFORE INSERT ON `activities` FOR EACH ROW BEGIN
    IF NEW.merchant_profile_id IS NULL AND NEW.merchant_id IS NOT NULL THEN
        SET NEW.merchant_profile_id = (
            SELECT mp.id
            FROM merchant_profiles mp
            WHERE mp.user_id = NEW.merchant_id
            LIMIT 1
        );
    ELSEIF NEW.merchant_id IS NULL AND NEW.merchant_profile_id IS NOT NULL THEN
        SET NEW.merchant_id = (
            SELECT mp.user_id
            FROM merchant_profiles mp
            WHERE mp.id = NEW.merchant_profile_id
            LIMIT 1
        );
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_activities_bu_fill_merchant_profile` BEFORE UPDATE ON `activities` FOR EACH ROW BEGIN
    IF NEW.merchant_profile_id IS NULL AND NEW.merchant_id IS NOT NULL THEN
        SET NEW.merchant_profile_id = (
            SELECT mp.id
            FROM merchant_profiles mp
            WHERE mp.user_id = NEW.merchant_id
            LIMIT 1
        );
    ELSEIF NEW.merchant_id IS NULL AND NEW.merchant_profile_id IS NOT NULL THEN
        SET NEW.merchant_id = (
            SELECT mp.user_id
            FROM merchant_profiles mp
            WHERE mp.id = NEW.merchant_profile_id
            LIMIT 1
        );
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `activity_bookings`
--

DROP TABLE IF EXISTS `activity_bookings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_bookings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `signup_time` datetime DEFAULT NULL,
  `status` varchar(20) DEFAULT 'registered',
  `payment_status` varchar(20) DEFAULT 'unpaid',
  `payment_time` datetime DEFAULT NULL,
  `reserve_no` varchar(50) DEFAULT NULL,
  `pay_amount` int DEFAULT '0',
  `payment_type` varchar(32) DEFAULT NULL,
  `participant_name` varchar(50) DEFAULT NULL,
  `participant_phone` varchar(20) DEFAULT NULL,
  `participant_count` int DEFAULT '1',
  `remark` varchar(200) DEFAULT NULL,
  `qr_code` varchar(100) DEFAULT NULL,
  `verification_time` datetime DEFAULT NULL,
  `verified_by` bigint DEFAULT NULL,
  `review_score` decimal(2,1) DEFAULT NULL,
  `review_content` varchar(500) DEFAULT NULL,
  `review_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_user` (`activity_id`,`user_id`),
  KEY `fk_activity_bookings_user` (`user_id`),
  CONSTRAINT `fk_activity_bookings_activity` FOREIGN KEY (`activity_id`) REFERENCES `activities` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_activity_bookings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `activity_reserve_participants`
--

DROP TABLE IF EXISTS `activity_reserve_participants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_reserve_participants` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reserve_id` bigint NOT NULL COMMENT '预订ID，关联activity_reserves表',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参与者姓名',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参与者电话',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_reserve_id` (`reserve_id`),
  CONSTRAINT `fk_reserve_participants_reserve` FOREIGN KEY (`reserve_id`) REFERENCES `activity_reserves` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动预订参与者信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `activity_reserves`
--

DROP TABLE IF EXISTS `activity_reserves`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_reserves` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reserve_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `activity_id` bigint NOT NULL,
  `activity_title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `activity_time` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contact_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contact_phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `participant_num` int NOT NULL,
  `total_amount` int NOT NULL,
  `pay_amount` int NOT NULL,
  `pay_status` tinyint DEFAULT '0',
  `reserve_status` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'registered',
  `remark` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_time` datetime DEFAULT NULL,
  `cancel_time` datetime DEFAULT NULL,
  `verify_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_reserves_reserve_no` (`reserve_no`),
  KEY `idx_activity_reserves_user_id` (`user_id`),
  KEY `idx_activity_reserves_merchant_id` (`merchant_id`),
  KEY `idx_activity_reserves_activity_id` (`activity_id`),
  KEY `idx_activity_reserves_status` (`reserve_status`),
  CONSTRAINT `fk_activity_reserves_activity` FOREIGN KEY (`activity_id`) REFERENCES `activities` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_activity_reserves_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_activity_reserves_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `attraction_images`
--

DROP TABLE IF EXISTS `attraction_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attraction_images` (
  `id` int NOT NULL AUTO_INCREMENT,
  `attraction_id` int NOT NULL,
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_index` int DEFAULT '0',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `attraction_id` (`attraction_id`),
  CONSTRAINT `attraction_images_ibfk_1` FOREIGN KEY (`attraction_id`) REFERENCES `attractions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=58 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `attractions`
--

DROP TABLE IF EXISTS `attractions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attractions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `intro` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '??????',
  `story` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '??????',
  `lng` decimal(10,6) NOT NULL,
  `lat` decimal(10,6) NOT NULL,
  `radius` int NOT NULL DEFAULT '150',
  `cover` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `rating` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '???',
  `duration` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '???2-3???',
  `best_season` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '??????',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `backup_merchant_applications_20260328`
--

DROP TABLE IF EXISTS `backup_merchant_applications_20260328`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `backup_merchant_applications_20260328` (
  `id` bigint NOT NULL DEFAULT '0',
  `user_id` bigint NOT NULL,
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `id_card` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `heritage_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `heritage_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `proof_images` json DEFAULT NULL,
  `shop_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `shop_address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `province` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '省份',
  `city` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '城市',
  `intro` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '商家简介',
  `application_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'pending',
  `audit_admin_id` bigint DEFAULT NULL,
  `audit_time` datetime DEFAULT NULL,
  `audit_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `backup_users_merchant_fields_20260328`
--

DROP TABLE IF EXISTS `backup_users_merchant_fields_20260328`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `backup_users_merchant_fields_20260328` (
  `id` bigint NOT NULL DEFAULT '0',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'user',
  `shop_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'none',
  `shop_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shop_cover` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shop_intro` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `conversations`
--

DROP TABLE IF EXISTS `conversations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversations` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'chat' COMMENT '会话类型: chat-聊天, comment-评论通知, collection-收藏通知',
  `user1_id` bigint DEFAULT NULL COMMENT '用户1ID（聊天类型）',
  `user2_id` bigint DEFAULT NULL COMMENT '用户2ID（聊天类型）',
  `last_message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后一条消息内容',
  `last_message_time` datetime DEFAULT NULL COMMENT '最后消息时间',
  `unread_count_user1` int DEFAULT '0' COMMENT '用户1未读数',
  `unread_count_user2` int DEFAULT '0' COMMENT '用户2未读数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_conv_user1` (`user1_id`),
  KEY `idx_conv_user2` (`user2_id`),
  KEY `idx_conv_type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `intangible_cultural_heritage`
--

DROP TABLE IF EXISTS `intangible_cultural_heritage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `intangible_cultural_heritage` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '非遗项目名称',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类别：传统美术/传统技艺/传统戏剧等',
  `subcategory` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '子类别',
  `dynasty` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '相关朝代',
  `region` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地域',
  `level` enum('national','provincial','municipal') COLLATE utf8mb4_unicode_ci DEFAULT 'national' COMMENT '级别：national-国家级，provincial-省级，municipal-市级',
  `introduction` text COLLATE utf8mb4_unicode_ci COMMENT '介绍',
  `history` text COLLATE utf8mb4_unicode_ci COMMENT '历史故事',
  `inheritance_value` text COLLATE utf8mb4_unicode_ci COMMENT '传承价值',
  `representative_inheritor` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '代表性传承人',
  `images` json DEFAULT NULL COMMENT '图片数组',
  `video_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '视频 URL',
  `related_poems` json DEFAULT NULL COMMENT '相关诗词',
  `related_solar_terms` json DEFAULT NULL COMMENT '相关节气',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(11,6) DEFAULT NULL COMMENT '经度',
  `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否推荐',
  `view_count` int DEFAULT '0' COMMENT '浏览数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_dynasty` (`dynasty`),
  KEY `idx_region` (`region`),
  KEY `idx_level` (`level`),
  KEY `idx_featured` (`is_featured`)
) ENGINE=InnoDB AUTO_INCREMENT=181 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='非遗项目库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `merchant_applications`
--

DROP TABLE IF EXISTS `merchant_applications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchant_applications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `real_name` varchar(50) NOT NULL,
  `id_card` varchar(18) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `heritage_type` varchar(50) DEFAULT NULL,
  `heritage_description` text,
  `proof_images` json DEFAULT NULL,
  `shop_name` varchar(100) DEFAULT NULL,
  `shop_address` varchar(200) DEFAULT NULL,
  `province` varchar(32) DEFAULT NULL COMMENT '省份',
  `city` varchar(32) DEFAULT NULL COMMENT '城市',
  `intro` varchar(255) DEFAULT NULL COMMENT '商家简介',
  `application_status` varchar(20) DEFAULT 'pending',
  `audit_admin_id` bigint DEFAULT NULL,
  `audit_time` datetime DEFAULT NULL,
  `audit_remark` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL,
  `merchant_profile_id` bigint DEFAULT NULL COMMENT 'normalized merchant profile id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_application` (`user_id`),
  KEY `idx_merchant_applications_profile_id` (`merchant_profile_id`),
  CONSTRAINT `fk_merchant_app_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_merchant_applications_profile` FOREIGN KEY (`merchant_profile_id`) REFERENCES `merchant_profiles` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_merchant_applications_bi_sync_profile` BEFORE INSERT ON `merchant_applications` FOR EACH ROW BEGIN
    CALL sp_upsert_merchant_profile_from_application(
        NEW.id,
        NEW.user_id,
        NEW.real_name,
        NEW.phone,
        NEW.heritage_type,
        NEW.heritage_description,
        NEW.proof_images,
        NEW.shop_name,
        NEW.shop_address,
        NEW.province,
        NEW.city,
        NEW.intro,
        NEW.application_status
    );

    SET NEW.merchant_profile_id = (
        SELECT mp.id
        FROM merchant_profiles mp
        WHERE mp.user_id = NEW.user_id
        LIMIT 1
    );
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_merchant_applications_ai_fix_latest_application` AFTER INSERT ON `merchant_applications` FOR EACH ROW BEGIN
    UPDATE merchant_profiles mp
    SET mp.latest_application_id = NEW.id,
        mp.business_status = CASE
            WHEN NEW.application_status IN ('active', 'approved', 'rejected', 'disabled', 'closed', 'pending') THEN NEW.application_status
            ELSE mp.business_status
        END,
        mp.update_time = CURRENT_TIMESTAMP
    WHERE mp.user_id = NEW.user_id;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_merchant_applications_bu_sync_profile` BEFORE UPDATE ON `merchant_applications` FOR EACH ROW BEGIN
    CALL sp_upsert_merchant_profile_from_application(
        OLD.id,
        NEW.user_id,
        NEW.real_name,
        NEW.phone,
        NEW.heritage_type,
        NEW.heritage_description,
        NEW.proof_images,
        NEW.shop_name,
        NEW.shop_address,
        NEW.province,
        NEW.city,
        NEW.intro,
        NEW.application_status
    );

    SET NEW.merchant_profile_id = (
        SELECT mp.id
        FROM merchant_profiles mp
        WHERE mp.user_id = NEW.user_id
        LIMIT 1
    );
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `merchant_profiles`
--

DROP TABLE IF EXISTS `merchant_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchant_profiles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT 'merchant owner account, users.id',
  `shop_name` varchar(100) DEFAULT NULL,
  `shop_cover` varchar(255) DEFAULT NULL,
  `shop_intro` varchar(500) DEFAULT NULL,
  `contact_name` varchar(50) DEFAULT NULL,
  `contact_phone` varchar(20) DEFAULT NULL,
  `contact_email` varchar(100) DEFAULT NULL,
  `province` varchar(32) DEFAULT NULL,
  `city` varchar(32) DEFAULT NULL,
  `district` varchar(32) DEFAULT NULL,
  `address` varchar(200) DEFAULT NULL,
  `heritage_type` varchar(50) DEFAULT NULL,
  `heritage_description` text,
  `proof_images` json DEFAULT NULL,
  `business_status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/active/rejected/disabled/closed',
  `latest_application_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_profiles_user` (`user_id`),
  KEY `idx_merchant_profiles_status` (`business_status`),
  KEY `idx_merchant_profiles_shop_name` (`shop_name`),
  CONSTRAINT `fk_merchant_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=104 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='canonical merchant profile table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `merchant_reviews`
--

DROP TABLE IF EXISTS `merchant_reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchant_reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `reserve_id` bigint DEFAULT NULL,
  `review_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activity',
  `score` decimal(2,1) NOT NULL,
  `content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_reviews_merchant` (`merchant_id`),
  KEY `idx_merchant_reviews_user` (`user_id`),
  KEY `idx_merchant_reviews_reserve` (`reserve_id`),
  CONSTRAINT `fk_merchant_reviews_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_merchant_reviews_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `messages`
--

DROP TABLE IF EXISTS `messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `messages` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` bigint NOT NULL COMMENT '会话ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID',
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'text' COMMENT '消息类型: text-文本',
  `source_lang` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '消息源语言代码: en, zh, ja等',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读: 0-未读, 1-已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `deleted_by_receiver` bit(1) DEFAULT NULL,
  `deleted_by_sender` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_msg_conv` (`conversation_id`),
  KEY `idx_msg_sender` (`sender_id`),
  KEY `idx_msg_receiver` (`receiver_id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '接收通知的用户ID',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知类型: comment-评论, collection-收藏',
  `from_user_id` bigint DEFAULT NULL COMMENT '触发通知的用户ID',
  `post_id` bigint DEFAULT NULL COMMENT '相关帖子ID',
  `comment_id` bigint DEFAULT NULL COMMENT '相关评论ID',
  `content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知内容',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读: 0-未读, 1-已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notif_user` (`user_id`),
  KEY `idx_notif_type` (`type`),
  KEY `idx_notif_time` (`create_time`),
  KEY `fk_notif_from_user` (`from_user_id`),
  KEY `fk_notif_post` (`post_id`),
  KEY `fk_notif_comment` (`comment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `official_contents`
--

DROP TABLE IF EXISTS `official_contents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `official_contents` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `category` varchar(50) DEFAULT 'introduction',
  `cover_image` varchar(255) DEFAULT NULL,
  `images` json DEFAULT NULL,
  `tags` json DEFAULT NULL,
  `is_public` tinyint(1) DEFAULT '1',
  `view_count` int DEFAULT '0',
  `admin_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_official_admin` (`admin_id`),
  CONSTRAINT `fk_official_admin` FOREIGN KEY (`admin_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(50) NOT NULL,
  `user_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL,
  `product_name` varchar(200) DEFAULT NULL,
  `quantity` int DEFAULT '1',
  `total_amount` int NOT NULL,
  `pay_amount` int NOT NULL,
  `status` varchar(20) DEFAULT 'pending_payment',
  `payment_type` varchar(20) DEFAULT NULL,
  `payment_time` datetime DEFAULT NULL,
  `receiver_name` varchar(50) DEFAULT NULL,
  `receiver_phone` varchar(20) DEFAULT NULL,
  `receiver_province` varchar(50) DEFAULT NULL,
  `receiver_city` varchar(50) DEFAULT NULL,
  `receiver_district` varchar(50) DEFAULT NULL,
  `receiver_address` varchar(200) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `logistics_company` varchar(50) DEFAULT NULL,
  `logistics_no` varchar(50) DEFAULT NULL,
  `delete_status` tinyint(1) DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `merchant_profile_id` bigint DEFAULT NULL COMMENT 'normalized merchant id, merchant_profiles.id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_no` (`order_no`),
  KEY `fk_orders_user` (`user_id`),
  KEY `fk_orders_merchant` (`merchant_id`),
  KEY `idx_orders_merchant_profile` (`merchant_profile_id`),
  CONSTRAINT `fk_orders_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_orders_merchant_profile` FOREIGN KEY (`merchant_profile_id`) REFERENCES `merchant_profiles` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_orders_bi_fill_merchant_profile` BEFORE INSERT ON `orders` FOR EACH ROW BEGIN
    IF NEW.merchant_profile_id IS NULL AND NEW.merchant_id IS NOT NULL THEN
        SET NEW.merchant_profile_id = (
            SELECT mp.id
            FROM merchant_profiles mp
            WHERE mp.user_id = NEW.merchant_id
            LIMIT 1
        );
    ELSEIF NEW.merchant_id IS NULL AND NEW.merchant_profile_id IS NOT NULL THEN
        SET NEW.merchant_id = (
            SELECT mp.user_id
            FROM merchant_profiles mp
            WHERE mp.id = NEW.merchant_profile_id
            LIMIT 1
        );
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_orders_bu_fill_merchant_profile` BEFORE UPDATE ON `orders` FOR EACH ROW BEGIN
    IF NEW.merchant_profile_id IS NULL AND NEW.merchant_id IS NOT NULL THEN
        SET NEW.merchant_profile_id = (
            SELECT mp.id
            FROM merchant_profiles mp
            WHERE mp.user_id = NEW.merchant_id
            LIMIT 1
        );
    ELSEIF NEW.merchant_id IS NULL AND NEW.merchant_profile_id IS NOT NULL THEN
        SET NEW.merchant_id = (
            SELECT mp.user_id
            FROM merchant_profiles mp
            WHERE mp.id = NEW.merchant_profile_id
            LIMIT 1
        );
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `phrases`
--

DROP TABLE IF EXISTS `phrases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `phrases` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '短语ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `text` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '短语文本',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '收藏' COMMENT '短语分类',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `original_text` longtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `idx_phrase_user` (`user_id`),
  KEY `idx_phrase_category` (`category`)
) ENGINE=InnoDB AUTO_INCREMENT=90 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='旅行常用语表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post_reports`
--

DROP TABLE IF EXISTS `post_reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_reports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `reporter_id` bigint NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `handler_id` bigint DEFAULT NULL,
  `handle_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post_reports_post_status` (`post_id`,`status`),
  KEY `idx_post_reports_reporter_status` (`reporter_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rag_activities`
--

DROP TABLE IF EXISTS `rag_activities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rag_activities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `heritage_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `season_tag` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `location_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `intro` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_rag_activity_heritage` (`heritage_id`),
  KEY `fk_rag_activity_merchant` (`merchant_id`),
  CONSTRAINT `fk_rag_activity_heritage` FOREIGN KEY (`heritage_id`) REFERENCES `rag_heritage_items` (`id`),
  CONSTRAINT `fk_rag_activity_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `rag_merchants` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rag_heritage_items`
--

DROP TABLE IF EXISTS `rag_heritage_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rag_heritage_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dynasty` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `province` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `city` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `theme_tags` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `poem_tags` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `intro` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `featured_score` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_rag_heritage_merchant` (`merchant_id`),
  CONSTRAINT `fk_rag_heritage_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `rag_merchants` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rag_merchants`
--

DROP TABLE IF EXISTS `rag_merchants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rag_merchants` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `province` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `city` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `intro` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reserve_status_logs`
--

DROP TABLE IF EXISTS `reserve_status_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reserve_status_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reserve_id` bigint NOT NULL,
  `old_status` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `new_status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `operator_id` bigint NOT NULL,
  `operator_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_reserve_status_logs_reserve_id` (`reserve_id`),
  KEY `idx_reserve_status_logs_operator_id` (`operator_id`),
  CONSTRAINT `fk_reserve_status_logs_operator` FOREIGN KEY (`operator_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reserve_status_logs_reserve` FOREIGN KEY (`reserve_id`) REFERENCES `activity_reserves` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL,
  `role_description` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_name` (`role_name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `route_plans`
--

DROP TABLE IF EXISTS `route_plans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `route_plans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `query` varchar(500) NOT NULL COMMENT '原始查询',
  `matched_count` int DEFAULT '0' COMMENT '命中项目数',
  `suggested_days` int DEFAULT '1' COMMENT '建议天数',
  `route_text` text COMMENT '路线文案',
  `daily_plans` json DEFAULT NULL COMMENT '每日行程JSON',
  `budget` json DEFAULT NULL COMMENT '预算明细JSON',
  `travel_tips` json DEFAULT NULL COMMENT '旅行贴士JSON',
  `parsed_query` json DEFAULT NULL COMMENT '解析后的查询条件JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='路线规划记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `travel_post_collections`
--

DROP TABLE IF EXISTS `travel_post_collections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `travel_post_collections` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collection_user_post` (`user_id`,`post_id`),
  KEY `idx_collection_post` (`post_id`),
  KEY `idx_collection_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子收藏记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `travel_post_comment_likes`
--

DROP TABLE IF EXISTS `travel_post_comment_likes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `travel_post_comment_likes` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `comment_id` bigint NOT NULL COMMENT '评论ID',
  `user_id` bigint NOT NULL COMMENT '点赞用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_like_user` (`comment_id`,`user_id`),
  KEY `idx_comment_like_comment` (`comment_id`),
  KEY `idx_comment_like_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `travel_post_comments`
--

DROP TABLE IF EXISTS `travel_post_comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `travel_post_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID，用于回复功能',
  `reply_to_user_id` bigint DEFAULT NULL COMMENT '回复目标用户ID',
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `like_count` int DEFAULT '0' COMMENT '评论点赞数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  `source_lang` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_comment_post` (`post_id`),
  KEY `idx_comment_user` (`user_id`),
  KEY `idx_comment_parent` (`parent_id`),
  KEY `fk_comment_reply_to_user` (`reply_to_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子评论';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `travel_post_history`
--

DROP TABLE IF EXISTS `travel_post_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `travel_post_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '历史记录ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '浏览用户ID',
  `view_count` int DEFAULT '1' COMMENT '浏览次数',
  `last_view_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最后浏览时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_history_user_post` (`user_id`,`post_id`),
  KEY `idx_history_user_time` (`user_id`,`last_view_time`),
  KEY `fk_history_post` (`post_id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子浏览历史';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `travel_posts`
--

DROP TABLE IF EXISTS `travel_posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `travel_posts` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '发布用户ID',
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '帖子标题',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '帖子内容',
  `category` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT 'travel' COMMENT '帖子分类',
  `audit_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'pending',
  `audit_remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核备注',
  `images` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '图片URL或Base64数组(JSON)',
  `tags` text COLLATE utf8mb4_unicode_ci COMMENT '标签数组(JSON)',
  `visibility` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'public',
  `status` tinyint(1) DEFAULT '1' COMMENT '帖子状态：1-正常，0-删除',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `comment_count` int DEFAULT '0' COMMENT '评论数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `collection_count` int DEFAULT NULL,
  `collect_count` int DEFAULT NULL,
  `source_lang` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_post_user` (`user_id`),
  KEY `idx_post_category` (`category`),
  KEY `idx_post_status` (`status`),
  KEY `idx_post_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发现页帖子';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_follows`
--

DROP TABLE IF EXISTS `user_follows`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_follows` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `follower_id` bigint NOT NULL,
  `following_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow_pair` (`follower_id`,`following_id`),
  KEY `idx_user_follows_following_id` (`following_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_profile_visits`
--

DROP TABLE IF EXISTS `user_profile_visits`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_profile_visits` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `viewer_id` bigint NOT NULL COMMENT 'profile viewer',
  `profile_user_id` bigint NOT NULL COMMENT 'visited profile owner',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'first visit time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'latest visit time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_profile_visit_pair` (`viewer_id`,`profile_user_id`),
  KEY `idx_user_profile_visits_profile_user_id` (`profile_user_id`),
  KEY `idx_user_profile_visits_update_time` (`update_time`),
  CONSTRAINT `fk_user_profile_visits_profile_user` FOREIGN KEY (`profile_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_profile_visits_viewer` FOREIGN KEY (`viewer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user profile visit footprints';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_unban_applications`
--

DROP TABLE IF EXISTS `user_unban_applications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_unban_applications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `apply_reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `admin_id` bigint DEFAULT NULL,
  `admin_remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `handle_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_unban_user_status` (`user_id`,`status`),
  KEY `idx_unban_status_create_time` (`status`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `country` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'China' COMMENT '国家/地区',
  `location` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `language` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'zh',
  `shop_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'none',
  `shop_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shop_cover` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_photo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shop_intro` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `collection_visibility` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'public',
  `lang_code` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `region_code` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bio` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signature` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `follower_count` int DEFAULT '0',
  `following_count` int DEFAULT '0',
  `avatar` text COLLATE utf8mb4_unicode_ci,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'user',
  `status` tinyint(1) DEFAULT '1',
  `last_login_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `github_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_super_admin` tinyint(1) NOT NULL DEFAULT '0',
  `is_merchant` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'effective merchant account flag',
  `ban_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ban_admin_id` bigint DEFAULT NULL,
  `ban_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `UK_g9s8emobrgjmob2ty2va0l354` (`github_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10048 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_users_bi_set_is_merchant` BEFORE INSERT ON `users` FOR EACH ROW BEGIN
    SET NEW.is_merchant = CASE
        WHEN COALESCE(NEW.role, 'user') = 'merchant' THEN 1
        WHEN COALESCE(NEW.shop_status, 'none') IN ('approved', 'active') THEN 1
        ELSE COALESCE(NEW.is_merchant, 0)
    END;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_users_ai_sync_merchant_profile` AFTER INSERT ON `users` FOR EACH ROW BEGIN
    IF NEW.role = 'merchant' OR COALESCE(NEW.shop_status, 'none') <> 'none' THEN
        CALL sp_upsert_merchant_profile_from_user(NEW.id);
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_users_bu_set_is_merchant` BEFORE UPDATE ON `users` FOR EACH ROW BEGIN
    SET NEW.is_merchant = CASE
        WHEN COALESCE(NEW.role, 'user') = 'merchant' THEN 1
        WHEN COALESCE(NEW.shop_status, 'none') IN ('approved', 'active') THEN 1
        WHEN EXISTS (
            SELECT 1
            FROM merchant_profiles mp
            WHERE mp.user_id = OLD.id
              AND mp.business_status IN ('approved', 'active')
        ) THEN 1
        ELSE 0
    END;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_users_au_sync_merchant_profile` AFTER UPDATE ON `users` FOR EACH ROW BEGIN
    IF NEW.role = 'merchant' OR COALESCE(NEW.shop_status, 'none') <> 'none' THEN
        CALL sp_upsert_merchant_profile_from_user(NEW.id);
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-03 22:50:38
