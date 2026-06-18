-- MySQL dump 10.13  Distrib 8.0.22, for Win64 (x86_64)
--
-- Host: localhost    Database: online_exam_system
-- ------------------------------------------------------
-- Server version	8.0.22

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
-- Table structure for table `chat_conversation`
--

DROP TABLE IF EXISTS `chat_conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user1_id` bigint NOT NULL COMMENT '用户1（较小ID）',
  `user2_id` bigint NOT NULL COMMENT '用户2（较大ID）',
  `last_message` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后一条消息摘要',
  `last_message_time` datetime DEFAULT NULL COMMENT '最后消息时间',
  `last_message_sender_id` bigint DEFAULT NULL COMMENT '最后一条消息的发送者ID（L3-M0-5：撤回文案区分"你/对方"）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `user1_hidden` tinyint DEFAULT '0' COMMENT 'user1 是否已隐藏该会话：0显示 1隐藏',
  `user2_hidden` tinyint DEFAULT '0' COMMENT 'user2 是否已隐藏该会话：0显示 1隐藏',
  `user1_pinned` tinyint DEFAULT '0' COMMENT 'user1 is_pinned 0=no 1=yes (L3-M0-7)',
  `user2_pinned` tinyint DEFAULT '0' COMMENT 'user2 is_pinned 0=no 1=yes (L3-M0-7)',
  `user1_muted` tinyint DEFAULT '0' COMMENT 'user1 is_muted 0=no 1=yes (L3-M0-7)',
  `user2_muted` tinyint DEFAULT '0' COMMENT 'user2 is_muted 0=no 1=yes (L3-M0-7)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users` (`user1_id`,`user2_id`),
  KEY `fk_conv_user2` (`user2_id`),
  CONSTRAINT `fk_conv_user1` FOREIGN KEY (`user1_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_conv_user2` FOREIGN KEY (`user2_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='聊天会话表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_conversation`
--

LOCK TABLES `chat_conversation` WRITE;
/*!40000 ALTER TABLE `chat_conversation` DISABLE KEYS */;
/*!40000 ALTER TABLE `chat_conversation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_message`
--

DROP TABLE IF EXISTS `chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` bigint NOT NULL COMMENT '会话ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID',
  `content` mediumtext COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息内容（mediumtext，支持长消息/图片 base64 内嵌）',
  `message_type` tinyint DEFAULT '1' COMMENT '消息类型：1文字 2图片(预留)',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读：0未读 1已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL COMMENT '软删时间（NULL=未删除）',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人用户ID（发送者撤回=自己；管理员强删=管理员ID）',
  PRIMARY KEY (`id`),
  KEY `idx_conv_time` (`conversation_id`,`create_time`),
  KEY `idx_receiver_read` (`receiver_id`,`is_read`),
  KEY `fk_msg_sender` (`sender_id`),
  KEY `idx_deleted` (`deleted_at`),
  CONSTRAINT `fk_msg_conv` FOREIGN KEY (`conversation_id`) REFERENCES `chat_conversation` (`id`),
  CONSTRAINT `fk_msg_sender` FOREIGN KEY (`sender_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=162 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='聊天消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_message`
--

LOCK TABLES `chat_message` WRITE;
/*!40000 ALTER TABLE `chat_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `chat_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `edu_class`
--

DROP TABLE IF EXISTS `edu_class`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `edu_class` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '班级ID',
  `class_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '班级名称',
  `grade` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '年级',
  `major_id` bigint DEFAULT NULL COMMENT '所属专业ID',
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_class_major` (`major_id`),
  CONSTRAINT `fk_class_major` FOREIGN KEY (`major_id`) REFERENCES `edu_major` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='班级表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `edu_class`
--

LOCK TABLES `edu_class` WRITE;
/*!40000 ALTER TABLE `edu_class` DISABLE KEYS */;
INSERT INTO `edu_class` VALUES (1,'计算机2201','2022级',1,'计算机科学与技术专业','2026-04-01 18:17:32'),(2,'计算机2202','2022级',1,'计算机科学与技术专业','2026-04-01 18:17:32'),(3,'软件工程2201','2022级',2,'软件工程专业','2026-04-01 18:17:32'),(4,'软件工程2202','2022级',2,'软件工程专业','2026-04-01 18:17:32'),(7,'人工智能2201','2022级',4,'人工智能专业','2026-04-01 18:17:32'),(8,'人工智能2202','2022级',4,'人工智能专业','2026-04-01 18:17:32');
/*!40000 ALTER TABLE `edu_class` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `edu_major`
--

DROP TABLE IF EXISTS `edu_major`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `edu_major` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '专业ID',
  `major_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '专业名称',
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='专业表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `edu_major`
--

LOCK TABLES `edu_major` WRITE;
/*!40000 ALTER TABLE `edu_major` DISABLE KEYS */;
INSERT INTO `edu_major` VALUES (1,'计算机科学与技术','计算机科学与技术专业','2026-04-01 18:17:32'),(2,'软件工程','软件工程专业','2026-04-01 18:17:32'),(4,'人工智能','人工智能专业','2026-04-01 18:17:32');
/*!40000 ALTER TABLE `edu_major` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `edu_subject`
--

DROP TABLE IF EXISTS `edu_subject`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `edu_subject` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '科目ID',
  `subject_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '科目名称',
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='科目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `edu_subject`
--

LOCK TABLES `edu_subject` WRITE;
/*!40000 ALTER TABLE `edu_subject` DISABLE KEYS */;
INSERT INTO `edu_subject` VALUES (1,'高等数学','公共基础课程','2026-04-01 18:17:32'),(2,'数据结构','专业核心课程','2026-04-01 18:17:32'),(3,'计算机网络','专业核心课程','2026-04-01 18:17:32'),(4,'数据库原理','专业核心课程','2026-04-01 18:17:32'),(5,'Java程序设计','专业核心课程','2026-04-01 18:17:32'),(6,'软件工程导论','软件工程方向课程','2026-04-01 18:17:32'),(7,'计算机组成原理','计算机科学方向课程','2026-04-01 18:17:32'),(9,'人工智能','人工智能方向课程','2026-04-01 18:17:32');
/*!40000 ALTER TABLE `edu_subject` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_answer`
--

DROP TABLE IF EXISTS `exam_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_answer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` bigint NOT NULL COMMENT '考试记录ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `answer` mediumtext COLLATE utf8mb4_general_ci COMMENT '学生答案（mediumtext，支持长简答题/代码题提交）',
  `is_correct` tinyint DEFAULT NULL COMMENT '是否正确：0错误 1正确 2部分正确',
  `score` decimal(5,2) DEFAULT '0.00' COMMENT '得分',
  `comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '教师评语（阅卷时填写）',
  `is_removed` tinyint DEFAULT '0' COMMENT '是否从错题本移除：0否 1是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_question` (`record_id`,`question_id`),
  KEY `idx_answer_record` (`record_id`),
  KEY `idx_answer_question` (`question_id`),
  CONSTRAINT `fk_answer_question` FOREIGN KEY (`question_id`) REFERENCES `exam_question` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_answer_record` FOREIGN KEY (`record_id`) REFERENCES `exam_record` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_answer_correct` CHECK (((`is_correct` is null) or (`is_correct` in (0,1,2)))),
  CONSTRAINT `chk_answer_removed` CHECK ((`is_removed` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=104 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='答题记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_answer`
--

LOCK TABLES `exam_answer` WRITE;
/*!40000 ALTER TABLE `exam_answer` DISABLE KEYS */;
/*!40000 ALTER TABLE `exam_answer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_exam`
--

DROP TABLE IF EXISTS `exam_exam`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_exam` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考试ID',
  `exam_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '考试名称',
  `paper_id` bigint NOT NULL COMMENT '试卷ID',
  `class_id` bigint NOT NULL COMMENT '班级ID',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `creator_id` bigint NOT NULL COMMENT '创建人ID（教师）',
  `status` tinyint DEFAULT '0' COMMENT '状态：0未开始 1进行中 2已结束',
  `score_published` tinyint(1) NOT NULL DEFAULT '0' COMMENT '成绩是否已发布 0未发布 1已发布',
  `last_publish_time` datetime DEFAULT NULL COMMENT '最后一次成绩发布时间（同时作为乐观锁版本号，支持无限次重发）',
  `anti_cheat_config` json DEFAULT NULL COMMENT '防作弊配置JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_exam_class_endtime` (`class_id`,`end_time`),
  KEY `idx_exam_creator` (`creator_id`),
  KEY `fk_exam_paper` (`paper_id`),
  CONSTRAINT `fk_exam_class` FOREIGN KEY (`class_id`) REFERENCES `edu_class` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_exam_creator` FOREIGN KEY (`creator_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_exam_paper` FOREIGN KEY (`paper_id`) REFERENCES `exam_paper` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_exam`
--

LOCK TABLES `exam_exam` WRITE;
/*!40000 ALTER TABLE `exam_exam` DISABLE KEYS */;
/*!40000 ALTER TABLE `exam_exam` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_paper`
--

DROP TABLE IF EXISTS `exam_paper`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_paper` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '试卷ID',
  `paper_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '试卷名称',
  `subject_id` bigint NOT NULL COMMENT '科目ID',
  `total_score` decimal(5,2) DEFAULT '100.00' COMMENT '总分',
  `pass_score` decimal(5,2) DEFAULT '60.00' COMMENT '及格分',
  `duration` int DEFAULT '120' COMMENT '考试时长（分钟）',
  `creator_id` bigint NOT NULL COMMENT '创建人ID（教师）',
  `status` tinyint DEFAULT '0' COMMENT '状态：0草稿 1已发布',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_paper_subject` (`subject_id`),
  KEY `idx_paper_creator` (`creator_id`),
  CONSTRAINT `fk_paper_creator` FOREIGN KEY (`creator_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_paper_subject` FOREIGN KEY (`subject_id`) REFERENCES `edu_subject` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_paper_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='试卷表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_paper`
--

LOCK TABLES `exam_paper` WRITE;
/*!40000 ALTER TABLE `exam_paper` DISABLE KEYS */;
/*!40000 ALTER TABLE `exam_paper` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_paper_question`
--

DROP TABLE IF EXISTS `exam_paper_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_paper_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paper_id` bigint NOT NULL COMMENT '试卷ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `score` decimal(5,2) NOT NULL COMMENT '本题分值',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_paper_question` (`paper_id`,`question_id`),
  KEY `fk_pq_question` (`question_id`),
  CONSTRAINT `fk_pq_paper` FOREIGN KEY (`paper_id`) REFERENCES `exam_paper` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pq_question` FOREIGN KEY (`question_id`) REFERENCES `exam_question` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=126 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='试卷题目关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_paper_question`
--

LOCK TABLES `exam_paper_question` WRITE;
/*!40000 ALTER TABLE `exam_paper_question` DISABLE KEYS */;
/*!40000 ALTER TABLE `exam_paper_question` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_paper_template`
--

DROP TABLE IF EXISTS `exam_paper_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_paper_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板名称',
  `subject_id` bigint NOT NULL COMMENT '科目ID',
  `target_score` decimal(5,2) DEFAULT '100.00' COMMENT '目标总分',
  `pass_score` decimal(5,2) DEFAULT '60.00' COMMENT '及格分',
  `duration` int DEFAULT '120' COMMENT '考试时长（分钟）',
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模板说明',
  `creator_id` bigint NOT NULL COMMENT '创建人ID（教师）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_template_subject` (`subject_id`),
  KEY `idx_template_creator` (`creator_id`),
  CONSTRAINT `fk_template_creator` FOREIGN KEY (`creator_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_template_subject` FOREIGN KEY (`subject_id`) REFERENCES `edu_subject` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='试卷模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_paper_template`
--

LOCK TABLES `exam_paper_template` WRITE;
/*!40000 ALTER TABLE `exam_paper_template` DISABLE KEYS */;
INSERT INTO `exam_paper_template` VALUES (6,'计算机组成原理期末（A卷）',7,100.00,60.00,120,NULL,13,'2026-04-01 18:17:32','2026-04-01 18:17:32'),(7,'Java语言程序设计期末（A卷）',5,100.00,60.00,120,'',14,'2026-04-07 14:52:48','2026-04-07 14:52:48');
/*!40000 ALTER TABLE `exam_paper_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_question`
--

DROP TABLE IF EXISTS `exam_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  `subject_id` bigint NOT NULL COMMENT '科目ID',
  `question_type` tinyint NOT NULL COMMENT '题型：1单选 2多选 3判断 4填空 5简答',
  `content` mediumtext COLLATE utf8mb4_general_ci NOT NULL COMMENT '题目内容（mediumtext 支持最长 16MB，足以容纳富文本题干/图片 base64）',
  `options` json DEFAULT NULL COMMENT '选项（JSON格式）',
  `answer` mediumtext COLLATE utf8mb4_general_ci NOT NULL COMMENT '正确答案（mediumtext，兼容简答题长答案）',
  `analysis` mediumtext COLLATE utf8mb4_general_ci COMMENT '答案解析（mediumtext，兼容长解析文本）',
  `score` decimal(5,2) DEFAULT '0.00' COMMENT '默认分值',
  `difficulty` tinyint DEFAULT '1' COMMENT '难度：1简单 2中等 3困难',
  `creator_id` bigint NOT NULL COMMENT '创建人ID（教师）',
  `deleted` tinyint DEFAULT '0' COMMENT '是否删除：0正常 1已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_question_subject` (`subject_id`),
  KEY `idx_question_type` (`question_type`),
  KEY `idx_question_difficulty` (`difficulty`),
  KEY `idx_question_creator` (`creator_id`),
  KEY `idx_question_deleted` (`deleted`),
  CONSTRAINT `fk_question_creator` FOREIGN KEY (`creator_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_question_subject` FOREIGN KEY (`subject_id`) REFERENCES `edu_subject` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_question_deleted` CHECK ((`deleted` in (0,1))),
  CONSTRAINT `chk_question_difficulty` CHECK ((`difficulty` in (1,2,3))),
  CONSTRAINT `chk_question_type` CHECK ((`question_type` in (1,2,3,4,5)))
) ENGINE=InnoDB AUTO_INCREMENT=4673 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='题目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_question`
--

LOCK TABLES `exam_question` WRITE;
/*!40000 ALTER TABLE `exam_question` DISABLE KEYS */;
INSERT INTO `exam_question` VALUES (4494,7,1,'计算机中最基本的存储单位是','[\"A.位(bit)\", \"B.字节(Byte)\", \"C.字(Word)\", \"D.千字节(KB)\"]','A','位(bit)是计算机中最小的数据单位，0或1',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4495,7,1,'冯·诺依曼计算机的核心思想是','[\"A.面向对象\", \"B.存储程序\", \"C.并行计算\", \"D.分布式计算\"]','B','冯·诺依曼体系的核心是存储程序的概念',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4496,7,1,'CPU中用于存储当前执行指令地址的寄存器是','[\"A.通用寄存器\", \"B.程序计数器(PC)\", \"C.指令寄存器(IR)\", \"D.地址寄存器(MAR)\"]','B','PC(程序计数器)保存下一条要执行的指令地址',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4497,7,1,'8位二进制补码能表示的数值范围是','[\"A.-127~127\", \"B.-128~127\", \"C.-128~128\", \"D.0~255\"]','B','8位补码范围：-2^7 ~ 2^7-1 即 -128~127',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4498,7,1,'Cache的作用是','[\"A.扩大内存容量\", \"B.提高CPU访问内存的速度\", \"C.代替主存储器\", \"D.永久存储数据\"]','B','Cache利用局部性原理，弥补CPU与主存之间的速度差距',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4499,7,2,'冯·诺依曼计算机由以下哪些部分组成','[\"A.运算器\", \"B.控制器\", \"C.存储器\", \"D.输入/输出设备\"]','A,B,C,D','冯·诺依曼计算机五大部件：运算器、控制器、存储器、输入设备、输出设备',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4500,7,2,'以下属于计算机总线的有','[\"A.数据总线\", \"B.地址总线\", \"C.控制总线\", \"D.网络总线\"]','A,B,C','计算机系统总线包括数据总线、地址总线和控制总线',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4501,7,3,'RAM是易失性存储器，断电后数据丢失',NULL,'1','RAM(随机存取存储器)是易失性的，断电数据消失；ROM是非易失性的',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4502,7,3,'指令周期等于时钟周期',NULL,'0','一个指令周期通常包含多个机器周期，每个机器周期包含多个时钟周期',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4503,7,3,'RISC指令集的特点是指令数量多、功能复杂',NULL,'0','RISC(精简指令集)特点是指令少、格式统一、执行速度快；CISC才是指令多而复杂',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4504,7,4,'1KB = ____字节',NULL,'1024','1KB = 2^10 = 1024 Bytes',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4505,7,4,'CPU主要由____和控制器两部分组成',NULL,'运算器','CPU = 运算器(ALU) + 控制器(CU)',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4506,7,5,'请说明计算机中原码、反码和补码的关系',NULL,'正数：原码=反码=补码。负数：反码是原码符号位不变其余位取反；补码是反码+1。使用补码的好处：统一了加减运算，0的表示唯一。例如-5：原码10000101，反码11111010，补码11111011。','需要分别说明正数和负数的三种编码规则及补码的优势',5.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4507,7,5,'请解释Cache的工作原理及局部性原理',NULL,'Cache是高速缓冲存储器，位于CPU和主存之间。工作原理：CPU先访问Cache，命中则直接读取（快），未命中则从主存取数据并复制到Cache。局部性原理：时间局部性（最近访问的数据可能再次被访问）和空间局部性（访问某地址后可能访问相邻地址）。','需要说明Cache的位置、工作流程和局部性原理的两个方面',5.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4508,7,1,'十进制数25转换为二进制是','[\"A.10011\", \"B.11001\", \"C.11010\", \"D.10101\"]','B','25=16+8+1=2^4+2^3+2^0=11001',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4509,7,1,'计算机中数据的最小存储单位是','[\"A.位\", \"B.字节\", \"C.字\", \"D.双字\"]','A','位(bit)是最小存储单位',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4510,7,1,'下列哪种存储器速度最快','[\"A.硬盘\", \"B.内存\", \"C.Cache\", \"D.寄存器\"]','D','寄存器速度最快但容量最小',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4511,7,1,'浮点数的表示范围主要取决于','[\"A.阶码位数\", \"B.尾数位数\", \"C.符号位\", \"D.基数\"]','A','阶码决定表示范围尾数决定精度',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4512,7,1,'CPU执行一条指令的过程不包括','[\"A.取指\", \"B.译码\", \"C.执行\", \"D.编译\"]','D','编译是高级语言翻译不是CPU执行步骤',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4513,7,1,'下列哪种总线用于连接CPU和主存','[\"A.数据总线\", \"B.地址总线\", \"C.控制总线\", \"D.系统总线\"]','D','系统总线连接CPU主存和I/O',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4514,7,1,'SRAM和DRAM相比SRAM的特点是','[\"A.需要刷新\", \"B.速度慢\", \"C.速度快不需刷新\", \"D.容量大\"]','C','SRAM速度快不需刷新用于Cache',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4515,7,1,'补码表示法中-128的8位补码是','[\"A.10000000\", \"B.11111111\", \"C.01111111\", \"D.00000000\"]','A','8位补码范围-128到127',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4516,7,1,'DMA方式传送数据时CPU','[\"A.完全不工作\", \"B.需要执行程序\", \"C.可以执行其他程序\", \"D.处于等待状态\"]','C','DMA不需CPU干预CPU可执行其他任务',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4517,7,1,'微程序控制器中微指令存储在','[\"A.主存\", \"B.Cache\", \"C.控制存储器\", \"D.硬盘\"]','C','微指令存储在控制存储器(CM)中',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4518,7,1,'虚拟存储器将主存和什么结合使用','[\"A.Cache\", \"B.寄存器\", \"C.辅存\", \"D.ROM\"]','C','虚拟存储器=主存+辅存',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4519,7,1,'计算机总线中地址总线的宽度决定了','[\"A.数据传输速率\", \"B.最大寻址空间\", \"C.指令类型数\", \"D.时钟频率\"]','B','地址总线宽度决定寻址范围',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4520,7,1,'中断处理过程中保护现场是保存','[\"A.中断向量\", \"B.程序状态字和寄存器内容\", \"C.中断屏蔽字\", \"D.设备状态\"]','B','保护现场即保存CPU寄存器和状态字',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4521,7,1,'IEEE754标准中32位浮点数的阶码占','[\"A.1位\", \"B.8位\", \"C.23位\", \"D.32位\"]','B','32位浮点数1位符号8位阶码23位尾数',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4522,7,1,'流水线技术主要用于提高','[\"A.单条指令执行速度\", \"B.指令吞吐率\", \"C.存储容量\", \"D.总线带宽\"]','B','流水线提高指令吞吐率而非单条速度',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4523,7,2,'冯诺依曼计算机的五大部件包括','[\"A.运算器\", \"B.控制器\", \"C.存储器\", \"D.输入设备和输出设备\"]','A,B,C,D','五大部件构成冯诺依曼体系结构',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4524,7,2,'CPU的主要组成部分包括','[\"A.运算器\", \"B.控制器\", \"C.寄存器组\", \"D.Cache\"]','A,B,C','Cache不是CPU的组成部分是独立的',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4525,7,2,'下列属于输入设备的有','[\"A.键盘\", \"B.鼠标\", \"C.扫描仪\", \"D.打印机\"]','A,B,C','打印机是输出设备',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4526,7,2,'关于Cache正确的有','[\"A.利用局部性原理\", \"B.速度介于CPU和主存之间\", \"C.对程序员透明\", \"D.容量比主存小\"]','A,B,C,D','都是Cache的特点',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4527,7,2,'计算机总线按功能分为','[\"A.数据总线\", \"B.地址总线\", \"C.控制总线\", \"D.I/O总线\"]','A,B,C','I/O总线按连接对象分不是按功能',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4528,7,2,'影响流水线性能的因素有','[\"A.数据冒险\", \"B.控制冒险\", \"C.结构冒险\", \"D.时钟频率\"]','A,B,C,D','都影响流水线性能',3.00,3,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4529,7,2,'关于中断正确的有','[\"A.是一种异步事件处理机制\", \"B.需要保护和恢复现场\", \"C.有优先级之分\", \"D.可以被屏蔽\"]','A,B,C,D','都是中断的特点',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4530,7,2,'主存的技术指标包括','[\"A.存储容量\", \"B.存取时间\", \"C.存储周期\", \"D.带宽\"]','A,B,C,D','都是主存的性能指标',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4531,7,2,'I/O设备与主机的连接方式有','[\"A.程序直接控制\", \"B.中断方式\", \"C.DMA方式\", \"D.通道方式\"]','A,B,C,D','都是I/O设备连接方式',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4532,7,2,'关于指令系统正确的有','[\"A.指令由操作码和操作数组成\", \"B.CISC指令数量多\", \"C.RISC指令数量少\", \"D.指令格式分固定和可变\"]','A,B,C,D','都是指令系统的知识点',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4533,7,2,'关于寻址方式正确的有','[\"A.立即寻址操作数在指令中\", \"B.直接寻址给出操作数地址\", \"C.间接寻址给出地址的地址\", \"D.寄存器寻址操作数在寄存器\"]','A,B,C,D','四种基本寻址方式',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4534,7,2,'关于补码运算正确的有','[\"A.可以用加法实现减法\", \"B.符号位参与运算\", \"C.溢出需要判断\", \"D.简化了硬件设计\"]','A,B,C,D','都是补码运算的特点',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4535,7,2,'存储器的层次结构从快到慢依次是','[\"A.寄存器\", \"B.Cache\", \"C.主存\", \"D.辅存\"]','A,B,C,D','速度递减容量递增',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4536,7,2,'关于虚拟存储器正确的有','[\"A.扩大了程序的地址空间\", \"B.利用了局部性原理\", \"C.需要页表进行地址转换\", \"D.可能产生缺页中断\"]','A,B,C,D','都是虚拟存储器的特点',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4537,7,2,'下列属于RISC特点的有','[\"A.指令数量少\", \"B.定长指令格式\", \"C.硬布线控制器\", \"D.Load/Store结构\"]','A,B,C,D','都是RISC的特点',3.00,3,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4538,7,2,'关于总线仲裁正确的有','[\"A.集中式仲裁有链式查询\", \"B.分布式仲裁无中央仲裁器\", \"C.解决多设备争用总线问题\", \"D.影响总线传输效率\"]','A,B,C,D','都是总线仲裁的特点',3.00,3,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4539,7,2,'关于磁盘存储器正确的有','[\"A.非易失性存储\", \"B.存取时间包括寻道时间\", \"C.旋转等待时间影响速度\", \"D.可以随机访问\"]','A,B,C,D','都是磁盘的特点',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4540,7,2,'计算机中的校验方法有','[\"A.奇偶校验\", \"B.海明校验\", \"C.CRC循环冗余校验\", \"D.模2运算\"]','A,B,C','模2运算是CRC的基础运算不是校验方法',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4541,7,3,'ROM中的内容断电后不会丢失',NULL,'1','ROM是非易失性存储器',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4542,7,3,'计算机的运算速度只取决于CPU主频',NULL,'0','还取决于指令集架构流水线等多因素',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4543,7,3,'补码的加法运算可能产生溢出',NULL,'1','两个同号数相加可能溢出',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4544,7,3,'DMA方式比中断方式数据传输效率高',NULL,'1','DMA不需CPU干预效率更高',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4545,7,3,'寄存器和主存都是易失性存储器',NULL,'1','都是RAM类型断电后数据丢失',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4546,7,3,'机器字长就是数据总线的宽度',NULL,'0','机器字长是CPU一次处理的数据位数不一定等于总线宽度',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4547,7,3,'流水线越多处理器性能一定越高',NULL,'0','流水线过深会带来更多冒险和开销',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4548,7,3,'Cache和主存之间的数据交换以块为单位',NULL,'1','Cache按块进行数据交换',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4549,7,3,'中断嵌套是指高优先级中断可以打断低优先级中断',NULL,'1','高优先级可以嵌套打断低优先级中断处理',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4550,7,3,'移码常用于表示浮点数的阶码',NULL,'1','移码便于比较浮点数大小',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4551,7,3,'通道是一种特殊的处理器专门负责I/O操作',NULL,'1','通道独立于CPU管理I/O传输',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4552,7,3,'程序计数器PC存放的是当前正在执行的指令',NULL,'0','PC存放的是下一条要执行的指令地址',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4553,7,3,'DRAM比SRAM便宜但速度慢',NULL,'1','DRAM密度高便宜但需刷新速度慢',2.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4554,7,3,'哈佛结构的特点是指令和数据分开存储',NULL,'1','哈佛结构指令和数据使用独立存储和总线',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4555,7,3,'相联存储器按内容访问',NULL,'1','相联存储器按内容检索不按地址',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4556,7,3,'变址寻址有利于处理数组问题',NULL,'1','变址寻址适合数组等数据结构的访问',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4557,7,3,'原码和反码的0有两种表示但补码只有一种',NULL,'1','补码中0的表示唯一',2.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4558,7,4,'计算机中1GB等于____MB',NULL,'1024','1GB=1024MB',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4559,7,4,'CPU中____寄存器用于存放下一条指令的地址',NULL,'程序计数器(PC)','PC指向下一条指令地址',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4560,7,4,'总线的带宽等于总线宽度乘以____',NULL,'总线频率','带宽=宽度×频率',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4561,7,4,'IEEE754标准中单精度浮点数的偏移量为____',NULL,'127','单精度偏移127双精度偏移1023',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4562,7,4,'Cache的命中率越高CPU访存的平均时间越____',NULL,'短','命中率高则更多从Cache取数据速度快',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4563,7,4,'中断响应的条件是CPU在____周期末检测到中断请求',NULL,'指令','CPU在指令执行结束时检测中断',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4564,7,4,'冯诺依曼计算机以____为中心',NULL,'运算器','冯诺依曼机以运算器为中心现代以存储器为中心',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4565,7,4,'原码表示法中最高位为____位',NULL,'符号','最高位0正1负',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4566,7,4,'CPU与I/O设备之间的接口称为____',NULL,'I/O接口(适配器)','I/O接口连接CPU和外设',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4567,7,4,'相对寻址以____的内容为基准地址',NULL,'程序计数器(PC)','相对寻址基于PC值加偏移',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4568,7,4,'磁盘的平均存取时间等于寻道时间加____时间加传输时间',NULL,'旋转等待','三部分组成存取时间',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4569,7,4,'流水线的吞吐率是指单位时间内完成的____数',NULL,'指令','吞吐率=完成指令数/时间',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4570,7,4,'Cache与主存的地址映射方式有直接映射、全相联映射和____映射',NULL,'组相联','三种映射方式',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4571,7,4,'CPI表示执行每条指令所需的平均____数',NULL,'时钟周期','CPI=Clock cycles Per Instruction',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4572,7,4,'计算机中定点小数的表示范围取决于____的位数',NULL,'尾数','尾数位数决定精度和范围',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4573,7,4,'存储器的____表示从启动一次存储器操作到完成该操作的时间',NULL,'存取时间','存取时间即访问延迟',3.00,1,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4574,7,4,'微指令的编码方式有直接编码和____编码',NULL,'字段','字段编码将微命令分组编码',3.00,3,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4575,7,4,'总线事务一般包括请求、仲裁、____和数据传输四个阶段',NULL,'寻址','总线事务四阶段',3.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4576,7,5,'请解释存储器的层次结构及局部性原理',NULL,'层次结构从高到低：寄存器-Cache-主存-辅存。速度递减容量递增价格递减。局部性原理：时间局部性最近访问的数据近期可能再次访问；空间局部性访问某地址后附近地址可能被访问。Cache利用局部性原理将常用数据放在快速存储器中提高整体访问速度。','需说明各层次特点和两种局部性',5.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4577,7,5,'请比较CISC和RISC指令集的特点',NULL,'CISC(复杂指令集)：指令数量多格式可变寻址方式多微程序控制适合复杂操作如x86。RISC(精简指令集)：指令数量少定长格式硬布线控制Load/Store结构流水线高效如ARM。RISC注重编译器优化CISC注重硬件实现。现代CPU常结合两者优势。','需从指令特点控制方式等角度比较',5.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4578,7,5,'请说明CPU中断处理的完整过程',NULL,'1.中断请求：外设通过中断请求线发出请求。2.中断判优：多个中断按优先级排序。3.中断响应：CPU在指令末尾检测到请求关中断保存断点。4.保护现场：保存寄存器和状态字入栈。5.中断服务：执行中断处理程序。6.恢复现场：出栈恢复寄存器。7.中断返回：开中断返回断点继续执行。','需说明完整的7个步骤',5.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4579,7,5,'请解释浮点数的表示方法及IEEE754标准',NULL,'浮点数=(-1)^S×M×2^E。S符号位M尾数E阶码。IEEE754单精度32位：1位符号+8位阶码+23位尾数，偏移127。双精度64位：1位符号+11位阶码+52位尾数，偏移1023。尾数隐含1即1.M形式。特殊值：阶码全0为0或非规格化数全1为无穷或NaN。','需说明格式和IEEE754标准细节',5.00,3,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4580,7,5,'请比较DMA方式和中断方式的异同',NULL,'相同：都是CPU与I/O设备交互方式都能提高效率。不同：中断方式每传一个字需CPU介入CPU执行中断服务程序；DMA方式批量传输不需CPU干预由DMA控制器直接管理内存和外设的数据传输。DMA效率更高适合高速大批量数据传输如磁盘。中断适合少量数据或异步事件处理。','需比较两种方式的原理和适用场景',5.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4581,7,5,'请说明流水线的概念及影响其性能的因素',NULL,'流水线将指令执行过程分为多个阶段(取指译码执行访存写回)各阶段并行处理不同指令。理想情况下每个时钟周期完成一条指令。影响因素：1.结构冒险资源冲突；2.数据冒险数据依赖；3.控制冒险分支指令。解决：前推技术旁路分支预测延迟槽等。流水线效率=理想时间/实际时间。','需说明概念三种冒险和解决方法',5.00,3,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4582,7,5,'请解释虚拟存储器的概念和实现方式',NULL,'虚拟存储器使程序的逻辑地址空间大于物理内存。实现：1.页式虚拟存储将地址空间分为固定大小的页通过页表映射到物理页框按需调入。2.段式虚拟存储按逻辑段划分。3.段页式结合两者。关键技术：页表TLB快表缺页中断处理页面置换算法(LRU FIFO等)。优点：扩大地址空间多道程序共享内存。','需说明概念实现方式和关键技术',5.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4583,7,5,'请说明计算机中原码反码补码的关系及补码的优点',NULL,'原码：最高位为符号位其余为真值。正数三码相同。负数：反码=原码符号位不变其余取反；补码=反码+1。补码优点：1.0的表示唯一(原码反码有+0和-0)；2.加减法统一用加法实现简化硬件；3.比原码多表示一个最小负数(-128)。补码运算：[X+Y]补=[X]补+[Y]补。','需说明三种码的转换关系和补码优点',5.00,2,13,0,'2026-04-15 15:59:03','2026-04-15 15:59:03'),(4584,5,1,'Java中，所有类的根类是','[\"A.Object\", \"B.Class\", \"C.String\", \"D.System\"]','A','java.lang.Object是所有Java类的基类',2.00,1,14,0,'2026-04-21 15:32:01','2026-04-21 15:32:01'),(4585,5,1,'以下哪个关键字用于实现接口','[\"A.extends\", \"B.implements\", \"C.abstract\", \"D.interface\"]','B','implements用于类实现接口',2.00,1,14,0,'2026-04-21 15:32:01','2026-04-21 15:32:01'),(4586,5,1,'Java中String类是','[\"A.可变的\", \"B.不可变的\", \"C.基本类型\", \"D.抽象类\"]','B','String是不可变类，一旦创建不可修改',2.00,1,14,0,'2026-04-21 15:32:01','2026-04-21 15:32:01'),(4587,5,1,'以下哪种集合是线程安全的','[\"A.ArrayList\", \"B.HashMap\", \"C.Vector\", \"D.LinkedList\"]','C','Vector是线程安全的，ArrayList不是',3.00,2,14,0,'2026-04-21 15:32:01','2026-04-21 15:32:01'),(4588,5,1,'Java异常处理中，finally块的作用是','[\"A.捕获异常\", \"B.抛出异常\", \"C.无论是否异常都执行\", \"D.定义异常类\"]','C','finally块中的代码无论是否发生异常都会执行',2.00,1,14,0,'2026-04-21 15:32:01','2026-04-21 15:32:01'),(4589,5,2,'Java中面向对象的三大特性包括','[\"A.封装\", \"B.继承\", \"C.多态\", \"D.抽象\"]','A,B,C','面向对象三大特性是封装、继承、多态。抽象是重要概念但不属于三大特性',3.00,2,14,0,'2026-04-21 15:32:01','2026-04-21 15:32:01'),(4590,5,2,'以下关于Java接口的说法正确的有','[\"A.接口中的方法默认是public abstract\", \"B.接口可以包含常量\", \"C.一个类可以实现多个接口\", \"D.接口可以实例化\"]','A,B,C','接口不能直接实例化',3.00,2,14,0,'2026-04-21 15:32:01','2026-04-21 15:32:01'),(4591,5,3,'Java支持多重继承',NULL,'0','Java不支持多重继承(一个类只能有一个父类)，但支持多接口实现',2.00,1,14,0,'2026-04-21 15:32:01','2026-04-21 15:32:01'),(4592,5,3,'抽象类可以有构造方法',NULL,'1','抽象类可以有构造方法，供子类调用',2.00,2,14,0,'2026-04-21 15:32:01','2026-04-21 15:32:01'),(4593,5,4,'Java中用于定义常量的关键字是____',NULL,'final','final修饰变量使其成为常量',2.00,1,14,0,'2026-04-21 15:32:01','2026-04-21 15:32:01'),(4594,5,4,'Java集合框架中，List接口的两个常用实现类是ArrayList和____',NULL,'LinkedList','ArrayList基于数组，LinkedList基于链表',2.00,1,14,0,'2026-04-21 15:32:01','2026-04-21 15:32:01'),(4595,5,5,'请解释Java中重载(Overload)和重写(Override)的区别',NULL,'重载：同一个类中方法名相同但参数列表不同（参数类型、个数或顺序不同），返回类型可以不同。重写：子类重新定义父类中已有的方法，方法名、参数列表、返回类型必须相同，访问权限不能更严格。','需要从定义位置、参数要求、返回类型等方面说明',5.00,2,14,0,'2026-04-21 15:32:01','2026-04-21 15:32:01'),(4596,5,5,'请说明ArrayList和LinkedList的区别及各自适用场景',NULL,'ArrayList基于动态数组，随机访问快O(1)，插入删除慢O(n)，适合频繁查询的场景。LinkedList基于双向链表，随机访问慢O(n)，插入删除快O(1)，适合频繁增删的场景。','从底层实现、性能特点和适用场景三个方面比较',5.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4597,5,1,'Java中的垃圾回收机制由谁负责','[\"A.程序员手动释放\", \"B.JVM自动管理\", \"C.操作系统管理\", \"D.编译器管理\"]','B','Java的垃圾回收(GC)由JVM自动管理',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4598,5,1,'下列哪项不是Java的基本数据类型','[\"A.int\", \"B.char\", \"C.String\", \"D.boolean\"]','C','String是引用类型不是基本类型',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4599,5,1,'Java中数组的长度通过什么获取','[\"A.array.size()\", \"B.array.length()\", \"C.array.length\", \"D.array.getLength()\"]','C','数组长度通过length属性获取',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4600,5,1,'下列哪个修饰符表示只能在本类中访问','[\"A.public\", \"B.protected\", \"C.default\", \"D.private\"]','D','private限制为本类访问',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4601,5,1,'Java中所有异常的父类是','[\"A.Error\", \"B.Exception\", \"C.Throwable\", \"D.RuntimeException\"]','C','Throwable是所有异常和错误的父类',2.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4602,5,1,'下列哪个集合是有序且不可重复的','[\"A.ArrayList\", \"B.HashSet\", \"C.TreeSet\", \"D.HashMap\"]','C','TreeSet有序且不重复',2.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4603,5,1,'Java中创建线程的方式不包括','[\"A.继承Thread类\", \"B.实现Runnable接口\", \"C.实现Callable接口\", \"D.继承Process类\"]','D','没有继承Process类创建线程的方式',2.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4604,5,1,'下列哪个关键字用于定义接口','[\"A.class\", \"B.abstract\", \"C.interface\", \"D.implements\"]','C','interface定义接口',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4605,5,1,'Java中String对象一旦创建就不能修改这体现了String的','[\"A.封装性\", \"B.不可变性\", \"C.多态性\", \"D.继承性\"]','B','String是不可变类',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4606,5,1,'下列哪个方法可以启动一个线程','[\"A.run()\", \"B.start()\", \"C.execute()\", \"D.init()\"]','B','start()启动线程会自动调用run()',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4607,5,1,'Java中的自动装箱是指','[\"A.基本类型转为包装类\", \"B.包装类转为基本类型\", \"C.类型强制转换\", \"D.自动类型推断\"]','A','自动装箱是基本类型自动转包装类',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4608,5,1,'下列哪个不是Java的访问修饰符','[\"A.public\", \"B.private\", \"C.protected\", \"D.static\"]','D','static是静态修饰符不是访问修饰符',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4609,5,1,'HashMap和Hashtable的主要区别是','[\"A.HashMap线程安全\", \"B.Hashtable允许null键\", \"C.HashMap允许null键Hashtable不允许\", \"D.两者完全相同\"]','C','HashMap允许null键值Hashtable不允许',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4610,5,1,'Java中finally块的执行时机是','[\"A.try块之前\", \"B.catch块之前\", \"C.无论是否异常都执行\", \"D.只在异常时执行\"]','C','finally块无论是否异常都会执行',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4611,5,1,'下列关于构造方法说法正确的是','[\"A.可以有返回值\", \"B.方法名与类名相同\", \"C.可以被static修饰\", \"D.可以被子类继承\"]','B','构造方法名必须与类名相同',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4612,5,2,'Java面向对象的特性包括','[\"A.封装\", \"B.继承\", \"C.多态\", \"D.抽象\"]','A,B,C,D','四大特性封装继承多态抽象',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4613,5,2,'下列属于Java集合框架的接口有','[\"A.List\", \"B.Set\", \"C.Map\", \"D.Array\"]','A,B,C','Array是数组不是集合接口',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4614,5,2,'关于Java异常处理正确的有','[\"A.try块后可跟多个catch\", \"B.finally块不是必须的\", \"C.catch可以捕获多种异常\", \"D.throw用于手动抛出异常\"]','A,B,C,D','都是正确的异常处理知识',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4615,5,2,'下列哪些是Java中的线程状态','[\"A.新建\", \"B.运行\", \"C.阻塞\", \"D.终止\"]','A,B,C,D','都是Java线程的生命周期状态',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4616,5,2,'关于Java中的static关键字正确的有','[\"A.可修饰变量\", \"B.可修饰方法\", \"C.可修饰类(内部类)\", \"D.静态方法可直接访问非静态变量\"]','A,B,C','静态方法不能直接访问非静态变量',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4617,5,2,'Java中实现多态的方式有','[\"A.方法重载\", \"B.方法重写\", \"C.接口实现\", \"D.抽象类继承\"]','A,B,C,D','都可以实现多态',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4618,5,2,'关于Java中final关键字正确的有','[\"A.修饰变量表示常量\", \"B.修饰方法不能被重写\", \"C.修饰类不能被继承\", \"D.修饰构造方法\"]','A,B,C','final不能修饰构造方法',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4619,5,2,'下列属于Java IO流的有','[\"A.FileInputStream\", \"B.BufferedReader\", \"C.Scanner\", \"D.PrintWriter\"]','A,B,C,D','都属于Java IO相关的类',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4620,5,2,'关于Java泛型正确的有','[\"A.提高类型安全\", \"B.减少类型转换\", \"C.编译时检查类型\", \"D.运行时保留类型信息\"]','A,B,C','Java泛型是类型擦除运行时不保留',3.00,3,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4621,5,2,'下列关于HashMap正确的有','[\"A.基于哈希表实现\", \"B.允许null键和值\", \"C.非线程安全\", \"D.JDK8后使用红黑树优化\"]','A,B,C,D','都是HashMap的特点',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4622,5,2,'Java中可以用来实现线程同步的有','[\"A.synchronized关键字\", \"B.ReentrantLock\", \"C.volatile关键字\", \"D.Semaphore\"]','A,B,C,D','都可用于线程同步或通信',3.00,3,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4623,5,2,'关于Java中抽象类正确的有','[\"A.不能被实例化\", \"B.可以有构造方法\", \"C.可以有非抽象方法\", \"D.子类必须实现所有抽象方法\"]','A,B,C','子类也可以是抽象类不必全实现',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4624,5,2,'下列属于设计模式的有','[\"A.单例模式\", \"B.工厂模式\", \"C.观察者模式\", \"D.冒泡排序\"]','A,B,C','冒泡排序是排序算法不是设计模式',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4625,5,2,'Java中String类的常用方法有','[\"A.length()\", \"B.charAt()\", \"C.substring()\", \"D.append()\"]','A,B,C','append()是StringBuilder的方法',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4626,5,2,'关于Java中的接口(JDK8+)正确的有','[\"A.可以有默认方法\", \"B.可以有静态方法\", \"C.方法默认public\", \"D.可以有构造方法\"]','A,B,C','接口不能有构造方法',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4627,5,2,'下列关于Java内存区域正确的有','[\"A.堆存放对象实例\", \"B.栈存放局部变量\", \"C.方法区存放类信息\", \"D.程序计数器记录当前指令地址\"]','A,B,C,D','都是JVM内存区域的特点',3.00,3,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4628,5,3,'Java是纯面向对象的语言没有基本数据类型',NULL,'0','Java有8种基本数据类型',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4629,5,3,'Java中子类可以继承父类的私有方法',NULL,'0','私有方法不能被继承',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4630,5,3,'接口中的变量默认是public static final的',NULL,'1','接口中变量默认是公开静态常量',2.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4631,5,3,'Java中的String类是可变的',NULL,'0','String是不可变类',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4632,5,3,'构造方法可以被继承',NULL,'0','构造方法不能被继承',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4633,5,3,'Java支持方法的重载和重写',NULL,'1','重载是编译时多态重写是运行时多态',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4634,5,3,'ArrayList是线程安全的',NULL,'0','ArrayList不是线程安全的Vector是',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4635,5,3,'Java中的异常分为检查型异常和非检查型异常',NULL,'1','检查型必须处理非检查型可以不处理',2.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4636,5,3,'static方法中可以直接使用this关键字',NULL,'0','静态方法中没有this引用',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4637,5,3,'Java中一个类可以实现多个接口',NULL,'1','Java通过接口实现多继承',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4638,5,3,'try块中有return语句finally块仍会执行',NULL,'1','finally块几乎总会执行',2.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4639,5,3,'Java中所有的类都直接继承自Object类',NULL,'0','直接或间接继承不一定是直接继承',2.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4640,5,3,'局部变量使用前必须初始化',NULL,'1','局部变量没有默认值必须初始化',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4641,5,3,'Java中的枚举类型本质上是类',NULL,'1','enum本质是继承Enum类的特殊类',2.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4642,5,3,'LinkedList比ArrayList的随机访问速度快',NULL,'0','ArrayList支持随机访问O(1)LinkedList需遍历O(n)',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4643,5,3,'Java中方法参数传递都是值传递',NULL,'1','Java只有值传递引用类型传的是引用的副本',2.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4644,5,3,'抽象类中必须有抽象方法',NULL,'0','抽象类可以没有抽象方法',2.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4645,5,4,'Java中用于实现接口的关键字是____',NULL,'implements','implements用于实现接口',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4646,5,4,'Java中字符串比较内容相等应使用____方法',NULL,'equals()','equals比较内容==比较引用',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4647,5,4,'Java中用于手动抛出异常的关键字是____',NULL,'throw','throw抛出异常throws声明异常',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4648,5,4,'Java中____关键字用于防止类被继承',NULL,'final','final修饰类不能被继承',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4649,5,4,'Java程序的入口方法签名是public static void ____(String[] args)',NULL,'main','main方法是程序入口',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4650,5,4,'Java中将基本类型转为包装类的过程叫____',NULL,'自动装箱','autoboxing',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4651,5,4,'Java中____接口的实现类可以用foreach循环遍历',NULL,'Iterable','实现Iterable接口可用增强for循环',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4652,5,4,'Java中线程池的核心类是____',NULL,'ThreadPoolExecutor','线程池核心实现类',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4653,5,4,'Java中____注解用于标记方法重写父类方法',NULL,'@Override','@Override检查是否正确重写',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4654,5,4,'Java中Lambda表达式用于简化____接口的实现',NULL,'函数式','只有一个抽象方法的函数式接口',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4655,5,4,'Java中____类是线程安全的字符串操作类',NULL,'StringBuffer','StringBuffer线程安全StringBuilder不安全',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4656,5,4,'Java中数组下标从____开始',NULL,'0','Java数组下标从0开始',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4657,5,4,'Java中用于声明抛出异常的关键字是____',NULL,'throws','throws在方法签名中声明异常',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4658,5,4,'Java中____关键字用于创建对象实例',NULL,'new','new关键字实例化对象',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4659,5,4,'Java中所有类的顶级父类是____',NULL,'Object','Object是所有类的根类',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4660,5,4,'Java中实现序列化需要实现____接口',NULL,'Serializable','Serializable标记接口',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4661,5,4,'Java8引入的新日期类位于____包下',NULL,'java.time','java.time包下的LocalDate等',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4662,5,4,'Java中用于读取字节流的基类是____',NULL,'InputStream','InputStream是字节输入流基类',3.00,1,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4663,5,5,'请解释Java中的多态性及其实现方式',NULL,'多态是同一操作作用于不同对象产生不同行为。实现方式：1.编译时多态(方法重载)同名方法不同参数；2.运行时多态(方法重写)子类重写父类方法通过父类引用调用子类实现。条件：继承关系、方法重写、父类引用指向子类对象。','需说明概念和两种实现方式',5.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4664,5,5,'请比较Java中抽象类和接口的区别',NULL,'抽象类：可有构造方法、成员变量、具体方法，单继承。接口：不能有构造方法(JDK8前)，变量默认public static final，方法默认public abstract(JDK8可有default方法)，多实现。选择：is-a关系用抽象类，has-a能力用接口。','从语法和设计角度比较',5.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4665,5,5,'请说明Java中的垃圾回收机制',NULL,'JVM自动管理内存回收不再使用的对象。判断：引用计数法(有循环引用问题)和可达性分析(GC Roots)。算法：标记-清除、复制算法、标记-整理、分代收集(新生代老年代)。程序员可调用System.gc()建议回收但不保证执行。','需说明判断标准和回收算法',5.00,3,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4666,5,5,'请解释Java中的集合框架体系结构',NULL,'顶层接口Collection和Map。Collection下有List(有序可重复:ArrayList,LinkedList)、Set(无序不重复:HashSet,TreeSet)、Queue(队列)。Map存储键值对(HashMap,TreeMap,LinkedHashMap)。选择：频繁查询用ArrayList，频繁插删用LinkedList，去重用Set，键值映射用Map。','需说明主要接口和实现类',5.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4667,5,5,'请说明Java中synchronized关键字的用法和原理',NULL,'synchronized实现线程同步。用法：1.修饰实例方法锁this对象；2.修饰静态方法锁Class对象；3.修饰代码块指定锁对象。原理：基于Monitor对象每个对象有监视器锁线程执行前需获取锁释放后其他线程才能获取。保证原子性和可见性。','需说明三种用法和底层原理',5.00,3,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4668,5,5,'请解释Java中IO流的分类',NULL,'按方向：输入流(InputStream/Reader)和输出流(OutputStream/Writer)。按类型：字节流(处理二进制)和字符流(处理文本)。按功能：节点流(直接连数据源如FileInputStream)和处理流(包装节点流如BufferedReader)。常用：文件操作FileXxx，缓冲BufferedXxx，转换InputStreamReader。','从方向类型功能三个维度分类',5.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4669,5,5,'请说明Java中HashMap的工作原理',NULL,'HashMap基于数组+链表+红黑树(JDK8)。put时：计算key的hash值确定数组下标，若为空直接插入，若有冲突则链表追加(链表长度>8转红黑树)。get时：计算hash找到桶位通过equals比较key。扩容：负载因子0.75时容量翻倍重新hash。时间复杂度平均O(1)。','需说明数据结构和put/get过程',5.00,3,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4670,5,5,'请比较Java中的checked异常和unchecked异常',NULL,'Checked异常(检查型)：编译器强制处理必须try-catch或throws如IOException,SQLException。Unchecked异常(非检查型)：RuntimeException及其子类编译器不强制处理如NullPointerException,ArrayIndexOutOfBoundsException。Error是严重错误程序无法处理如OutOfMemoryError。','需说明两类异常的区别和常见例子',5.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4671,5,2,'关于Java中的Stream API正确的有','[\"A.支持链式操作\", \"B.可以并行处理\", \"C.属于函数式编程\", \"D.只能用于集合\"]','A,B,C','Stream也可用于数组和IO',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02'),(4672,5,2,'关于Java中的注解正确的有','[\"A.@Override检查方法重写\", \"B.@Deprecated标记过时\", \"C.@SuppressWarnings抑制警告\", \"D.注解可以自定义\"]','A,B,C,D','都是Java注解的特点',3.00,2,14,0,'2026-04-21 15:32:02','2026-04-21 15:32:02');
/*!40000 ALTER TABLE `exam_question` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_record`
--

DROP TABLE IF EXISTS `exam_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `exam_id` bigint NOT NULL COMMENT '考试ID',
  `user_id` bigint NOT NULL COMMENT '学生ID',
  `paper_id` bigint NOT NULL COMMENT '试卷ID',
  `start_time` datetime DEFAULT NULL COMMENT '开始答题时间',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `total_score` decimal(5,2) DEFAULT NULL COMMENT '总得分',
  `objective_score` decimal(5,2) DEFAULT NULL COMMENT '客观题得分',
  `subjective_score` decimal(5,2) DEFAULT NULL COMMENT '主观题得分',
  `status` tinyint DEFAULT '0' COMMENT '状态：0未开始 1答题中 2已交卷 3已批改 4缺考',
  `switch_count` int DEFAULT '0' COMMENT '切屏次数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_user` (`exam_id`,`user_id`),
  KEY `idx_record_user_exam` (`user_id`,`exam_id`,`status`),
  KEY `fk_record_paper` (`paper_id`),
  CONSTRAINT `fk_record_exam` FOREIGN KEY (`exam_id`) REFERENCES `exam_exam` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_record_paper` FOREIGN KEY (`paper_id`) REFERENCES `exam_paper` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_record_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_record`
--

LOCK TABLES `exam_record` WRITE;
/*!40000 ALTER TABLE `exam_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `exam_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_template_rule`
--

DROP TABLE IF EXISTS `exam_template_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_template_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `question_type` tinyint NOT NULL COMMENT '题型：1单选 2多选 3判断 4填空 5简答',
  `question_count` int NOT NULL COMMENT '题目数量',
  `score_per_question` decimal(5,2) NOT NULL COMMENT '每题分值',
  `difficulty` tinyint DEFAULT NULL COMMENT '难度要求：1简单 2中等 3困难，NULL表示不限',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `fk_rule_template` (`template_id`),
  CONSTRAINT `fk_rule_template` FOREIGN KEY (`template_id`) REFERENCES `exam_paper_template` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_rule_difficulty` CHECK (((`difficulty` is null) or (`difficulty` in (1,2,3)))),
  CONSTRAINT `chk_rule_type` CHECK ((`question_type` in (1,2,3,4,5)))
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模板组卷规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_template_rule`
--

LOCK TABLES `exam_template_rule` WRITE;
/*!40000 ALTER TABLE `exam_template_rule` DISABLE KEYS */;
INSERT INTO `exam_template_rule` VALUES (29,6,1,6,5.00,NULL,1),(30,6,2,2,5.00,NULL,2),(31,6,3,2,5.00,NULL,3),(32,6,4,2,5.00,NULL,4),(33,6,5,2,20.00,NULL,5),(34,7,1,10,3.00,NULL,1),(35,7,4,10,2.00,NULL,2),(36,7,5,5,10.00,NULL,3);
/*!40000 ALTER TABLE `exam_template_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `subject_major`
--

DROP TABLE IF EXISTS `subject_major`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subject_major` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `subject_id` bigint NOT NULL COMMENT '科目ID',
  `major_id` bigint NOT NULL COMMENT '专业ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_major` (`subject_id`,`major_id`),
  KEY `fk_sm_major` (`major_id`),
  CONSTRAINT `fk_sm_major` FOREIGN KEY (`major_id`) REFERENCES `edu_major` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sm_subject` FOREIGN KEY (`subject_id`) REFERENCES `edu_subject` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='科目专业关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `subject_major`
--

LOCK TABLES `subject_major` WRITE;
/*!40000 ALTER TABLE `subject_major` DISABLE KEYS */;
INSERT INTO `subject_major` VALUES (1,1,1,'2026-04-01 18:17:32'),(2,1,2,'2026-04-01 18:17:32'),(3,2,1,'2026-04-01 18:17:32'),(4,3,1,'2026-04-01 18:17:32'),(5,4,1,'2026-04-01 18:17:32'),(6,5,1,'2026-04-01 18:17:32'),(7,6,2,'2026-04-01 18:17:32'),(8,7,1,'2026-04-01 18:17:32'),(9,9,4,'2026-04-01 18:17:32');
/*!40000 ALTER TABLE `subject_major` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notification`
--

DROP TABLE IF EXISTS `sys_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '接收者用户ID',
  `type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知类型: EXAM_PUBLISHED/EXAM_UPDATED/EXAM_CANCELLED/EXAM_CREATED/EXAM_SUBMITTED/EXAM_AUTO_SUBMITTED/EXAM_ABSENT/EXAM_END_SUMMARY/SCORE_PUBLISHED/SCORE_UPDATED/ACCOUNT_CREATED/USER_CREATED',
  `title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知标题',
  `content` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '通知内容详情',
  `biz_type` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联业务类型: exam/score/user',
  `biz_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读: 0未读 1已读',
  `priority` tinyint NOT NULL DEFAULT '2' COMMENT '优先级: 1=紧急 2=普通 3=次要',
  `payload` json DEFAULT NULL COMMENT '扩展载荷: {senderId,senderName,senderAvatar,actionUrl,extras}',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_noti_user_read` (`user_id`,`is_read`,`create_time` DESC),
  KEY `idx_noti_user_time` (`user_id`,`create_time` DESC),
  KEY `idx_noti_user_type_biz` (`user_id`,`type`,`biz_id`),
  KEY `idx_noti_read_time` (`is_read`,`create_time`),
  KEY `idx_noti_user_type_read_time` (`user_id`,`type`,`is_read`,`create_time` DESC),
  CONSTRAINT `fk_noti_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_noti_read` CHECK ((`is_read` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=312 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统通知表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notification`
--

LOCK TABLES `sys_notification` WRITE;
/*!40000 ALTER TABLE `sys_notification` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码：ADMIN/TEACHER/STUDENT',
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'管理员','ADMIN','系统管理员','2026-04-01 18:17:30'),(2,'教师','TEACHER','教师','2026-04-01 18:17:30'),(3,'学生','STUDENT','学生','2026-04-01 18:17:30');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `real_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '真实姓名',
  `avatar` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像',
  `email` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号',
  `gender` tinyint DEFAULT '0' COMMENT '性别：0未知 1男 2女',
  `role_id` bigint DEFAULT NULL COMMENT '角色ID',
  `class_id` bigint DEFAULT NULL COMMENT '班级ID（学生）',
  `status` tinyint DEFAULT '1' COMMENT '状态：0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `fk_user_role` (`role_id`),
  KEY `fk_user_class` (`class_id`),
  CONSTRAINT `fk_user_class` FOREIGN KEY (`class_id`) REFERENCES `edu_class` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE SET NULL,
  CONSTRAINT `chk_user_gender` CHECK ((`gender` in (0,1,2))),
  CONSTRAINT `chk_user_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'admin','$2a$10$Wc9zeUbSzaSrRCZjr6nCD.G3WaOfxwxKQUchjtOPoG83UvE4poPEq','系统管理员','/uploads/avatar/avatar_1_1772977536402.jpg',NULL,NULL,0,1,NULL,1,'2026-04-01 18:17:32','2026-04-01 18:17:32'),(8,'taozhan','$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW','陶展','/uploads/avatar/avatar_8_1772957830070.jpg','313141451@qq.com','123455525265262',1,3,1,1,'2026-04-01 18:17:32','2026-04-01 18:17:32'),(9,'zhouxiang','$2a$10$zhtrvuSHPjQ1nJ/2hH/yo.Bv6aRV0yTs/Xs8sFzxuWw0Rlj99tOam','周祥','/uploads/avatar/avatar_9_1776766158236.jpeg','qwrqr','14121251512',1,3,2,1,'2026-04-01 18:17:32','2026-04-01 18:17:32'),(10,'zhaoyu','$2a$10$9Q/HA4ADfU7YDn1ybP7N9ubGDHu.COmbSk7/fJYjtLUkXVj21Vcdq','赵宇',NULL,'13412514','4623764377',1,3,3,1,'2026-04-01 18:17:32','2026-04-01 18:17:32'),(11,'gaowei','$2a$10$zseirE7L3pLXMLUQhq26N.qYPMjUCHoCn30tNIyqPYV7KnJ5FlpSW','高巍',NULL,'gaowei@test.com','13800138000',1,3,4,1,'2026-04-01 18:17:32','2026-04-01 18:17:32'),(13,'lixinglaing','$2a$10$0QEM9fOkvcZihOyYBYK6WOGMZWTcxwor9DUAPx7XmLSWw4/nD83jy','李兴良','/uploads/avatar/avatar_13_1772977565870.png','','',1,2,NULL,1,'2026-04-01 18:17:32','2026-04-01 18:17:32'),(14,'luweizhong','$2a$10$x2BK4PHI8NjVlZIxXUK/auBGKYqlLLC8wSqtx2TZJyf9mxn3.GURW','陆卫忠','/uploads/avatar/avatar_14_1776413688621.png','431128479817@qq.com','1781498461',1,2,NULL,1,'2026-04-01 18:17:32','2026-04-01 18:17:32'),(28,'ruanjian01','$2a$10$3yw.IZuveF0unm8hzDgQiOKmmLpoLX5IJp2AGxCYybGULFHjDgSGG','ruanjian01',NULL,'','',1,3,3,1,'2026-04-01 18:17:32','2026-04-01 18:17:32'),(29,'ruanjian02','$2a$10$/ogXmzPAyVQwhesXeO/V0.u/LMIXk2cxzEkSYj0Q9eRr0/ztWKHK2','ruanjian02',NULL,'','',1,3,4,1,'2026-04-01 18:17:32','2026-04-01 18:17:32');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_class`
--

DROP TABLE IF EXISTS `teacher_class`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_class` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `class_id` bigint NOT NULL COMMENT '班级ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_class` (`teacher_id`,`class_id`),
  KEY `fk_tc_class` (`class_id`),
  CONSTRAINT `fk_tc_class` FOREIGN KEY (`class_id`) REFERENCES `edu_class` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tc_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师班级关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_class`
--

LOCK TABLES `teacher_class` WRITE;
/*!40000 ALTER TABLE `teacher_class` DISABLE KEYS */;
INSERT INTO `teacher_class` VALUES (1,13,3,'2026-04-01 18:17:32'),(2,13,4,'2026-04-01 18:17:32'),(3,14,1,'2026-04-01 18:17:32'),(4,14,2,'2026-04-01 18:17:32');
/*!40000 ALTER TABLE `teacher_class` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_subject`
--

DROP TABLE IF EXISTS `teacher_subject`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_subject` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `subject_id` bigint NOT NULL COMMENT '科目ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_subject` (`teacher_id`,`subject_id`),
  KEY `fk_ts_subject` (`subject_id`),
  CONSTRAINT `fk_ts_subject` FOREIGN KEY (`subject_id`) REFERENCES `edu_subject` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ts_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师科目关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_subject`
--

LOCK TABLES `teacher_subject` WRITE;
/*!40000 ALTER TABLE `teacher_subject` DISABLE KEYS */;
INSERT INTO `teacher_subject` VALUES (1,13,7,'2026-04-01 18:17:32'),(2,14,5,'2026-04-01 18:17:32');
/*!40000 ALTER TABLE `teacher_subject` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'online_exam_system'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-29 15:39:35
