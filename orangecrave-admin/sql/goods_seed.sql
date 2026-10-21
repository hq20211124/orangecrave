-- ============================================================
-- goods 模块种子：1 系列 / 3 品类 / 2 SPU（按张 + 按面积）/ 运费模板 / 工艺 / 画师
-- ============================================================

INSERT INTO pms_designer (id, designer_name, intro, status) VALUES
    (1, '示例画师', 'v1 只做归属，不做分成', 1);

INSERT INTO pms_series (id, series_name, ip_name, sort, status) VALUES
    (1, '某番联名', '示例 IP', 1, 1);

INSERT INTO pms_category (id, category_name, parent_id, sort, status) VALUES
    (1, '小卡',   0, 1, 1),
    (2, '亚克力', 0, 2, 1),
    (3, '手机壳', 0, 3, 1);

-- 运费模板：基础邮费 4 元/单，满 28 元减 4 元（见 03 篇 §5.2）。续件费 0 表示整单固定 4 元。
INSERT INTO pms_freight_template (id, template_name, charge_type, first_unit, first_fee, extra_unit, extra_fee, free_threshold) VALUES
    (1, '默认运费模板', 1, 1, 4.00, 1, 0.00, 28.00);

INSERT INTO pms_craft (id, craft_name, craft_type, add_price, add_price_type, status) VALUES
    (1, '白墨', 'material', 2.00,  2, 1),
    (2, '烫金', 'process',  0.50,  1, 1),
    (3, '覆膜', 'process',  1.00,  2, 1);

-- SPU 1：小卡（按张计价）
INSERT INTO pms_spu (id, spu_name, sub_title, series_id, category_id, goods_type, pricing_type,
                     base_price, min_order_qty, increment_qty, freight_template_id, need_proof, status) VALUES
    (1, '定制小卡', '86×54mm 亮面', 1, 1, 1, 1, 2.00, 10, 10, 1, 1, 1);

INSERT INTO pms_sku (id, spu_id, sku_code, price, stock, weight, length_mm, width_mm, status) VALUES
    (1, 1, 'CARD-86x54', 2.00, 0, 5.00, 86, 54, 1);

-- SPU 2：亚克力相框（按面积计价）
INSERT INTO pms_spu (id, spu_name, sub_title, series_id, category_id, goods_type, pricing_type,
                     area_price, min_order_qty, increment_qty, freight_template_id, need_proof, status) VALUES
    (2, '亚克力相框', '透明亚克力', 1, 2, 1, 2, 300.00, 1, 1, 1, 0, 1);

INSERT INTO pms_sku (id, spu_id, sku_code, price, stock, weight, length_mm, width_mm, status) VALUES
    (2, 2, 'AC-100x150', 0.00, 0, 80.00, 100, 150, 1);

INSERT INTO pms_sku_craft (sku_id, craft_id) VALUES (1, 1), (1, 2), (2, 1), (2, 3);
