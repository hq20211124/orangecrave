-- ============================================================
-- 橙刻定制 · 剧情演示数据（demo.sql）
--
-- 一个真实运营的四周：从小程序开张到第一周发货、第一次售后、
-- 第一次秒杀、第一次侵权投诉。按业务发生顺序演绎整个系统。
--
-- 使用方法：
--   1. 已执行全部 *_schema.sql 和 *_seed.sql（结构+基础配置）
--   2. 直接 source 本文件
--   3. 登录后台，按左侧菜单从上往下走一遍，每个页面都有对应数据
--
-- 所有数据均为虚构，但分布、时间、金额、备注均按真实业务规律设计。
-- ============================================================
SET NAMES utf8mb4;

-- 清理之前的演示数据（幂等，可重复执行）
-- 保留 *_seed.sql 里的基础配置（sys_*、pms_品类/SPU/SKU/工艺、aud_sensitive_word）
DELETE FROM ums_user;
DELETE FROM ums_identity_cert;
DELETE FROM ums_address;
DELETE FROM wms_work;
DELETE FROM wms_work_image;
DELETE FROM wms_share_code;
DELETE FROM wms_collection;
DELETE FROM wms_collection_item;
DELETE FROM aud_image_audit;
DELETE FROM aud_copyright_promise;
DELETE FROM mkt_activity;
DELETE FROM mkt_coupon;
DELETE FROM mkt_coupon_user;
DELETE FROM mkt_group;
DELETE FROM mkt_group_member;
DELETE FROM mkt_invite;
DELETE FROM oms_order;
DELETE FROM oms_order_item;
DELETE FROM oms_order_image;
DELETE FROM oms_order_status_log;
DELETE FROM oms_delivery;
DELETE FROM oms_aftersale;
DELETE FROM oms_invoice;
DELETE FROM svc_ticket;
DELETE FROM svc_ticket_reply;
DELETE FROM fin_refund_ledger;
DELETE FROM fin_daily_settlement;
DELETE FROM aud_blacklist;
DELETE FROM tsk_delay_log;
DELETE FROM pms_template;
DELETE FROM pms_sku_craft WHERE sku_id IN (3, 4);
DELETE FROM pms_sku WHERE id IN (3, 4);
DELETE FROM pms_spu WHERE id IN (3, 4);

-- ============================================================
-- 第零幕：补齐商品目录
-- ============================================================
-- *_seed.sql 只建了「定制小卡」「亚克力相框」两个 SPU（v1 的最小目录）。
-- 下面的剧情还要卖手机壳和亚克力钥匙扣，先把这两个商品补进来，
-- 否则订单、作品、品类排行都会指向不存在的 SPU（JOIN 一丢，数据就"消失"了）。
INSERT INTO pms_spu (id, spu_name, sub_title, cover, series_id, category_id, goods_type, pricing_type,
                     base_price, area_price, min_order_qty, increment_qty, freight_template_id, need_proof, status, create_time, update_time)
VALUES
(3, '定制手机壳', 'iPhone 15 系列 · 透明壳', 'https://api.trae.cn/mock/img/spu-case.jpg', 1, 3, 1, 1,
 39.00, 0.00, 1, 1, 1, 1, 1, '2026-10-14 10:00:00', '2026-10-14 10:00:00'),
(4, '亚克力钥匙扣', '5cm 双层透明', 'https://api.trae.cn/mock/img/spu-key.jpg', 1, 2, 1, 1,
 9.90, 0.00, 1, 1, 1, 0, 1, '2026-10-14 10:00:00', '2026-10-14 10:00:00');

INSERT INTO pms_sku (id, spu_id, sku_code, price, stock, weight, length_mm, width_mm, status, create_time, update_time)
VALUES
(3, 3, 'CASE-IP15-CL', 39.00, 0, 30.00, 150, 75, 1, '2026-10-14 10:00:00', '2026-10-14 10:00:00'),
(4, 4, 'KEY-5CM', 9.90, 0, 8.00, 50, 50, 1, '2026-10-14 10:00:00', '2026-10-14 10:00:00');

INSERT INTO pms_sku_craft (sku_id, craft_id) VALUES (3, 1), (4, 1), (4, 3);

-- ============================================================
-- 第一幕：开张前夜（管理员准备）
-- ============================================================
-- 周一早上，运营在后台建了一个新人专享券，一张满50减5的小卡券。
-- 同时开了第一场秒杀：10月16日晚上8点，亚克力钥匙扣 9.9 元抢，限量 50 个。
INSERT INTO mkt_activity (id, activity_name, activity_type, spu_ids, sku_ids, start_time, end_time, rule_config, status, total_stock, sold_stock, create_time, update_time)
VALUES (1, '秒杀-亚克力钥匙扣', 2, JSON_ARRAY(4), JSON_ARRAY(4), '2026-10-16 20:00:00', '2026-10-16 21:00:00', JSON_OBJECT('seckill_price', 9.90), 2, 50, 3, '2026-10-15 14:20:00', '2026-10-16 21:00:05');

-- 同一周还挂着两场长期活动：邀请好友（一直进行）+ 亚克力相框秒杀（10/17 起）
INSERT INTO mkt_activity (id, activity_name, activity_type, spu_ids, sku_ids, start_time, end_time, rule_config, status, total_stock, sold_stock, create_time, update_time)
VALUES
(2, '邀请好友-双方各得 3 元券', 4, NULL, NULL, '2026-10-15 00:00:00', '2026-11-15 23:59:59',
 JSON_OBJECT('inviter_reward', 3.00, 'invitee_reward', 3.00, 'valid_days', 7), 1, NULL, 0, '2026-10-14 19:00:00', '2026-10-14 19:00:00'),
(3, '秒杀-亚克力相框 7 折', 2, JSON_ARRAY(2), JSON_ARRAY(2), '2026-10-17 00:00:00', '2026-10-23 23:59:59',
 JSON_OBJECT('seckill_price', 210.00), 1, 30, 0, '2026-10-16 22:10:00', '2026-10-16 22:10:00');

INSERT INTO mkt_coupon (id, activity_id, coupon_name, coupon_type, threshold, amount, valid_days, total_qty, issued_qty, create_time, update_time)
VALUES (1, NULL, '新人专享·满50减5', 1, 50.00, 5.00, 30, 200, 12, '2026-10-15 14:30:00', '2026-10-15 14:30:00');

