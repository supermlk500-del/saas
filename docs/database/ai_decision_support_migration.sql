-- AI 智能排产与 AI 质检分析菜单增量迁移。
-- 适用于已经执行过 auth_schema.sql 的现有数据库，不会删除或重建权限表。

CREATE TABLE IF NOT EXISTS `aischedulerecord` (
  `recordId` BIGINT NOT NULL,
  `planId` BIGINT NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
  `selectedStrategy` VARCHAR(20) NULL,
  `snapshotJson` LONGTEXT NOT NULL,
  `deptId` BIGINT NOT NULL,
  `createdBy` BIGINT NOT NULL,
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `appliedTime` DATETIME NULL,
  PRIMARY KEY (`recordId`),
  KEY `idx_ai_schedule_plan_time` (`planId`, `createTime`),
  KEY `idx_ai_schedule_scope` (`deptId`, `createdBy`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `sys_menu`
(`menuId`,`menuKey`,`menuName`,`parentId`,`orderNum`,`path`,`menuType`,`visible`,`status`,`perms`,`deleted`)
VALUES
(5005,'schedule-ai-advisor','AI智能排产',5000,5,'/schedule/ai-advisor','C','0','0','plan:production:list',0),
(6004,'quality-ai-analysis','AI质检分析',6000,4,'/quality/ai-analysis','C','0','0','quality:realtime:view',0)
ON DUPLICATE KEY UPDATE
`menuName` = VALUES(`menuName`),
`parentId` = VALUES(`parentId`),
`orderNum` = VALUES(`orderNum`),
`path` = VALUES(`path`),
`perms` = VALUES(`perms`),
`status` = '0',
`deleted` = 0;

INSERT IGNORE INTO `sys_role_menu` (`roleId`,`menuId`) VALUES
(1,5005),(1,6004),
(5,5005),
(6,6004),
(7,6004),
(8,5005),(8,6004);
