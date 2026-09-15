-- =====================================================================
-- 冷冻对虾全产业链溯源系统 —— 优化脚本 v2（索引 + 密码列扩容）
-- 目标数据库：seafood_traceability_system
--
-- 【设计原则：增量、非破坏、可重复执行】
--   1) 只操作 seafood_traceability_system 这一个库；
--   2) 加索引前先查 information_schema，已存在则跳过，重复执行不报错；
--   3) 密码列扩容是可重复执行的 DDL（MODIFY 幂等）。
--
-- 执行方式（Windows 命令行，注意必须带字符集参数）：
--   mysql -uroot -p --default-character-set=utf8mb4 < optimize_v2.sql
-- 或在 Navicat / Workbench 中连接编码选 UTF-8 后执行。
-- =====================================================================

SET NAMES utf8mb4;
USE seafood_traceability_system;

-- ---------------------------------------------------------------------
-- 1. 密码列扩容
--    BCrypt 哈希固定 60 字符，原 varchar(20) 存不下。
--    若先上线代码后执行本脚本，新建/改密会直接报错，故本步骤需最先执行。
-- ---------------------------------------------------------------------
ALTER TABLE `admin`
    MODIFY `password` VARCHAR(100) NOT NULL COMMENT '登录密码（BCrypt 哈希，兼容历史明文）';

ALTER TABLE `node_info`
    MODIFY `password` VARCHAR(100) NOT NULL COMMENT '登录密码（BCrypt 哈希，兼容历史明文）';

-- ---------------------------------------------------------------------
-- 2. 索引（MySQL 不支持 CREATE INDEX IF NOT EXISTS，用存储过程实现幂等）
-- ---------------------------------------------------------------------
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

-- node_info：企业类型筛选与统计、注册趋势、省市区联动
CALL add_idx_if_absent('node_info', 'idx_node_type',     '`type`');
CALL add_idx_if_absent('node_info', 'idx_node_reg_date', '`reg_date`');
CALL add_idx_if_absent('node_info', 'idx_node_prov',     '`prov_id`');
CALL add_idx_if_absent('node_info', 'idx_node_city',     '`city_id`');

-- 四类批号：状态筛选（列表页按状态 Tab 查询）
CALL add_idx_if_absent('farm_batch', 'idx_farm_status', '`status`');
CALL add_idx_if_absent('froz_batch', 'idx_froz_status', '`status`');
CALL add_idx_if_absent('whol_batch', 'idx_whol_status', '`status`');
CALL add_idx_if_absent('reta_batch', 'idx_reta_status', '`status`');

-- 四类批号：归属企业（所有按企业隔离的查询都依赖它）
CALL add_idx_if_absent('farm_batch', 'idx_farm_node', '`node_id`');
CALL add_idx_if_absent('froz_batch', 'idx_froz_node', '`node_id`');
CALL add_idx_if_absent('whol_batch', 'idx_whol_node', '`node_id`');
CALL add_idx_if_absent('reta_batch', 'idx_reta_node', '`node_id`');

-- 下游三张表：上游链路回溯（消费者溯源逐级上溯时使用）
CALL add_idx_if_absent('froz_batch', 'idx_froz_up', '`up_node_id`,`up_batch_no`');
CALL add_idx_if_absent('whol_batch', 'idx_whol_up', '`up_node_id`,`up_batch_no`');
CALL add_idx_if_absent('reta_batch', 'idx_reta_up', '`up_node_id`,`up_batch_no`');

DROP PROCEDURE add_idx_if_absent;

-- ---------------------------------------------------------------------
-- 3. 执行结果确认
-- ---------------------------------------------------------------------
SELECT '优化脚本执行完成' AS message;
SELECT TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS columns_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND index_name <> 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;
