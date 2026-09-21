-- =====================================================================
-- 冷冻对虾全产业链溯源系统 —— 以产品为中心的溯源改造 v3
-- 目标数据库：seafood_traceability_system
--
-- 【设计原则：增量、非破坏、可重复执行】
--   1) 只操作 seafood_traceability_system 这一个库；
--   2) 所有加列先查 information_schema，已存在则跳过（MySQL 8 的 ADD COLUMN
--      不支持 IF NOT EXISTS，重复执行会直接报错，必须用存储过程守卫）；
--   3) 所有数据回填都带 WHERE ... IS NULL，绝不覆盖人工改过的值。
--
-- 【本次改造解决什么】
--   现状：消费者查到的其实是一条"零售批号"，页面展示的是四家企业 —— 是"企业链"
--         而不是"产品"。且产品形态（鲜/冻）、规格等级、环节合格状态、检测记录
--         在库里一个字段都没有（规格目前是当自由文本塞在 process_record.temperature）。
--   改后：把"商品身份"从"企业内部批号"里剥出来 ——
--         新增 product_code（对外产品编号，加工环节定型时确定性生成，下游继承），
--         与既有的 batch_no（企业内部批号，链路上溯串联）、
--         trace_code（溯源码，随机生成、承担防伪）三者分工明确。
--
-- 【三个编号的分工，答辩要点】
--   batch_no      单表唯一，跨表可重号  → 只负责链路串联
--   product_code  确定性生成、可回填    → 负责识别与检索（客服、管理端按产品查）
--   trace_code    随机生成、不可预测    → 负责防伪（印在包装上，防伪造）
--
-- 【执行顺序说明】
--   第 5 节（补历史缺失链路）必须排在第 6 节（回填）之前 —— 因为产品编号公式
--   依赖自增主键 froz_batch_id，只能在行插入之后由 UPDATE 计算，无法在
--   INSERT ... SELECT 时确定。
--
-- 执行方式（Windows 命令行，注意必须带字符集参数）：
--   mysql -uroot -p --default-character-set=utf8mb4 < product_center_v3.sql
-- 或在 Navicat / Workbench 中连接编码选 UTF-8 后执行。
-- =====================================================================

SET NAMES utf8mb4;
USE seafood_traceability_system;

-- ---------------------------------------------------------------------
-- 1. 幂等 DDL 工具
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS add_col_if_absent;

DELIMITER $$
CREATE PROCEDURE add_col_if_absent(IN p_table VARCHAR(64), IN p_col VARCHAR(64), IN p_ddl VARCHAR(600))
BEGIN
    IF NOT EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = p_table
                     AND column_name = p_col) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS add_idx_if_absent;

DELIMITER $$
CREATE PROCEDURE add_idx_if_absent(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_cols VARCHAR(255))
BEGIN
    IF NOT EXISTS (SELECT 1
                   FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = p_table
                     AND index_name = p_index) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_cols, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 2. 四张批号表补列
--
-- 【命名红线 —— 新列绝对不能叫 status 或 node_id】
--   OwnedBatchService.requireOwned() 用 entity.getStatus() 做"工作流状态白名单"、
--   用 getNodeId() 做归属校验，而 NodeOwned 的 getStatus() 是默认方法，
--   Lombok @Data 生成的 getter 会覆盖它。若新增列叫 status，
--   P0 的越权/状态校验会静默失效且不报错。故质量状态统一命名 quality_status。
--
-- 【字段语义分工】
--   product_type （已有）商品品类：冷冻整虾 / 冷冻虾仁 / 虾滑 / 冰鲜整虾
--   product_form （新增）形态等级：鲜虾 / 冻虾
--   spec_grade   （新增）规格等级：30-40只/斤 等
--   breed_stage  （已有）养殖阶段：虾苗 / 成虾（养殖环节的"规格"由它表达）
--   quality_status（新增）本环节是否合格 —— 不继承，各环节独立填写
-- ---------------------------------------------------------------------

-- 2.1 养殖环节：来源方式（人工养殖 / 海洋捕捞）+ 形态 + 本环节质量状态
CALL add_col_if_absent('farm_batch', 'source_type',
    "`source_type` VARCHAR(20) DEFAULT NULL COMMENT '来源方式：人工养殖/海洋捕捞' AFTER `breed_stage`");
CALL add_col_if_absent('farm_batch', 'product_form',
    "`product_form` VARCHAR(10) NOT NULL DEFAULT '鲜虾' COMMENT '产品形态：鲜虾/冻虾（养殖环节出塘即鲜虾）' AFTER `source_type`");
