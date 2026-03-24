-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: travelate
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
-- Table structure for table `attraction_images`
--

DROP TABLE IF EXISTS `attraction_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attraction_images` (
  `id` int NOT NULL AUTO_INCREMENT,
  `attraction_id` int NOT NULL,
  `image_url` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
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
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `intro` text COLLATE utf8mb4_unicode_ci COMMENT '景点介绍',
  `story` text COLLATE utf8mb4_unicode_ci COMMENT '历史故事',
  `lng` decimal(10,6) NOT NULL,
  `lat` decimal(10,6) NOT NULL,
  `radius` int NOT NULL DEFAULT '150',
  `cover` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `rating` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '景区',
  `duration` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '建议2-3小时',
  `best_season` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '四季皆宜',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  KEY `idx_conv_type` (`type`),
  CONSTRAINT `fk_conv_user1` FOREIGN KEY (`user1_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_conv_user2` FOREIGN KEY (`user2_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';
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
  KEY `idx_msg_receiver` (`receiver_id`),
  CONSTRAINT `fk_msg_conv` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_msg_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_msg_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';
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
  KEY `fk_notif_comment` (`comment_id`),
  CONSTRAINT `fk_notif_comment` FOREIGN KEY (`comment_id`) REFERENCES `travel_post_comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_notif_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_notif_post` FOREIGN KEY (`post_id`) REFERENCES `travel_posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_notif_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
/*!40101 SET character_set_client = @saved_cs_client */;

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
  KEY `idx_phrase_category` (`category`),
  CONSTRAINT `fk_phrase_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=90 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='旅行常用语表';
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
  KEY `idx_collection_user` (`user_id`),
  CONSTRAINT `fk_collection_post` FOREIGN KEY (`post_id`) REFERENCES `travel_posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_collection_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子收藏记录';
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
  KEY `idx_comment_like_user` (`user_id`),
  CONSTRAINT `fk_comment_like_comment` FOREIGN KEY (`comment_id`) REFERENCES `travel_post_comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_like_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞记录';
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
  KEY `fk_comment_reply_to_user` (`reply_to_user_id`),
  CONSTRAINT `fk_comment_parent` FOREIGN KEY (`parent_id`) REFERENCES `travel_post_comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `travel_posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_reply_to_user` FOREIGN KEY (`reply_to_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子评论';
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
  KEY `fk_history_post` (`post_id`),
  CONSTRAINT `fk_history_post` FOREIGN KEY (`post_id`) REFERENCES `travel_posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_history_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
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
  `images` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '图片URL或Base64数组(JSON)',
  `tags` text COLLATE utf8mb4_unicode_ci COMMENT '标签数组(JSON)',
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
  KEY `idx_post_create_time` (`create_time`),
  CONSTRAINT `fk_travel_posts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发现页帖子';
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
  `lang_code` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `region_code` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar` text COLLATE utf8mb4_unicode_ci,
  `status` tinyint(1) DEFAULT '1',
  `last_login_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `github_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `UK_g9s8emobrgjmob2ty2va0l354` (`github_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10024 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-23  2:54:17
