SET NAMES utf8mb4;
-- ============================================================
-- system 模块种子：角色 / 菜单 / 角色授权 / 系统参数
-- 管理员账号由 DataInitializer 在启动时创建（BCrypt 密码运行时生成）
-- ============================================================

-- 内置角色（见 00 篇 §4.3）
INSERT INTO sys_role (id, role_name, role_key, remark, status) VALUES
    (1, '超级管理员', 'super_admin',      '全部权限，含系统配置',   1),
    (2, '客服',       'customer_service', '订单查询、工单、售后',   1),
    (3, '仓管',       'warehouse',        '生产推进、发货',         1),
    (4, '运营',       'operator',         '商品、活动、优惠券',     1),
    (5, '审核',       'auditor',          '定制图与作品审核',       1),
    (6, '财务',       'finance',          '对账、退款复核',         1);

-- 菜单与按钮权限（见 06 篇 §4 页面清单）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, perm_key, icon, sort) VALUES
    (1,   0, '运营待办',  2, '/home/todo',           'fin:dashboard:todo', 'dashboard', 1),

    -- 订单管理（oms:）
    (20,  0, '订单管理',  1, '/order',               NULL,                 'shopping',  2),
    (21, 20, '订单列表',  2, '/order/list',          'oms:order:list',     NULL, 1),
    (216,21,'订单备注', 3, NULL,                   'oms:order:remark', NULL, 9),
    (215,21,'代客支付', 3, NULL,                   'oms:order:pay', NULL, 9),
    (211,21, '改价',      3, NULL,                   'order:price:update', NULL, 1),
    (212,21, '强制退款',  3, NULL,                   'order:refund:force', NULL, 2),
    (217,21, '退款复核',  3, NULL,                   'order:refund:review', NULL, 3),
    (213,21, '修改状态',  3, NULL,                   'order:status:update',NULL, 3),
    (214,21, '批量导出',  3, NULL,                   'oms:order:export',   NULL, 4),
    (22, 20, '生产工单',  2, '/order/production',    'oms:production:list',NULL, 2),
    (224,22,'推进生产', 3, NULL,                   'oms:production:advance', NULL, 9),
    (221,22, '审核放行',  3, NULL,                   'oms:production:audit',NULL, 1),
    (222,22, '录工单号',  3, NULL,                   'oms:production:work-order', NULL, 2),
    (223,22, 'MES同步',   3, NULL,                   'oms:production:sync',NULL, 3),
    (23, 20, '发货管理',  2, '/order/delivery',      'oms:delivery:list',  NULL, 3),
    (231,23, '发货',      3, NULL,                   'oms:delivery:ship',  NULL, 1),
    (232,23, '批量发货',  3, NULL,                   'oms:delivery:batch', NULL, 2),
    (24, 20, '售后管理',  2, '/order/aftersale',     'oms:aftersale:list', NULL, 4),
    (242,24,'发起售后', 3, NULL,                   'oms:aftersale:apply', NULL, 9),
    (241,24, '售后审核',  3, NULL,                   'oms:aftersale:audit',NULL, 1),
    (25, 20, '发票管理',  2, '/order/invoice',       'oms:invoice:list',   NULL, 5),
    (252,25,'发起开票', 3, NULL,                   'oms:invoice:apply', NULL, 9),
    (251,25, '开票审核',  3, NULL,                   'oms:invoice:audit',  NULL, 1),

    -- 作品与分享（wms:）
    (30,  0, '作品与分享',1, '/work',                NULL,                 'picture',   3),
    (31, 30, '作品列表',  2, '/work/list',           'wms:work:list',      NULL, 1),
    (311,31, '改状态',    3, NULL,                   'work:work:update',   NULL, 1),
    (312,31, '删除作品',  3, NULL,                   'work:work:delete',   NULL, 2),
    (32, 30, '分享码',    2, '/work/share-code',     'wms:share-code:list',NULL, 2),
    (321,32, '强制失效',  3, NULL,                   'work:share-code:invalidate', NULL, 1),
    (322,32, '退余额',    3, NULL,                   'work:share-code:refund', NULL, 2),
    (33, 30, '合集管理',  2, '/work/collection',     'wms:collection:list',NULL, 3),
    (331,33, '删除合集',  3, NULL,                   'work:collection:delete', NULL, 1),
    (34, 30, '打样单',    2, '/work/proof',          'wms:proof:list',     NULL, 4),

    -- 审核中心（audit:）
    (40,  0, '审核中心',  1, '/audit',               NULL,                 'safety',    4),
    (41, 40, '待审队列',  2, '/audit/list',          'audit:image:list',   NULL, 1),
    (414,41,'批量审核', 3, NULL,                   'audit:image:batch', NULL, 9),
    (413,41,'图片检测', 3, NULL,                   'audit:image:check', NULL, 9),
    (411,41, '审核通过',  3, NULL,                   'audit:image:approve',NULL, 1),
    (412,41, '审核驳回',  3, NULL,                   'audit:image:reject', NULL, 2),
    (42, 40, '审核记录',  2, '/audit/record',        'audit:image:list',   NULL, 2),
    (43, 40, '黑名单库',  2, '/audit/blacklist',     'audit:blacklist:list',NULL, 3),
    (432,43,'维护黑名单', 3, NULL,                   'audit:blacklist:update', NULL, 9),
    (431,43, '删除黑名单',3, NULL,                   'audit:blacklist:delete', NULL, 1),
    (44, 40, '敏感词',    2, '/audit/sensitive',     'audit:sensitive:list',NULL, 4),
    (442,44,'维护敏感词', 3, NULL,                   'audit:sensitive-word:update', NULL, 9),
    (441,44, '删除敏感词',3, NULL,                   'audit:sensitive-word:delete', NULL, 1),
    (45, 40, '版权承诺',  2, '/audit/copyright',     'audit:promise:list', NULL, 5),

    -- 商品管理（goods:）
    (50,  0, '商品管理',  1, '/goods',               NULL,                 'goods',     5),
    (51, 50, '商品列表',  2, '/goods/list',          'goods:spu:list',     NULL, 1),
    (517,51,'维护规格值', 3, NULL,                   'goods:spec-value:update', NULL, 9),
    (516,51,'维护规格组', 3, NULL,                   'goods:spec-group:update', NULL, 9),
    (515,51,'维护 SKU', 3, NULL,                   'goods:sku:update', NULL, 9),
    (514,51,'维护商品', 3, NULL,                   'goods:spu:update', NULL, 9),
    (511,51, '改库存',    3, NULL,                   'goods:stock:update', NULL, 1),
    (512,51, '改价',      3, NULL,                   'goods:price:update', NULL, 2),
    (513,51, '批量改价',  3, NULL,                   'goods:price:batch',  NULL, 3),
    (518,51, '商品编辑',  2, '/goods/spu-edit',      'goods:spu:list',     NULL, 8),
    (52, 50, '系列管理',  2, '/goods/series',        'goods:series:list',  NULL, 2),
    (521,52,'维护系列', 3, NULL,                   'goods:series:update', NULL, 9),
    (53, 50, '品类管理',  2, '/goods/category',      'goods:category:list',NULL, 3),
    (531,53,'维护品类', 3, NULL,                   'goods:category:update', NULL, 9),
    (54, 50, '工艺管理',  2, '/goods/craft',         'goods:craft:list',   NULL, 4),
    (541,54,'维护工艺', 3, NULL,                   'goods:craft:update', NULL, 9),
    (55, 50, '运费模板',  2, '/goods/freight',       'goods:freight:list', NULL, 5),
    (551,55,'维护运费', 3, NULL,                   'goods:freight:update', NULL, 9),
    (56, 50, '画师管理',  2, '/goods/designer',      'goods:designer:list',NULL, 6),
    (561,56,'维护画师', 3, NULL,                   'goods:designer:update', NULL, 9),
    (57, 50, '模板库',    2, '/goods/template',      'goods:template:list',NULL, 7),
    (571,57,'维护模板', 3, NULL,                   'goods:template:update', NULL, 9),

    -- 营销管理（marketing:）
    (60,  0, '营销管理',  1, '/marketing',           NULL,                 'gift',      6),
    (61, 60, '活动列表',  2, '/marketing/activity',  'mkt:activity:list',  NULL, 1),
    (612,61,'活动启停', 3, NULL,                   'mkt:activity:status', NULL, 9),
    (613,61, '活动编辑',  2, '/marketing/activity-edit', 'mkt:activity:list', NULL, 2),
    (611,61,'维护活动', 3, NULL,                   'mkt:activity:update', NULL, 9),
    (62, 60, '拼团管理',  2, '/marketing/group',     'mkt:group:list',     NULL, 2),
    (621,62, '强制成团',  3, NULL,                   'mkt:group:force',    NULL, 1),
    (63, 60, '秒杀管理',  2, '/marketing/seckill',   'mkt:seckill:list',   NULL, 3),
    (631,63,'维护秒杀', 3, NULL,                   'mkt:seckill:update', NULL, 9),
    (64, 60, '优惠券',    2, '/marketing/coupon',    'mkt:coupon:list',    NULL, 4),
    (642,64,'维护券', 3, NULL,                   'mkt:coupon:update', NULL, 9),
    (641,64, '发放',      3, NULL,                   'mkt:coupon:grant',   NULL, 1),
    (65, 60, '邀请记录',  2, '/marketing/invite',    'mkt:invite:list',    NULL, 5),

    -- 客服工单（svc:）
    (70,  0, '客服工单',  1, '/service',             NULL,                 'message',   7),
    (71, 70, '工单列表',  2, '/service/ticket',      'svc:ticket:list',    NULL, 1),
    (713,71,'关闭工单', 3, NULL,                   'svc:ticket:close', NULL, 9),
    (712,71,'处理工单', 3, NULL,                   'svc:ticket:update', NULL, 9),
    (711,71, '回复',      3, NULL,                   'svc:ticket:reply',   NULL, 1),

    -- 财务与看板（fin:）
    (80,  0, '财务与看板',1, '/finance',             NULL,                 'money',     8),
    (81, 80, '经营看板',  2, '/finance/dashboard',   'fin:dashboard:view', NULL, 1),
    (82, 80, '财务对账',  2, '/finance/settlement',  'fin:settlement:list',NULL, 2),
    (83, 80, '退款流水',  2, '/finance/refund',      'fin:refund:list',    NULL, 3),
    (831,83, '导出',      3, NULL,                   'fin:refund:export',  NULL, 1),

    -- 系统管理（system:）
    (10,  0, '系统管理',  1, '/system',              NULL,                 'setting',   9),
    (11, 10, '管理员',    2, '/system/user',         'system:user:list',   NULL, 1),
    (110,11, '新增/编辑', 3, NULL,                   'system:user:save',   NULL, 1),
    (111,11, '重置密码',  3, NULL,                   'system:user:password:reset', NULL, 2),
    (112,11, '分配角色',  3, NULL,                   'system:user:roles',  NULL, 3),
    (12, 10, '角色权限',  2, '/system/role',         'system:role:list',   NULL, 2),
    (121,12,'维护角色', 3, NULL,                   'system:role:update', NULL, 9),
    (120,12, '分配权限',  3, NULL,                   'system:role:menus',  NULL, 1),
    (13, 10, '菜单管理',  2, '/system/menu',         'system:menu:list',   NULL, 3),
    (131,13,'维护菜单', 3, NULL,                   'system:menu:update', NULL, 9),
    (14, 10, '系统参数',  2, '/system/config',       'system:config:list', NULL, 4),
    (140,14, '修改参数',  3, NULL,                   'system:config:update', NULL, 1),
    (15, 10, '操作日志',  2, '/system/oper-log',     'system:log:oper',    NULL, 5),
    (16, 10, '登录日志',  2, '/system/login-log',    'system:log:login',   NULL, 6),
    (17, 10, '延迟任务',  2, '/system/task-log',     'system:log:task',    NULL, 7),
    (171,17,'触发扫描', 3, NULL,                   'system:log:task:trigger', NULL, 9),
    (170,17, '重试',      3, NULL,                   'system:log:task:retry', NULL, 1),

    -- 用户管理（ums:）
    (90,  0, '用户管理',  1, '/user',                NULL,                 'user',     10),
    (91, 90, '用户列表',  2, '/user/list',           'ums:user:list',      NULL, 1),
    (911,91,'维护用户', 3, NULL,                   'ums:user:update', NULL, 9),
    (92, 90, '身份认证',  2, '/user/cert',           'ums:cert:list',      NULL, 2),
    (921,92, '认证通过',  3, NULL,                   'user:cert:approve',  NULL, 1),
    (922,92, '认证驳回',  3, NULL,                   'user:cert:reject',   NULL, 2),
    (93, 90, '渠道管理',  2, '/user/channel',        'ums:channel:list',   NULL, 3),
    (931,93,'维护渠道', 3, NULL,                   'ums:channel:update', NULL, 9);