-- ============================================================
-- 第二幕：第一波用户（15日晚上-16日白天）
-- ============================================================
-- 第一批 12 个用户，有真名有昵称有头像，注册时间从晚上8点到凌晨2点，
-- 符合真实"下班逛小程序"的节奏。其中 2 个是团长，准备开团购单。
INSERT INTO ums_user (id, openid, nickname, avatar, phone, identity_type, cert_status, create_time, update_time)
VALUES
(1, 'wx_mock_001', '用户8647', 'https://api.trae.cn/mock/avatar/1.png', '13800138001', 0, 0, '2026-10-15 20:14:22', '2026-10-15 20:14:22'),
(2, 'wx_mock_002', '奶茶三分糖', 'https://api.trae.cn/mock/avatar/2.png', '13800138002', 0, 0, '2026-10-15 20:47:55', '2026-10-15 20:47:55'),
(3, 'wx_mock_003', 'wx_4f8a2b1c', NULL, NULL, 0, 0, '2026-10-15 21:03:11', '2026-10-15 21:03:11'),
(4, 'wx_mock_004', '莉莉安的小卖部', 'https://api.trae.cn/mock/avatar/4.png', '13800138004', 2, 1, '2026-10-15 21:22:48', '2026-10-15 21:22:48'),
(5, 'wx_mock_005', '用户1314', 'https://api.trae.cn/mock/avatar/5.png', '13800138005', 0, 0, '2026-10-15 22:05:30', '2026-10-15 22:05:30'),
(6, 'wx_mock_006', '阿木', NULL, NULL, 0, 0, '2026-10-15 22:31:07', '2026-10-15 22:31:07'),
(7, 'wx_mock_007', '华南金牌应援', 'https://api.trae.cn/mock/avatar/7.png', '13800138007', 1, 2, '2026-10-15 22:58:44', '2026-10-16 09:15:00'),
(8, 'wx_mock_008', '纸飞机工作室', 'https://api.trae.cn/mock/avatar/8.png', '13800138008', 1, 3, '2026-10-16 09:42:18', '2026-10-16 10:02:00'),
(9, 'wx_mock_009', '用户wx_2a9f', NULL, NULL, 0, 0, '2026-10-16 12:35:20', '2026-10-16 12:35:20'),
(10, 'wx_mock_010', 'Yuki酱', 'https://api.trae.cn/mock/avatar/10.png', '13800138010', 0, 0, '2026-10-16 13:18:06', '2026-10-16 13:18:06'),
(11, 'wx_mock_011', '摆摊的老王', 'https://api.trae.cn/mock/avatar/11.png', '13800138011', 2, 2, '2026-10-16 14:05:33', '2026-10-16 14:30:00'),
(12, 'wx_mock_012', '用户9527', NULL, NULL, 0, 0, '2026-10-16 18:47:50', '2026-10-16 18:47:50');

-- 7号（团长）和11号（摊主）的实名认证已通过；4号刚提交待审，8号材料不符被驳回
INSERT INTO ums_identity_cert (id, user_id, identity_type, real_name, id_card_no, material_urls, status, audit_user_id, audit_time, audit_remark, create_time, update_time)
VALUES
(1, 7, 1, '陈晓燕', '440300**********24', JSON_ARRAY('https://api.trae.cn/mock/cert/7-1.jpg'), 2, 1, '2026-10-16 09:15:00', '手持身份证清晰，通过', '2026-10-16 08:55:00', '2026-10-16 09:15:00'),
(2, 11, 2, '王建国', '440300**********37', JSON_ARRAY('https://api.trae.cn/mock/cert/11-1.jpg'), 2, 1, '2026-10-16 14:30:00', '摊位照片+身份证通过', '2026-10-16 14:10:00', '2026-10-16 14:30:00'),
(3, 4, 2, '李莉', '440300**********45', JSON_ARRAY('https://api.trae.cn/mock/cert/4-1.jpg'), 1, NULL, NULL, NULL, '2026-10-22 08:40:00', '2026-10-22 08:40:00'),
(4, 8, 1, '周航', '440300**********18', JSON_ARRAY('https://api.trae.cn/mock/cert/8-1.jpg'), 3, 1, '2026-10-21 16:10:00', '营业执照与店铺名称不一致，请补充授权书', '2026-10-21 15:30:00', '2026-10-21 16:10:00');

-- 新人券发出 12 张（注册即领，但只有部分人会用）
INSERT INTO mkt_coupon_user (coupon_id, user_id, code, status, expire_time, source, create_time)
SELECT 1, id, CONCAT('NC', LPAD(id, 6, '0')), 1, '2026-11-20 23:59:59', 3, '2026-10-15 14:35:00'
FROM ums_user WHERE id <= 12;

-- ============================================================
-- 第三幕：第一周订单（16日-20日）
-- ============================================================
-- 订单时间严格按状态机推进：待付款 -> 已支付 -> 待审核 -> 审核通过
-- -> 生产中 -> 待发货 -> 已发货 -> 已完成 / 售后
-- 备注里有人间烟火："急！25号演唱会用"、"上次那个图再来一单"

-- 订单 1：1号用户 16日晚 8 点抢秒杀（亚克力钥匙扣 9.9）+ 顺带买了 5 张小卡
-- 用了一张新人券，实付 54.5
INSERT INTO oms_order (id, order_no, user_id, order_type, status, total_amount, freight_amount, discount_amount, pay_amount, coupon_id, pay_time, pay_type, transaction_id, receiver_snapshot, remark, create_time, update_time)
VALUES (1, 'OC20261016200001', 1, 1, 8, 56.00, 0.00, 5.00, 51.00, 1, '2026-10-16 20:00:08', 1, '42000020261016200001',
        JSON_OBJECT('name','陈小姐','phone','13800138001','address','深圳市南山区科技园南区R2-B栋 放前台'),
        '急！25号演唱会用', '2026-10-16 19:59:45', '2026-10-16 20:00:10');

INSERT INTO oms_order_item (id, order_id, order_no, spu_id, sku_id, goods_name, sku_spec, work_id, activity_id, qty, unit_price, total_price, create_time)
VALUES
(1, 1, 'OC20261016200001', 4, 4, '亚克力钥匙扣', '5cm 双层透明', NULL, 1, 1, 9.90, 9.90, '2026-10-16 20:00:10'),
(2, 1, 'OC20261016200001', 1, 1, '定制小卡', '哑面', 1, NULL, 5, 9.22, 46.10, '2026-10-16 20:00:10');

INSERT INTO oms_order_image (order_item_id, slot_index, image_url, copies, create_time)
VALUES (2, 0, 'https://api.trae.cn/mock/img/1-1.jpg', 5, '2026-10-16 20:00:10');

-- 订单 1 的状态流：下单->支付->审核->生产->发货->完成
INSERT INTO oms_order_status_log (order_id, from_status, to_status, operator_type, remark, create_time)
VALUES
(1, 0, 2, 'user', '微信支付', '2026-10-16 20:00:08'),
(1, 2, 3, 'system', '自动过审（纯文字+无敏感图）', '2026-10-16 20:00:30'),
(1, 3, 4, 'system', 'MES 接单', '2026-10-16 20:05:00'),
(1, 4, 5, 'factory', '印刷完成，转质检', '2026-10-17 15:10:00'),
(1, 5, 6, 'factory', '质检通过，待发货', '2026-10-17 15:30:00'),
(1, 6, 7, 'admin', '中通 731025896314', '2026-10-17 16:20:00'),
(1, 7, 8, 'user', '确认收货', '2026-10-20 10:15:00');

INSERT INTO oms_delivery (order_id, delivery_company, delivery_no, delivery_time, receive_time, status, create_time)
VALUES (1, '中通快递', '731025896314', '2026-10-17 16:20:00', '2026-10-20 10:15:00', 2, '2026-10-17 16:20:00');

-- 新人券已使用
UPDATE mkt_coupon_user SET status=2, use_time='2026-10-16 20:00:08', order_id=1 WHERE coupon_id=1 AND user_id=1;