CALL add_col_if_absent('farm_batch', 'quality_status',
    "`quality_status` TINYINT NOT NULL DEFAULT 0 COMMENT '本环节质量状态：0待检 1合格 2不合格' AFTER `product_form`");

-- 2.2 冷冻加工环节：形态与规格在此定型，并生成对外产品编号
CALL add_col_if_absent('froz_batch', 'product_form',
    "`product_form` VARCHAR(10) NOT NULL DEFAULT '冻虾' COMMENT '产品形态：鲜虾/冻虾（加工环节确定，下游继承）' AFTER `product_type`");
CALL add_col_if_absent('froz_batch', 'spec_grade',
    "`spec_grade` VARCHAR(30) DEFAULT NULL COMMENT '规格等级，如 40-50只/斤（加工环节确定，下游继承）' AFTER `product_form`");
CALL add_col_if_absent('froz_batch', 'quality_status',
    "`quality_status` TINYINT NOT NULL DEFAULT 0 COMMENT '本环节质量状态：0待检 1合格 2不合格' AFTER `spec_grade`");
CALL add_col_if_absent('froz_batch', 'product_code',
    "`product_code` VARCHAR(30) DEFAULT NULL COMMENT '对外产品编号（加工环节定型时生成，下游继承）' AFTER `quality_status`");

-- 2.3 批发环节：形态/规格/产品编号一律继承上游（冗余存储，便于按编号直接检索）
CALL add_col_if_absent('whol_batch', 'product_form',
    "`product_form` VARCHAR(10) NOT NULL DEFAULT '冻虾' COMMENT '产品形态（继承上游加工环节）' AFTER `product_type`");
CALL add_col_if_absent('whol_batch', 'spec_grade',
    "`spec_grade` VARCHAR(30) DEFAULT NULL COMMENT '规格等级（继承上游加工环节）' AFTER `product_form`");
CALL add_col_if_absent('whol_batch', 'quality_status',
    "`quality_status` TINYINT NOT NULL DEFAULT 0 COMMENT '本环节质量状态：0待检 1合格 2不合格' AFTER `spec_grade`");
CALL add_col_if_absent('whol_batch', 'product_code',
    "`product_code` VARCHAR(30) DEFAULT NULL COMMENT '对外产品编号（继承上游加工环节）' AFTER `quality_status`");

-- 2.4 零售环节：+ 商品展示字段（消费者端"在售产品"列表需要）
CALL add_col_if_absent('reta_batch', 'product_form',
    "`product_form` VARCHAR(10) NOT NULL DEFAULT '冻虾' COMMENT '产品形态（继承上游）' AFTER `product_type`");
CALL add_col_if_absent('reta_batch', 'spec_grade',
    "`spec_grade` VARCHAR(30) DEFAULT NULL COMMENT '规格等级（继承上游）' AFTER `product_form`");
CALL add_col_if_absent('reta_batch', 'quality_status',
    "`quality_status` TINYINT NOT NULL DEFAULT 0 COMMENT '本环节质量状态：0待检 1合格 2不合格' AFTER `spec_grade`");
CALL add_col_if_absent('reta_batch', 'product_code',
    "`product_code` VARCHAR(30) DEFAULT NULL COMMENT '对外产品编号（继承上游）' AFTER `quality_status`");
CALL add_col_if_absent('reta_batch', 'price',
    "`price` DECIMAL(10,2) DEFAULT NULL COMMENT '零售单价（元/件）' AFTER `product_code`");
CALL add_col_if_absent('reta_batch', 'image_url',
    "`image_url` VARCHAR(255) DEFAULT NULL COMMENT '商品图片地址，为空时前端回退默认图' AFTER `price`");

