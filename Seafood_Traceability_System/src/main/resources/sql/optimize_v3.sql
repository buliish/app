-- =====================================================================
-- 冷冻对虾全产业链溯源系统 —— 优化脚本 v3（批次商品属性列 + 检测记录表）
-- 目标数据库：seafood_traceability_system
--
-- 【为什么需要本脚本】
--   optimize 分支的实体类新增了商品属性字段（RetaBatch.productCode /
--   qualityStatus / productForm / specGrade / price / imageUrl，
--   FarmBatch.sourceType 等），但 v2 脚本只做了密码列扩容与索引，
--   并没有把这些列建出来。缺列会导致 MyBatis-Plus 把它们拼进 SELECT，
--   运行时直接报 Unknown column。
--
-- 【设计原则：增量、非破坏、可重复执行】
--   1) 只操作 seafood_traceability_system 这一个库；
--   2) MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，用 information_schema
--      判断后再动态执行，重复执行不报错；
--   3) 已有的数据行不受影响（新列允许 NULL）。
--
-- 执行方式（Windows 命令行，注意必须带字符集参数）：
--   mysql -uroot -p --default-character-set=utf8mb4 < optimize_v3.sql
-- 依赖：请先执行 optimize_v2.sql（密码列扩容 + 索引）
-- =====================================================================

SET NAMES utf8mb4;
USE seafood_traceability_system;

-- ---------------------------------------------------------------------
-- 1. 幂等加列过程
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS add_col_if_absent;
DROP PROCEDURE IF EXISTS add_idx_if_absent;

DELIMITER $$
CREATE PROCEDURE add_col_if_absent(IN p_table VARCHAR(64),
                                   IN p_column VARCHAR(64),
                                   IN p_definition VARCHAR(255))
BEGIN
    IF NOT EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = p_table
                     AND column_name = p_column) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

-- optimize_v2.sql 结尾把本过程 DROP 了，v3 要单独可执行，这里重新定义一份
CREATE PROCEDURE add_idx_if_absent(IN p_table VARCHAR(64),
                                   IN p_index VARCHAR(64),
                                   IN p_cols VARCHAR(255))
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
-- 2. 四类批号的商品属性列
--    形态/规格/产品编号随加工环节定型后由下游继承，故四张表都有；
--    单价与图片只在零售环节对外展示，只有 reta_batch 有；
--    来源方式（人工养殖/海洋捕捞）是链路最源头的属性，只有 farm_batch 有。
-- ---------------------------------------------------------------------

-- 2.1 养殖批号：来源方式 + 形态 + 质量状态
CALL add_col_if_absent('farm_batch', 'source_type',    "VARCHAR(20)  NULL COMMENT '来源方式：人工养殖/海洋捕捞'");
CALL add_col_if_absent('farm_batch', 'product_form',   "VARCHAR(20)  NULL COMMENT '产品形态：鲜虾/冻虾'");
CALL add_col_if_absent('farm_batch', 'quality_status', "TINYINT      NULL COMMENT '质量状态：0待检 1合格 2不合格'");

-- 2.2 冷冻加工批号：形态 + 规格等级 + 质量状态 + 对外产品编号
CALL add_col_if_absent('froz_batch', 'product_form',   "VARCHAR(20)  NULL COMMENT '产品形态：鲜虾/冻虾'");
CALL add_col_if_absent('froz_batch', 'spec_grade',     "VARCHAR(30)  NULL COMMENT '规格等级，如 40-50只/斤'");
CALL add_col_if_absent('froz_batch', 'quality_status', "TINYINT      NULL COMMENT '质量状态：0待检 1合格 2不合格'");
CALL add_col_if_absent('froz_batch', 'product_code',   "VARCHAR(50)  NULL COMMENT '对外产品编号（加工环节定型时生成，下游继承）'");

-- 2.3 批发商批号：形态 + 规格等级 + 质量状态 + 对外产品编号
CALL add_col_if_absent('whol_batch', 'product_form',   "VARCHAR(20)  NULL COMMENT '产品形态：鲜虾/冻虾'");
CALL add_col_if_absent('whol_batch', 'spec_grade',     "VARCHAR(30)  NULL COMMENT '规格等级，如 40-50只/斤'");
CALL add_col_if_absent('whol_batch', 'quality_status', "TINYINT      NULL COMMENT '质量状态：0待检 1合格 2不合格'");
CALL add_col_if_absent('whol_batch', 'product_code',   "VARCHAR(50)  NULL COMMENT '对外产品编号（继承自加工环节）'");

