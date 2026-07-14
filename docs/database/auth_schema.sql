SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_login_log`;
DROP TABLE IF EXISTS `sys_role_dept`;
DROP TABLE IF EXISTS `sys_role_menu`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_menu`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_post`;
DROP TABLE IF EXISTS `sys_dept`;

CREATE TABLE `sys_dept` (
  `deptId` BIGINT NOT NULL,
  `parentId` BIGINT NOT NULL DEFAULT 0,
  `ancestors` VARCHAR(500) NOT NULL DEFAULT '0',
  `deptName` VARCHAR(100) NOT NULL,
  `orderNum` INT NOT NULL DEFAULT 0,
  `leader` VARCHAR(50) NULL,
  `phone` VARCHAR(20) NULL,
  `email` VARCHAR(100) NULL,
  `status` CHAR(1) NOT NULL DEFAULT '0',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `createdBy` BIGINT NULL,
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedBy` BIGINT NULL,
  `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`deptId`),
  KEY `idx_sys_dept_parent` (`parentId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sys_post` (
  `postId` BIGINT NOT NULL,
  `postCode` VARCHAR(64) NOT NULL,
  `postName` VARCHAR(100) NOT NULL,
  `postSort` INT NOT NULL DEFAULT 0,
  `status` CHAR(1) NOT NULL DEFAULT '0',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `createdBy` BIGINT NULL,
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedBy` BIGINT NULL,
  `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` VARCHAR(500) NULL,
  PRIMARY KEY (`postId`),
  UNIQUE KEY `uk_sys_post_code` (`postCode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sys_role` (
  `roleId` BIGINT NOT NULL,
  `roleName` VARCHAR(100) NOT NULL,
  `roleKey` VARCHAR(100) NOT NULL,
  `roleSort` INT NOT NULL DEFAULT 0,
  `dataScope` VARCHAR(32) NOT NULL DEFAULT 'DEPT',
  `status` CHAR(1) NOT NULL DEFAULT '0',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `createdBy` BIGINT NULL,
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedBy` BIGINT NULL,
  `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` VARCHAR(500) NULL,
  PRIMARY KEY (`roleId`),
  UNIQUE KEY `uk_sys_role_key` (`roleKey`),
  UNIQUE KEY `uk_sys_role_name` (`roleName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sys_menu` (
  `menuId` BIGINT NOT NULL,
  `menuKey` VARCHAR(100) NOT NULL,
  `menuName` VARCHAR(100) NOT NULL,
  `parentId` BIGINT NOT NULL DEFAULT 0,
  `orderNum` INT NOT NULL DEFAULT 0,
  `path` VARCHAR(255) NULL,
  `component` VARCHAR(255) NULL,
  `menuType` CHAR(1) NOT NULL,
  `visible` CHAR(1) NOT NULL DEFAULT '0',
  `status` CHAR(1) NOT NULL DEFAULT '0',
  `perms` VARCHAR(150) NULL,
  `icon` VARCHAR(100) NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `createdBy` BIGINT NULL,
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedBy` BIGINT NULL,
  `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` VARCHAR(500) NULL,
  PRIMARY KEY (`menuId`),
  UNIQUE KEY `uk_sys_menu_key` (`menuKey`),
  KEY `idx_sys_menu_parent` (`parentId`),
  KEY `idx_sys_menu_perms` (`perms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sys_user` (
  `userId` BIGINT NOT NULL,
  `deptId` BIGINT NOT NULL,
  `postId` BIGINT NOT NULL,
  `roleId` BIGINT NOT NULL,
  `userName` VARCHAR(64) NOT NULL,
  `nickName` VARCHAR(100) NOT NULL,
  `email` VARCHAR(100) NULL,
  `phonenumber` VARCHAR(20) NULL,
  `avatar` VARCHAR(255) NULL,
  `password` VARCHAR(100) NOT NULL,
  `status` CHAR(1) NOT NULL DEFAULT '0',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `loginIp` VARCHAR(64) NULL,
  `loginDate` DATETIME NULL,
  `pwdUpdateTime` DATETIME NULL,
  `createdBy` BIGINT NULL,
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedBy` BIGINT NULL,
  `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` VARCHAR(500) NULL,
  PRIMARY KEY (`userId`),
  UNIQUE KEY `uk_sys_user_name` (`userName`),
  UNIQUE KEY `uk_sys_user_phone` (`phonenumber`),
  UNIQUE KEY `uk_sys_user_email` (`email`),
  KEY `idx_sys_user_dept` (`deptId`),
  KEY `idx_sys_user_role` (`roleId`),
  CONSTRAINT `fk_sys_user_dept` FOREIGN KEY (`deptId`) REFERENCES `sys_dept` (`deptId`),
  CONSTRAINT `fk_sys_user_post` FOREIGN KEY (`postId`) REFERENCES `sys_post` (`postId`),
  CONSTRAINT `fk_sys_user_role` FOREIGN KEY (`roleId`) REFERENCES `sys_role` (`roleId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sys_role_menu` (
  `roleId` BIGINT NOT NULL,
  `menuId` BIGINT NOT NULL,
  PRIMARY KEY (`roleId`, `menuId`),
  CONSTRAINT `fk_role_menu_role` FOREIGN KEY (`roleId`) REFERENCES `sys_role` (`roleId`) ON DELETE CASCADE,
  CONSTRAINT `fk_role_menu_menu` FOREIGN KEY (`menuId`) REFERENCES `sys_menu` (`menuId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sys_role_dept` (
  `roleId` BIGINT NOT NULL,
  `deptId` BIGINT NOT NULL,
  PRIMARY KEY (`roleId`, `deptId`),
  CONSTRAINT `fk_role_dept_role` FOREIGN KEY (`roleId`) REFERENCES `sys_role` (`roleId`) ON DELETE CASCADE,
  CONSTRAINT `fk_role_dept_dept` FOREIGN KEY (`deptId`) REFERENCES `sys_dept` (`deptId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sys_login_log` (
  `infoId` BIGINT NOT NULL,
  `userId` BIGINT NULL,
  `userName` VARCHAR(64) NULL,
  `sessionId` VARCHAR(64) NULL,
  `ipaddr` VARCHAR(64) NULL,
  `browser` VARCHAR(100) NULL,
  `os` VARCHAR(100) NULL,
  `status` VARCHAR(20) NOT NULL,
  `message` VARCHAR(500) NULL,
  `loginTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`infoId`),
  KEY `idx_login_log_user` (`userId`),
  KEY `idx_login_log_time` (`loginTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `sys_dept` (`deptId`,`parentId`,`ancestors`,`deptName`,`orderNum`) VALUES
(100,0,'0','织慧通公司',1),(110,100,'0,100','第一工厂',1),(111,110,'0,100,110','订单管理部',1),
(112,110,'0,100,110','来料仓',2),(113,110,'0,100,110','工艺部',3),(114,110,'0,100,110','生产车间',4),
(115,110,'0,100,110','质量部',5),(116,110,'0,100,110','设备管理组',6);

INSERT INTO `sys_post` (`postId`,`postCode`,`postName`,`postSort`) VALUES
(1,'SYSTEM_ADMIN','系统管理员',1),(2,'ORDER_CLERK','订单专员',2),(3,'WAREHOUSE_MANAGER','仓库管理员',3),
(4,'PROCESS_ENGINEER','工艺工程师',4),(5,'SCHEDULER','排产员',5),(6,'QC_INSPECTOR','质检员',6),
(7,'QC_MANAGER','质量主管',7),(8,'EQUIPMENT_MANAGER','设备管理员',8),(9,'VIEWER','只读人员',9);

INSERT INTO `sys_role` (`roleId`,`roleName`,`roleKey`,`roleSort`,`dataScope`,`remark`) VALUES
(1,'超级管理员','admin',1,'ALL','系统内置，不可删除或停用'),(2,'订单管理员','order_manager',2,'DEPT','订单与批次分配'),
(3,'来料管理员','material_manager',3,'DEPT','来料与资源池'),(4,'工艺工程师','process_engineer',4,'DEPT','工艺与设备能力'),
(5,'排产员','scheduler',5,'DEPT_AND_CHILD','排产与计划'),(6,'质检员','qc_inspector',6,'SELF','质检执行'),
(7,'质量主管','qc_manager',7,'DEPT_AND_CHILD','质量复核与异常'),(8,'只读用户','viewer',8,'DEPT','只读访问');

INSERT INTO `sys_menu` (`menuId`,`menuKey`,`menuName`,`parentId`,`orderNum`,`path`,`menuType`,`perms`) VALUES
(1000,'dashboard','看板',0,1,'/dashboard','C','dashboard:view'),
(2000,'order','订单管理',0,2,NULL,'M',NULL),(2001,'order-list','订单列表',2000,1,'/order/list','C','order:order:list'),
(2011,'order-add','订单新增',2001,1,NULL,'F','order:order:add'),(2012,'order-edit','订单修改',2001,2,NULL,'F','order:order:edit'),(2013,'order-remove','订单删除',2001,3,NULL,'F','order:order:remove'),
(3000,'gray','来料资源',0,3,NULL,'M',NULL),(3001,'gray-inbound','来料资源池',3000,1,'/gray/inbound','C','batch:resource:list'),
(3011,'batch-add','来料新增',3001,1,NULL,'F','batch:resource:add'),(3012,'batch-edit','来料修改',3001,2,NULL,'F','batch:resource:edit'),(3013,'batch-remove','来料删除',3001,3,NULL,'F','batch:resource:remove'),
(4000,'process-center','工艺中心',0,4,NULL,'M',NULL),(4001,'process-center-process','工艺路线',4000,1,'/process-center/process','C','process:route:list'),
(4002,'master-machine','设备管理',4000,2,'/master/machine','C','process:machine:list'),(4011,'process-manage','工艺维护',4001,1,NULL,'F','process:route:edit'),(4012,'machine-manage','设备维护',4002,1,NULL,'F','process:machine:edit'),
(5000,'schedule','排产管理',0,5,NULL,'M',NULL),(5001,'schedule-order-pool','订单排产池',5000,1,'/schedule/order-pool','C','plan:production:list'),
(5002,'schedule-pool','批次执行池',5000,2,'/schedule/pool','C','plan:production:list'),(5003,'schedule-main','生产计划',5000,3,'/schedule/main','C','plan:production:list'),
(5004,'schedule-board','调度甘特图',5000,4,'/schedule/board','C','plan:production:list'),(5005,'schedule-ai-advisor','AI智能排产',5000,5,'/schedule/ai-advisor','C','plan:production:list'),(5011,'plan-create','创建计划',5003,1,NULL,'F','plan:production:create'),
(5012,'plan-edit','修改计划',5003,2,NULL,'F','plan:production:edit'),(5013,'plan-reschedule','计划重排',5003,3,NULL,'F','plan:production:reschedule'),
(6000,'quality','质量管理',0,6,NULL,'M',NULL),(6001,'quality-realtime','实时质检',6000,1,'/quality/realtime','C','quality:realtime:view'),
(6002,'quality-ncr','异常闭环',6000,2,'/quality/ncr','C','exception:record:list'),(6003,'quality-rework','返工处理',6000,3,'/quality/rework','C','exception:record:list'),(6004,'quality-ai-analysis','AI质检分析',6000,4,'/quality/ai-analysis','C','quality:realtime:view'),
(6011,'quality-detect','执行质检',6001,1,NULL,'F','quality:realtime:detect'),(6012,'quality-review','质检复核',6001,2,NULL,'F','quality:record:review'),
(6013,'exception-handle','异常处理',6002,1,NULL,'F','exception:record:handle'),(6014,'exception-rework','返工登记',6003,1,NULL,'F','exception:record:rework'),
(7000,'system','系统管理',0,7,NULL,'M',NULL),(7001,'system-user','用户管理',7000,1,'/system/user','C','system:user:list'),
(7002,'system-role','角色管理',7000,2,'/system/role','C','system:role:list'),(7003,'system-menu','菜单管理',7000,3,'/system/menu','C','system:menu:list'),
(7004,'system-dept','部门管理',7000,4,'/system/dept','C','system:dept:list'),(7005,'system-post','岗位管理',7000,5,'/system/post','C','system:post:list'),
(7006,'system-online','在线用户',7000,6,'/system/online','C','system:online:list'),
(7011,'system-user-manage','用户维护',7001,1,NULL,'F','system:user:manage'),(7012,'system-role-manage','角色维护',7002,1,NULL,'F','system:role:manage'),
(7013,'system-menu-manage','菜单维护',7003,1,NULL,'F','system:menu:manage'),(7014,'system-dept-manage','部门维护',7004,1,NULL,'F','system:dept:manage'),
(7015,'system-post-manage','岗位维护',7005,1,NULL,'F','system:post:manage'),(7016,'system-online-force','强制下线',7006,1,NULL,'F','system:online:forceLogout');

INSERT INTO `sys_user` (`userId`,`deptId`,`postId`,`roleId`,`userName`,`nickName`,`email`,`password`,`status`,`pwdUpdateTime`,`remark`) VALUES
(1,100,1,1,'admin','超级管理员','admin@zhihuitong.local','$2a$10$GsGsTb82EMoUZzZmp75M5uFPiBwa2lJd1eJrF84IdocUoJR5DKEoy','0',NOW(),'初始密码 Admin@123，首次部署后必须修改');

INSERT INTO `sys_role_menu` (`roleId`,`menuId`) SELECT 1, `menuId` FROM `sys_menu`;
INSERT INTO `sys_role_menu` VALUES
(2,1000),(2,2000),(2,2001),(2,2011),(2,2012),(2,2013),(2,3000),(2,3001),
(3,1000),(3,3000),(3,3001),(3,3011),(3,3012),(3,3013),
(4,1000),(4,4000),(4,4001),(4,4002),(4,4011),(4,4012),
(5,1000),(5,2000),(5,2001),(5,3000),(5,3001),(5,5000),(5,5001),(5,5002),(5,5003),(5,5004),(5,5005),(5,5011),(5,5012),(5,5013),
(6,1000),(6,6000),(6,6001),(6,6004),(6,6011),(6,6002),(6,6003),
(7,1000),(7,6000),(7,6001),(7,6002),(7,6003),(7,6004),(7,6012),(7,6013),(7,6014),
(8,1000),(8,2000),(8,2001),(8,3000),(8,3001),(8,4000),(8,4001),(8,4002),(8,5000),(8,5001),(8,5002),(8,5003),(8,5004),(8,5005),(8,6000),(8,6001),(8,6002),(8,6003),(8,6004);

INSERT INTO `sys_role_dept` (`roleId`,`deptId`) VALUES (2,111),(3,112),(4,113),(5,114),(6,115),(7,115),(8,110);

SET FOREIGN_KEY_CHECKS = 1;
