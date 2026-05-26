SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `orderinfo` (
  `orderId` bigint NOT NULL,
  `orderNo` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `customerName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `orderDate` datetime NOT NULL,
  `deliveryDate` datetime NOT NULL,
  `priority` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`orderId`) USING BTREE,
  UNIQUE INDEX `uk_orderinfo_orderNo`(`orderNo` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

CREATE TABLE IF NOT EXISTS `orderitem` (
  `orderItemId` bigint NOT NULL,
  `orderId` bigint NOT NULL,
  `productCode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `productName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `specification` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `color` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `quantity` decimal(12, 2) NOT NULL,
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `requiredWidth` decimal(8, 2) NULL DEFAULT NULL,
  `requiredWeight` decimal(12, 2) NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`orderItemId`) USING BTREE,
  INDEX `idx_orderitem_orderId`(`orderId` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

CREATE TABLE IF NOT EXISTS `orderbatchlink` (
  `id` bigint NOT NULL,
  `orderId` bigint NOT NULL,
  `orderItemId` bigint NOT NULL,
  `batchId` bigint NOT NULL,
  `allocatedWeight` decimal(12, 2) NULL DEFAULT NULL,
  `allocatedQuantity` decimal(12, 2) NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_orderbatchlink_orderId`(`orderId` ASC) USING BTREE,
  INDEX `idx_orderbatchlink_orderItemId`(`orderItemId` ASC) USING BTREE,
  INDEX `idx_orderbatchlink_batchId`(`batchId` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

SET @has_orderId := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'productionplan'
    AND COLUMN_NAME = 'orderId'
);
SET @sql_orderId := IF(@has_orderId = 0,
  'ALTER TABLE `productionplan` ADD COLUMN `orderId` bigint NULL DEFAULT NULL AFTER `planId`',
  'SELECT 1');
PREPARE stmt_orderId FROM @sql_orderId;
EXECUTE stmt_orderId;
DEALLOCATE PREPARE stmt_orderId;

SET @has_orderItemId := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'productionplan'
    AND COLUMN_NAME = 'orderItemId'
);
SET @sql_orderItemId := IF(@has_orderItemId = 0,
  'ALTER TABLE `productionplan` ADD COLUMN `orderItemId` bigint NULL DEFAULT NULL AFTER `orderId`',
  'SELECT 1');
PREPARE stmt_orderItemId FROM @sql_orderItemId;
EXECUTE stmt_orderItemId;
DEALLOCATE PREPARE stmt_orderItemId;

SET @has_idx_orderId := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'productionplan'
    AND INDEX_NAME = 'idx_productionplan_orderId'
);
SET @sql_idx_orderId := IF(@has_idx_orderId = 0,
  'CREATE INDEX `idx_productionplan_orderId` ON `productionplan` (`orderId`)',
  'SELECT 1');
PREPARE stmt_idx_orderId FROM @sql_idx_orderId;
EXECUTE stmt_idx_orderId;
DEALLOCATE PREPARE stmt_idx_orderId;

SET @has_idx_orderItemId := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'productionplan'
    AND INDEX_NAME = 'idx_productionplan_orderItemId'
);
SET @sql_idx_orderItemId := IF(@has_idx_orderItemId = 0,
  'CREATE INDEX `idx_productionplan_orderItemId` ON `productionplan` (`orderItemId`)',
  'SELECT 1');
PREPARE stmt_idx_orderItemId FROM @sql_idx_orderItemId;
EXECUTE stmt_idx_orderItemId;
DEALLOCATE PREPARE stmt_idx_orderItemId;
