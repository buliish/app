-- =====================================================================
-- 冷冻对虾全产业链溯源系统 —— 批号数量（投入/产出）与超领校验
-- 目标数据库：seafood_traceability_system
--
-- 【为什么需要】
--   在此之前，链路只表达"谁从谁那儿进的货"，不表达"进了多少"。
--   现实中一批货是分批领用的：养殖场出场 3 吨冻虾，加工厂领 1 吨做虾滑、
--   1 吨做虾丸、1 吨做虾球，分别流向三个下游。这既是一对多的"结构"，
--   也是一对多的"数量"—— 结构已由 up_batch_no 表达，数量由本脚本补上。
--
--   补上数量之后，"上游确认"这个动作才有实质内容：
--   不再是空盖章，而是核对下游申报的领用量是否与自己的出货量相符。
--
-- 【两个量，不能只记一个】
--   quantity_kg       本批号的量
--                     养殖环节＝出场量；加工及以下＝产出量
--   up_quantity_kg    从上游批号领用的量（养殖环节没有，它是链头）
--
--   加工厂那批虾滑即：up_quantity_kg=1000（领 1 吨虾）、quantity_kg=550
--   （产出 550kg 虾滑）—— 加工损耗 45% 一目了然。
--
-- 【单位统一用 kg】
--   吨、袋、件都能换算成公斤；混着存一定会对不上账，故只存 kg。
--
-- 【老数据兼容】
--   新增列一律允许 NULL，语义是"未登记数量"。
--   校验规则：只有上游 quantity_kg 非 NULL 时才做超领校验 ——
--   既有的历史批号没有数量，一刀切去校验会把它们全判成超领。
--
-- 【幂等】MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，用 information_schema 守卫。
--
-- 执行方式：
--   mysql -uroot -p --default-character-set=utf8mb4 < add_batch_quantity.sql
-- =====================================================================

SET NAMES utf8mb4;
USE seafood_traceability_system;

DROP PROCEDURE IF EXISTS add_col_if_absent;

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
DELIMITER ;

-- ---------------------------------------------------------------------
-- 1. 本批号的量（四张表都有）
-- ---------------------------------------------------------------------
CALL add_col_if_absent('farm_batch', 'quantity_kg',
    "DECIMAL(12,2) NULL COMMENT '本批出场量(kg)'");
CALL add_col_if_absent('froz_batch', 'quantity_kg',
    "DECIMAL(12,2) NULL COMMENT '本批产出量(kg)'");
CALL add_col_if_absent('whol_batch', 'quantity_kg',
    "DECIMAL(12,2) NULL COMMENT '本批库存量(kg)'");
CALL add_col_if_absent('reta_batch', 'quantity_kg',
    "DECIMAL(12,2) NULL COMMENT '本批进货量(kg)'");

-- ---------------------------------------------------------------------
-- 2. 向上游领用的量（下游三张表；养殖是链头，没有上游）
-- ---------------------------------------------------------------------
CALL add_col_if_absent('froz_batch', 'up_quantity_kg',
    "DECIMAL(12,2) NULL COMMENT '自上游批号领用量(kg)'");
CALL add_col_if_absent('whol_batch', 'up_quantity_kg',
    "DECIMAL(12,2) NULL COMMENT '自上游批号领用量(kg)'");
CALL add_col_if_absent('reta_batch', 'up_quantity_kg',
    "DECIMAL(12,2) NULL COMMENT '自上游批号领用量(kg)'");

DROP PROCEDURE add_col_if_absent;

-- ---------------------------------------------------------------------
-- 3. 配套索引
--    超领校验要按「上游批号 + 上游企业」汇总下游领用量（SUM(up_quantity_kg)），
--    demo 数据量下问题不大，但既然有现成的复合索引就复用，避免全表扫。
--    已有 idx_froz_up / idx_whol_up / idx_reta_up (up_node_id, up_batch_no)，
--    这里无需重复建。
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- 4. 执行结果确认
-- ---------------------------------------------------------------------
SELECT '批号数量字段添加完成' AS message;

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND COLUMN_NAME IN ('quantity_kg', 'up_quantity_kg')
ORDER BY TABLE_NAME, COLUMN_NAME;