-- 订单 2：4号用户（莉莉安的小卖部）17日凌晨下单 20 张小卡（副业进货）
-- 没有用券，付了全款 184
INSERT INTO oms_order (id, order_no, user_id, order_type, status, total_amount, freight_amount, discount_amount, pay_amount, pay_time, pay_type, transaction_id, receiver_snapshot, remark, create_time, update_time)
VALUES (2, 'OC20261017231502', 4, 1, 8, 184.00, 0.00, 0.00, 184.00, '2026-10-17 23:15:30', 1, '42000020261017231502',
        JSON_OBJECT('name','莉莉安','phone','13800138004','address','广州市天河区棠下村北社大街 7 号 小卖部'),
        NULL, '2026-10-17 23:12:10', '2026-10-17 23:15:35');

INSERT INTO oms_order_item (id, order_id, order_no, spu_id, sku_id, goods_name, sku_spec, work_id, qty, unit_price, total_price, create_time)
VALUES (3, 2, 'OC20261017231502', 1, 1, '定制小卡', '哑面', 2, 20, 9.20, 184.00, '2026-10-17 23:15:35');

INSERT INTO oms_order_image (order_item_id, slot_index, image_url, copies, create_time)
VALUES (3, 0, 'https://api.trae.cn/mock/img/4-1.jpg', 20, '2026-10-17 23:15:35');

INSERT INTO oms_order_status_log (order_id, from_status, to_status, operator_type, remark, create_time)
VALUES
(2, 0, 2, 'user', '微信支付', '2026-10-17 23:15:30'),
(2, 2, 3, 'system', '自动过审', '2026-10-17 23:16:00'),
(2, 3, 4, 'system', 'MES 接单', '2026-10-17 23:20:00'),
(2, 4, 5, 'factory', '印刷完成，转质检', '2026-10-18 18:20:00'),
(2, 5, 6, 'factory', '质检通过，待发货', '2026-10-18 18:45:00'),
(2, 6, 7, 'admin', '圆通 YT102584796312', '2026-10-18 19:30:00'),
(2, 7, 8, 'user', '确认收货', '2026-10-21 14:22:00');

INSERT INTO oms_delivery (order_id, delivery_company, delivery_no, delivery_time, receive_time, status, create_time)
VALUES (2, '圆通速递', 'YT102584796312', '2026-10-18 19:30:00', '2026-10-21 14:22:00', 2, '2026-10-18 19:30:00');

-- 订单 3：7号团长（华南金牌应援）18日上午开团，300 张小卡团购
-- 团购价 8.5/张，团员分摊，团长不垫资。团 ID 挂到订单上。
INSERT INTO mkt_group (id, activity_id, leader_user_id, target_size, current_size, status, expire_time, create_time, update_time)
VALUES (1, NULL, 7, 300, 300, 2, '2026-10-19 10:00:00', '2026-10-18 10:00:00', '2026-10-19 09:00:00');

INSERT INTO oms_order (id, order_no, user_id, order_type, status, group_id, total_amount, freight_amount, discount_amount, pay_amount, pay_time, pay_type, transaction_id, receiver_snapshot, remark, create_time, update_time)
VALUES (3, 'OC20261018103003', 7, 1, 8, 1, 2550.00, 0.00, 0.00, 2550.00, '2026-10-18 10:30:22', 1, '42000020261018103003',
        JSON_OBJECT('name','陈晓燕','phone','13800138007','address','深圳市福田区车公庙泰然大厦 2103'),
        '300 张，顺丰到付', '2026-10-18 10:28:00', '2026-10-18 10:30:25');

INSERT INTO oms_order_item (id, order_id, order_no, spu_id, sku_id, goods_name, sku_spec, work_id, qty, unit_price, total_price, create_time)
VALUES (4, 3, 'OC20261018103003', 1, 1, '定制小卡', '哑面', 3, 300, 8.50, 2550.00, '2026-10-18 10:30:25');

INSERT INTO oms_order_image (order_item_id, slot_index, image_url, copies, create_time)
VALUES (4, 0, 'https://api.trae.cn/mock/img/7-1.jpg', 300, '2026-10-18 10:30:25');

INSERT INTO oms_order_status_log (order_id, from_status, to_status, operator_type, remark, create_time)
VALUES
(3, 0, 2, 'user', '微信支付', '2026-10-18 10:30:22'),
(3, 2, 3, 'admin', '人工审核：图稿清晰，但文字含艺人名，已电话确认授权', '2026-10-18 11:05:00'),
(3, 3, 4, 'system', 'MES 接单', '2026-10-18 11:10:00'),
(3, 4, 5, 'factory', '300 张印刷完成，抽检', '2026-10-19 19:30:00'),
(3, 5, 6, 'factory', '质检通过，待发货', '2026-10-19 20:00:00'),
(3, 6, 7, 'admin', '顺丰 SF102584796318', '2026-10-19 21:00:00'),
(3, 7, 8, 'user', '确认收货', '2026-10-21 16:00:00');

INSERT INTO oms_delivery (order_id, delivery_company, delivery_no, delivery_time, receive_time, status, create_time)
VALUES (3, '顺丰速运', 'SF102584796318', '2026-10-19 21:00:00', '2026-10-21 16:00:00', 2, '2026-10-19 21:00:00');

-- 订单 10：2号用户（奶茶三分糖）19 日下午 13:50 下单 1 个手机壳，图拍糊了
-- 审核驳回 → 订单取消（系统里驳回即取消，用户需重新下单，不支持原地改图）
INSERT INTO oms_order (id, order_no, user_id, order_type, status, total_amount, freight_amount, discount_amount, pay_amount, pay_time, pay_type, transaction_id, receiver_snapshot, remark, create_time, update_time)
VALUES (10, 'OC20261019135010', 2, 1, 9, 39.00, 0.00, 0.00, 39.00, '2026-10-19 13:50:10', 1, '42000020261019135010',
        JSON_OBJECT('name','奶茶','phone','13800138002','address','深圳市宝安区西乡街道盐田社区 12 栋 501'),
        NULL, '2026-10-19 13:48:30', '2026-10-19 14:35:00');

INSERT INTO oms_order_item (id, order_id, order_no, spu_id, sku_id, goods_name, sku_spec, work_id, qty, unit_price, total_price, create_time)
VALUES (12, 10, 'OC20261019135010', 3, 3, '定制手机壳', 'iPhone 15 透明壳', NULL, 1, 39.00, 39.00, '2026-10-19 13:50:10');

INSERT INTO oms_order_image (order_item_id, slot_index, image_url, copies, create_time)
VALUES (12, 0, 'https://api.trae.cn/mock/img/2-1.jpg', 1, '2026-10-19 13:50:10');

-- 审核记录：第一单图糊被驳回，重新拍高清图后才有了订单 4
INSERT INTO aud_image_audit (biz_type, biz_id, image_url, auto_result, auto_remark, manual_result, audit_user_id, audit_time, audit_remark, create_time)
VALUES
(1, 10, 'https://api.trae.cn/mock/img/2-1.jpg', 2, '图片模糊，无法识别文字', 2, 1, '2026-10-19 14:35:00', '图糊了，请重新拍高清原图', '2026-10-19 13:52:00'),
(1, 4, 'https://api.trae.cn/mock/img/2-2.jpg', 1, '通过', 1, 1, '2026-10-19 15:05:00', '清晰，通过', '2026-10-19 15:03:00');