-- 超级管理员拥有全部菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- 客服：仅运营待办 + 查看操作/登录日志（示例子集，后续模块再扩）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 1), (2, 15), (2, 16);

-- 系统参数（见 01 篇 §1「必须落库的参数」+ 00/05 篇配置项）
INSERT INTO sys_config (config_key, config_value, config_type, remark) VALUES
    ('orderUnpaidMinutes',   '30',  1, '待付款超时分钟数'),
    ('groupDefaultSize',     '3',   1, '拼团默认人数'),
    ('groupDurationHours',   '24',  1, '拼团默认时长（小时）'),
    ('refundWindowHours',    '4',   1, '付款后退款窗口（小时）'),
    ('autoConfirmDays',      '10',  1, '自动确认收货天数'),
    ('pendingDeliveryHours', '72',  1, '待发货超时告警（小时）'),
    ('shareCodeExpireDays',  '7',   1, '分享码默认有效期（天）'),
    ('simulateGroupEnabled', '0',   1, '模拟成团开关（默认关闭）'),
    ('freeShippingThreshold','28',  1, '满额包邮阈值（元）'),
    ('baseFreight',          '4',   1, '基础邮费（元）'),
    ('audit.autoRejectOnBlacklist', 'true', 1, '黑名单命中直接驳回'),
    ('audit.pHashThreshold',        '5',    1, 'pHash 汉明距离阈值'),
    ('audit.manualQueueEnabled',    'false',1, '是否所有图都进人工'),
    ('audit.sensitiveWordsEnabled', 'true', 1, '文本审核开关'),
    ('workshare.auditBeforePublic','true',  1, '分享前是否必审'),
    ('workshare.expireDays',       '30',    1, '作品默认有效期（天）'),
    ('freeShareConfig',      '["1","1"]', 1, '无料分享配置：[开关, 主题]（价格档位在服务端，【待确认】）'),
    ('enableMyWork',         '1',   1, '是否开启「我的作品」'),
    ('enableMyWorkForVip',   '0',   1, 'VIP 是否开启「我的作品」'),
    ('batchCustom',          '0_',  1, '批量定制：[开关]_[跳转地址]');