-- 2.4 零售商批号：形态 + 规格等级 + 质量状态 + 对外产品编号 + 单价 + 图片
CALL add_col_if_absent('reta_batch', 'product_form',   "VARCHAR(20)   NULL COMMENT '产品形态：鲜虾/冻虾'");
CALL add_col_if_absent('reta_batch', 'spec_grade',     "VARCHAR(30)   NULL COMMENT '规格等级，如 40-50只/斤'");
CALL add_col_if_absent('reta_batch', 'quality_status', "TINYINT       NULL COMMENT '质量状态：0待检 1合格 2不合格'");
CALL add_col_if_absent('reta_batch', 'product_code',   "VARCHAR(50)   NULL COMMENT '对外产品编号（加工环节定型时生成，下游继承）'");
CALL add_col_if_absent('reta_batch', 'price',          "DECIMAL(10,2) NULL COMMENT '零售单价（元/件），消费者端商品卡展示'");
CALL add_col_if_absent('reta_batch', 'image_url',      "VARCHAR(255)  NULL COMMENT '商品图片地址，为空时前端回退默认图'");

-- ---------------------------------------------------------------------
-- 3. 检测记录表
--    四个环节共用一张表，用 stage_type 区分记录挂在哪类批号上，
--    避免为四类批号各建一张结构完全相同的检测表。
--    stage_type 取值与 node_info.type 一致：1养殖 2冷冻加工 3批发 4零售
--    结果取值：1合格 2不合格（与前端 el-radio 的 value 一致）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inspection_record` (
    `record_id`      INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `stage_type`     TINYINT NOT NULL COMMENT '环节类型：1养殖 2冷冻加工 3批发 4零售',
    `batch_id`       INT NOT NULL COMMENT '所属环节批号主键（按 stage_type 对应四张批号表）',
    `node_id`        INT NOT NULL COMMENT '录入企业节点ID（归属校验用）',
    `item_name`      VARCHAR(50)  NOT NULL COMMENT '检测项目名称',
    `item_value`     VARCHAR(50)  DEFAULT NULL COMMENT '检测值，如 0.62 mg/kg',
    `standard_value` VARCHAR(50)  DEFAULT NULL COMMENT '标准限值，如 ≤0.5 mg/kg',
    `result`         TINYINT      DEFAULT NULL COMMENT '单项判定：1合格 2不合格',
    `conclusion`     TINYINT      DEFAULT NULL COMMENT '整批结论：1合格 2不合格',
    `report_no`      VARCHAR(50)  DEFAULT NULL COMMENT '检测报告编号',
    `org_name`       VARCHAR(100) DEFAULT NULL COMMENT '检测机构名称',
    `inspector`      VARCHAR(20)  DEFAULT NULL COMMENT '检测人姓名',
    `inspect_date`   DATE         DEFAULT NULL COMMENT '检测日期',
    `remark`         VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time`    DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time`    DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`record_id`),
    KEY `idx_insp_batch` (`stage_type`, `batch_id`),
    KEY `idx_insp_node`  (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='各环节检测记录表';

-- ---------------------------------------------------------------------
-- 4. 新增列的配套索引
--    product_code 供消费者端"按产品编号查溯源"使用；
--    quality_status 供在售商品列表过滤不合格品使用。
--    注意：product_code 不建唯一索引 —— 同一产品编号可对应多个零售批次，
--    查询侧用 ORDER BY reta_batch_id DESC LIMIT 1 取最新一条。
-- ---------------------------------------------------------------------
CALL add_idx_if_absent('reta_batch', 'idx_reta_product_code', '`product_code`');
CALL add_idx_if_absent('reta_batch', 'idx_reta_quality',      '`quality_status`');
CALL add_idx_if_absent('froz_batch', 'idx_froz_product_code', '`product_code`');
CALL add_idx_if_absent('whol_batch', 'idx_whol_product_code', '`product_code`');

DROP PROCEDURE add_col_if_absent;
DROP PROCEDURE add_idx_if_absent;

-- ---------------------------------------------------------------------
-- 5. 执行结果确认
-- ---------------------------------------------------------------------
SELECT '优化脚本 v3 执行完成' AS message;

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (
        (TABLE_NAME = 'reta_batch' AND COLUMN_NAME IN ('product_form','spec_grade','quality_status','product_code','price','image_url'))
     OR (TABLE_NAME = 'farm_batch' AND COLUMN_NAME IN ('source_type','product_form','quality_status'))
     OR (TABLE_NAME = 'froz_batch' AND COLUMN_NAME IN ('product_form','spec_grade','quality_status','product_code'))
     OR (TABLE_NAME = 'whol_batch' AND COLUMN_NAME IN ('product_form','spec_grade','quality_status','product_code'))
      )
ORDER BY TABLE_NAME, COLUMN_NAME;