INSERT INTO oms_order_status_log (order_id, from_status, to_status, operator_type, remark, create_time)
VALUES
(10, 0, 2, 'user', '微信支付', '2026-10-19 13:50:10'),
(10, 2, 9, 'admin', '审核驳回：图糊了，订单取消并退款', '2026-10-19 14:35:00');

-- 订单 4：同一用户 14:58 换高清图重新下单，已过审进车间（生产中）
INSERT INTO oms_order (id, order_no, user_id, order_type, status, total_amount, freight_amount, discount_amount, pay_amount, pay_time, pay_type, transaction_id, receiver_snapshot, remark, create_time, update_time)
VALUES (4, 'OC20261019150204', 2, 1, 4, 39.00, 0.00, 0.00, 39.00, '2026-10-19 15:02:10', 1, '42000020261019150204',
        JSON_OBJECT('name','奶茶','phone','13800138002','address','深圳市宝安区西乡街道盐田社区 12 栋 501'),
        '重拍了一次，这次应该清楚了', '2026-10-19 14:58:30', '2026-10-19 15:10:00');

INSERT INTO oms_order_item (id, order_id, order_no, spu_id, sku_id, goods_name, sku_spec, work_id, qty, unit_price, total_price, create_time)
VALUES (5, 4, 'OC20261019150204', 3, 3, '定制手机壳', 'iPhone 15 透明壳', 4, 1, 39.00, 39.00, '2026-10-19 15:02:10');

INSERT INTO oms_order_image (order_item_id, slot_index, image_url, copies, create_time)
VALUES (5, 0, 'https://api.trae.cn/mock/img/2-2.jpg', 1, '2026-10-19 15:02:10');

INSERT INTO oms_order_status_log (order_id, from_status, to_status, operator_type, remark, create_time)
VALUES
(4, 0, 2, 'user', '微信支付', '2026-10-19 15:02:10'),
(4, 2, 3, 'admin', '审核通过', '2026-10-19 15:05:00'),
(4, 3, 4, 'system', 'MES 接单', '2026-10-19 15:10:00');

-- 订单 5：5号用户 20 日晚 9 点下单 10 张小卡，一直没付款，已超时关闭
INSERT INTO oms_order (id, order_no, user_id, order_type, status, total_amount, freight_amount, discount_amount, pay_amount, expire_time, receiver_snapshot, remark, create_time, update_time)
VALUES (5, 'OC20261020210005', 5, 1, 9, 92.00, 0.00, 0.00, 92.00, '2026-10-20 21:30:00',
        JSON_OBJECT('name','用户1314','phone','13800138005','address','广州市白云区嘉禾望岗 地铁站A口 放菜鸟驿站'),
        NULL, '2026-10-20 21:00:00', '2026-10-20 21:35:00');

INSERT INTO oms_order_item (id, order_id, order_no, spu_id, sku_id, goods_name, sku_spec, work_id, qty, unit_price, total_price, create_time)
VALUES (6, 5, 'OC20261020210005', 1, 1, '定制小卡', '哑面', NULL, 10, 9.20, 92.00, '2026-10-20 21:00:00');

INSERT INTO oms_order_status_log (order_id, from_status, to_status, operator_type, remark, create_time)
VALUES
(5, 0, 9, 'system', '超时未支付，自动取消', '2026-10-20 21:30:00');

-- 订单 6：8号团长（纸飞机工作室）21 日上午下单 50 张小卡 + 20 个钥匙扣
-- 工作室接的漫画展物料单，金额较大，已发货且已签收
INSERT INTO oms_order (id, order_no, user_id, order_type, status, total_amount, freight_amount, discount_amount, pay_amount, pay_time, pay_type, transaction_id, receiver_snapshot, remark, create_time, update_time)
VALUES (6, 'OC20261021091506', 8, 1, 7, 658.00, 0.00, 0.00, 658.00, '2026-10-21 09:15:40', 1, '42000020261021091506',
        JSON_OBJECT('name','纸飞机','phone','13800138008','address','深圳市南山区创意园北区 3 栋 204'),
        '漫画展物料，31号前要到', '2026-10-21 09:12:00', '2026-10-22 10:00:00');

INSERT INTO oms_order_item (id, order_id, order_no, spu_id, sku_id, goods_name, sku_spec, work_id, qty, unit_price, total_price, create_time)
VALUES
(7, 6, 'OC20261021091506', 1, 1, '定制小卡', '哑面', 6, 50, 9.20, 460.00, '2026-10-21 09:15:45'),
(8, 6, 'OC20261021091506', 4, 4, '亚克力钥匙扣', '5cm 双层透明', 9, 20, 9.90, 198.00, '2026-10-21 09:15:45');

INSERT INTO oms_order_image (order_item_id, slot_index, image_url, copies, create_time)
VALUES
(7, 0, 'https://api.trae.cn/mock/img/8-1.jpg', 50, '2026-10-21 09:15:45'),
(8, 0, 'https://api.trae.cn/mock/img/8-2.jpg', 20, '2026-10-21 09:15:45');

INSERT INTO oms_order_status_log (order_id, from_status, to_status, operator_type, remark, create_time)
VALUES
(6, 0, 2, 'user', '微信支付', '2026-10-21 09:15:40'),
(6, 2, 3, 'system', '自动过审', '2026-10-21 09:16:00'),
(6, 3, 4, 'system', 'MES 接单', '2026-10-21 09:20:00'),
(6, 4, 5, 'factory', '印刷完成，转质检', '2026-10-21 15:10:00'),
(6, 5, 6, 'factory', '质检通过，待发货', '2026-10-21 16:00:00'),
(6, 6, 7, 'admin', '顺丰 SF102584796321', '2026-10-21 18:00:00');

INSERT INTO oms_delivery (order_id, delivery_company, delivery_no, delivery_time, receive_time, status, create_time)
VALUES (6, '顺丰速运', 'SF102584796321', '2026-10-21 18:00:00', '2026-10-22 10:00:00', 2, '2026-10-21 18:00:00');

-- 订单 7：10号用户（Yuki酱）21 日晚 8 点下单 3 张小卡，图稿涉及第三方 IP
-- 自动审核命中疑似，人工驳回，订单关闭（退款）
INSERT INTO oms_order (id, order_no, user_id, order_type, status, total_amount, freight_amount, discount_amount, pay_amount, pay_time, pay_type, transaction_id, receiver_snapshot, remark, create_time, update_time)
VALUES (7, 'OC20261021200007', 10, 1, 9, 27.60, 0.00, 0.00, 27.60, '2026-10-21 20:00:15', 1, '42000020261021200007',
        JSON_OBJECT('name','Yuki','phone','13800138010','address','深圳市龙华区民治街道 樟坑一区 8 栋 302'),
        NULL, '2026-10-21 19:58:00', '2026-10-21 20:30:00');

INSERT INTO oms_order_item (id, order_id, order_no, spu_id, sku_id, goods_name, sku_spec, work_id, qty, unit_price, total_price, create_time)
VALUES (9, 7, 'OC20261021200007', 1, 1, '定制小卡', '哑面', 8, 3, 9.20, 27.60, '2026-10-21 20:00:15');

