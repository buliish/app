-- =====================================================================
-- 冷冻对虾全产业链溯源系统 —— 演示数据 v3（以产品为中心的溯源改造配套）
-- 目标数据库：seafood_traceability_system
--
-- 【幂等】用 demo_data_seed 表守卫（与 demo_data_v2.sql 一致），
--         全部 INSERT IGNORE，主键显式指定并避开已用区间。
--         注意：本库有两张 seed 表，建表脚本用 sql_demo_seed，
--         demo_data_v2 起改用 demo_data_seed —— 本脚本跟后者。
--
-- 【主键区间】自增已用到 1~12，v2 用了 301/302，本脚本统一用 601 起，
--             避免 PK 撞车导致 INSERT IGNORE 静默跳过（最难排查的一类问题）。
--
-- 【要演示的四条链路】
--   A 冻虾 · 人工养殖 · 全环节合格   → 消费者可见，¥68.00
--   B 鲜虾 · 海洋捕捞 · 全环节合格   → 消费者可见，¥88.00（与 A 形成形态/来源对比）
--   C 冻虾 · 人工养殖 · 加工环节不合格 → 已下架，消费者看不到、管理端查得到
--   D 冻虾 · 人工养殖 · 待检         → 消费者可见，质量状态三态齐全
--
-- 【企业】复用已存在的 farm101/froz101/whol101/reta101（node_id 111/112/113/114），
--         这些是库里真实存在的编码。历史上曾有脚本引用 farm001/reta001/whol001
--         这类不存在的编码，导致 INSERT...SELECT 命中 0 行静默失败（见 product_center_v3.sql 第 5 节）。
--
-- 执行方式：
--   mysql -uroot -p --default-character-set=utf8mb4 < demo_data_v3.sql
-- =====================================================================

SET NAMES utf8mb4;
USE seafood_traceability_system;

