START TRANSACTION;

-- Clean up the demo order range first so the script can be re-run safely.
DELETE FROM orderbatchlink WHERE id BETWEEN 70001 AND 70020;
DELETE FROM orderitem WHERE orderItemId BETWEEN 91001 AND 91020;
DELETE FROM orderinfo WHERE orderId BETWEEN 90001 AND 90020;

UPDATE productionplan
SET orderId = NULL, orderItemId = NULL
WHERE planId IN (60001, 60002, 60003, 60004);

-- 1. Orders
INSERT INTO orderinfo (orderId, orderNo, customerName, orderDate, deliveryDate, priority, status, remark, createTime) VALUES
(90001, 'ORD-20260520-001', '海澜之家针织事业部', '2026-05-20 09:10:00', '2026-05-30 18:00:00', 'HIGH', 'IN_PROGRESS', '出口休闲系列汗布订单，优先保交期', '2026-05-20 09:15:00'),
(90002, 'ORD-20260520-002', '迪卡侬华东供应链', '2026-05-20 11:30:00', '2026-05-29 18:00:00', 'HIGH', 'IN_PROGRESS', '秋冬运动双面布订单，夜班染色窗口已锁定', '2026-05-20 11:35:00'),
(90003, 'ORD-20260519-003', '际华工装面料部', '2026-05-19 15:20:00', '2026-05-28 12:00:00', 'MEDIUM', 'DONE', '工装功能整理订单，本批已完工待发运', '2026-05-19 15:25:00'),
(90004, 'ORD-20260521-004', '巴拉巴拉婴童面料中心', '2026-05-21 10:10:00', '2026-06-01 18:00:00', 'HIGH', 'PLANNING', '婴童浅色布订单，待客户确认克重窗口后下发', '2026-05-21 10:15:00'),
(90005, 'ORD-20260522-005', '都市丽人家居服事业部', '2026-05-22 14:40:00', '2026-06-03 18:00:00', 'MEDIUM', 'READY', '莫代尔棉家居服面料，待排柔软整理方案', '2026-05-22 14:45:00'),
(90006, 'ORD-20260522-006', '安踏运动针织开发部', '2026-05-22 16:10:00', '2026-06-05 18:00:00', 'MEDIUM', 'READY', '锦氨高弹运动面料，待确认定型和后整理路线', '2026-05-22 16:15:00');

-- 2. Order items
INSERT INTO orderitem (orderItemId, orderId, productCode, productName, specification, color, quantity, unit, requiredWidth, requiredWeight, remark) VALUES
(91001, 90001, 'HLJ-JERSEY-32S', '32S精梳棉弹汗布', '180g/m2', '军绿', 15000.00, 'm', 168.00, 2400.00, '需控制色差，适配外贸休闲男装'),
(91002, 90002, 'DK-DBL-SPORT', '涤氨双面运动布', '260g/m2', '深灰', 12600.00, 'm', 180.00, 3000.00, '重点控制回弹手感与深色匀染'),
(91003, 90003, 'JH-WORK-FUNC', '涤棉工装功能面料', '220g/m2', '藏青', 13800.00, 'm', 175.00, 2800.00, '需做吸湿排汗功能整理'),
(91004, 90004, 'BLB-KIDS-WHITE', '全棉婴童白坯布', '165g/m2', '米白', 16000.00, 'm', 168.00, 2500.00, '对白度与手感要求较高'),
(91005, 90005, 'DSLR-MODAL-HOME', '莫代尔棉家居服面料', '190g/m2', '豆沙粉', 11800.00, 'm', 160.00, 1900.00, '建议后续补柔软整理路线'),
(91006, 90006, 'ANTA-NYLON-ELASTIC', '锦氨高弹运动面料', '240g/m2', '黑色', 13500.00, 'm', 182.00, 3200.00, '弹力与门幅稳定性是重点');

-- 3. Order to batch links
INSERT INTO orderbatchlink (id, orderId, orderItemId, batchId, allocatedWeight, allocatedQuantity, remark) VALUES
(70001, 90001, 91001, 10001, 2400.00, 15000.00, '首批主料已全部分配到汗布订单'),
(70002, 90002, 91002, 10002, 3000.00, 12600.00, '深灰双面布已锁定夜班染色窗口'),
(70003, 90003, 91003, 10003, 2800.00, 13800.00, '工装功能整理单全量分配'),
(70004, 90004, 91004, 10004, 2500.00, 16000.00, '婴童浅色布已进入待排状态'),
(70005, 90005, 91005, 10005, 1900.00, 11800.00, '家居服面料待补整理方案后排产'),
(70006, 90006, 91006, 10006, 3200.00, 13500.00, '高弹运动面料待确认最终工艺路线');

-- 4. Bind existing production plans to orders
UPDATE productionplan SET orderId = 90001, orderItemId = 91001 WHERE planId = 60001;
UPDATE productionplan SET orderId = 90002, orderItemId = 91002 WHERE planId = 60002;
UPDATE productionplan SET orderId = 90003, orderItemId = 91003 WHERE planId = 60003;
UPDATE productionplan SET orderId = 90004, orderItemId = 91004 WHERE planId = 60004;

COMMIT;
