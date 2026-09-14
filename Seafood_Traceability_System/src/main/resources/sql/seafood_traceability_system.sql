-- =====================================================================
-- 冷冻对虾全产业链溯源系统 —— 数据库脚本
-- 目标数据库：seafood_traceability_system（与 application.yaml 中的连接串一致）
--
-- 产业链：养殖企业（虾苗/成虾）→ 冷冻加工企业（清洗/分级/冷冻/包装）
--         → 批发商 → 零售商 → 消费者
--
-- 【设计原则：增量、非破坏、可重复执行】
--   1) 全程只操作 seafood_traceability_system 这一个库，不会新建/切换到其它库；
--   2) 基础表 admin / province / city / node_info 若已存在则原样保留：
--      不 DROP、不清空、不覆盖库中已有的记录，只补齐缺失的字典与演示节点；
--   3) 业务表 farm_batch / froz_batch / whol_batch / reta_batch / process_record
--      仅在缺失时创建（CREATE TABLE IF NOT EXISTS）；
--   4) 演示数据使用显式主键 + INSERT IGNORE + 种子标记写入，
--      重复执行不会产生重复行，也不会覆盖你手工改过的数据。
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 0. 使用既有数据库（不存在时才创建）
-- ---------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS seafood_traceability_system
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE seafood_traceability_system;