-- 演示数据写入标记表（若建表脚本已建则跳过）
CREATE TABLE IF NOT EXISTS demo_data_seed (
    seed_key VARCHAR(50) NOT NULL COMMENT '演示数据批次标记',
    exec_time DATETIME DEFAULT NULL COMMENT '写入时间',
    PRIMARY KEY (seed_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演示数据写入标记表';

-- ---------------------------------------------------------------------
-- 1. 链路 A：冻虾 / 人工养殖 / 全环节合格（主链路，消费者端第一张卡）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO farm_batch
    (farm_batch_id, node_id, batch_no, breed, breed_stage, source_type, product_form, quality_status,
     quarantine_no, inspector, status, create_time, update_time)
SELECT 601, n.node_id, 'FARM20260801', '南美白对虾', '成虾', '人工养殖', '鲜虾', 1,
       '闽动检（2026）第0801号', '张伟', 2, '2026-08-02 08:30:00', '2026-08-03 09:00:00'
FROM node_info n WHERE n.code = 'farm101';

INSERT IGNORE INTO froz_batch
    (froz_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed,
     quarantine_no, inspector, product_type, product_form, spec_grade, quality_status, product_code,
     status, create_time, update_time)
SELECT 601, n.node_id, 'FROZ20260801', u.prov_id, u.city_id, u.node_id, 'FARM20260801', '南美白对虾',
       '闽品检（2026）第0801号', '陈静', '冷冻整虾', '冻虾', '40-50只/斤', 1,
       'SP20260804601', 3, '2026-08-04 09:00:00', '2026-08-05 10:00:00'
FROM node_info n, node_info u WHERE n.code = 'froz101' AND u.code = 'farm101';

INSERT IGNORE INTO whol_batch
    (whol_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type,
     product_form, spec_grade, quality_status, product_code, status, create_time, update_time)
SELECT 601, n.node_id, 'WHOL20260801', u.prov_id, u.city_id, u.node_id, 'FROZ20260801', '南美白对虾', '冷冻整虾',
       '冻虾', '40-50只/斤', 1, 'SP20260804601', 3, '2026-08-06 09:00:00', '2026-08-07 10:00:00'
FROM node_info n, node_info u WHERE n.code = 'whol101' AND u.code = 'froz101';

INSERT IGNORE INTO reta_batch
    (reta_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type,
     product_form, spec_grade, quality_status, product_code, price,
     status, trace_code, trace_time, create_time, update_time)
SELECT 601, n.node_id, 'RETA20260801', u.prov_id, u.city_id, u.node_id, 'WHOL20260801', '南美白对虾', '冷冻整虾',
       '冻虾', '40-50只/斤', 1, 'SP20260804601', 68.00,
       3, 'SHZ202608010001', '2026-08-08 10:00:00', '2026-08-08 09:00:00', '2026-08-08 10:00:00'
FROM node_info n, node_info u WHERE n.code = 'reta101' AND u.code = 'whol101';

-- 链路 A 的加工工序
INSERT IGNORE INTO process_record (record_id, froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT 601, 601, f.node_id, '清洗', '2026-08-04 09:30:00', '常温清水', '李工', '冲洗去除泥沙与杂质'
FROM froz_batch f WHERE f.froz_batch_id = 601;
INSERT IGNORE INTO process_record (record_id, froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT 602, 601, f.node_id, '分级', '2026-08-04 11:00:00', '40-50只/斤', '王工', '按规格分级筛选'
FROM froz_batch f WHERE f.froz_batch_id = 601;
INSERT IGNORE INTO process_record (record_id, froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT 603, 601, f.node_id, '冷冻', '2026-08-04 14:00:00', '-35℃', '张工', '速冻30分钟后转-18℃冷藏'
FROM froz_batch f WHERE f.froz_batch_id = 601;
INSERT IGNORE INTO process_record (record_id, froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT 604, 601, f.node_id, '包装', '2026-08-04 16:00:00', '-18℃', '赵工', '真空包装 5kg/箱'
FROM froz_batch f WHERE f.froz_batch_id = 601;

-- ---------------------------------------------------------------------
-- 2. 链路 B：鲜虾 / 海洋捕捞 / 全环节合格
--    与 A 形成"形态（鲜/冻）"与"来源（养殖/捕捞）"两组对比
-- ---------------------------------------------------------------------
INSERT IGNORE INTO farm_batch
    (farm_batch_id, node_id, batch_no, breed, breed_stage, source_type, product_form, quality_status,
     quarantine_no, inspector, status, create_time, update_time)
SELECT 602, n.node_id, 'FARM20260802', '斑节对虾', '成虾', '海洋捕捞', '鲜虾', 1,
       '闽动检（2026）第0802号', '张伟', 2, '2026-08-09 06:00:00', '2026-08-09 08:00:00'
FROM node_info n WHERE n.code = 'farm101';

INSERT IGNORE INTO froz_batch
    (froz_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed,
     quarantine_no, inspector, product_type, product_form, spec_grade, quality_status, product_code,
     status, create_time, update_time)
SELECT 602, n.node_id, 'FROZ20260802', u.prov_id, u.city_id, u.node_id, 'FARM20260802', '斑节对虾',
       '闽品检（2026）第0802号', '陈静', '冰鲜整虾', '鲜虾', '30-40只/斤', 1,
       'SP20260810602', 3, '2026-08-10 09:00:00', '2026-08-11 10:00:00'
FROM node_info n, node_info u WHERE n.code = 'froz101' AND u.code = 'farm101';

INSERT IGNORE INTO whol_batch
    (whol_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type,
     product_form, spec_grade, quality_status, product_code, status, create_time, update_time)
SELECT 602, n.node_id, 'WHOL20260802', u.prov_id, u.city_id, u.node_id, 'FROZ20260802', '斑节对虾', '冰鲜整虾',
       '鲜虾', '30-40只/斤', 1, 'SP20260810602', 3, '2026-08-11 09:00:00', '2026-08-12 10:00:00'
FROM node_info n, node_info u WHERE n.code = 'whol101' AND u.code = 'froz101';

INSERT IGNORE INTO reta_batch
    (reta_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type,
     product_form, spec_grade, quality_status, product_code, price,
     status, trace_code, trace_time, create_time, update_time)
SELECT 602, n.node_id, 'RETA20260802', u.prov_id, u.city_id, u.node_id, 'WHOL20260802', '斑节对虾', '冰鲜整虾',
       '鲜虾', '30-40只/斤', 1, 'SP20260810602', 88.00,
       3, 'SHZ202608020002', '2026-08-12 11:00:00', '2026-08-12 10:00:00', '2026-08-12 11:00:00'
FROM node_info n, node_info u WHERE n.code = 'reta101' AND u.code = 'whol101';

INSERT IGNORE INTO process_record (record_id, froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT 605, 602, f.node_id, '清洗', '2026-08-10 09:30:00', '冰水 2℃', '李工', '冰水冲洗保鲜'
FROM froz_batch f WHERE f.froz_batch_id = 602;
INSERT IGNORE INTO process_record (record_id, froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT 606, 602, f.node_id, '分级', '2026-08-10 10:30:00', '30-40只/斤', '王工', '按规格分级筛选'
FROM froz_batch f WHERE f.froz_batch_id = 602;
INSERT IGNORE INTO process_record (record_id, froz_batch_id, node_id, step, step_time, temperature, operator, remark)
SELECT 607, 602, f.node_id, '包装', '2026-08-10 12:00:00', '0-4℃', '赵工', '冰鲜装箱 2kg/箱'
FROM froz_batch f WHERE f.froz_batch_id = 602;

-- ---------------------------------------------------------------------
-- 3. 链路 C：冻虾 / 人工养殖 / 加工环节不合格 → 已下架
--    演示"消费者看不到、管理端查得到"，且保留 trace_code 以便按码检索
-- ---------------------------------------------------------------------
INSERT IGNORE INTO farm_batch
    (farm_batch_id, node_id, batch_no, breed, breed_stage, source_type, product_form, quality_status,
     quarantine_no, inspector, status, create_time, update_time)
SELECT 603, n.node_id, 'FARM20260803', '日本对虾', '成虾', '人工养殖', '鲜虾', 1,
       '闽动检（2026）第0803号', '张伟', 2, '2026-08-13 08:30:00', '2026-08-14 09:00:00'
FROM node_info n WHERE n.code = 'farm101';

INSERT IGNORE INTO froz_batch
    (froz_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed,
     quarantine_no, inspector, product_type, product_form, spec_grade, quality_status, product_code,
     status, create_time, update_time)
SELECT 603, n.node_id, 'FROZ20260803', u.prov_id, u.city_id, u.node_id, 'FARM20260803', '日本对虾',
       '闽品检（2026）第0803号', '陈静', '冷冻虾仁', '冻虾', '50-60只/斤', 2,
       'SP20260815603', 3, '2026-08-15 09:00:00', '2026-08-16 10:00:00'
FROM node_info n, node_info u WHERE n.code = 'froz101' AND u.code = 'farm101';

INSERT IGNORE INTO whol_batch
    (whol_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type,
     product_form, spec_grade, quality_status, product_code, status, create_time, update_time)
SELECT 603, n.node_id, 'WHOL20260803', u.prov_id, u.city_id, u.node_id, 'FROZ20260803', '日本对虾', '冷冻虾仁',
       '冻虾', '50-60只/斤', 2, 'SP20260815603', 3, '2026-08-16 09:00:00', '2026-08-17 10:00:00'
FROM node_info n, node_info u WHERE n.code = 'whol101' AND u.code = 'froz101';

INSERT IGNORE INTO reta_batch
    (reta_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type,
     product_form, spec_grade, quality_status, product_code, price,
     status, trace_code, trace_time, create_time, update_time)
SELECT 603, n.node_id, 'RETA20260803', u.prov_id, u.city_id, u.node_id, 'WHOL20260803', '日本对虾', '冷冻虾仁',
       '冻虾', '50-60只/斤', 2, 'SP20260815603', 58.00,
       4, 'SHZ202608030003', '2026-08-17 10:00:00', '2026-08-17 09:00:00', '2026-08-18 10:00:00'
FROM node_info n, node_info u WHERE n.code = 'reta101' AND u.code = 'whol101';

-- ---------------------------------------------------------------------
-- 4. 链路 D：冻虾 / 人工养殖 / 待检（把质量状态三态凑齐）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO farm_batch
    (farm_batch_id, node_id, batch_no, breed, breed_stage, source_type, product_form, quality_status,
     quarantine_no, inspector, status, create_time, update_time)
SELECT 604, n.node_id, 'FARM20260804', '中国对虾', '虾苗', '人工养殖', '鲜虾', 0,
       '闽动检（2026）第0804号', '张伟', 2, '2026-08-19 08:30:00', '2026-08-20 09:00:00'
FROM node_info n WHERE n.code = 'farm101';

INSERT IGNORE INTO froz_batch
    (froz_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed,
     quarantine_no, inspector, product_type, product_form, spec_grade, quality_status, product_code,
     status, create_time, update_time)
SELECT 604, n.node_id, 'FROZ20260804', u.prov_id, u.city_id, u.node_id, 'FARM20260804', '中国对虾',
       '闽品检（2026）第0804号', '陈静', '虾滑', '冻虾', '60-70只/斤', 0,
       'SP20260821604', 3, '2026-08-21 09:00:00', '2026-08-22 10:00:00'
FROM node_info n, node_info u WHERE n.code = 'froz101' AND u.code = 'farm101';

INSERT IGNORE INTO whol_batch
    (whol_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type,
     product_form, spec_grade, quality_status, product_code, status, create_time, update_time)
SELECT 604, n.node_id, 'WHOL20260804', u.prov_id, u.city_id, u.node_id, 'FROZ20260804', '中国对虾', '虾滑',
       '冻虾', '60-70只/斤', 0, 'SP20260821604', 3, '2026-08-22 09:00:00', '2026-08-23 10:00:00'
FROM node_info n, node_info u WHERE n.code = 'whol101' AND u.code = 'froz101';

INSERT IGNORE INTO reta_batch
    (reta_batch_id, node_id, batch_no, prov_id, city_id, up_node_id, up_batch_no, breed, product_type,
     product_form, spec_grade, quality_status, product_code, price,
     status, trace_code, trace_time, create_time, update_time)
SELECT 604, n.node_id, 'RETA20260804', u.prov_id, u.city_id, u.node_id, 'WHOL20260804', '中国对虾', '虾滑',
       '冻虾', '60-70只/斤', 0, 'SP20260821604', 45.00,
       3, 'SHZ202608040004', '2026-08-24 10:00:00', '2026-08-24 09:00:00', '2026-08-25 10:00:00'
FROM node_info n, node_info u WHERE n.code = 'reta101' AND u.code = 'whol101';

-- ---------------------------------------------------------------------
-- 5. 检测记录
--    每环节一份报告、若干检测项。链路 C 的加工环节刻意含不合格项，
--    用于演示"不合格 → 整批判不合格 → 下游连带 → 最终下架"。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO inspection
    (inspection_id, node_id, stage_type, batch_id, batch_no, item_name, item_value, standard_value,
     result, conclusion, report_no, org_name, inspector, inspect_date, remark, create_time)
VALUES
-- 链路 A：养殖
(601, 111, 1, 601, 'FARM20260801', '感官', '色泽正常、无异味', '符合 GB 2733', 1, 1, 'SNJC-2026-0801', '福建省水产品质量检验中心', '林检', '2026-08-03', NULL, '2026-08-03 10:00:00'),
(602, 111, 1, 601, 'FARM20260801', '氯霉素', '未检出', '不得检出', 1, 1, 'SNJC-2026-0801', '福建省水产品质量检验中心', '林检', '2026-08-03', NULL, '2026-08-03 10:00:00'),
(603, 111, 1, 601, 'FARM20260801', '重金属镉', '0.08 mg/kg', '≤0.5 mg/kg', 1, 1, 'SNJC-2026-0801', '福建省水产品质量检验中心', '林检', '2026-08-03', NULL, '2026-08-03 10:00:00'),
-- 链路 A：加工
(604, 112, 2, 601, 'FROZ20260801', '菌落总数', '2.1×10^4 CFU/g', '≤1×10^5 CFU/g', 1, 1, 'JGJC-2026-0801', '福州市食品检验所', '黄检', '2026-08-05', NULL, '2026-08-05 11:00:00'),
(605, 112, 2, 601, 'FROZ20260801', '大肠菌群', '未检出', '≤100 MPN/100g', 1, 1, 'JGJC-2026-0801', '福州市食品检验所', '黄检', '2026-08-05', NULL, '2026-08-05 11:00:00'),
(606, 112, 2, 601, 'FROZ20260801', '水分', '76.5%', '≤80%', 1, 1, 'JGJC-2026-0801', '福州市食品检验所', '黄检', '2026-08-05', NULL, '2026-08-05 11:00:00'),
-- 链路 A：零售
(607, 114, 4, 601, 'RETA20260801', '感官', '包装完好、无解冻迹象', '符合标准', 1, 1, 'SJJC-2026-0801', '厦门市食品药品检验所', '吴检', '2026-08-08', NULL, '2026-08-08 11:00:00'),
(608, 114, 4, 601, 'RETA20260801', '净含量', '5.02 kg', '≥5 kg', 1, 1, 'SJJC-2026-0801', '厦门市食品药品检验所', '吴检', '2026-08-08', NULL, '2026-08-08 11:00:00'),

-- 链路 B：养殖（来源为海洋捕捞）
(611, 111, 1, 602, 'FARM20260802', '感官', '鲜活、体表完整', '符合 GB 2733', 1, 1, 'SNJC-2026-0802', '福建省水产品质量检验中心', '林检', '2026-08-09', '海洋捕捞批次', '2026-08-09 09:00:00'),
(612, 111, 1, 602, 'FARM20260802', '重金属镉', '0.11 mg/kg', '≤0.5 mg/kg', 1, 1, 'SNJC-2026-0802', '福建省水产品质量检验中心', '林检', '2026-08-09', NULL, '2026-08-09 09:00:00'),
-- 链路 B：加工
(613, 112, 2, 602, 'FROZ20260802', '菌落总数', '1.4×10^4 CFU/g', '≤1×10^5 CFU/g', 1, 1, 'JGJC-2026-0802', '福州市食品检验所', '黄检', '2026-08-10', NULL, '2026-08-10 11:00:00'),
(614, 112, 2, 602, 'FROZ20260802', '挥发性盐基氮', '8.2 mg/100g', '≤15 mg/100g', 1, 1, 'JGJC-2026-0802', '福州市食品检验所', '黄检', '2026-08-10', '鲜度指标', '2026-08-10 11:00:00'),
-- 链路 B：零售
(615, 114, 4, 602, 'RETA20260802', '感官', '冰鲜状态良好', '符合标准', 1, 1, 'SJJC-2026-0802', '厦门市食品药品检验所', '吴检', '2026-08-12', NULL, '2026-08-12 11:00:00'),

-- 链路 C：加工环节——菌落总数超标，整批判不合格
(621, 111, 1, 603, 'FARM20260803', '感官', '色泽正常', '符合 GB 2733', 1, 1, 'SNJC-2026-0803', '福建省水产品质量检验中心', '林检', '2026-08-14', NULL, '2026-08-14 10:00:00'),
(622, 112, 2, 603, 'FROZ20260803', '菌落总数', '3.8×10^5 CFU/g', '≤1×10^5 CFU/g', 2, 2, 'JGJC-2026-0803', '福州市食品检验所', '黄检', '2026-08-16', '菌落总数超标，判定不合格', '2026-08-16 11:00:00'),
(623, 112, 2, 603, 'FROZ20260803', '大肠菌群', '430 MPN/100g', '≤100 MPN/100g', 2, 2, 'JGJC-2026-0803', '福州市食品检验所', '黄检', '2026-08-16', '大肠菌群超标', '2026-08-16 11:00:00'),
(624, 112, 2, 603, 'FROZ20260803', '感官', '轻微异味', '符合标准', 2, 2, 'JGJC-2026-0803', '福州市食品检验所', '黄检', '2026-08-16', NULL, '2026-08-16 11:00:00'),

-- 链路 D：待检（只有养殖环节送了检，其余环节尚无记录）
(631, 111, 1, 604, 'FARM20260804', '感官', '色泽正常', '符合 GB 2733', 1, 1, 'SNJC-2026-0804', '福建省水产品质量检验中心', '林检', '2026-08-20', NULL, '2026-08-20 10:00:00');

-- ---------------------------------------------------------------------
-- 6. 写入标记
-- ---------------------------------------------------------------------
INSERT IGNORE INTO demo_data_seed (seed_key, exec_time) VALUES ('product_center_v3', NOW());

-- ---------------------------------------------------------------------
-- 7. 自检
-- ---------------------------------------------------------------------
SELECT '演示数据 v3 写入完成' AS message;

SELECT r.trace_code AS 溯源码, r.product_code AS 产品编号, r.product_form AS 形态,
       fm.source_type AS 来源, r.spec_grade AS 规格, r.quality_status AS 质量,
       r.status AS 工作流状态, r.price AS 价格
FROM reta_batch r
         LEFT JOIN whol_batch w  ON w.batch_no  = r.up_batch_no  AND w.node_id  = r.up_node_id
         LEFT JOIN froz_batch f  ON f.batch_no  = w.up_batch_no  AND f.node_id  = w.up_node_id
         LEFT JOIN farm_batch fm ON fm.batch_no = f.up_batch_no  AND fm.node_id = f.up_node_id
WHERE r.reta_batch_id BETWEEN 601 AND 604
ORDER BY r.reta_batch_id;

SELECT '各质量状态数量' AS 指标, CONCAT('待检=', SUM(quality_status = 0), ' 合格=', SUM(quality_status = 1),
       ' 不合格=', SUM(quality_status = 2)) AS 明细
FROM reta_batch;

SELECT '链路断裂数(应为0)' AS 指标, COUNT(*) AS 明细
FROM reta_batch r
         LEFT JOIN whol_batch w  ON w.batch_no  = r.up_batch_no  AND w.node_id  = r.up_node_id
         LEFT JOIN froz_batch f  ON f.batch_no  = w.up_batch_no  AND f.node_id  = w.up_node_id
         LEFT JOIN farm_batch fm ON fm.batch_no = f.up_batch_no  AND fm.node_id = f.up_node_id
WHERE r.status = 3 AND r.trace_code IS NOT NULL
  AND (w.whol_batch_id IS NULL OR f.froz_batch_id IS NULL OR fm.farm_batch_id IS NULL);