INSERT INTO aud_image_audit (biz_type, biz_id, image_url, auto_result, auto_remark, manual_result, audit_user_id, audit_time, audit_remark, create_time)
VALUES
(1, 8, 'https://api.trae.cn/mock/img/10-1.jpg', 2, '疑似第三方 IP（动漫角色）', 2, 1, '2026-10-21 20:30:00', '涉及第三方 IP，请提供授权证明或更换图稿', '2026-10-21 20:05:00');

INSERT INTO oms_order_status_log (order_id, from_status, to_status, operator_type, remark, create_time)
VALUES
(7, 0, 2, 'user', '微信支付', '2026-10-21 20:00:15'),
(7, 2, 9, 'admin', '审核驳回：第三方 IP 风险，订单取消并退款', '2026-10-21 20:30:00');

-- 订单 8：6号用户（阿木）22 日凌晨 1 点下单 1 张小卡，还没审核
-- 真实场景：深夜下单，审核员第二天上班才看到
INSERT INTO oms_order (id, order_no, user_id, order_type, status, total_amount, freight_amount, discount_amount, pay_amount, pay_time, pay_type, transaction_id, receiver_snapshot, remark, create_time, update_time)
VALUES (8, 'OC20261022010008', 6, 1, 2, 9.20, 0.00, 0.00, 9.20, '2026-10-22 01:00:05', 1, '42000020261022010008',
        JSON_OBJECT('name','阿木','phone','13800138006','address','深圳市龙岗区坂田街道 万科城 12 栋 803'),
        NULL, '2026-10-22 00:58:00', '2026-10-22 01:00:05');

INSERT INTO oms_order_item (id, order_id, order_no, spu_id, sku_id, goods_name, sku_spec, work_id, qty, unit_price, total_price, create_time)
VALUES (10, 8, 'OC20261022010008', 1, 1, '定制小卡', '哑面', 5, 1, 9.20, 9.20, '2026-10-22 01:00:05');

INSERT INTO oms_order_status_log (order_id, from_status, to_status, operator_type, remark, create_time)
VALUES
(8, 0, 2, 'user', '微信支付', '2026-10-22 01:00:05');

-- ============================================================
-- 第四幕：售后与客服（20-22日）
-- ============================================================

-- 售后 1：订单 2（莉莉安）收到后发现有一张印糊了，申请部分退款 9.2
INSERT INTO oms_aftersale (id, order_id, order_item_id, aftersale_no, type, reason, description, amount, status, audit_user_id, audit_time, audit_remark, refund_no, refund_time, create_time, update_time)
VALUES (1, 2, 3, 'AS20261021001', 1, '有一张印糊了', '第 7 张中间有一道白线，影响使用', 9.20, 4, 1, '2026-10-21 15:30:00', '情况属实，退单张款', '42000020261021153001', '2026-10-21 15:35:00', '2026-10-21 15:20:00', '2026-10-21 15:35:00');

-- 售后 2：订单 3（团长应援单）收到货后发现有 5 张色差，申请色差处理
-- 客服驳回：定制商品色差在合理范围内，建议下次打样确认
INSERT INTO oms_aftersale (id, order_id, order_item_id, aftersale_no, type, reason, description, amount, status, audit_user_id, audit_time, audit_remark, create_time, update_time)
VALUES (2, 3, 4, 'AS20261021002', 3, '5 张色差', '第 12、45、89、120、200 张颜色偏暗', 0.00, 3, 1, '2026-10-21 18:00:00', '色差在工艺合理范围内（±5%），建议大批量前打样', '2026-10-21 17:45:00', '2026-10-21 18:00:00');

-- 工单 1：1 号用户问"我的单到哪了"（订单 1，已签收，用户没看物流）
INSERT INTO svc_ticket (id, ticket_no, user_id, type, order_id, title, content, status, handler_id, source, create_time, update_time)
VALUES (1, 'TK20261020001', 1, 4, 1, '我的单到哪了', '25 号演唱会要用的，现在到哪了？', 3, 1, 1, '2026-10-20 09:30:00', '2026-10-20 09:45:00');

INSERT INTO svc_ticket_reply (ticket_id, reply_type, user_id, content, create_time)
VALUES
(1, 1, 1, '亲，您的订单已发货，中通 731025896314，今天应该能到，请留意快递电话', '2026-10-20 09:45:00'),
(1, 2, 1, '收到了，谢谢！', '2026-10-20 10:20:00');

-- 工单 2：7 号团长投诉订单 3 的色差问题（已走售后，但情绪比较激动）
INSERT INTO svc_ticket (id, ticket_no, user_id, type, order_id, title, content, status, handler_id, source, create_time, update_time)
VALUES (2, 'TK20261021002', 7, 3, 3, '应援小卡色差严重', '300 张里有 5 张明显偏暗，粉丝不满意，要求重印或赔偿', 3, 1, 1, '2026-10-21 17:50:00', '2026-10-21 18:30:00');

INSERT INTO svc_ticket_reply (ticket_id, reply_type, user_id, content, create_time)
VALUES
(2, 1, 1, '非常抱歉给您带来困扰。色差在定制印刷中属于工艺合理范围（±5%），建议您下次大批量前先做打样确认。本次我们补偿您 5 张重印，您看可以吗？', '2026-10-21 18:00:00'),
(2, 2, 7, '行吧，下次一定先打样。重印的什么时候能发？', '2026-10-21 18:20:00'),
(2, 1, 1, '今天安排，顺丰到付，明天发出', '2026-10-21 18:30:00');

-- 工单 3：11 号摊主（摆摊的老王）不会传图，电话打过来，客服代建工单
INSERT INTO svc_ticket (id, ticket_no, user_id, type, title, content, status, handler_id, source, create_time, update_time)
VALUES (3, 'TK20261021003', 11, 4, '图传不上去', '老王打电话说在小程序里传图一直转圈，客服远程指导后解决', 3, 1, 2, '2026-10-21 20:00:00', '2026-10-21 20:15:00');

-- ============================================================
-- 第五幕：作品与分享（贯穿全周）
-- ============================================================

-- 作品状态：1 正常 / 2 待审核 / 3 待修改 / 7 违规（见 WorkStatus）
INSERT INTO wms_work (id, user_id, work_name, cover_url, spu_id, sku_id, status, audit_remark, create_time, update_time)
VALUES
(1, 1, '演唱会应援手幅', 'https://api.trae.cn/mock/img/1-1.jpg', 1, 1, 1, NULL, '2026-10-16 19:55:00', '2026-10-16 20:00:00'),
(2, 4, '小卖部招牌卡', 'https://api.trae.cn/mock/img/4-1.jpg', 1, 1, 1, NULL, '2026-10-17 23:10:00', '2026-10-17 23:15:00'),
(3, 7, '某歌手应援卡', 'https://api.trae.cn/mock/img/7-1.jpg', 1, 1, 1, '人工确认授权', '2026-10-18 10:25:00', '2026-10-18 11:05:00'),
(4, 2, '手机壳图案', 'https://api.trae.cn/mock/img/2-2.jpg', 3, 3, 1, NULL, '2026-10-19 14:15:00', '2026-10-19 15:05:00'),
(5, 6, '阿木的小卡', 'https://api.trae.cn/mock/img/6-1.jpg', 1, 1, 2, NULL, '2026-10-22 00:55:00', '2026-10-22 00:55:00'),
(6, 8, '漫画展物料', 'https://api.trae.cn/mock/img/8-1.jpg', 1, 1, 1, NULL, '2026-10-21 09:10:00', '2026-10-21 09:16:00'),
(7, 10, '动漫角色卡', 'https://api.trae.cn/mock/img/10-1.jpg', 1, 1, 7, '第三方 IP，驳回', '2026-10-21 19:50:00', '2026-10-21 20:30:00'),
(8, 11, '摊位价目表', 'https://api.trae.cn/mock/img/11-1.jpg', 1, 1, 1, NULL, '2026-10-16 14:00:00', '2026-10-16 14:30:00'),
(9, 8, '漫画展钥匙扣图', 'https://api.trae.cn/mock/img/8-2.jpg', 4, 4, 1, NULL, '2026-10-21 09:12:00', '2026-10-21 09:16:00');

