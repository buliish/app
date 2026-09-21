-- =====================================================================
-- 冷冻对虾全产业链溯源系统 —— 演示数据：一对多产业链（树）
-- 目标数据库：seafood_traceability_system
--
-- 【为什么单独一个文件】
--   demo_data_v2 / demo_data_v3 造的都是"一条线"的链路：每个上游批号
--   只有一个下游批号。而现实中一批虾可以同时被加工成虾滑、虾丸、
--   冷冻整虾等多种产品 —— 产业链是"上游一条线、下游一棵树"。
--   本脚本专门补这种树形数据，用于演示产业链树与"同源产品"。
--
-- 【造出来的树】
--   FARM20260201  南美白对虾（厦门翔安南美白对虾养殖场，1 批虾）
--     ├── FROZ20260201   冷冻虾仁 → WHOL20260201  → RETA20260201 （福建，已有）
--     ├── FROZ20260201B  虾滑     → WHOL20260201B → RETA20260201B（浙江，本脚本）
--     └── FROZ20260201C  虾丸     → WHOL20260201C → RETA20260201C（江苏，本脚本）
--
--   两条新分支复用既有企业节点，不新增企业：
--     加工 112 froz101 福州连江对虾冷冻加工厂
--     批发 143 whol104 温州菜篮子水产批发市场 / 153 whol105 连云港海州水产批发市场
--     零售 144 reta104 宁波三江购物超市     / 154 reta105 南通大润发超市
--
-- 【幂等】INSERT IGNORE + 显式主键（401/402 段；
--         现有数据用到 3xx，demo_data_v3.sql 用 601+，互不冲突），
--         并用 demo_data_seed 记录执行标记，重复执行不会产生重复行。
--
-- 执行方式：
--   mysql -uroot -p --default-character-set=utf8mb4 < demo_chain_tree.sql
-- =====================================================================

SET NAMES utf8mb4;
USE seafood_traceability_system;

