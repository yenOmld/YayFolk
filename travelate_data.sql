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
-- Dumping data for table `attraction_images`
--

LOCK TABLES `attraction_images` WRITE;
/*!40000 ALTER TABLE `attraction_images` DISABLE KEYS */;
INSERT INTO `attraction_images` (`id`, `attraction_id`, `image_url`, `order_index`, `create_time`) VALUES (1,7,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/7/3d617a56-8836-4262-b932-2a01e73e4cb6.jpg',1,'2026-03-17 06:05:31'),(2,7,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/7/11074744-1028-4544-b1fd-04a0e1d50e3d.jpg',2,'2026-03-17 06:05:31'),(3,7,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/7/2fe7f473-5a41-44f9-b6c7-9a9343137f75.jpg',3,'2026-03-17 06:05:31'),(4,7,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/7/01162e06-0efc-4303-9a1f-4b864fb9db15.jpg',4,'2026-03-17 06:05:31'),(5,7,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/7/e7570418-935f-4f66-8287-49a83dccb953.jpg',5,'2026-03-17 06:05:33'),(6,5,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/5/272295d6-d7ed-4a73-83c4-fd8684c5f099.jpg',1,'2026-03-17 06:56:41'),(7,5,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/5/7ca9e029-4e9a-4237-90f1-ef3e5fca0d32.jpg',1,'2026-03-17 07:09:05'),(8,5,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/5/b170813a-19c0-448f-99eb-3f2864b19317.jpg',3,'2026-03-17 07:09:41'),(9,5,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/5/303a42b1-ab4c-437e-80e7-814eead70fcc.jpg',4,'2026-03-17 07:09:41'),(10,5,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/5/9a4aa269-4db2-424a-84d4-57febb9b7267.jpg',5,'2026-03-17 07:09:41'),(11,6,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/6/6ce679fc-8cdc-4361-bb56-e1fc5f1fb10d.jpg',1,'2026-03-17 07:09:58'),(12,6,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/6/3ae9f5df-df60-4506-b021-ba150ceacf97.jpg',2,'2026-03-17 07:09:58'),(13,6,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/6/2956219d-116f-4799-94a5-6ea8d4a7df65.jpg',3,'2026-03-17 07:09:58'),(14,6,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/6/bd49c146-0cef-4d03-a541-920f6862124f.jpg',4,'2026-03-17 07:09:58'),(15,6,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/6/d9f002fc-3fe8-4fa6-9381-b88460b685a9.jpg',5,'2026-03-17 07:09:58'),(16,13,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/13/2336b14f-5262-45b1-b8fc-e0171eaf3faf.jpg',1,'2026-03-17 07:10:31'),(17,13,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/13/bcc91cc4-92c6-4280-9991-d74a5e84311b.jpg',2,'2026-03-17 07:10:31'),(18,13,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/13/b18d659e-f3eb-41ed-b566-a878f729956e.jpg',3,'2026-03-17 07:10:31'),(19,13,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/13/a0e598a9-9f02-47b5-9fee-f2a7643bdb65.jpg',4,'2026-03-17 07:10:31'),(20,13,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/13/9c26bb93-997e-48a8-b0b2-ee0faebd3fe5.jpg',5,'2026-03-17 07:10:31'),(21,13,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/13/a0cb9de8-ea42-44f2-a709-51f7e94afff2.jpg',6,'2026-03-17 07:10:31'),(22,10,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/10/3b2f6b5d-4c82-44e2-b874-e661bc3cffb1.jpg',1,'2026-03-17 07:11:21'),(23,10,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/10/f12d5000-d090-4a32-966d-4cb5a3404747.jpg',2,'2026-03-17 07:11:21'),(24,10,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/10/7fa64dcb-6550-4b08-801d-23fafe2c90ba.jpg',3,'2026-03-17 07:11:21'),(25,10,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/10/2a0874cb-37d4-4480-ac4d-442d57603122.jpg',4,'2026-03-17 07:11:21'),(26,15,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/15/c89cb923-6254-4940-80e3-1bbd3e56556a.jpg',1,'2026-03-17 07:11:21'),(27,15,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/15/08f1e23e-0904-49cf-ba8f-a9395d4b8a21.jpg',2,'2026-03-17 07:11:21'),(28,15,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/15/f8f903d9-2c83-4be0-afe7-e19b48382714.jpg',3,'2026-03-17 07:11:21'),(29,15,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/15/064b7f3b-fa15-4498-b78d-80186f7b3b85.jpg',4,'2026-03-17 07:11:21'),(30,15,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/15/4c6ebb1a-4bd5-4d26-9cf7-261b602c311a.jpg',5,'2026-03-17 07:11:21'),(31,8,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/8/4e88ec9f-89fe-4789-a076-13e105df1012.jpg',1,'2026-03-17 07:11:21'),(32,8,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/8/5ad6ca14-a014-4d31-ab0a-bd0b8507f045.jpg',2,'2026-03-17 07:11:21'),(33,8,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/8/f3db8bfb-91f6-4455-9e77-72a7249c3cf9.jpg',3,'2026-03-17 07:11:21'),(34,8,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/8/46b047cc-e34a-443f-a0b6-be07ec94e595.jpg',4,'2026-03-17 07:11:21'),(35,8,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/8/1e5f2e22-08f5-4906-a7b0-7b102c59b590.jpg',5,'2026-03-17 07:11:21'),(36,4,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/4/4d540f31-7654-4f0b-811c-d83d8ded37c7.jpg',1,'2026-03-17 07:11:21'),(37,4,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/4/993e8d92-45da-4d64-8cad-746ae6b17322.jpg',2,'2026-03-17 07:11:21'),(38,4,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/4/699cae92-daf4-4097-864f-d94837ef7f8f.jpg',3,'2026-03-17 07:11:21'),(39,4,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/4/85a7aa6e-0b44-40bf-8ecb-989a254fceb5.jpg',4,'2026-03-17 07:11:21'),(40,4,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/4/a241c175-e4f3-4afa-95a6-a8ec802060a6.jpg',5,'2026-03-17 07:11:21'),(41,12,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/12/a52b87fb-816b-4f16-a012-67e49cc1df35.jpg',1,'2026-03-17 07:11:21'),(42,12,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/12/7053a69e-0a27-4e8e-b814-ecd7384538fa.jpg',2,'2026-03-17 07:11:21'),(43,12,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/12/89b43e33-db1c-405c-aef4-e4605a69c383.jpg',3,'2026-03-17 07:11:21'),(44,12,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/12/8044fa04-d611-4fa2-bcf5-936656991079.jpg',4,'2026-03-17 07:11:21'),(45,12,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/12/d805e758-c53d-4cce-9151-9057b50c3f49.jpg',5,'2026-03-17 07:11:21'),(46,12,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/12/8ced3f37-c8ad-4b3d-a046-0e4b8b9bb18d.jpg',6,'2026-03-17 07:11:21'),(47,9,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/9/3075678a-0e32-4ad9-a416-ab90c98730fa.jpg',1,'2026-03-17 07:11:21'),(48,9,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/9/a1b9e489-18e8-45d3-b383-2c69a1a5767e.jpg',2,'2026-03-17 07:11:21'),(49,9,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/9/d84c8b62-0348-482f-b95b-becfd904f3ae.jpg',3,'2026-03-17 07:11:22'),(50,9,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/9/4008641a-b801-4429-9dc5-c0a317bc0d2c.jpg',4,'2026-03-17 07:11:22'),(51,9,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/9/da81b1e4-bcf9-416d-bcbe-0becc8e4c1d0.jpg',5,'2026-03-17 07:11:22'),(52,14,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/14/ed2f620e-9847-4177-bbf7-0195c5f7c20c.jpg',1,'2026-03-17 07:11:22'),(53,14,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/14/5f1b0b21-0d7d-4202-a108-c804d1b12f91.jpg',2,'2026-03-17 07:11:22'),(54,14,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/14/e4d49c46-b923-43fb-85f3-65295ad40591.jpg',3,'2026-03-17 07:11:22'),(55,14,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/14/976b3f13-aa51-4609-b64c-4a71250c0f73.jpg',4,'2026-03-17 07:11:22'),(56,14,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/14/5a6d5bc9-ee91-4aac-b1e5-bfddfe73d5b5.jpg',5,'2026-03-17 07:11:22'),(57,14,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/images/14/cf4ecd1c-23db-459d-a4b2-502060b154d2.jpg',6,'2026-03-17 07:11:22');
/*!40000 ALTER TABLE `attraction_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `attractions`
--

LOCK TABLES `attractions` WRITE;
/*!40000 ALTER TABLE `attractions` DISABLE KEYS */;
INSERT INTO `attractions` (`id`, `name`, `intro`, `story`, `lng`, `lat`, `radius`, `cover`, `create_time`, `rating`, `duration`, `best_season`) VALUES (4,'武汉黄鹤楼','黄鹤楼位于武汉蛇山之巅，濒临长江，是江南三大名楼之一。始建于三国时期东吴黄武二年（223年），最初是作为军事瞭望楼。现楼为1985年重建，以清代同治年间楼式为蓝本，高51.4米，共五层。','黄鹤楼因唐代诗人崔颢题《黄鹤楼》诗而名扬天下：\"昔人已乘黄鹤去，此地空余黄鹤楼。黄鹤一去不复返，白云千载空悠悠。\"李白、白居易、苏轼等历代文人墨客在此留下无数诗篇。楼内藏有历代黄鹤楼模型和名人书画。',114.302467,30.544649,150,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/covers/fcc5e006-2897-4306-ab81-a45513d20127.jpg','2026-03-14 02:39:55','AAAAA级景区','日场 08：30-18：00；夜场 19：30-22：00','四季皆宜（春秋季最佳）'),(5,'北京故宫','北京故宫，旧称紫禁城，位于北京中轴线的中心，是明清两代的皇家宫殿。始建于明永乐四年（1406年），永乐十八年（1420年）建成。占地面积72万平方米，建筑面积约15万平方米，有大小宫殿七十多座，房屋九千余间。','故宫是世界上现存规模最大、保存最为完整的木质结构古建筑群。曾居住过24位皇帝。1911年辛亥革命后，溥仪退位仍居内廷。1925年10月10日成立故宫博物院。1987年被列为世界文化遗产。',116.397029,39.917839,300,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/covers/81486f01-3881-435c-8faa-01e62e485142.jpg','2026-03-14 02:39:55','AAAAA级景区','08:30-16:30','四季皆宜（秋季最佳）'),(6,'北京颐和园','颐和园是中国现存规模最大、保存最完整的皇家园林，位于北京西北郊。全园占地3.009平方公里，水面约占四分之三。以万寿山和昆明湖为基础，汲取江南园林设计手法而建。','颐和园前身为清漪园，始建于清乾隆十五年（1750年）。1860年被英法联军焚毁，光绪十二年（1886年）慈禧太后挪用海军经费重建，改名颐和园。1900年又遭八国联军破坏。1961年成为第一批全国重点文物保护单位。',116.275179,39.999617,300,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/covers/2c240876-cac6-4553-ac8a-3c160b261762.jpg','2026-03-14 02:39:55','AAAAA级景区','06:30-19:00','四季皆宜（夏秋季最佳）'),(7,'北京天坛','天坛是明清两代皇帝祭天、祈谷和祈雨的场所，始建于明永乐十八年（1420年）。占地约273万平方米，是中国现存最大的古代祭祀性建筑群。','天坛最著名的建筑是祈年殿，是一座鎏金宝顶、蓝瓦红柱的三重檐圆形大殿。独特的圜丘坛、皇穹宇等建筑体现了\"天圆地方\"的宇宙观。回音壁的声学现象更是令人称奇。1998年被列为世界文化遗产。',116.410829,39.881913,200,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/covers/e6b4fe67-ac3f-499a-a970-23ac454a90f1.jpg','2026-03-14 02:39:55','AAAAA级景区','06:30-22:00','四季皆宜（春秋季最佳）'),(8,'杭州西湖三潭印月','三潭印月是西湖十景之一，位于西湖外湖西南部的小瀛洲岛及附近水面。岛上建有先贤祠、九曲桥等建筑，湖中有三座石塔，塔高2.5米，造型优美。','三潭印月始建于北宋元祐五年（1090年），苏轼疏浚西湖时在湖中立三塔为标志，规定三塔之内不得种植菱藕。明万历三十五年（1607年）钱塘县令聂心汤取湖中葑泥堆筑小瀛洲。现在的石塔为明天启年间重建。',120.145369,30.238845,100,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/covers/ccc73f7f-04d3-4e5d-97ef-e71515288b8f.jpg','2026-03-14 02:39:55','AAAAA级景区（西湖）','08:00-17:00','四季皆宜（春秋季最佳）'),(9,'西安大雁塔','大雁塔位于西安大慈恩寺内，始建于唐永徽三年（652年），是玄奘法师为保存从印度带回的佛经和佛像而主持修建。最初五层，后增至七层，高64.5米。','大雁塔是现存最早、规模最大的唐代四方楼阁式砖塔。玄奘法师在此翻译佛经75部，共1335卷。塔内保存有唐代精美的线刻画。唐代新科进士有\"雁塔题名\"的习俗。1961年成为首批全国重点文物保护单位。',108.964176,34.218229,150,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/covers/987f2cbd-fc62-458e-9eb0-245681707919.jpg','2026-03-14 02:39:55','AAAAA级景区','09:00-21:00','四季皆宜（春秋季最佳）'),(10,'开封铁塔','开封铁塔原名开宝寺塔，位于开封市东北角，始建于北宋皇祐元年（1049年）。塔高55.88米，八角十三层，因塔身全部用褐色琉璃砖砌成，远看似铁色，故称\"铁塔\"。','铁塔历经37次地震，18次大风，15次水患，仍巍然屹立。琉璃砖上雕有飞天、麒麟、菩萨等图案。1938年曾遭日军炮击，塔身中弹数十发，但仍屹立不倒，被誉为\"天下第一塔\"。',114.369748,34.816081,120,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/covers/eda03b97-d05f-4fea-a939-a27c359638e0.jpg','2026-03-14 02:39:55','AAAA级景区','08:30-21:00','四季皆宜（秋季最佳）'),(12,'武汉东湖','武汉东湖是中国最大的城中湖，水域面积33平方公里，是西湖的6倍。东湖绿道总长101.98公里，是国内最长5A级景区绿道。','东湖自古以来就是游览胜地，屈原曾在东湖\"行吟泽畔\"。毛泽东曾多次入住东湖宾馆，称东湖为\"白云黄鹤的地方\"。东湖磨山樱园是世界三大樱花园之一。',114.413970,30.549509,500,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/covers/b7a470b5-3469-4266-bab0-9bdeca172174.jpg','2026-03-17 02:41:31','AAAAA级景区','全天开放 (内部景点8:30-17:00)','四季皆宜（春秋最佳）'),(13,'杭州雷峰塔','雷峰塔位于杭州西湖南岸夕照山上，始建于北宋太平兴国二年（977年），是吴越国王钱俶为供奉佛螺髻发舍利而建。','雷峰塔因《白蛇传》传说而家喻户晓。旧塔于1924年倒塌，2002年重建。新塔保留了遗址，采用钢结构，塔内设有电梯。登塔可俯瞰西湖全景。',120.148849,30.230934,120,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/covers/aae61e49-93d0-4a1e-8a85-627613ab5541.jpg','2026-03-17 02:41:31','AAAA级景区','08:00-20:00','四季皆宜（春秋最佳）'),(14,'西安钟楼','西安钟楼位于西安市中心，始建于明洪武十七年（1384年），原址在今西大街广济街口，后迁至现址。是中国现存规模最大、保存最完整的钟楼。','钟楼昔日楼上悬一口大钟，作为报时之用。楼体为砖木结构，高36米，基座为正方形，四面各有券门。楼内藏有明清瓷器、字画等文物。',108.947030,34.259430,100,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/covers/97d74755-0977-4a0d-8301-a20f4a0783ce.jpg','2026-03-17 02:41:31','AAAA级景区','08:30-20:30','四季皆宜'),(15,'开封开封府','开封府位于开封市包公湖东岸，是北宋京都官吏行政、司法的衙署。重建的开封府占地60余亩，建筑面积1.36万平方米。','开封府以包公打坐南衙而闻名。府内每天上演\"开衙仪式\"、\"包公审案\"等实景演出。主要景点有仪门、正厅、议事厅、梅花堂等。',114.346500,34.789726,150,'https://travelate.oss-cn-wuhan-lr.aliyuncs.com/attractions/covers/9d3c711c-017e-4a57-9921-4cfae1501a93.jpg','2026-03-17 02:41:31','AAAA级景区','07:30-19:00','四季皆宜（春秋最佳）');
/*!40000 ALTER TABLE `attractions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `conversations`
--

LOCK TABLES `conversations` WRITE;
/*!40000 ALTER TABLE `conversations` DISABLE KEYS */;
INSERT INTO `conversations` (`id`, `type`, `user1_id`, `user2_id`, `last_message`, `last_message_time`, `unread_count_user1`, `unread_count_user2`, `create_time`, `update_time`) VALUES (4,'chat',10000,10011,'你好','2026-03-21 00:16:23',0,0,'2026-03-19 20:03:18','2026-03-21 01:57:42'),(5,'chat',10020,10011,'在吗','2026-03-21 03:28:54',0,0,'2026-03-21 03:28:08','2026-03-21 03:40:55');
/*!40000 ALTER TABLE `conversations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `messages`
--

LOCK TABLES `messages` WRITE;
/*!40000 ALTER TABLE `messages` DISABLE KEYS */;
INSERT INTO `messages` (`id`, `conversation_id`, `sender_id`, `receiver_id`, `content`, `type`, `source_lang`, `is_read`, `create_time`, `deleted_by_receiver`, `deleted_by_sender`) VALUES (12,4,10000,10011,'hello,what is your name?','text','en',1,'2026-03-20 15:33:31',b'0',b'0'),(13,4,10011,10000,'你好我叫Jane','text','auto',1,'2026-03-20 15:42:04',b'0',b'0'),(14,4,10011,10000,'你呢，你叫什么','text','auto',1,'2026-03-20 15:43:35',b'0',b'0'),(15,4,10011,10000,'你好','text','zh',1,'2026-03-21 00:16:23',b'0',b'0'),(16,5,10020,10011,'你好','text','zh',1,'2026-03-21 03:28:20',b'0',b'0'),(18,5,10020,10011,'在吗','text','zh',1,'2026-03-21 03:28:54',b'0',b'0');
/*!40000 ALTER TABLE `messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` (`id`, `user_id`, `type`, `from_user_id`, `post_id`, `comment_id`, `content`, `is_read`, `create_time`) VALUES (3,10011,'comment',10000,1,20,'y0 评论了你的帖子: ceshi',1,'2026-03-19 19:21:58'),(5,10011,'collection',10000,1,NULL,'y0 收藏了你的帖子',1,'2026-03-19 20:14:40'),(6,10000,'collection',10011,2,NULL,'+1 收藏了你的帖子',1,'2026-03-20 09:26:59'),(7,10000,'comment',10011,2,24,'+1 评论了你的帖子: 好看',1,'2026-03-20 09:27:06'),(8,10011,'collection',10000,4,NULL,'y0 收藏了你的帖子',1,'2026-03-20 19:00:02'),(9,10011,'collection',10020,6,NULL,'123 收藏了你的帖子',1,'2026-03-21 03:26:13'),(10,10011,'comment',10020,6,31,'123 评论了你的帖子: 543',1,'2026-03-21 03:26:19');
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `phrases`
--

LOCK TABLES `phrases` WRITE;
/*!40000 ALTER TABLE `phrases` DISABLE KEYS */;
INSERT INTO `phrases` (`id`, `user_id`, `text`, `category`, `create_time`, `original_text`) VALUES (1,10011,'没关系','收藏','2026-03-20 02:40:18',NULL),(2,10011,'我需要帮助','收藏','2026-03-20 02:40:43',NULL),(4,10011,'非常感谢。','收藏','2026-03-20 17:10:09',NULL),(5,10011,'怎么去那里','收藏','2026-03-20 17:11:52',NULL),(6,10000,'非常感谢','收藏','2026-03-20 17:14:16',NULL),(7,10000,'嗨，很高兴认识你','收藏','2026-03-20 19:33:51','hi,nice to meet you'),(56,10011,'你好吗我很好。','收藏','2026-03-21 02:57:14','How are you?I am fine.'),(57,10020,'你好，请问附近有洗手间吗？','日常','2026-03-21 03:16:08','こんにちは、近くにトイレはありますか？'),(58,10020,'这个多少钱？','買い物','2026-03-21 03:16:08','これはいくらですか？'),(59,10020,'请给我一杯咖啡','飲食','2026-03-21 03:16:08','コーヒーを一杯ください'),(60,10020,'请问怎么去机场？','交通','2026-03-21 03:16:08','空港へはどうやって行きますか？'),(61,10020,'我需要帮助','緊急','2026-03-21 03:16:08','助けが必要です'),(62,10020,'谢谢你的帮助','礼儀','2026-03-21 03:16:08','助けてくれてありがとう'),(63,10020,'我不会说当地语言','コミュニケーション','2026-03-21 03:16:08','現地の言葉が話せません'),(64,10020,'请说慢一点','コミュニケーション','2026-03-21 03:16:08','もっとゆっくり話してください'),(82,10023,'你好，请问附近有洗手间吗？','日常','2026-03-22 07:19:34','你好，请问附近有洗手间吗？'),(83,10023,'这个多少钱？','购物','2026-03-22 07:19:34','这个多少钱？'),(84,10023,'请给我一杯咖啡','餐饮','2026-03-22 07:19:34','请给我一杯咖啡'),(85,10023,'请问怎么去机场？','交通','2026-03-22 07:19:34','请问怎么去机场？'),(86,10023,'我需要帮助','紧急','2026-03-22 07:19:34','我需要帮助'),(87,10023,'谢谢你的帮助','礼貌','2026-03-22 07:19:34','谢谢你的帮助'),(88,10023,'我不会说当地语言','沟通','2026-03-22 07:19:34','我不会说当地语言'),(89,10023,'请说慢一点','沟通','2026-03-22 07:19:34','请说慢一点');
/*!40000 ALTER TABLE `phrases` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `travel_post_collections`
--

LOCK TABLES `travel_post_collections` WRITE;
/*!40000 ALTER TABLE `travel_post_collections` DISABLE KEYS */;
INSERT INTO `travel_post_collections` (`id`, `post_id`, `user_id`, `create_time`) VALUES (11,1,10000,'2026-03-19 20:14:40'),(12,4,10011,'2026-03-20 09:25:24'),(13,2,10011,'2026-03-20 09:26:59'),(15,4,10000,'2026-03-20 19:00:02'),(16,6,10011,'2026-03-21 03:06:32'),(17,6,10020,'2026-03-21 03:26:13');
/*!40000 ALTER TABLE `travel_post_collections` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `travel_post_comment_likes`
--

LOCK TABLES `travel_post_comment_likes` WRITE;
/*!40000 ALTER TABLE `travel_post_comment_likes` DISABLE KEYS */;
INSERT INTO `travel_post_comment_likes` (`id`, `comment_id`, `user_id`, `create_time`) VALUES (1,2,10000,'2026-03-19 17:38:05');
/*!40000 ALTER TABLE `travel_post_comment_likes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `travel_post_comments`
--

LOCK TABLES `travel_post_comments` WRITE;
/*!40000 ALTER TABLE `travel_post_comments` DISABLE KEYS */;
INSERT INTO `travel_post_comments` (`id`, `post_id`, `user_id`, `parent_id`, `reply_to_user_id`, `content`, `like_count`, `create_time`, `source_lang`) VALUES (1,1,10011,NULL,NULL,'hi，every one,this is my first post',0,'2026-03-19 13:51:50','zh'),(2,1,10000,NULL,NULL,'欢迎来到travelate!!!',1,'2026-03-19 14:05:28','zh'),(3,3,10000,NULL,NULL,'夏夜里的晚风',0,'2026-03-19 17:23:41','zh'),(4,1,10000,2,10000,'回复功能也这么轻而易举被我实现了',0,'2026-03-19 17:36:14','zh'),(6,1,10000,4,10000,'删除评论、点赞评论也是轻而易举',0,'2026-03-19 17:45:13','zh'),(18,2,10000,NULL,NULL,'11',0,'2026-03-19 19:08:42','zh'),(19,2,10000,NULL,NULL,'76',0,'2026-03-19 19:21:16','zh'),(20,1,10000,NULL,NULL,'ceshi',0,'2026-03-19 19:21:58','zh'),(24,2,10011,NULL,NULL,'好看',0,'2026-03-20 09:27:06','auto'),(25,6,10011,NULL,NULL,'你好',0,'2026-03-21 03:06:35','zh'),(26,6,10011,25,10011,'你也好',0,'2026-03-21 03:06:43','zh'),(27,7,10020,NULL,NULL,'12345',0,'2026-03-21 03:25:29','auto'),(31,6,10020,NULL,NULL,'543',0,'2026-03-21 03:26:18','auto');
/*!40000 ALTER TABLE `travel_post_comments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `travel_post_history`
--

LOCK TABLES `travel_post_history` WRITE;
/*!40000 ALTER TABLE `travel_post_history` DISABLE KEYS */;
INSERT INTO `travel_post_history` (`id`, `post_id`, `user_id`, `view_count`, `last_view_time`) VALUES (1,1,10011,25,'2026-03-21 03:11:01'),(3,1,10000,24,'2026-03-19 20:14:28'),(4,2,10000,50,'2026-03-20 15:52:57'),(5,3,10000,15,'2026-03-20 17:44:16'),(6,2,10011,32,'2026-03-21 03:07:43'),(7,3,10011,6,'2026-03-21 00:12:01'),(8,4,10011,12,'2026-03-21 00:05:30'),(9,4,10000,2,'2026-03-20 17:49:40'),(10,5,10000,7,'2026-03-20 18:59:49'),(11,5,10011,4,'2026-03-21 03:08:08'),(12,6,10011,15,'2026-03-21 03:42:28'),(14,6,10020,6,'2026-03-21 03:29:11'),(15,5,10020,1,'2026-03-21 03:19:04'),(16,7,10020,3,'2026-03-21 03:26:11');
/*!40000 ALTER TABLE `travel_post_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `travel_posts`
--

LOCK TABLES `travel_posts` WRITE;
/*!40000 ALTER TABLE `travel_posts` DISABLE KEYS */;
INSERT INTO `travel_posts` (`id`, `user_id`, `title`, `content`, `category`, `images`, `tags`, `status`, `view_count`, `comment_count`, `create_time`, `update_time`, `collection_count`, `collect_count`, `source_lang`) VALUES (1,10011,'游戏美景治愈人心','漂浮在寂静无垠的大海上','travel','[\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/1/3264b64d-d59b-4620-8689-2e35dbd01152.png\",\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/1/c159c89f-a9fd-4651-9abd-0ec45e032aa4.png\"]','[\"风景\",\"游戏\",\"vibe\"]',1,54,5,'2026-03-19 13:51:19','2026-03-21 09:48:17',NULL,1,'zh'),(2,10000,'黄鹤楼不只是与火车同框｜与汽车同框也出片','黄鹤楼与京广线火车同框是一网红机位（透过铁丝网拍摄），外地人来黄鹤楼游玩打卡，通常会在此机位拍摄一两张黄鹤楼与火车同框照。但很少人知道，在此机位的马路正对面人行道上，也有一个拍摄黄鹤楼的绝佳机位：可以拍到黄鹤楼与过往长江大桥的汽车同框照。\n机位点：\n与网红铁丝网打卡点马路相隔，即大桥南侧人行道上；\n到达路线：\n①司门口天桥横穿铁路、公路，上南侧人行道，往桥头堡方向步行五六百米即到；\n②中华路码头沿江逆行，穿过大桥后立即左拐，沿台阶拾级而上，到达公路桥面即是。','travel','[\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/2/85d1f028-6071-4825-92a0-4e92322710df.jpg\",\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/2/c1c45dd4-d65b-44d5-b1ae-d49d20792514.jpg\",\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/2/e7160a3b-94da-47dd-82f5-eae2ca242c76.jpg\"]','[\"旅行\",\"风景\",\"攻略\"]',1,83,3,'2026-03-19 14:36:54','2026-03-21 09:48:17',NULL,1,'zh'),(3,10000,'有被这几晚的东湖美到！','下午6点多，在东湖边的慢跑~','travel','[\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/3/3eaa5725-3a4d-42bd-a57d-daf6e2deb1ca.jpg\",\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/3/863ace20-e1a5-4eb6-8417-563837e8757e.jpg\"]','[\"武汉\",\"东湖\",\"大自然\",\"风景\",\"晚霞\"]',1,21,1,'2026-03-19 16:05:31','2026-03-21 09:48:17',NULL,1,'zh'),(4,10011,'武汉被严重低估的山寺樱花！好有禅意','宝通寺樱花 宝通寺对面梦时代的粉色樱花开的真好，有十几颗，楼上可以拍宝通寺全景','tips','[\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/4/c8307dfe-800f-4c4a-8cb5-50ff389a4ff0.jpg\",\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/4/67e5ea6c-165b-4ec4-a143-3c865bb2fce4.jpg\",\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/4/418b65c4-2945-4986-9bb9-b26cf9e6efc1.jpg\"]','[\"风景\",\"宝通寺\",\"樱花\",\"春天\"]',1,15,0,'2026-03-20 09:20:15','2026-03-21 09:48:17',NULL,2,'zh'),(5,10000,'A one-day tour of the Forbidden City','Today I started my Beijing trip and my first stop was the Forbidden City. Take a look at the photos I took.','travel','[\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/5/eb8873f9-2140-4958-985a-7de9ca7565c8.jpg\",\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/5/7d8f8992-da48-4b95-956a-e8db25bc0aa5.jpg\",\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/5/392f3f84-ef3c-4e70-8256-6b62f75a2fd3.jpg\"]','[\"Travel\",\"ForbiddenCity\",\"Beijing\"]',1,14,0,'2026-03-20 17:53:41','2026-03-21 12:00:31',NULL,0,'auto'),(6,10011,'111','interesting\nfunny','travel','[\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/6/67129ed2-ae7d-48c1-bbef-2c48ded959ed.jpg\"]','[\"风景\"]',1,24,3,'2026-03-21 00:17:37','2026-03-21 11:57:17',NULL,2,'en'),(7,10020,'别只拍金光穿洞了，来拍佛香阁亮灯吧！','今天给大家分享的是佛香阁亮灯。\n一年一度金光穿洞又开始，不过人依旧多得给力，实在挤不进去的话建议还是从西门进，来畅观堂附近拍颐和园亮灯。尤其这个机位，佛香阁和豳风桥都亮起来后，等到蓝调时刻，会非常有意境，推荐大家都来打卡。佛香阁周五周六亮灯。','travel','[\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/7/5ea1bc27-60d3-452a-b5ba-dd0e2c5235e9.jpg\",\"https://travelate.oss-cn-wuhan-lr.aliyuncs.com/posts/7/fa523867-578a-4892-9cb4-59e020ca2a70.jpg\"]','[\"北京旅游\",\"古建筑摄影\",\"颐和园\",\"摄影\"]',1,4,1,'2026-03-21 03:23:40','2026-03-21 12:11:12',NULL,0,'zh');
/*!40000 ALTER TABLE `travel_posts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` (`id`, `username`, `password`, `phone`, `email`, `country`, `lang_code`, `region_code`, `nickname`, `avatar`, `status`, `last_login_time`, `create_time`, `update_time`, `github_id`) VALUES (10000,'15171483964','$2a$10$HVUF5YBQdeLGoYyPOcMYx.fUZSjCl8mlE4afIgaSUq83ajH9PZOpa','15171483964',NULL,'China','ja','CN','y0','https://api.dicebear.com/7.x/avataaars/svg?seed=15171483964',1,'2026-03-22 07:01:10','2026-03-10 02:52:02','2026-03-22 07:01:10',NULL),(10007,'yenOmld','$2a$10$cmYPMGS7d/wYCq.wnGbaTOBCM80X8T5Tpg62WnFz1ef6O0FL9KI4S',NULL,'2971549832@qq.com','China',NULL,NULL,'用户6724','https://avatars.githubusercontent.com/u/189572187?v=4',1,'2026-03-10 12:27:06','2026-03-10 12:11:21','2026-03-11 08:42:07','189572187'),(10011,'qinjiayi_1106@qq.com','$2a$10$Bw/vbiFWTIl0ermD7JF7e.CdWtBTTWUnRl34hWP/s2LFYB09df3W2',NULL,'qinjiayi_1106@qq.com','China','en','CN','+1','https://api.dicebear.com/7.x/avataaars/svg?seed=qinjiayi_1106@qq.com',1,'2026-03-21 07:44:24','2026-03-13 01:57:15','2026-03-21 07:44:24',NULL),(10020,'2894717528@qq.com','$2a$10$kb5zRuvBKTbthpHrGI8zyeCwvAbDbQWK8.iKLiQp89HUfmxGlihrW',NULL,'2894717528@qq.com','Japan','zh','JP','123','https://travelate.oss-cn-wuhan-lr.aliyuncs.com/avatars/b5e816fc-6097-4482-ad9d-81f5017317b6.jpg',1,'2026-03-21 03:16:11','2026-03-21 03:16:08','2026-03-21 09:48:17',NULL),(10023,'qianrui2006@qq.com','$2a$10$920GHF7Gc5Prpfh2K0PRaeNJpF/sR9DuAbK/d0l3XvG7LDzhUccvS',NULL,'qianrui2006@qq.com','China','zh','CN','用户8346','https://api.dicebear.com/7.x/avataaars/svg?seed=qianrui2006@qq.com',1,'2026-03-22 07:19:36','2026-03-22 07:19:34','2026-03-22 07:19:36',NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-23  3:20:29