-- 作品 7 被驳回时，用户签了版权承诺（虽然最后没通过）
INSERT INTO aud_copyright_promise (user_id, biz_type, biz_id, promise_version, ip, create_time)
VALUES (10, 2, 7, 'v1.0', '183.9.196.161', '2026-10-21 19:52:00');

-- 1 号用户把作品分享给了 3 号用户（无料分享，对方下单 1 张）
INSERT INTO wms_share_code (id, share_code, user_id, work_id, share_type, payer_type, max_order_qty, used_qty, expire_time, status, create_time)
VALUES (1, 'SH20261016001', 1, 1, 2, 1, 10, 1, '2026-11-21 00:00:00', 1, '2026-10-16 20:05:00');

INSERT INTO oms_order (id, order_no, user_id, order_type, status, share_code_id, total_amount, freight_amount, discount_amount, pay_amount, pay_time, pay_type, transaction_id, receiver_snapshot, remark, create_time, update_time)
VALUES (9, 'OC20261016210009', 3, 3, 8, 1, 9.20, 0.00, 0.00, 9.20, '2026-10-16 21:00:10', 1, '42000020261016210009',
        JSON_OBJECT('name','wx_4f8a2b1c','phone','13800138003','address','深圳市南山区科技园南区R2-B栋 放前台'),
        NULL, '2026-10-16 20:58:00', '2026-10-16 21:00:15');

INSERT INTO oms_order_item (id, order_id, order_no, spu_id, sku_id, goods_name, sku_spec, work_id, qty, unit_price, total_price, create_time)
VALUES (11, 9, 'OC20261016210009', 1, 1, '定制小卡', '哑面', 1, 1, 9.20, 9.20, '2026-10-16 21:00:15');

INSERT INTO oms_order_status_log (order_id, from_status, to_status, operator_type, remark, create_time)
VALUES
(9, 0, 2, 'user', '微信支付', '2026-10-16 21:00:10'),
(9, 2, 3, 'system', '自动过审', '2026-10-16 21:00:30'),
(9, 3, 4, 'system', 'MES 接单', '2026-10-16 21:05:00'),
(9, 4, 5, 'factory', '印刷完成，转质检', '2026-10-17 15:15:00'),
(9, 5, 6, 'factory', '质检通过，待发货', '2026-10-17 15:30:00'),
(9, 6, 7, 'admin', '中通 731025896315', '2026-10-17 16:20:00'),
(9, 7, 8, 'user', '确认收货', '2026-10-20 11:00:00');

INSERT INTO oms_delivery (order_id, delivery_company, delivery_no, delivery_time, receive_time, status, create_time)
VALUES (9, '中通快递', '731025896315', '2026-10-17 16:20:00', '2026-10-20 11:00:00', 2, '2026-10-17 16:20:00');

-- ============================================================
-- 第七幕：生产工单与发票（订单的 B 面）
-- ============================================================

-- 生产工单（work_order_no / expected_delivery / sync_time 就在 oms_order 上）
-- 已完成的单：MES 工单号 + 实际生产节奏
UPDATE oms_order SET work_order_no='MES2026101701', expected_delivery='2026-10-18 18:00:00', sync_time='2026-10-17 15:35:00' WHERE id=1;
UPDATE oms_order SET work_order_no='MES2026101702', expected_delivery='2026-10-19 18:00:00', sync_time='2026-10-18 18:50:00' WHERE id=2;
UPDATE oms_order SET work_order_no='MES2026101803', expected_delivery='2026-10-20 18:00:00', sync_time='2026-10-19 20:05:00' WHERE id=3;
UPDATE oms_order SET work_order_no='MES2026101704', expected_delivery='2026-10-18 18:00:00', sync_time='2026-10-17 15:35:00' WHERE id=9;
-- 生产中的单：订单 4（手机壳，图糊打回过一次，车间排产靠后）
UPDATE oms_order SET work_order_no='MES2026101905', expected_delivery='2026-10-23 18:00:00', sync_time='2026-10-21 09:00:00' WHERE id=4;
-- 待发货的单：订单 6（漫画展物料，生产完等打包发货）
UPDATE oms_order SET work_order_no='MES2026102106', expected_delivery='2026-10-23 18:00:00', sync_time='2026-10-22 11:35:00' WHERE id=6;
-- 订单 8（凌晨下单）还没进 MES，等待审核放行——生产工单页能看到它卡在入口

-- 发票：两笔已开、一笔待审、一笔驳回
-- 1. 莉莉安（订单 2）已收到已开的电子普票（个人抬头）
INSERT INTO oms_invoice (id, invoice_no, user_id, order_ids, title_type, title, tax_no, amount, email, type, status, file_url, create_time, update_time)
VALUES (1, 'INV20261022001', 4, '2', 1, '莉莉安', NULL, 184.00, 'lilian@example.com', 1, 2,
        'https://api.trae.cn/mock/invoice/INV20261022001.pdf', '2026-10-22 09:10:00', '2026-10-22 09:30:00');

-- 2. 团长陈晓燕（订单 3 大额 2550）开企业抬头发票，已开
INSERT INTO oms_invoice (id, invoice_no, user_id, order_ids, title_type, title, tax_no, amount, email, type, status, file_url, create_time, update_time)
VALUES (2, 'INV20261022002', 7, '3', 2, '广州星途文化传媒有限公司', '91440101MA9XXXXX2K', 2550.00, 'finance@xingtu.example.com', 1, 2,
        'https://api.trae.cn/mock/invoice/INV20261022002.pdf', '2026-10-22 10:05:00', '2026-10-22 10:40:00');

-- 3. 纸飞机工作室（订单 6，658）上午刚申请，财务还没审（待审）
INSERT INTO oms_invoice (id, invoice_no, user_id, order_ids, title_type, title, tax_no, amount, email, type, status, create_time, update_time)
VALUES (3, 'INV20261022003', 8, '6', 2, '深圳纸飞机文化创意工作室', '91440300MA5XXXXX7T', 658.00, 'paperplane@example.com', 1, 1,
        '2026-10-22 11:20:00', '2026-10-22 11:20:00');

