/*
 Navicat Premium Dump SQL

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : zhihuitong

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 22/05/2026 10:49:13
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for batchinfo
-- ----------------------------
DROP TABLE IF EXISTS `batchinfo`;
CREATE TABLE `batchinfo`  (
  `batchId` bigint NOT NULL,
  `batchNo` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `supplier` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `inDate` datetime NULL DEFAULT NULL,
  `weight` decimal(12, 2) NULL DEFAULT NULL,
  `width` decimal(8, 2) NULL DEFAULT NULL,
  `composition` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `note` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`batchId`) USING BTREE,
  UNIQUE INDEX `batchNo`(`batchNo` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exceptionrecord
-- ----------------------------
DROP TABLE IF EXISTS `exceptionrecord`;
CREATE TABLE `exceptionrecord`  (
  `exceptionId` bigint NOT NULL,
  `planStepId` bigint NULL DEFAULT NULL,
  `exceptionType` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `exceptionLevel` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `handleResult` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `createTime` datetime NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`exceptionId`) USING BTREE,
  INDEX `planStepId`(`planStepId` ASC) USING BTREE,
  CONSTRAINT `exceptionrecord_ibfk_1` FOREIGN KEY (`planStepId`) REFERENCES `planstep` (`planStepId`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for inspectiondata
-- ----------------------------
DROP TABLE IF EXISTS `inspectiondata`;
CREATE TABLE `inspectiondata`  (
  `dataId` bigint NOT NULL,
  `qcRecordId` bigint NULL DEFAULT NULL,
  `cameraId` bigint NULL DEFAULT NULL,
  `fileType` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `filePath` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `fileName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `captureTime` datetime NULL DEFAULT NULL,
  `resultSummary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`dataId`) USING BTREE,
  INDEX `qcRecordId`(`qcRecordId` ASC) USING BTREE,
  INDEX `cameraId`(`cameraId` ASC) USING BTREE,
  CONSTRAINT `inspectiondata_ibfk_1` FOREIGN KEY (`qcRecordId`) REFERENCES `qcrecord` (`inspectionId`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `inspectiondata_ibfk_2` FOREIGN KEY (`cameraId`) REFERENCES `qccamera` (`cameraId`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for machine
-- ----------------------------
DROP TABLE IF EXISTS `machine`;
CREATE TABLE `machine`  (
  `machineId` bigint NOT NULL,
  `machineCode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `machineName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `machineType` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT NULL,
  `createTime` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`machineId`) USING BTREE,
  UNIQUE INDEX `machineCode`(`machineCode` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for planstep
-- ----------------------------
DROP TABLE IF EXISTS `planstep`;
CREATE TABLE `planstep`  (
  `planStepId` bigint NOT NULL,
  `planId` bigint NULL DEFAULT NULL,
  `stepId` bigint NULL DEFAULT NULL,
  `machineId` bigint NULL DEFAULT NULL,
  `planStartTime` datetime NULL DEFAULT NULL,
  `planEndTime` datetime NULL DEFAULT NULL,
  `planHours` decimal(12, 2) NULL DEFAULT NULL,
  `sequenceNo` tinyint NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`planStepId`) USING BTREE,
  INDEX `planId`(`planId` ASC) USING BTREE,
  INDEX `stepId`(`stepId` ASC) USING BTREE,
  INDEX `machineId`(`machineId` ASC) USING BTREE,
  CONSTRAINT `planstep_ibfk_1` FOREIGN KEY (`planId`) REFERENCES `productionplan` (`planId`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `planstep_ibfk_2` FOREIGN KEY (`stepId`) REFERENCES `processstep` (`stepId`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `planstep_ibfk_3` FOREIGN KEY (`machineId`) REFERENCES `machine` (`machineId`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for processparameter
-- ----------------------------
DROP TABLE IF EXISTS `processparameter`;
CREATE TABLE `processparameter`  (
  `paramId` bigint NOT NULL,
  `planStepId` bigint NULL DEFAULT NULL,
  `paramName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `paramValue` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `paramType` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `recordTime` datetime NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`paramId`) USING BTREE,
  INDEX `planStepId`(`planStepId` ASC) USING BTREE,
  CONSTRAINT `processparameter_ibfk_1` FOREIGN KEY (`planStepId`) REFERENCES `planstep` (`planStepId`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for processroute
-- ----------------------------
DROP TABLE IF EXISTS `processroute`;
CREATE TABLE `processroute`  (
  `routeId` bigint NOT NULL,
  `routeName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `isActive` tinyint NULL DEFAULT NULL,
  `createTime` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`routeId`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for processstep
-- ----------------------------
DROP TABLE IF EXISTS `processstep`;
CREATE TABLE `processstep`  (
  `stepId` bigint NOT NULL,
  `stepCode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `stepName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `stepType` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `sortOrder` int NULL DEFAULT NULL,
  `defaultHours` decimal(8, 2) NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `isActive` tinyint NULL DEFAULT NULL,
  PRIMARY KEY (`stepId`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for productionplan
-- ----------------------------
DROP TABLE IF EXISTS `productionplan`;
CREATE TABLE `productionplan`  (
  `planId` bigint NOT NULL,
  `batchId` bigint NULL DEFAULT NULL,
  `routeId` bigint NULL DEFAULT NULL,
  `planStartTime` datetime NULL DEFAULT NULL,
  `planEndTime` datetime NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `createTime` datetime NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`planId`) USING BTREE,
  INDEX `batchId`(`batchId` ASC) USING BTREE,
  INDEX `routeId`(`routeId` ASC) USING BTREE,
  CONSTRAINT `productionplan_ibfk_1` FOREIGN KEY (`batchId`) REFERENCES `batchinfo` (`batchId`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `productionplan_ibfk_2` FOREIGN KEY (`routeId`) REFERENCES `processroute` (`routeId`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qccamera
-- ----------------------------
DROP TABLE IF EXISTS `qccamera`;
CREATE TABLE `qccamera`  (
  `cameraId` bigint NOT NULL,
  `cameraCode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `cameraName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `cameraType` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ipAddress` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`cameraId`) USING BTREE,
  UNIQUE INDEX `cameraCode`(`cameraCode` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qcitem
-- ----------------------------
DROP TABLE IF EXISTS `qcitem`;
CREATE TABLE `qcitem`  (
  `qcItemId` bigint NOT NULL,
  `qcItemCode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `qcItemName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `qcType` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `standardMin` decimal(12, 4) NULL DEFAULT NULL,
  `standardMax` decimal(12, 4) NULL DEFAULT NULL,
  `isActive` tinyint NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`qcItemId`) USING BTREE,
  UNIQUE INDEX `qcItemCode`(`qcItemCode` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qcrecord
-- ----------------------------
DROP TABLE IF EXISTS `qcrecord`;
CREATE TABLE `qcrecord`  (
  `inspectionId` bigint NOT NULL,
  `planStepId` bigint NULL DEFAULT NULL,
  `qcItemId` bigint NULL DEFAULT NULL,
  `inspectTime` datetime NULL DEFAULT NULL,
  `inspectType` enum('offline','video') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `cameraId` bigint NULL DEFAULT NULL,
  `frameTime` datetime NULL DEFAULT NULL,
  `imageUrl` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `confidenceScore` decimal(5, 2) NULL DEFAULT NULL,
  `resultValue` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `resultJudge` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `inspector` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`inspectionId`) USING BTREE,
  INDEX `planStepId`(`planStepId` ASC) USING BTREE,
  INDEX `qcItemId`(`qcItemId` ASC) USING BTREE,
  CONSTRAINT `qcrecord_ibfk_1` FOREIGN KEY (`planStepId`) REFERENCES `planstep` (`planStepId`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `qcrecord_ibfk_2` FOREIGN KEY (`qcItemId`) REFERENCES `qcitem` (`qcItemId`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for routestep
-- ----------------------------
DROP TABLE IF EXISTS `routestep`;
CREATE TABLE `routestep`  (
  `routeStepId` bigint NOT NULL,
  `routeId` bigint NULL DEFAULT NULL,
  `stepId` bigint NULL DEFAULT NULL,
  `sortOrder` int NULL DEFAULT NULL,
  `isMandatory` tinyint NULL DEFAULT NULL,
  PRIMARY KEY (`routeStepId`) USING BTREE,
  INDEX `routeId`(`routeId` ASC) USING BTREE,
  INDEX `stepId`(`stepId` ASC) USING BTREE,
  CONSTRAINT `routestep_ibfk_1` FOREIGN KEY (`routeId`) REFERENCES `processroute` (`routeId`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `routestep_ibfk_2` FOREIGN KEY (`stepId`) REFERENCES `processstep` (`stepId`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for stepmachinecapability
-- ----------------------------
DROP TABLE IF EXISTS `stepmachinecapability`;
CREATE TABLE `stepmachinecapability`  (
  `capId` bigint NOT NULL,
  `stepId` bigint NULL DEFAULT NULL,
  `machineId` bigint NULL DEFAULT NULL,
  `minWidth` decimal(8, 2) NULL DEFAULT NULL,
  `maxWidth` decimal(8, 2) NULL DEFAULT NULL,
  `maxSpeed` decimal(8, 2) NULL DEFAULT NULL,
  `maxBatchWeight` decimal(12, 2) NULL DEFAULT NULL,
  `isActive` tinyint NULL DEFAULT NULL,
  PRIMARY KEY (`capId`) USING BTREE,
  INDEX `stepId`(`stepId` ASC) USING BTREE,
  INDEX `machineId`(`machineId` ASC) USING BTREE,
  CONSTRAINT `stepmachinecapability_ibfk_1` FOREIGN KEY (`stepId`) REFERENCES `processstep` (`stepId`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `stepmachinecapability_ibfk_2` FOREIGN KEY (`machineId`) REFERENCES `machine` (`machineId`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
