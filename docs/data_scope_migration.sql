-- 数据权限归属字段迁移。先执行 auth_schema.sql，再执行本脚本。
-- 历史数据统一归属公司根部门(100)和超级管理员(1)，新数据由登录会话自动写入。
ALTER TABLE `orderinfo` ADD COLUMN `deptId` BIGINT NULL AFTER `status`, ADD COLUMN `createdBy` BIGINT NULL AFTER `deptId`, ADD INDEX `idx_order_scope` (`deptId`, `createdBy`);
ALTER TABLE `batchinfo` ADD COLUMN `deptId` BIGINT NULL AFTER `composition`, ADD COLUMN `createdBy` BIGINT NULL AFTER `deptId`, ADD INDEX `idx_batch_scope` (`deptId`, `createdBy`);
ALTER TABLE `productionplan` ADD COLUMN `deptId` BIGINT NULL AFTER `status`, ADD COLUMN `createdBy` BIGINT NULL AFTER `deptId`, ADD INDEX `idx_plan_scope` (`deptId`, `createdBy`);
ALTER TABLE `qcrecord` ADD COLUMN `deptId` BIGINT NULL AFTER `inspector`, ADD COLUMN `createdBy` BIGINT NULL AFTER `deptId`, ADD INDEX `idx_qc_scope` (`deptId`, `createdBy`);
ALTER TABLE `exceptionrecord` ADD COLUMN `deptId` BIGINT NULL AFTER `status`, ADD COLUMN `createdBy` BIGINT NULL AFTER `deptId`, ADD INDEX `idx_exception_scope` (`deptId`, `createdBy`);
UPDATE `orderinfo` SET `deptId` = 100, `createdBy` = 1 WHERE `deptId` IS NULL OR `createdBy` IS NULL;
UPDATE `batchinfo` SET `deptId` = 100, `createdBy` = 1 WHERE `deptId` IS NULL OR `createdBy` IS NULL;
UPDATE `productionplan` SET `deptId` = 100, `createdBy` = 1 WHERE `deptId` IS NULL OR `createdBy` IS NULL;
UPDATE `qcrecord` SET `deptId` = 100, `createdBy` = 1 WHERE `deptId` IS NULL OR `createdBy` IS NULL;
UPDATE `exceptionrecord` SET `deptId` = 100, `createdBy` = 1 WHERE `deptId` IS NULL OR `createdBy` IS NULL;
ALTER TABLE `orderinfo` MODIFY `deptId` BIGINT NOT NULL, MODIFY `createdBy` BIGINT NOT NULL;
ALTER TABLE `batchinfo` MODIFY `deptId` BIGINT NOT NULL, MODIFY `createdBy` BIGINT NOT NULL;
ALTER TABLE `productionplan` MODIFY `deptId` BIGINT NOT NULL, MODIFY `createdBy` BIGINT NOT NULL;
ALTER TABLE `qcrecord` MODIFY `deptId` BIGINT NOT NULL, MODIFY `createdBy` BIGINT NOT NULL;
ALTER TABLE `exceptionrecord` MODIFY `deptId` BIGINT NOT NULL, MODIFY `createdBy` BIGINT NOT NULL;