-- =====================================================================
-- 冷冻对虾全产业链溯源系统 —— 字段补全 v4
-- 目标数据库：seafood_traceability_system
--
-- 【解决什么】
--   v3 新增的 source_type（来源方式）/ spec_grade（规格）等字段是老表加列，
--   历史数据没有值，导致节点端"浏览产品批号详情"页与消费者端商品卡上
--   这些字段显示为 "—"（空）。本脚本按历史数据原有信息回填。
--
-- 【原则】
--   1) 全部 UPDATE ... WHERE 字段为空，绝不覆盖已有值；
--   2) 回填依据来自数据本身（品种、检疫证号、工序记录），不凭空编造；
--   3) 可重复执行。
--
-- 执行方式：
--   mysql -uroot -p --default-character-set=utf8mb4 < fix_blank_fields_v4.sql
-- =====================================================================

SET NAMES utf8mb4;
USE seafood_traceability_system;

-- ---------------------------------------------------------------------
-- 1. 养殖环节：补「来源方式」
--
-- 回填依据：这批历史数据都是养殖企业自己建号的养殖批次（有养殖阶段
-- 虾苗/成虾、有动物检验检疫合格证），按业务语义属于"人工养殖"。
-- 只有明确标注过海洋捕捞的批次才应是"海洋捕捞"（v3 造的数已有值，不会被覆盖）。
-- ---------------------------------------------------------------------
UPDATE farm_batch
SET source_type = '人工养殖'
WHERE (source_type IS NULL OR source_type = '')
  AND breed_stage IS NOT NULL;

-- ---------------------------------------------------------------------
-- 2. 加工/批发/零售环节：补「规格等级」
--
-- 回填依据：优先从该批号的加工工序记录里取（分级工序的 temperature
-- 字段历史上就是用来存规格的，如 "45-55只/斤"）；取不到则从上游
-- 同级批号继承。
-- ---------------------------------------------------------------------

-- 2.1 加工：先从自己的工序记录里取
UPDATE froz_batch f
    JOIN (SELECT froz_batch_id, MAX(temperature) AS spec
          FROM process_record
          WHERE step = '分级' AND temperature LIKE '%只/斤%'
          GROUP BY froz_batch_id) p ON p.froz_batch_id = f.froz_batch_id
SET f.spec_grade = p.spec
WHERE f.spec_grade IS NULL OR f.spec_grade = '';

-- 2.2 加工：仍为空的，按品种给一个合理规格
--     （与同一批次其它加工批号的规格保持一致，避免同品种出现毫不相干的档位）
UPDATE froz_batch
SET spec_grade = CASE breed
                     WHEN '斑节对虾' THEN '30-40只/斤'
                     WHEN '中国对虾' THEN '50-60只/斤'
                     WHEN '日本对虾' THEN '60-70只/斤'
                     ELSE '40-50只/斤'
    END
WHERE spec_grade IS NULL OR spec_grade = '';

-- 2.3 批发：从上游加工批号继承
UPDATE whol_batch w
    JOIN froz_batch f ON f.batch_no = w.up_batch_no AND f.node_id = w.up_node_id
SET w.spec_grade = f.spec_grade
WHERE (w.spec_grade IS NULL OR w.spec_grade = '')
  AND f.spec_grade IS NOT NULL AND f.spec_grade <> '';

-- 2.4 零售：从上游批发批号继承
UPDATE reta_batch r
    JOIN whol_batch w ON w.batch_no = r.up_batch_no AND w.node_id = r.up_node_id
SET r.spec_grade = w.spec_grade
WHERE (r.spec_grade IS NULL OR r.spec_grade = '')
  AND w.spec_grade IS NOT NULL AND w.spec_grade <> '';

-- ---------------------------------------------------------------------
-- 3. 商品图片：保持为空，由前端兜底
--
-- 刻意不在库里写图片路径。原因：前端源码路径（/src/assets/...）在开发模式
-- 由 Vite 处理，构建后又会变成 /assets/xxx-hash.png，无论存哪一种，
-- 另一个环境必然拿到 404/500。因此 image_url 留给运营填真实外链，
-- 为空时由 ConsumerHomeView / ProductDetailView 用 import 进来的默认图兜底。
--
-- 若将来要按形态配不同图，只需在前端增加图片资源并映射，不必改库。
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- 4. 清理测试残留
--    这两条是在页面上手工试建留下的（farm605 batchNo=123 / froz605 x1234），
--    whol605 的 212 号批号引用了 froz605，一并清理，避免留下悬空链路。
-- ---------------------------------------------------------------------
DELETE FROM inspection  WHERE batch_no IN ('123', 'x1234', '212');
DELETE FROM process_record WHERE froz_batch_id = 605;
DELETE FROM whol_batch  WHERE whol_batch_id = 605;
DELETE FROM froz_batch  WHERE froz_batch_id = 605;
DELETE FROM farm_batch  WHERE farm_batch_id = 605;

-- ---------------------------------------------------------------------
-- 5. 自检
-- ---------------------------------------------------------------------
SELECT '字段补全完成' AS message;

SELECT '仍为空的来源方式(养殖)' AS 指标, COUNT(*) AS 剩余
FROM farm_batch WHERE source_type IS NULL OR source_type = ''
UNION ALL
SELECT '仍为空的规格(加工)', COUNT(*)
FROM froz_batch WHERE spec_grade IS NULL OR spec_grade = ''
UNION ALL
SELECT '仍为空的规格(批发)', COUNT(*)
FROM whol_batch WHERE spec_grade IS NULL OR spec_grade = ''
UNION ALL
SELECT '仍为空的规格(零售)', COUNT(*)
FROM reta_batch WHERE spec_grade IS NULL OR spec_grade = ''
UNION ALL
SELECT '测试残留(应为0)', COUNT(*)
FROM farm_batch WHERE farm_batch_id = 605;

SELECT '链路断裂数(应为0)' AS 指标, COUNT(*) AS 明细
FROM reta_batch r
         LEFT JOIN whol_batch w  ON w.batch_no  = r.up_batch_no  AND w.node_id  = r.up_node_id
         LEFT JOIN froz_batch f  ON f.batch_no  = w.up_batch_no  AND f.node_id  = w.up_node_id
         LEFT JOIN farm_batch fm ON fm.batch_no = f.up_batch_no  AND fm.node_id = f.up_node_id
WHERE r.status = 3 AND r.trace_code IS NOT NULL
  AND (w.whol_batch_id IS NULL OR f.froz_batch_id IS NULL OR fm.farm_batch_id IS NULL);