-- 4. Yuki酱（订单 7 审核驳回取消）曾申请开票被驳回：订单已取消，不可开
INSERT INTO oms_invoice (id, invoice_no, user_id, order_ids, title_type, title, amount, email, type, status, create_time, update_time)
VALUES (4, 'INV20261021004', 10, '7', 1, 'Yuki酱', 27.60, 'yuki@example.com', 1, 3,
        '2026-10-21 21:00:00', '2026-10-21 21:30:00');

-- ============================================================
-- 第八幕：22 日——还欠着没处理的事
-- ============================================================
-- 前七幕都是"已经发生的事"。运营打开后台，看到的应该是待办，
-- 所以这一幕专门补：审核员有单可审、客服有单可回、财务有账可核、仓管有单可发。
-- 时间锚点：本幕之前的单都过了几天，本幕的待办都发生在 22 日（今天）。

-- 1) 订单 12：12号用户 19:35 下单 5 张小卡，还没付款（待付款 1 单）
INSERT INTO oms_order (id, order_no, user_id, order_type, status, total_amount, freight_amount, discount_amount, pay_amount, expire_time, receiver_snapshot, remark, create_time, update_time)
VALUES (12, 'OC20261022193512', 12, 1, 0, 46.00, 0.00, 0.00, 46.00, '2026-10-22 23:59:00',
        JSON_OBJECT('name','用户9527','phone','13800138012','address','佛山市禅城区祖庙路 33 号 8 楼'),
        NULL, '2026-10-22 19:35:00', '2026-10-22 19:35:00');

INSERT INTO oms_order_item (id, order_id, order_no, spu_id, sku_id, goods_name, sku_spec, work_id, qty, unit_price, total_price, create_time)
VALUES (14, 12, 'OC20261022193512', 1, 1, '定制小卡', '哑面', NULL, 5, 9.20, 46.00, '2026-10-22 19:35:00');

-- 2) 订单 13：9号用户 21 日傍晚下单 10 张小卡，今早质检完成，卡在"待发货"
-- 仓管下午 5 点前要出库，是今天真正要动手的一件事
INSERT INTO oms_order (id, order_no, user_id, order_type, status, total_amount, freight_amount, discount_amount, pay_amount, pay_time, pay_type, transaction_id, receiver_snapshot, remark, create_time, update_time)
VALUES (13, 'OC20261021162013', 9, 1, 6, 92.00, 0.00, 0.00, 92.00, '2026-10-21 16:20:30', 1, '42000020261021162013',
        JSON_OBJECT('name','wx_2a9f','phone','13800138009','address','东莞市南城街道 鸿福路 200 号 3 栋 601'),
        NULL, '2026-10-21 16:18:00', '2026-10-22 09:40:00');

INSERT INTO oms_order_item (id, order_id, order_no, spu_id, sku_id, goods_name, sku_spec, work_id, qty, unit_price, total_price, create_time)
VALUES (15, 13, 'OC20261021162013', 1, 1, '定制小卡', '哑面', NULL, 10, 9.20, 92.00, '2026-10-21 16:20:30');

INSERT INTO oms_order_image (order_item_id, slot_index, image_url, copies, create_time)
VALUES (15, 0, 'https://api.trae.cn/mock/img/9-1.jpg', 10, '2026-10-21 16:20:30');

INSERT INTO oms_order_status_log (order_id, from_status, to_status, operator_type, remark, create_time)
VALUES
(13, 0, 2, 'user', '微信支付', '2026-10-21 16:20:30'),
(13, 2, 3, 'system', '自动过审', '2026-10-21 16:21:00'),
(13, 3, 4, 'system', 'MES 接单', '2026-10-21 16:25:00'),
(13, 4, 5, 'factory', '印刷完成，转质检', '2026-10-22 09:10:00'),
(13, 5, 6, 'factory', '质检通过，待发货', '2026-10-22 09:40:00');

-- 3) 打样单：团长陈晓燕按客服建议，3000 张前先打样 10 张（order_type=2）
-- 22 日早上刚过审，卡在"待生产"，车间还没排期
INSERT INTO oms_order (id, order_no, user_id, order_type, status, total_amount, freight_amount, discount_amount, pay_amount, pay_time, pay_type, transaction_id, receiver_snapshot, remark, create_time, update_time)
VALUES (11, 'OC20261022081511', 7, 2, 3, 85.00, 0.00, 0.00, 85.00, '2026-10-22 08:15:20', 1, '42000020261022081511',
        JSON_OBJECT('name','陈晓燕','phone','13800138007','address','深圳市福田区车公庙泰然大厦 2103'),
        '先打样 10 张确认色差，没问题再下 3000 张', '2026-10-22 08:12:00', '2026-10-22 08:20:00');

INSERT INTO oms_order_item (id, order_id, order_no, spu_id, sku_id, goods_name, sku_spec, work_id, qty, unit_price, total_price, create_time)
VALUES (13, 11, 'OC20261022081511', 1, 1, '定制小卡', '哑面', 3, 10, 8.50, 85.00, '2026-10-22 08:15:20');

INSERT INTO oms_order_image (order_item_id, slot_index, image_url, copies, create_time)
VALUES (13, 0, 'https://api.trae.cn/mock/img/7-1.jpg', 10, '2026-10-22 08:15:20');

INSERT INTO oms_order_status_log (order_id, from_status, to_status, operator_type, remark, create_time)
VALUES
(11, 0, 2, 'user', '微信支付', '2026-10-22 08:15:20'),
(11, 2, 3, 'admin', '审核通过（打样单优先排产）', '2026-10-22 08:20:00');

-- 4) 售后 3：纸飞机工作室（订单 6，已签收）清点发现少了 5 张，申请退差价 46（待审）
-- 仓库还没复核实物，所以订单仍是"已发货"
INSERT INTO oms_aftersale (id, order_id, order_item_id, aftersale_no, type, reason, description, amount, status, create_time, update_time)
VALUES (3, 6, 7, 'AS20261022003', 1, '少发 5 张', '清点后只有 45 张，少了 5 张，展会要用', 46.00, 1, '2026-10-22 10:30:00', '2026-10-22 10:30:00');

-- 5) 工单 4：9号用户问企业抬头发票怎么开（待受理，客服还没接）
INSERT INTO svc_ticket (id, ticket_no, user_id, type, title, content, status, source, create_time, update_time)
VALUES (4, 'TK20261022004', 9, 4, '怎么开企业抬头发票', '公司报销需要专票，小程序里没找到入口', 1, 1, '2026-10-22 07:20:00', '2026-10-22 07:20:00');

-- 6) 合集：纸飞机工作室把漫画展的两个图稿打包成合集，方便二次分享
INSERT INTO wms_collection (id, user_id, collection_name, cover_url, status, create_time, update_time)
VALUES (1, 8, '漫画展物料合集', 'https://api.trae.cn/mock/img/8-1.jpg', 1, '2026-10-21 18:00:00', '2026-10-21 18:00:00');

INSERT INTO wms_collection_item (collection_id, work_id, create_time, update_time)
VALUES
(1, 6, '2026-10-21 18:00:00', '2026-10-21 18:00:00'),
(1, 9, '2026-10-21 18:00:00', '2026-10-21 18:00:00');