-- ---------------------------------------------------------------------
-- 3. 各环节检测记录表
--
-- 【关联方式用多态 stage_type + batch_id，不能用 node_id + batch_no】
--   因为 batch_no 只在单表内唯一、跨表可以重号，用 (node_id, batch_no) 关联
--   会在四张批号表之间串表。node_id / batch_no 作为冗余列保留，
--   分别服务于归属校验（配合 NodeOwned）和列表展示/检索。
--
-- 一条记录 = 一份报告的一个检测项；report_no 把同一份报告的多个项聚成一组。
-- 整批是否合格同时冗余在批号表的 quality_status 上（列表看字段、明细看本表），
-- 两侧一致性由后端录入接口保证，不用触发器。
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inspection (
    inspection_id  INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    node_id        INT NOT NULL COMMENT '检测所属环节企业ID（用于归属校验）',
    stage_type     TINYINT NOT NULL COMMENT '环节类型：1养殖 2冷冻加工 3批发 4零售（同 node_info.type，决定 batch_id 指向哪张批号表）',
    batch_id       INT NOT NULL COMMENT '所属批号主键（四张批号表之一，由 stage_type 决定）',
    batch_no       VARCHAR(30) NOT NULL COMMENT '所属批号（冗余，便于列表展示与检索）',
    item_name      VARCHAR(50) NOT NULL COMMENT '检测项目：感官/菌落总数/大肠菌群/氯霉素/重金属镉/水分等',
    item_value     VARCHAR(50) DEFAULT NULL COMMENT '检测值',
    standard_value VARCHAR(50) DEFAULT NULL COMMENT '标准限值',
    result         TINYINT NOT NULL DEFAULT 1 COMMENT '单项判定：1合格 2不合格',
    conclusion     TINYINT DEFAULT NULL COMMENT '本报告整批结论：1合格 2不合格（一条批号可有多份报告）',
    report_no      VARCHAR(50) DEFAULT NULL COMMENT '检测报告编号',
    org_name       VARCHAR(100) DEFAULT NULL COMMENT '检测机构名称',
    inspector      VARCHAR(20) DEFAULT NULL COMMENT '检测人/检验员',
    inspect_date   DATE DEFAULT NULL COMMENT '检测日期',
    remark         VARCHAR(200) DEFAULT NULL COMMENT '备注',
    create_time    DATETIME DEFAULT NULL,
    PRIMARY KEY (inspection_id),
    KEY idx_insp_batch (stage_type, batch_id),
    KEY idx_insp_node (node_id),
    KEY idx_insp_batch_no (batch_no),
    KEY idx_insp_report (report_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='各环节检测记录表';

-- ---------------------------------------------------------------------
-- 4. 索引
--    froz_batch.product_code 可以建唯一索引：编号由"SP+日期+主键"确定性生成，
--    主键唯一则编号必唯一。
--    whol/reta 只能建普通索引：同一个上游加工批号可能卖给多个批发商，
--    它们的 product_code 相同，建唯一索引会直接插入失败。
-- ---------------------------------------------------------------------
CALL add_idx_if_absent('froz_batch', 'uk_froz_product_code', '`product_code`');
CALL add_idx_if_absent('whol_batch', 'idx_whol_product_code', '`product_code`');
CALL add_idx_if_absent('reta_batch', 'idx_reta_product_code', '`product_code`');
CALL add_idx_if_absent('reta_batch', 'idx_reta_quality',      '`quality_status`');

-- ---------------------------------------------------------------------
-- 5. 修复"示例溯源码查不到"的历史缺陷
--
-- 【根因】建表脚本 6.4 段的写法是：
--     INSERT IGNORE INTO reta_batch (...) SELECT ... FROM node_info n, node_info u
--     WHERE n.code='reta001' AND u.code='whol001' AND @seed = 0;
--   而 node_info 里根本不存在 reta001 / whol001（只有 froz001）。
--   SELECT 命中 0 行 → INSERT 静默插入 0 条，不报任何错；
--   紧接着脚本 6.6 段又无条件写入了 seed 标记，于是这个缺失被永久固化，
--   重复执行脚本再也不会补。结果 UI 三处硬编码提示的 SHZ202601010001 查不到。
--
-- 【修法】用真实存在的企业（farm101/froz101/whol101/reta101）补一条 1 月的完整链路，
--   并让它的零售批号使用该示例溯源码。故意不用 *001 那些不存在的编码，
--   避免重蹈"静默 0 行"的覆辙。
--   全部 INSERT IGNORE + 唯一键(batch_no/trace_code)兜底，重复执行安全。
--   product_code 此处留空，由第 6.1~6.3 节按统一公式回填（需自增主键，插完才能算）。
-- ---------------------------------------------------------------------

-- 5.1 养殖
INSERT IGNORE INTO farm_batch
    (node_id, batch_no, breed, breed_stage, source_type, product_form, quality_status,
     quarantine_no, inspector, status, create_time, update_time)
SELECT n.node_id, 'FARM20260101', '南美白对虾', '成虾', '人工养殖', '鲜虾', 1,
       '闽动检（2026）第0101号', '张伟', 2, '2026-01-05 09:20:00', '2026-01-06 10:00:00'
FROM node_info n WHERE n.code = 'farm101';

-- 5.2 冷冻加工（进场所属区域取自上游养殖企业所在地）
INSERT IGNORE INTO froz_batch
    (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed,
     quarantine_no, inspector, product_type, product_form, spec_grade, quality_status,
     status, create_time, update_time)
SELECT n.node_id, 'FROZ20260101', u.prov_id, u.city_id, u.node_id, 'FARM20260101', '南美白对虾',
       '闽品检（2026）第0101号', '陈静', '冷冻整虾', '冻虾', '40-50只/斤', 1,
       3, '2026-01-07 09:00:00', '2026-01-08 10:00:00'
FROM node_info n, node_info u WHERE n.code = 'froz101' AND u.code = 'farm101';

-- 5.3 批发
INSERT IGNORE INTO whol_batch
    (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type,
     product_form, spec_grade, quality_status, status, create_time, update_time)
SELECT n.node_id, 'WHOL20260101', u.prov_id, u.city_id, u.node_id, 'FROZ20260101', '南美白对虾', '冷冻整虾',
       '冻虾', '40-50只/斤', 1,
       3, '2026-01-12 09:00:00', '2026-01-13 10:00:00'
FROM node_info n, node_info u WHERE n.code = 'whol101' AND u.code = 'froz101';

-- 5.4 零售（带示例溯源码 SHZ202601010001）
INSERT IGNORE INTO reta_batch
    (node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type,
     product_form, spec_grade, quality_status, price,
     status, trace_code, trace_time, create_time, update_time)
SELECT n.node_id, 'RETA20260101', u.prov_id, u.city_id, u.node_id, 'WHOL20260101', '南美白对虾', '冷冻整虾',
       '冻虾', '40-50只/斤', 1, 68.00,
       3, 'SHZ202601010001', '2026-01-14 10:00:00', '2026-01-13 11:00:00', '2026-01-14 10:00:00'
FROM node_info n, node_info u WHERE n.code = 'reta101' AND u.code = 'whol101';

-- ---------------------------------------------------------------------
-- 6. 历史数据回填
--    产品编号用与 Java 代码完全相同的确定性公式，保证"回填值 == 新生成值"：
--      "SP" + 定型日期(yyyyMMdd) + 批号主键左补零4位
--    对应 FarmBatchServiceImpl.confirmDownstream() 里的生成逻辑。
--    顺序不能颠倒：froz 算出编号 → whol 继承 → reta 继承。
-- ---------------------------------------------------------------------

-- 6.1 加工环节：已确认(status=3)的批号补产品编号
UPDATE froz_batch
SET product_code = CONCAT('SP', DATE_FORMAT(create_time, '%Y%m%d'), LPAD(froz_batch_id, 4, '0'))
WHERE status = 3
  AND product_code IS NULL
  AND create_time IS NOT NULL;

-- 6.2 批发环节：从上游加工批号继承
UPDATE whol_batch w
    JOIN froz_batch f ON f.batch_no = w.up_batch_no AND f.node_id = w.up_node_id
SET w.product_code = f.product_code,
    w.product_form = f.product_form,
    w.spec_grade   = COALESCE(NULLIF(f.spec_grade, ''), w.spec_grade)
WHERE w.product_code IS NULL;

-- 6.3 零售环节：从上游批发批号继承
UPDATE reta_batch r
    JOIN whol_batch w ON w.batch_no = r.up_batch_no AND w.node_id = r.up_node_id
SET r.product_code = w.product_code,
    r.product_form = w.product_form,
    r.spec_grade   = COALESCE(NULLIF(w.spec_grade, ''), r.spec_grade)
WHERE r.product_code IS NULL;

-- 6.4 形态默认值兜底（避免消费者端商品卡出现空字段）
UPDATE froz_batch SET product_form = '冻虾' WHERE product_form IS NULL OR product_form = '';
UPDATE whol_batch SET product_form = '冻虾' WHERE product_form IS NULL OR product_form = '';
UPDATE reta_batch SET product_form = '冻虾' WHERE product_form IS NULL OR product_form = '';

-- 6.5 质量状态兜底：已进入流通（已发布/已确认）的历史批号视为合格，其余留作待检
UPDATE farm_batch SET quality_status = 1 WHERE quality_status = 0 AND status IN (2, 3);
UPDATE froz_batch SET quality_status = 1 WHERE quality_status = 0 AND status = 3;
UPDATE whol_batch SET quality_status = 1 WHERE quality_status = 0 AND status = 3;
UPDATE reta_batch SET quality_status = 1 WHERE quality_status = 0 AND status = 3;

-- 6.6 在售商品的价格兜底（让消费者端首页立刻有内容）
UPDATE reta_batch
SET price = 68.00
WHERE status = 3 AND trace_code IS NOT NULL AND price IS NULL;

-- 6.7 规格兜底：从既有的加工工序记录里把规格文本捞出来
--     （历史上"40-50只/斤"这类规格是被当自由文本塞在 process_record.temperature 的）
UPDATE froz_batch f
    JOIN (SELECT froz_batch_id, MAX(temperature) AS spec
          FROM process_record
          WHERE step = '分级' AND temperature LIKE '%只/斤%'
          GROUP BY froz_batch_id) p ON p.froz_batch_id = f.froz_batch_id
SET f.spec_grade = p.spec
WHERE f.spec_grade IS NULL OR f.spec_grade = '';

UPDATE whol_batch w
    JOIN froz_batch f ON f.batch_no = w.up_batch_no AND f.node_id = w.up_node_id
SET w.spec_grade = f.spec_grade
WHERE (w.spec_grade IS NULL OR w.spec_grade = '')
  AND f.spec_grade IS NOT NULL AND f.spec_grade <> '';

UPDATE reta_batch r
    JOIN whol_batch w ON w.batch_no = r.up_batch_no AND w.node_id = r.up_node_id
SET r.spec_grade = w.spec_grade
WHERE (r.spec_grade IS NULL OR r.spec_grade = '')
  AND w.spec_grade IS NOT NULL AND w.spec_grade <> '';

-- ---------------------------------------------------------------------
-- 7. 清理 DDL 工具
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS add_col_if_absent;
DROP PROCEDURE IF EXISTS add_idx_if_absent;

-- ---------------------------------------------------------------------
-- 8. 执行结果自检
--    最后两条最该常驻 —— 它们能在演示前 10 秒发现
--    "某个溯源码查不到"这类问题。
-- ---------------------------------------------------------------------
SELECT '改造脚本执行完成' AS message;

SELECT '各表行数' AS 指标, CONCAT(
    'node=', (SELECT COUNT(*) FROM node_info),
    ' farm=', (SELECT COUNT(*) FROM farm_batch),
    ' froz=', (SELECT COUNT(*) FROM froz_batch),
    ' whol=', (SELECT COUNT(*) FROM whol_batch),
    ' reta=', (SELECT COUNT(*) FROM reta_batch),
    ' insp=', (SELECT COUNT(*) FROM inspection)
) AS 明细;

SELECT '可用溯源码' AS 指标, GROUP_CONCAT(trace_code ORDER BY trace_code) AS 明细
FROM reta_batch WHERE status = 3 AND trace_code IS NOT NULL;

SELECT '在售商品数(消费者端可见)' AS 指标, CAST(COUNT(*) AS CHAR) AS 明细
FROM reta_batch WHERE status = 3 AND trace_code IS NOT NULL AND quality_status <> 2;

SELECT '不合格批号数(仅管理端可见)' AS 指标, CAST(COUNT(*) AS CHAR) AS 明细
FROM reta_batch WHERE quality_status = 2;

-- 链路断裂的零售批号：状态已确认、有溯源码，但上溯不到完整四级
-- 正常应为 0；不为 0 说明演示数据缺环节
SELECT '链路断裂的零售批号(应为0)' AS 指标, CAST(COUNT(*) AS CHAR) AS 明细
FROM reta_batch r
         LEFT JOIN whol_batch w ON w.batch_no = r.up_batch_no AND w.node_id = r.up_node_id
         LEFT JOIN froz_batch f ON f.batch_no = w.up_batch_no AND f.node_id = w.up_node_id
         LEFT JOIN farm_batch fm ON fm.batch_no = f.up_batch_no AND fm.node_id = f.up_node_id
WHERE r.status = 3
  AND r.trace_code IS NOT NULL
  AND (w.whol_batch_id IS NULL OR f.froz_batch_id IS NULL OR fm.farm_batch_id IS NULL);

-- 产品编号回填情况
SELECT '产品编号回填' AS 指标, CONCAT(
    'froz=', (SELECT COUNT(*) FROM froz_batch WHERE product_code IS NOT NULL),
    ' whol=', (SELECT COUNT(*) FROM whol_batch WHERE product_code IS NOT NULL),
    ' reta=', (SELECT COUNT(*) FROM reta_batch WHERE product_code IS NOT NULL)
) AS 明细;