-- ---------------------------------------------------------------------
-- 1. 基础表（结构与原库保持一致；已存在则跳过）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `admin` (
  `admin_id` int NOT NULL AUTO_INCREMENT COMMENT '管理员编号',
  `admin_name` varchar(20) NOT NULL COMMENT '管理员登入名称',
  `password` varchar(20) NOT NULL COMMENT '管理员登入密码',
  PRIMARY KEY (`admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员表';

CREATE TABLE IF NOT EXISTS `province` (
  `prov_id` int NOT NULL AUTO_INCREMENT COMMENT '省行政区编号',
  `prov_name` varchar(10) NOT NULL COMMENT '省行政区名称',
  `remarks` varchar(200) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`prov_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='省行政区域信息表';

CREATE TABLE IF NOT EXISTS `city` (
  `city_id` int NOT NULL AUTO_INCREMENT COMMENT '市行政区编号',
  `city_name` varchar(10) NOT NULL COMMENT '市行政区名称',
  `prov_id` int NOT NULL COMMENT '所属省编号',
  `remarks` varchar(200) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`city_id`),
  KEY `fk_city_prov` (`prov_id`),
  CONSTRAINT `fk_city_prov` FOREIGN KEY (`prov_id`) REFERENCES `province` (`prov_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='市行政区域信息表';

CREATE TABLE IF NOT EXISTS `node_info` (
  `node_id` int NOT NULL AUTO_INCREMENT COMMENT '节点企业编号',
  `code` varchar(20) NOT NULL COMMENT '登录编码',
  `password` varchar(20) NOT NULL COMMENT '登录密码',
  `name` varchar(100) NOT NULL COMMENT '节点企业名称',
  `type` int NOT NULL COMMENT '节点类型：1-养殖企业，2-冷冻加工企业，3-批发商，4-零售商',
  `prov_id` int DEFAULT NULL COMMENT '所在省编号',
  `city_id` int DEFAULT NULL COMMENT '所在市编号',
  `address` varchar(200) DEFAULT NULL COMMENT '节点企业地址',
  `business_id` varchar(30) NOT NULL COMMENT '营业执照代码（所有节点必填）',
  `ep_id` varchar(30) DEFAULT NULL COMMENT '动物防疫条件合格证编号（养殖节点必填）',
  `eia_id` varchar(30) DEFAULT NULL COMMENT '环境影响评价资质证书编号（养殖、冷冻加工节点必填）',
  `cir_id` varchar(30) DEFAULT NULL COMMENT '食品流通许可证编号（批发商必填）',
  `fb_id` varchar(30) DEFAULT NULL COMMENT '食品经营许可证编号（批发商、零售商必填）',
  `corporation` varchar(10) DEFAULT NULL COMMENT '法定代表人',
  `telephone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `reg_date` date DEFAULT NULL COMMENT '注册日期',
  `remarks` varchar(200) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`node_id`),
  KEY `fk_node_info_prov` (`prov_id`),
  KEY `fk_node_info_city` (`city_id`),
  CONSTRAINT `fk_node_info_city` FOREIGN KEY (`city_id`) REFERENCES `city` (`city_id`),
  CONSTRAINT `fk_node_info_prov` FOREIGN KEY (`prov_id`) REFERENCES `province` (`prov_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='节点企业信息表';

-- ---------------------------------------------------------------------
-- 2. 业务表（本系统新增，缺失时创建）
--    状态含义：
--      养殖批号   1 待发布  2 已发布  3 已下架
--      其它批号   1 新建    2 待确认  3 已确认  4 已下架
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS farm_batch (
    farm_batch_id INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    node_id INT NOT NULL COMMENT '养殖企业节点ID',
    batch_no VARCHAR(30) NOT NULL COMMENT '产品批号',
    breed VARCHAR(30) DEFAULT NULL COMMENT '产品品种',
    breed_stage VARCHAR(20) DEFAULT NULL COMMENT '养殖阶段：虾苗/成虾',
    quarantine_no VARCHAR(50) DEFAULT NULL COMMENT '动物检验检疫合格证',
    inspector VARCHAR(20) DEFAULT NULL COMMENT '官方检疫员名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1待发布 2已发布 3已下架',
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (farm_batch_id),
    UNIQUE KEY uk_farm_batch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='养殖企业产品批号表';

CREATE TABLE IF NOT EXISTS froz_batch (
    froz_batch_id INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    node_id INT NOT NULL COMMENT '冷冻加工企业节点ID',
    batch_no VARCHAR(30) NOT NULL COMMENT '产品批号',
    prov_id INT DEFAULT NULL COMMENT '上游养殖企业所在省',
    city_id INT DEFAULT NULL COMMENT '上游养殖企业所在市',
    up_node_id INT DEFAULT NULL COMMENT '上游养殖企业ID',
    up_batch_no VARCHAR(30) DEFAULT NULL COMMENT '上游养殖企业产品批号',
    breed VARCHAR(30) DEFAULT NULL COMMENT '产品品种',
    quarantine_no VARCHAR(50) DEFAULT NULL COMMENT '产品检验检疫合格证',
    inspector VARCHAR(20) DEFAULT NULL COMMENT '官方检验员名称',
    product_type VARCHAR(30) DEFAULT NULL COMMENT '产品类型',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1新建 2待确认 3已确认 4已下架',
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (froz_batch_id),
    UNIQUE KEY uk_froz_batch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='冷冻加工企业产品批号表';

CREATE TABLE IF NOT EXISTS whol_batch (
    whol_batch_id INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    node_id INT NOT NULL COMMENT '批发商节点ID',
    batch_no VARCHAR(30) NOT NULL COMMENT '产品批号',
    prov_id INT DEFAULT NULL COMMENT '上游冷冻加工企业所在省',
    city_id INT DEFAULT NULL COMMENT '上游冷冻加工企业所在市',
    up_node_id INT DEFAULT NULL COMMENT '上游冷冻加工企业ID',
    up_batch_no VARCHAR(30) DEFAULT NULL COMMENT '上游产品批号',
    breed VARCHAR(30) DEFAULT NULL COMMENT '产品品种',
    product_type VARCHAR(30) DEFAULT NULL COMMENT '产品类型',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1新建 2待确认 3已确认 4已下架',
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (whol_batch_id),
    UNIQUE KEY uk_whol_batch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批发商产品批号表';

CREATE TABLE IF NOT EXISTS reta_batch (
    reta_batch_id INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    node_id INT NOT NULL COMMENT '零售商节点ID',
    batch_no VARCHAR(30) NOT NULL COMMENT '产品批号',
    prov_id INT DEFAULT NULL COMMENT '上游批发商所在省',
    city_id INT DEFAULT NULL COMMENT '上游批发商所在市',
    up_node_id INT DEFAULT NULL COMMENT '上游批发商ID',
    up_batch_no VARCHAR(30) DEFAULT NULL COMMENT '上游产品批号',
    breed VARCHAR(30) DEFAULT NULL COMMENT '产品品种',
    product_type VARCHAR(30) DEFAULT NULL COMMENT '产品类型',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1新建 2待确认 3已确认 4已下架',
    trace_code VARCHAR(50) DEFAULT NULL COMMENT '溯源标识码（已确认时生成）',
    trace_time DATETIME DEFAULT NULL COMMENT '溯源标识码生成时间',
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (reta_batch_id),
    UNIQUE KEY uk_reta_batch_no (batch_no),
    UNIQUE KEY uk_trace_code (trace_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='零售商产品批号表';

CREATE TABLE IF NOT EXISTS process_record (
    record_id INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    froz_batch_id INT NOT NULL COMMENT '冷冻加工批号ID',
    node_id INT NOT NULL COMMENT '加工企业节点ID',
    step VARCHAR(20) NOT NULL COMMENT '工序：清洗/分级/冷冻/包装',
    step_time DATETIME DEFAULT NULL COMMENT '工序时间',
    temperature VARCHAR(20) DEFAULT NULL COMMENT '工艺参数（温度等）',
    operator VARCHAR(20) DEFAULT NULL COMMENT '操作人',
    remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (record_id),
    KEY idx_froz_batch (froz_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='冷冻加工工序记录表';

-- 演示数据写入标记（保证脚本重复执行时不会重复写入演示数据）
CREATE TABLE IF NOT EXISTS sql_demo_seed (
    seed_key VARCHAR(50) NOT NULL COMMENT '演示数据批次标记',
    applied_time DATETIME DEFAULT NULL COMMENT '写入时间',
    PRIMARY KEY (seed_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演示数据写入标记表';

-- ---------------------------------------------------------------------
-- 3. 基础字典数据（主键存在即跳过，不会覆盖已有行）
-- ---------------------------------------------------------------------
-- 3.1 系统管理员：admin / 123456
INSERT IGNORE INTO admin (admin_id, admin_name, password) VALUES (1, 'admin', '123456');

-- 3.2 省行政区
INSERT IGNORE INTO province (prov_id, prov_name, remarks) VALUES
(1, '广东省', NULL),
(2, '福建省', NULL),
(3, '山东省', NULL),
(4, '海南省', NULL),
(5, '浙江省', NULL),
(6, '江苏省', NULL),
(7, '广西壮族自治区', NULL);

-- 3.3 市行政区
INSERT IGNORE INTO city (city_id, city_name, prov_id, remarks) VALUES
(101, '湛江市', 1, NULL), (102, '阳江市', 1, NULL), (103, '广州市', 1, NULL),
(104, '深圳市', 1, NULL), (105, '珠海市', 1, NULL), (106, '汕头市', 1, NULL),
(201, '福州市', 2, NULL), (202, '厦门市', 2, NULL), (203, '宁德市', 2, NULL), (204, '漳州市', 2, NULL),
(301, '青岛市', 3, NULL), (302, '烟台市', 3, NULL), (303, '威海市', 3, NULL), (304, '日照市', 3, NULL),
(401, '海口市', 4, NULL), (402, '三亚市', 4, NULL), (403, '儋州市', 4, NULL),
(501, '宁波市', 5, NULL), (502, '舟山市', 5, NULL), (503, '温州市', 5, NULL), (504, '台州市', 5, NULL),
(601, '连云港市', 6, NULL), (602, '南通市', 6, NULL), (603, '盐城市', 6, NULL),
(701, '北海市', 7, NULL), (702, '钦州市', 7, NULL), (703, '防城港市', 7, NULL);

-- ---------------------------------------------------------------------
-- 4. 演示节点企业
--    使用显式 node_id（101 起），已存在则跳过；
--    库中原有的 4 条记录（node_id 5~8）保持不动、不删除、不改名。
--    登录密码统一为 123456
-- ---------------------------------------------------------------------
INSERT IGNORE INTO node_info (node_id, code, password, name, type, prov_id, city_id, address, business_id, corporation, telephone, reg_date) VALUES
-- 广东链路的冷冻加工企业（与库中已有的 farm001 / whol001 / reta001 组成完整链路）
(101, 'froz001', '123456', '湛江国联对虾冷冻加工厂', 2, 1, 101, '广东省湛江市霞山区', 'BUS-FROZ001', '林建国', '13905000002', '2025-10-20'),
-- 福建
(111, 'farm101', '123456', '厦门翔安南美白对虾养殖场', 1, 2, 202, '福建省厦门市翔安区', 'BUS-FARM101', '陈海生', '13905000005', '2025-10-12'),
(112, 'froz101', '123456', '福州连江对虾冷冻加工厂', 2, 2, 201, '福建省福州市连江县', 'BUS-FROZ101', '林建国', '13905000006', '2025-10-26'),
(113, 'whol101', '123456', '厦门夏商对虾批发市场', 3, 2, 202, '福建省厦门市湖里区', 'BUS-WHOL101', '黄志明', '13905000007', '2025-11-08'),
(114, 'reta101', '123456', '厦门元初食品超市', 4, 2, 202, '福建省厦门市思明区', 'BUS-RETA101', '吴小丽', '13905000008', '2025-11-19'),
-- 山东
(121, 'farm102', '123456', '青岛即墨南美白对虾养殖基地', 1, 3, 301, '山东省青岛市即墨区', 'BUS-FARM102', '赵海军', '13905000009', '2025-12-03'),
(122, 'froz102', '123456', '烟台海和冷冻食品厂', 2, 3, 302, '山东省烟台市芝罘区', 'BUS-FROZ102', '孙立', '13905000010', '2025-12-15'),
(123, 'whol102', '123456', '青岛城阳水产批发市场', 3, 3, 301, '山东省青岛市城阳区', 'BUS-WHOL102', '周强', '13905000011', '2025-12-28'),
(124, 'reta102', '123456', '烟台振华量贩超市', 4, 3, 302, '山东省烟台市莱山区', 'BUS-RETA102', '刘芳', '13905000012', '2026-01-09'),
-- 海南
(131, 'farm103', '123456', '儋州金鲳对虾养殖场', 1, 4, 403, '海南省儋州市', 'BUS-FARM103', '何伟', '13905000013', '2026-01-21'),
(132, 'froz103', '123456', '海口椰岛对虾加工厂', 2, 4, 401, '海南省海口市美兰区', 'BUS-FROZ103', '郑海', '13905000014', '2026-02-02'),
(133, 'whol103', '123456', '海口南北水产批发市场', 3, 4, 401, '海南省海口市龙华区', 'BUS-WHOL103', '冯军', '13905000015', '2026-02-14'),
(134, 'reta103', '123456', '三亚旺豪超市', 4, 4, 402, '海南省三亚市吉阳区', 'BUS-RETA103', '黎娜', '13905000016', '2026-02-25'),
-- 浙江
(141, 'farm104', '123456', '舟山普陀对虾养殖合作社', 1, 5, 502, '浙江省舟山市普陀区', 'BUS-FARM104', '徐建平', '13905000017', '2026-03-08'),
(142, 'froz104', '123456', '宁波甬江对虾冷冻厂', 2, 5, 501, '浙江省宁波市江北区', 'BUS-FROZ104', '潘伟', '13905000018', '2026-03-19'),
(143, 'whol104', '123456', '温州菜篮子水产批发市场', 3, 5, 503, '浙江省温州市鹿城区', 'BUS-WHOL104', '蔡明', '13905000019', '2026-03-30'),
(144, 'reta104', '123456', '宁波三江购物超市', 4, 5, 501, '浙江省宁波市海曙区', 'BUS-RETA104', '叶芳', '13905000020', '2026-04-10'),
-- 江苏
(151, 'farm105', '123456', '南通如东对虾养殖场', 1, 6, 602, '江苏省南通市如东县', 'BUS-FARM105', '曹阳', '13905000021', '2026-04-21'),
(152, 'froz105', '123456', '盐城射阳对虾加工厂', 2, 6, 603, '江苏省盐城市射阳县', 'BUS-FROZ105', '谢军', '13905000022', '2026-05-02'),
(153, 'whol105', '123456', '连云港海州水产批发市场', 3, 6, 601, '江苏省连云港市海州区', 'BUS-WHOL105', '乔磊', '13905000023', '2026-05-13'),
(154, 'reta105', '123456', '南通大润发超市', 4, 6, 602, '江苏省南通市崇川区', 'BUS-RETA105', '韩雪', '13905000024', '2026-05-24'),
-- 广西
(161, 'farm106', '123456', '北海银海对虾养殖基地', 1, 7, 701, '广西壮族自治区北海市银海区', 'BUS-FARM106', '邓华', '13905000025', '2026-06-04'),
(162, 'froz106', '123456', '钦州保税港对虾加工厂', 2, 7, 702, '广西壮族自治区钦州市', 'BUS-FROZ106', '卢强', '13905000026', '2026-06-15'),
(163, 'whol106', '123456', '防城港水产批发市场', 3, 7, 703, '广西壮族自治区防城港市港口区', 'BUS-WHOL106', '廖明', '13905000027', '2026-06-26'),
(164, 'reta106', '123456', '北海万达超市', 4, 7, 701, '广西壮族自治区北海市海城区', 'BUS-RETA106', '秦岚', '13905000028', '2026-07-07'),
-- 广东（补充）
(171, 'farm107', '123456', '阳江闸坡对虾养殖场', 1, 1, 102, '广东省阳江市江城区', 'BUS-FARM107', '谭海', '13905000029', '2026-07-18'),
(172, 'froz107', '123456', '珠海斗门对虾冷冻厂', 2, 1, 105, '广东省珠海市斗门区', 'BUS-FROZ107', '温伟', '13905000030', '2026-07-29'),
(173, 'whol107', '123456', '汕头对虾批发中心', 3, 1, 106, '广东省汕头市金平区', 'BUS-WHOL107', '马强', '13905000031', '2026-08-09'),
(174, 'reta107', '123456', '广州永辉超市', 4, 1, 103, '广东省广州市天河区', 'BUS-RETA107', '曾丽', '13905000032', '2026-08-20');

-- ---------------------------------------------------------------------
-- 5. 为库中"已存在但省/市为空"的节点补齐区域信息
--    仅在字段为空时写入，绝不覆盖已有值，也不修改企业名称与编码
-- ---------------------------------------------------------------------
UPDATE node_info SET prov_id = 1, city_id = 101 WHERE code = 'farm001' AND (prov_id IS NULL OR city_id IS NULL);
UPDATE node_info SET prov_id = 1, city_id = 102 WHERE code = 'slau001' AND (prov_id IS NULL OR city_id IS NULL);
UPDATE node_info SET prov_id = 1, city_id = 103 WHERE code = 'whol001' AND (prov_id IS NULL OR city_id IS NULL);
UPDATE node_info SET prov_id = 1, city_id = 104 WHERE code = 'reta001' AND (prov_id IS NULL OR city_id IS NULL);

-- 可选：如需把节点类型注释从"屠宰企业"改为"冷冻加工企业"，可自行执行下面这句
-- ALTER TABLE node_info MODIFY COLUMN `type` int NOT NULL COMMENT '节点类型：1-养殖企业，2-冷冻加工企业，3-批发商，4-零售商';

-- ---------------------------------------------------------------------
-- 6. 演示业务数据（仅首次执行时写入；重复执行不会重复插入）
-- ---------------------------------------------------------------------
SET @seed := (SELECT COUNT(*) FROM sql_demo_seed WHERE seed_key = 'seafood_demo_v1');

-- 6.1 养殖企业产品批号
INSERT IGNORE INTO farm_batch (node_id, batch_no, breed, breed_stage, quarantine_no, inspector, status, create_time, update_time)
SELECT node_id, 'FARM20260101', '南美白对虾', '成虾', '粤动检（2026）第0101号', '李明', 2, '2026-01-05 09:20:00', '2026-01-06 10:00:00' FROM node_info WHERE code='farm001' AND @seed = 0;
INSERT IGNORE INTO farm_batch (node_id, batch_no, breed, breed_stage, quarantine_no, inspector, status, create_time, update_time)
SELECT node_id, 'FARM20260102', '斑节对虾', '虾苗', '粤动检（2026）第0102号', '李明', 1, '2026-01-07 08:30:00', '2026-01-07 08:30:00' FROM node_info WHERE code='farm001' AND @seed = 0;
INSERT IGNORE INTO farm_batch (node_id, batch_no, breed, breed_stage, quarantine_no, inspector, status, create_time, update_time)
SELECT node_id, 'FARM20251201', '中国对虾', '成虾', '粤动检（2025）第1201号', '李明', 3, '2025-12-18 14:00:00', '2025-12-25 09:00:00' FROM node_info WHERE code='farm001' AND @seed = 0;
INSERT IGNORE INTO farm_batch (node_id, batch_no, breed, breed_stage, quarantine_no, inspector, status, create_time, update_time)
SELECT node_id, 'FARM20260201', '南美白对虾', '成虾', '闽动检（2026）第0201号', '张伟', 2, '2026-02-06 10:10:00', '2026-02-07 11:00:00' FROM node_info WHERE code='farm101' AND @seed = 0;
INSERT IGNORE INTO farm_batch (node_id, batch_no, breed, breed_stage, quarantine_no, inspector, status, create_time, update_time)
SELECT node_id, 'FARM20260202', '日本对虾', '虾苗', '闽动检（2026）第0202号', '张伟', 1, '2026-02-08 09:00:00', '2026-02-08 09:00:00' FROM node_info WHERE code='farm101' AND @seed = 0;
INSERT IGNORE INTO farm_batch (node_id, batch_no, breed, breed_stage, quarantine_no, inspector, status, create_time, update_time)
SELECT node_id, 'FARM20260301', '南美白对虾', '成虾', '鲁动检（2026）第0301号', '王强', 2, '2026-03-10 08:40:00', '2026-03-11 15:20:00' FROM node_info WHERE code='farm102' AND @seed = 0;
INSERT IGNORE INTO farm_batch (node_id, batch_no, breed, breed_stage, quarantine_no, inspector, status, create_time, update_time)
SELECT node_id, 'FARM20260302', '斑节对虾', '成虾', '粤动检（2026）第0302号', '李明', 2, '2026-03-15 09:30:00', '2026-03-16 10:00:00' FROM node_info WHERE code='farm107' AND @seed = 0;
INSERT IGNORE INTO farm_batch (node_id, batch_no, breed, breed_stage, quarantine_no, inspector, status, create_time, update_time)
SELECT node_id, 'FARM20260401', '南美白对虾', '虾苗', '琼动检（2026）第0401号', '周敏', 1, '2026-04-02 11:00:00', '2026-04-02 11:00:00' FROM node_info WHERE code='farm103' AND @seed = 0;
INSERT IGNORE INTO farm_batch (node_id, batch_no, breed, breed_stage, quarantine_no, inspector, status, create_time, update_time)
SELECT node_id, 'FARM20260501', '中国对虾', '成虾', '浙动检（2026）第0501号', '陈刚', 2, '2026-05-06 09:10:00', '2026-05-08 16:00:00' FROM node_info WHERE code='farm104' AND @seed = 0;
INSERT IGNORE INTO farm_batch (node_id, batch_no, breed, breed_stage, quarantine_no, inspector, status, create_time, update_time)
SELECT node_id, 'FARM20260601', '南美白对虾', '成虾', '苏动检（2026）第0601号', '刘洋', 2, '2026-06-08 08:20:00', '2026-06-09 10:30:00' FROM node_info WHERE code='farm105' AND @seed = 0;
INSERT IGNORE INTO farm_batch (node_id, batch_no, breed, breed_stage, quarantine_no, inspector, status, create_time, update_time)
SELECT node_id, 'FARM20260701', '斑节对虾', '成虾', '桂动检（2026）第0701号', '韦东', 2, '2026-07-09 09:50:00', '2026-07-10 14:10:00' FROM node_info WHERE code='farm106' AND @seed = 0;
INSERT IGNORE INTO farm_batch (node_id, batch_no, breed, breed_stage, quarantine_no, inspector, status, create_time, update_time)
SELECT node_id, 'FARM20260303', '日本对虾', '成虾', '粤动检（2026）第0303号', '李明', 3, '2026-03-20 10:00:00', '2026-03-28 17:00:00' FROM node_info WHERE code='farm107' AND @seed = 0;

-- 6.2 冷冻加工企业产品批号
INSERT IGNORE INTO froz_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, quarantine_no, inspector, product_type, status, create_time, update_time)
SELECT n.node_id, 'FROZ20260101', u.prov_id, u.city_id, u.node_id, 'FARM20260101', '南美白对虾', '粤食检（2026）第0101号', '王强', '冷冻整虾', 3, '2026-01-08 08:30:00', '2026-01-10 16:00:00'
FROM node_info n, node_info u WHERE n.code='froz001' AND u.code='farm001' AND @seed = 0;
INSERT IGNORE INTO froz_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, quarantine_no, inspector, product_type, status, create_time, update_time)
SELECT n.node_id, 'FROZ20260102', u.prov_id, u.city_id, u.node_id, 'FARM20260102', '斑节对虾', '粤食检（2026）第0102号', '王强', '冷冻虾仁', 1, '2026-01-12 09:00:00', '2026-01-12 09:00:00'
FROM node_info n, node_info u WHERE n.code='froz001' AND u.code='farm001' AND @seed = 0;
INSERT IGNORE INTO froz_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, quarantine_no, inspector, product_type, status, create_time, update_time)
SELECT n.node_id, 'FROZ20251201', u.prov_id, u.city_id, u.node_id, 'FARM20251201', '中国对虾', '粤食检（2025）第1201号', '王强', '冷冻整虾', 4, '2025-12-26 10:00:00', '2026-01-05 09:00:00'
FROM node_info n, node_info u WHERE n.code='froz001' AND u.code='farm001' AND @seed = 0;
INSERT IGNORE INTO froz_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, quarantine_no, inspector, product_type, status, create_time, update_time)
SELECT n.node_id, 'FROZ20260201', u.prov_id, u.city_id, u.node_id, 'FARM20260201', '南美白对虾', '闽食检（2026）第0201号', '张伟', '冷冻虾仁', 3, '2026-02-09 08:30:00', '2026-02-12 15:30:00'
FROM node_info n, node_info u WHERE n.code='froz101' AND u.code='farm101' AND @seed = 0;
INSERT IGNORE INTO froz_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, quarantine_no, inspector, product_type, status, create_time, update_time)
SELECT n.node_id, 'FROZ20260202', u.prov_id, u.city_id, u.node_id, 'FARM20260202', '日本对虾', '闽食检（2026）第0202号', '张伟', '虾滑', 2, '2026-02-13 09:40:00', '2026-02-14 10:00:00'
FROM node_info n, node_info u WHERE n.code='froz101' AND u.code='farm101' AND @seed = 0;
INSERT IGNORE INTO froz_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, quarantine_no, inspector, product_type, status, create_time, update_time)
SELECT n.node_id, 'FROZ20260301', u.prov_id, u.city_id, u.node_id, 'FARM20260301', '南美白对虾', '鲁食检（2026）第0301号', '赵磊', '冷冻整虾', 3, '2026-03-12 08:20:00', '2026-03-15 16:20:00'
FROM node_info n, node_info u WHERE n.code='froz102' AND u.code='farm102' AND @seed = 0;
INSERT IGNORE INTO froz_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, quarantine_no, inspector, product_type, status, create_time, update_time)
SELECT n.node_id, 'FROZ20260302', u.prov_id, u.city_id, u.node_id, 'FARM20260302', '斑节对虾', '粤食检（2026）第0302号', '王强', '冷冻虾仁', 2, '2026-03-17 09:00:00', '2026-03-18 09:30:00'
FROM node_info n, node_info u WHERE n.code='froz107' AND u.code='farm107' AND @seed = 0;
INSERT IGNORE INTO froz_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, quarantine_no, inspector, product_type, status, create_time, update_time)
SELECT n.node_id, 'FROZ20260501', u.prov_id, u.city_id, u.node_id, 'FARM20260501', '中国对虾', '浙食检（2026）第0501号', '陈刚', '虾滑', 3, '2026-05-09 08:30:00', '2026-05-12 14:40:00'
FROM node_info n, node_info u WHERE n.code='froz104' AND u.code='farm104' AND @seed = 0;
INSERT IGNORE INTO froz_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, quarantine_no, inspector, product_type, status, create_time, update_time)
SELECT n.node_id, 'FROZ20260601', u.prov_id, u.city_id, u.node_id, 'FARM20260601', '南美白对虾', '苏食检（2026）第0601号', '刘洋', '冷冻整虾', 1, '2026-06-10 09:20:00', '2026-06-10 09:20:00'
FROM node_info n, node_info u WHERE n.code='froz105' AND u.code='farm105' AND @seed = 0;
INSERT IGNORE INTO froz_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, quarantine_no, inspector, product_type, status, create_time, update_time)
SELECT n.node_id, 'FROZ20260701', u.prov_id, u.city_id, u.node_id, 'FARM20260701', '斑节对虾', '桂食检（2026）第0701号', '韦东', '冷冻整虾', 3, '2026-07-11 08:10:00', '2026-07-14 15:10:00'
FROM node_info n, node_info u WHERE n.code='froz106' AND u.code='farm106' AND @seed = 0;

-- 6.3 批发商产品批号
INSERT IGNORE INTO whol_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type, status, create_time, update_time)
SELECT n.node_id, 'WHOL20260101', u.prov_id, u.city_id, u.node_id, 'FROZ20260101', '南美白对虾', '冷冻整虾', 3, '2026-01-11 09:00:00', '2026-01-13 10:30:00'
FROM node_info n, node_info u WHERE n.code='whol001' AND u.code='froz001' AND @seed = 0;
INSERT IGNORE INTO whol_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type, status, create_time, update_time)
SELECT n.node_id, 'WHOL20260102', u.prov_id, u.city_id, u.node_id, 'FROZ20260102', '斑节对虾', '冷冻虾仁', 1, '2026-01-14 09:00:00', '2026-01-14 09:00:00'
FROM node_info n, node_info u WHERE n.code='whol001' AND u.code='froz001' AND @seed = 0;
INSERT IGNORE INTO whol_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type, status, create_time, update_time)
SELECT n.node_id, 'WHOL20260201', u.prov_id, u.city_id, u.node_id, 'FROZ20260201', '南美白对虾', '冷冻虾仁', 3, '2026-02-13 09:30:00', '2026-02-15 11:00:00'
FROM node_info n, node_info u WHERE n.code='whol101' AND u.code='froz101' AND @seed = 0;
INSERT IGNORE INTO whol_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type, status, create_time, update_time)
SELECT n.node_id, 'WHOL20260202', u.prov_id, u.city_id, u.node_id, 'FROZ20260202', '日本对虾', '虾滑', 2, '2026-02-16 10:00:00', '2026-02-17 10:00:00'
FROM node_info n, node_info u WHERE n.code='whol101' AND u.code='froz101' AND @seed = 0;
INSERT IGNORE INTO whol_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type, status, create_time, update_time)
SELECT n.node_id, 'WHOL20260301', u.prov_id, u.city_id, u.node_id, 'FROZ20260301', '南美白对虾', '冷冻整虾', 3, '2026-03-16 09:00:00', '2026-03-18 10:00:00'
FROM node_info n, node_info u WHERE n.code='whol102' AND u.code='froz102' AND @seed = 0;
INSERT IGNORE INTO whol_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type, status, create_time, update_time)
SELECT n.node_id, 'WHOL20260501', u.prov_id, u.city_id, u.node_id, 'FROZ20260501', '中国对虾', '虾滑', 3, '2026-05-13 09:00:00', '2026-05-15 10:00:00'
FROM node_info n, node_info u WHERE n.code='whol104' AND u.code='froz104' AND @seed = 0;
INSERT IGNORE INTO whol_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type, status, create_time, update_time)
SELECT n.node_id, 'WHOL20260701', u.prov_id, u.city_id, u.node_id, 'FROZ20260701', '斑节对虾', '冷冻整虾', 3, '2026-07-15 09:00:00', '2026-07-17 10:30:00'
FROM node_info n, node_info u WHERE n.code='whol106' AND u.code='froz106' AND @seed = 0;

-- 6.4 零售商产品批号（已确认的批号带溯源标识码）
INSERT IGNORE INTO reta_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type, status, trace_code, trace_time, create_time, update_time)
SELECT n.node_id, 'RETA20260101', u.prov_id, u.city_id, u.node_id, 'WHOL20260101', '南美白对虾', '冷冻整虾', 3, 'SHZ202601010001', '2026-01-14 10:00:00', '2026-01-13 11:00:00', '2026-01-14 10:00:00'
FROM node_info n, node_info u WHERE n.code='reta001' AND u.code='whol001' AND @seed = 0;
INSERT IGNORE INTO reta_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type, status, trace_code, trace_time, create_time, update_time)
SELECT n.node_id, 'RETA20260102', u.prov_id, u.city_id, u.node_id, 'WHOL20260102', '斑节对虾', '冷冻虾仁', 1, NULL, NULL, '2026-01-15 09:00:00', '2026-01-15 09:00:00'
FROM node_info n, node_info u WHERE n.code='reta001' AND u.code='whol001' AND @seed = 0;
INSERT IGNORE INTO reta_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type, status, trace_code, trace_time, create_time, update_time)
SELECT n.node_id, 'RETA20260201', u.prov_id, u.city_id, u.node_id, 'WHOL20260201', '南美白对虾', '冷冻虾仁', 3, 'SHZ202602010002', '2026-02-16 10:00:00', '2026-02-15 11:30:00', '2026-02-16 10:00:00'
FROM node_info n, node_info u WHERE n.code='reta101' AND u.code='whol101' AND @seed = 0;
INSERT IGNORE INTO reta_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type, status, trace_code, trace_time, create_time, update_time)
SELECT n.node_id, 'RETA20260202', u.prov_id, u.city_id, u.node_id, 'WHOL20260202', '日本对虾', '虾滑', 2, NULL, NULL, '2026-02-18 09:00:00', '2026-02-19 09:00:00'
FROM node_info n, node_info u WHERE n.code='reta101' AND u.code='whol101' AND @seed = 0;
INSERT IGNORE INTO reta_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type, status, trace_code, trace_time, create_time, update_time)
SELECT n.node_id, 'RETA20260301', u.prov_id, u.city_id, u.node_id, 'WHOL20260301', '南美白对虾', '冷冻整虾', 3, 'SHZ202603010003', '2026-03-19 10:00:00', '2026-03-18 11:00:00', '2026-03-19 10:00:00'
FROM node_info n, node_info u WHERE n.code='reta102' AND u.code='whol102' AND @seed = 0;
INSERT IGNORE INTO reta_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type, status, trace_code, trace_time, create_time, update_time)
SELECT n.node_id, 'RETA20260501', u.prov_id, u.city_id, u.node_id, 'WHOL20260501', '中国对虾', '虾滑', 3, 'SHZ202605010004', '2026-05-16 10:00:00', '2026-05-15 11:00:00', '2026-05-16 10:00:00'
FROM node_info n, node_info u WHERE n.code='reta104' AND u.code='whol104' AND @seed = 0;
INSERT IGNORE INTO reta_batch (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type, status, trace_code, trace_time, create_time, update_time)
SELECT n.node_id, 'RETA20260701', u.prov_id, u.city_id, u.node_id, 'WHOL20260701', '斑节对虾', '冷冻整虾', 3, 'SHZ202607010005', '2026-07-18 10:00:00', '2026-07-17 11:30:00', '2026-07-18 10:00:00'
FROM node_info n, node_info u WHERE n.code='reta106' AND u.code='whol106' AND @seed = 0;

-- 6.5 冷冻加工工序记录（清洗 / 分级 / 冷冻 / 包装）
INSERT INTO process_record (froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT b.froz_batch_id, b.node_id, '清洗', '2026-01-08 09:00:00', '常温清水', '李工', '冲洗去除泥沙与杂质' FROM froz_batch b WHERE b.batch_no='FROZ20260101' AND @seed = 0;
INSERT INTO process_record (froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT b.froz_batch_id, b.node_id, '分级', '2026-01-08 11:00:00', '30-40只/斤', '王工', '按规格分级筛选' FROM froz_batch b WHERE b.batch_no='FROZ20260101' AND @seed = 0;
INSERT INTO process_record (froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT b.froz_batch_id, b.node_id, '冷冻', '2026-01-08 14:00:00', '-35℃', '张工', '速冻30分钟后转-18℃冷藏' FROM froz_batch b WHERE b.batch_no='FROZ20260101' AND @seed = 0;
INSERT INTO process_record (froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT b.froz_batch_id, b.node_id, '包装', '2026-01-08 16:00:00', '-18℃', '赵工', '真空包装 5kg/箱' FROM froz_batch b WHERE b.batch_no='FROZ20260101' AND @seed = 0;
INSERT INTO process_record (froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT b.froz_batch_id, b.node_id, '清洗', '2026-02-09 09:00:00', '常温清水', '李工', '冲洗去除泥沙与杂质' FROM froz_batch b WHERE b.batch_no='FROZ20260201' AND @seed = 0;
INSERT INTO process_record (froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT b.froz_batch_id, b.node_id, '分级', '2026-02-09 10:30:00', '40-50只/斤', '王工', '按规格分级筛选' FROM froz_batch b WHERE b.batch_no='FROZ20260201' AND @seed = 0;
INSERT INTO process_record (froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT b.froz_batch_id, b.node_id, '冷冻', '2026-02-09 13:00:00', '-35℃', '张工', '速冻30分钟后转-18℃冷藏' FROM froz_batch b WHERE b.batch_no='FROZ20260201' AND @seed = 0;
INSERT INTO process_record (froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT b.froz_batch_id, b.node_id, '包装', '2026-02-09 15:30:00', '-18℃', '赵工', '真空包装 1kg/袋' FROM froz_batch b WHERE b.batch_no='FROZ20260201' AND @seed = 0;

-- 6.6 记录本轮演示数据已写入，后续重复执行脚本时自动跳过
INSERT IGNORE INTO sql_demo_seed (seed_key, applied_time) VALUES ('seafood_demo_v1', NOW());

-- ---------------------------------------------------------------------
-- 7. 执行结果
-- ---------------------------------------------------------------------
SELECT '数据库脚本执行完成' AS message,
       (SELECT COUNT(*) FROM node_info)                    AS node_count,
       (SELECT COUNT(*) FROM farm_batch)                   AS farm_batch_count,
       (SELECT COUNT(*) FROM froz_batch)                   AS froz_batch_count,
       (SELECT COUNT(*) FROM whol_batch)                   AS whol_batch_count,
       (SELECT COUNT(*) FROM reta_batch)                   AS reta_batch_count,
       (SELECT COUNT(*) FROM process_record)               AS process_record_count,
       (SELECT COUNT(*) FROM reta_batch WHERE trace_code IS NOT NULL) AS trace_code_count;