CREATE TABLE IF NOT EXISTS demo_data_seed (
    seed_key VARCHAR(50) NOT NULL COMMENT '演示数据批次标记',
    exec_time DATETIME DEFAULT NULL COMMENT '写入时间',
    PRIMARY KEY (seed_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演示数据写入标记表';

SET @todo := (SELECT COUNT(*) = 0 FROM demo_data_seed WHERE seed_key = 'demo_chain_tree');

-- ---------------------------------------------------------------------
-- 1. 加工批号：同一个上游批号 FARM20260201 派生出的两条新分支
--    这两行是本脚本的核心 —— 它们与已有的 FROZ20260201 共用同一个
--    up_batch_no，从而形成"一批虾 → 三种产品"。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO froz_batch
    (froz_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no,
     breed, quarantine_no, inspector, product_type, product_form, spec_grade,
     quality_status, status, create_time, update_time)
SELECT 401, 112, 'FROZ20260201B', 2, 202, 111, 'FARM20260201',
       '南美白对虾', '闽食检（2026）第0201B号', '张伟', '虾滑', '冻虾', '500g/袋',
       1, 3, '2026-02-10 08:30:00', '2026-02-12 15:30:00'
WHERE @todo = 1;

INSERT IGNORE INTO froz_batch
    (froz_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no,
     breed, quarantine_no, inspector, product_type, product_form, spec_grade,
     quality_status, status, create_time, update_time)
SELECT 402, 112, 'FROZ20260201C', 2, 202, 111, 'FARM20260201',
       '南美白对虾', '闽食检（2026）第0201C号', '张伟', '虾丸', '冻虾', '300g/袋',
       1, 3, '2026-02-10 10:00:00', '2026-02-12 16:00:00'
WHERE @todo = 1;

-- ---------------------------------------------------------------------
-- 2. 批发批号：两条新分支各自流向不同省份的批发商
--    prov_id/city_id 记的是上游加工企业所在地（福建 福州），
--    与既有数据同一口径。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO whol_batch
    (whol_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no,
     breed, product_type, product_form, spec_grade, quality_status,
     status, create_time, update_time)
SELECT 401, 143, 'WHOL20260201B', 2, 201, 112, 'FROZ20260201B',
       '南美白对虾', '虾滑', '冻虾', '500g/袋', 1,
       3, '2026-02-13 09:00:00', '2026-02-14 10:00:00'
WHERE @todo = 1;

INSERT IGNORE INTO whol_batch
    (whol_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no,
     breed, product_type, product_form, spec_grade, quality_status,
     status, create_time, update_time)
SELECT 402, 153, 'WHOL20260201C', 2, 201, 112, 'FROZ20260201C',
       '南美白对虾', '虾丸', '冻虾', '300g/袋', 1,
       3, '2026-02-13 11:00:00', '2026-02-14 11:00:00'
WHERE @todo = 1;

-- ---------------------------------------------------------------------
-- 3. 零售批号：已确认 + 已生成溯源码，消费者端才查得到
--    price / product_code 一并给出，消费者端商品卡与"同源产品"才有内容。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO reta_batch
    (reta_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no,
     breed, product_type, product_form, spec_grade, quality_status,
     product_code, price, image_url,
     status, trace_code, trace_time, create_time, update_time)
SELECT 401, 144, 'RETA20260201B', 5, 503, 143, 'WHOL20260201B',
       '南美白对虾', '虾滑', '冻虾', '500g/袋', 1,
       'SP20260201B', 39.90, NULL,
       3, 'SHZ202602010010', '2026-02-15 10:00:00', '2026-02-14 11:30:00', '2026-02-15 10:00:00'
WHERE @todo = 1;

INSERT IGNORE INTO reta_batch
    (reta_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no,
     breed, product_type, product_form, spec_grade, quality_status,
     product_code, price, image_url,
     status, trace_code, trace_time, create_time, update_time)
SELECT 402, 154, 'RETA20260201C', 6, 602, 153, 'WHOL20260201C',
       '南美白对虾', '虾丸', '冻虾', '300g/袋', 1,
       'SP20260201C', 29.50, NULL,
       3, 'SHZ202602010011', '2026-02-15 11:00:00', '2026-02-14 14:00:00', '2026-02-15 11:00:00'
WHERE @todo = 1;

-- ---------------------------------------------------------------------
-- 4. 加工工序记录：两条新分支各录一套，消费者端详情页才有工序可看
-- ---------------------------------------------------------------------
INSERT IGNORE INTO process_record
    (record_id, froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT 401, 401, 112, '清洗', '2026-02-10 09:00:00', '常温清水', '李工', '冲洗去除泥沙与杂质'
WHERE @todo = 1;
INSERT IGNORE INTO process_record
    (record_id, froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT 402, 401, 112, '分级', '2026-02-10 10:00:00', '40-50只/斤', '王工', '按规格分级筛选'
WHERE @todo = 1;
INSERT IGNORE INTO process_record
    (record_id, froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT 403, 401, 112, '冷冻', '2026-02-10 13:00:00', '-35℃', '张工', '速冻30分钟后转-18℃冷藏'
WHERE @todo = 1;
INSERT IGNORE INTO process_record
    (record_id, froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT 404, 401, 112, '包装', '2026-02-10 15:00:00', '-18℃', '赵工', '真空包装 500g/袋（虾滑）'
WHERE @todo = 1;

INSERT IGNORE INTO process_record
    (record_id, froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT 405, 402, 112, '清洗', '2026-02-10 09:30:00', '常温清水', '李工', '冲洗去除泥沙与杂质'
WHERE @todo = 1;
INSERT IGNORE INTO process_record
    (record_id, froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT 406, 402, 112, '分级', '2026-02-10 10:30:00', '40-50只/斤', '王工', '按规格分级筛选'
WHERE @todo = 1;
INSERT IGNORE INTO process_record
    (record_id, froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT 407, 402, 112, '冷冻', '2026-02-10 14:00:00', '-35℃', '张工', '速冻30分钟后转-18℃冷藏'
WHERE @todo = 1;
INSERT IGNORE INTO process_record
    (record_id, froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT 408, 402, 112, '包装', '2026-02-10 16:00:00', '-18℃', '赵工', '真空包装 300g/袋（虾丸）'
WHERE @todo = 1;

-- ---------------------------------------------------------------------
-- 5. 执行标记
-- ---------------------------------------------------------------------
INSERT IGNORE INTO demo_data_seed (seed_key) VALUES ('demo_chain_tree');

-- ---------------------------------------------------------------------
-- 6. 数量登记：让"一对多"不只是结构，还有"分了多少量"
--    场景：养殖场出场 3 吨虾 → 加工厂分三批领用，各领 1 吨，
--          做成冷冻虾仁 / 虾滑 / 虾丸，各产出 550 kg（损耗 45%）。
--
--    这批 UPDATE 是固定值、可重复执行；放在 seed 守卫之外，
--    是为了让"已灌过演示数据"的库也能补上数量（守卫只挡 INSERT）。
--    若你手工改过这些批号的数量，重跑本脚本会覆盖回演示值。
-- ---------------------------------------------------------------------
UPDATE farm_batch SET quantity_kg = 3000
WHERE batch_no = 'FARM20260201';

UPDATE froz_batch SET up_quantity_kg = 1000, quantity_kg = 550
WHERE batch_no IN ('FROZ20260201', 'FROZ20260201B', 'FROZ20260201C');

-- 批发、零售不加工，领多少是多少
UPDATE whol_batch SET up_quantity_kg = 550, quantity_kg = 550
WHERE batch_no IN ('WHOL20260201', 'WHOL20260201B', 'WHOL20260201C');

UPDATE reta_batch SET up_quantity_kg = 550, quantity_kg = 550
WHERE batch_no IN ('RETA20260201', 'RETA20260201B', 'RETA20260201C');

-- ---------------------------------------------------------------------
-- 6. 执行结果确认：应看到 FARM20260201 下挂了 3 个加工批号
-- ---------------------------------------------------------------------
SELECT '一对多产业链演示数据执行完成' AS message;

SELECT z.batch_no     AS 加工批号,
       z.product_type AS 产品类型,
       zn.name        AS 加工企业,
       w.batch_no     AS 批发批号,
       wn.name        AS 批发企业,
       r.batch_no     AS 零售批号,
       r.trace_code   AS 溯源标识码,
       rn.name        AS 零售企业
FROM froz_batch z
         LEFT JOIN node_info zn ON zn.node_id = z.node_id
         LEFT JOIN whol_batch w ON w.up_batch_no = z.batch_no AND w.up_node_id = z.node_id
         LEFT JOIN node_info wn ON wn.node_id = w.node_id
         LEFT JOIN reta_batch r ON r.up_batch_no = w.batch_no AND r.up_node_id = w.node_id
         LEFT JOIN node_info rn ON rn.node_id = r.node_id
WHERE z.up_batch_no = 'FARM20260201'
ORDER BY z.batch_no;