-- 7) 邀请记录：1号邀请3号（3号已下单，双方各得 3 元券）；
--    2号邀请了9号，9号一直没下单，奖励没生效
INSERT INTO mkt_invite (id, activity_id, inviter_user_id, invitee_user_id, order_id, status, inviter_reward_granted, invitee_reward_granted, create_time, update_time)
VALUES
(1, 2, 1, 3, 9, 2, 1, 1, '2026-10-15 21:30:00', '2026-10-16 21:00:10'),
(2, 2, 2, 9, NULL, 1, 0, 0, '2026-10-16 12:40:00', '2026-10-16 12:40:00');

-- 8) 黑名单：两起侵权投诉驳回后，图库留下指纹，命中即自动拦截
INSERT INTO aud_blacklist (id, pHash, source, remark, status, create_time, update_time)
VALUES
(1, 7264538912047361, '侵权投诉', '动漫角色卡（第三方 IP，投诉驳回）', 1, '2026-10-21 20:35:00', '2026-10-21 20:35:00'),
(2, 3412876590123482, '人工上传', '艺人写真卡（未获授权，品牌方投诉）', 1, '2026-10-19 11:20:00', '2026-10-19 11:20:00');

-- 9) 分享码 2：团长给团员发的无料码，10/21 到期已失效
INSERT INTO wms_share_code (id, share_code, user_id, work_id, share_type, payer_type, max_order_qty, used_qty, expire_time, status, invalid_reason, create_time)
VALUES (2, 'SH20261018002', 7, 3, 2, 2, 20, 0, '2026-10-21 00:00:00', 0, '已到期自动失效', '2026-10-18 11:20:00');

-- 10) 延迟任务日志：超时取消、待发货告警、一次失败的兜底任务
INSERT INTO tsk_delay_log (id, task_type, biz_id, fire_time, exec_time, status, retry_count, error_msg, create_time, update_time)
VALUES
(1, 'ORDER_UNPAID_CANCEL', 5, '2026-10-20 21:30:00', '2026-10-20 21:35:00', 2, 0, NULL, '2026-10-20 21:00:00', '2026-10-20 21:35:00'),
(2, 'ORDER_DELIVERY_ALERT', 13, '2026-10-25 09:40:00', NULL, 1, 0, NULL, '2026-10-22 09:40:00', '2026-10-22 09:40:00'),
(3, 'ORDER_UNPAID_CANCEL', 12, '2026-10-22 23:59:00', NULL, 1, 0, NULL, '2026-10-22 19:35:00', '2026-10-22 19:35:00'),
(4, 'SHARE_CODE_EXPIRE', 2, '2026-10-21 00:00:00', '2026-10-21 00:05:00', 3, 3, '退款渠道超时，已由兜底扫描补偿', '2026-10-18 11:20:00', '2026-10-21 00:05:00');

-- 11) 收货地址：三个高频用户存了地址簿，小程序下单不用每次手填
INSERT INTO ums_address (user_id, receiver_name, receiver_phone, province, city, district, detail, is_default, create_time, update_time)
VALUES
(1, '陈小姐', '13800138001', '广东省', '深圳市', '南山区', '科技园南区 R2-B 栋 前台', 1, '2026-10-15 20:20:00', '2026-10-15 20:20:00'),
(4, '莉莉安', '13800138004', '广东省', '广州市', '天河区', '棠下村北社大街 7 号 小卖部', 1, '2026-10-15 21:30:00', '2026-10-15 21:30:00'),
(8, '纸飞机', '13800138008', '广东省', '深圳市', '南山区', '创意园北区 3 栋 204', 1, '2026-10-16 09:50:00', '2026-10-16 09:50:00');

-- 12) 财务：退款流水（3 笔成功 + 1 笔强制退款待复核）
INSERT INTO fin_refund_ledger (id, refund_no, biz_type, biz_id, order_id, user_id, amount, channel, status, review_status, remark, create_time, update_time)
VALUES
(1, 'RF20261019143501', 1, 10, 10, 2, 39.00, 1, 2, 0, '审核驳回（图糊），原路退回', '2026-10-19 14:35:00', '2026-10-19 14:36:00'),
(2, 'RF20261021153501', 4, 1, 2, 4, 9.20, 1, 2, 0, '售后 AS20261021001 印糊退款，退单张', '2026-10-21 15:35:00', '2026-10-21 15:36:00'),
(3, 'RF20261021203001', 1, 7, 7, 10, 27.60, 1, 2, 0, '第三方 IP 审核驳回，原路退回', '2026-10-21 20:30:00', '2026-10-21 20:31:00'),
(4, 'RF20261022081001', 1, 4, 4, 2, 39.00, 1, 1, 1, '强制退款：生产中订单用户要求取消，等待财务复核', '2026-10-22 08:10:00', '2026-10-22 08:10:00');

-- 13) 财务：日结（普通单按支付口径、拼团单按成团口径，退款单列不冲销售额）
--     10-18 的 2550 是团购成团，计入成团口径；
--     10-21 有 658 + 27.6 + 92 三笔支付，退 9.2（售后）+ 27.6（审核驳回）
INSERT INTO fin_daily_settlement (id, settle_date, order_amount, groupon_amount, refund_amount, net_amount, order_count, create_time, update_time)
VALUES
(1, '2026-10-16', 60.20, 0.00, 0.00, 60.20, 2, '2026-10-17 02:00:00', '2026-10-17 02:00:00'),
(2, '2026-10-17', 184.00, 0.00, 0.00, 184.00, 1, '2026-10-18 02:00:00', '2026-10-18 02:00:00'),
(3, '2026-10-18', 0.00, 2550.00, 0.00, 2550.00, 1, '2026-10-19 02:00:00', '2026-10-19 02:00:00'),
(4, '2026-10-19', 78.00, 0.00, 39.00, 39.00, 2, '2026-10-20 02:00:00', '2026-10-20 02:00:00'),
(5, '2026-10-21', 777.60, 0.00, 36.80, 740.80, 3, '2026-10-22 02:00:00', '2026-10-22 02:00:00');

-- ============================================================
-- 数据汇总（给运营看板，截至 10 月 22 日晚）
-- ============================================================
--   用户：12（2 个团长 / 1 个摊主；4 份实名认证：2 通过、1 待审、1 驳回）
--   订单：13 单 = 普通 12 + 打样 1（打样不进销售口径）
--        状态分布：待付款 1、待审核 1、待生产 1、生产中 1、待发货 1、
--                 已发货 1、已完成 4、已取消 3
--   销售额：3659.00（支付口径，含已退款的 39 + 27.6；不含打样单 85）
--   退款：75.80（3 笔已退）+ 39.00（1 笔待财务复核）+ 46.00（售后待审）
--   作品：9（正常 7、待审核 1、违规 1）+ 合集 1 + 分享码 2
--   售后：3（完成 1、驳回 1、待审 1）
--   工单：4（已解决 3、待受理 1）
--   发票：4（已开 2、待审 1、驳回 1）
--   日结：5 天（10-16 ~ 10-21，22 日当天还没跑日结）
--   延迟任务：4 条（待执行 2、成功 1、失败 1）
-- ============================================================

-- 结束语：这套数据支撑后台 8 个一级菜单、30+ 页面的全部演示场景。
-- 每个页面从上到下走一遍，就是一个完整的"橙刻定制"运营故事。

