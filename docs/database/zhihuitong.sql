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

 Date: 20/06/2026 19:59:24
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for aischedulerecord
-- ----------------------------
DROP TABLE IF EXISTS `aischedulerecord`;
CREATE TABLE `aischedulerecord`  (
  `recordId` bigint NOT NULL,
  `planId` bigint NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'GENERATED',
  `selectedStrategy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `snapshotJson` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `deptId` bigint NOT NULL,
  `createdBy` bigint NOT NULL,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `appliedTime` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`recordId`) USING BTREE,
  INDEX `idx_ai_schedule_plan_time`(`planId` ASC, `createTime` ASC) USING BTREE,
  INDEX `idx_ai_schedule_scope`(`deptId` ASC, `createdBy` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of aischedulerecord
-- ----------------------------
INSERT INTO `aischedulerecord` VALUES (2068300098185072641, 2067894171762089985, 'GENERATED', NULL, '{\"recordId\":null,\"planId\":\"2067894171762089985\",\"orderNo\":\"1\",\"batchNo\":\"YL-JN-20260618-004\",\"deliveryDate\":\"2026-06-19 12:00:14\",\"decisionEngine\":\"OR-Tools CP-SAT\",\"explanationModel\":\"deepseek-v4-flash\",\"notice\":\"排产结果由运筹优化算法计算，大模型仅用于解释；应用前必须人工确认。\",\"recordStatus\":null,\"selectedStrategy\":null,\"createTime\":null,\"appliedTime\":null,\"options\":[{\"strategy\":\"DELIVERY\",\"strategyName\":\"交期优先\",\"solverStatus\":\"OPTIMAL\",\"startTime\":\"2026-06-19 16:55:57\",\"endTime\":\"2026-06-20 11:49:57\",\"totalMinutes\":\"1134\",\"delayMinutes\":\"1429\",\"machineChanges\":6,\"estimatedUtilizationScore\":100,\"explanation\":\"好的，这是对您提供的 OR-Tools 排产结果的分析。\\n\\n### 方案优势\\n\\n*   **交期优先策略**：方案明确以“交期优先”为目标，旨在最大化满足客户交期。\\n*   **连续生产**：从退浆到卷装，所有工序无缝衔接，没有等待时间，生产周期紧凑。\\n*   **设备利用率高**：预估利用率评分达到100%，说明方案在理论上充分利用了所选机台的生产能力。\\n\\n### 主要风险\\n\\n*   **严重延期**：**总延期时间高达1429分钟（约23.8小时）**。这是本方案最大的问题，意味着即使采用交期优先策略，该订单也无法按时完成，存在严重的交付风险。\\n*   **机台切换频繁**：方案涉及6次机台切换，增加了物料搬运和换线准备的工作量，可能引入额外的等待或操作失误风险。\\n\\n### 人工确认重点\\n\\n1.  **延期原因**：请务必与计划员确认，为何在“交期优先”策略下仍产生如此严重的延期？是产能不足、物料未到，还是交期本身设定不合理？\\n2.  **机台可用性**：方案中所有机台在分配的时间段内均显示为“IDLE”（空闲）。请人工核实这些机台是否确实可用，有无被其他紧急订单占用或处于维修状态。\\n3.  **染色工序**：染色工序（高温溢流染色机1号）耗时最长（5.4小时），是整个流程的瓶颈。请确认该机台的能力（如浴比、温度控制）是否完全匹配此订单的工艺要求。\\n4.  **最终交付时间**：方案预计在 **2026-06-20 11:49:57** 完成。请将此时间与客户要求的交期进行对比，评估延期带来的商务影响，并决定是否需要调整生产优先级或与客户沟通。\",\"model\":\"deepseek-v4-flash\",\"aiGenerated\":true,\"fallbackReason\":null,\"steps\":[{\"planStepId\":\"2067894172089245697\",\"stepId\":\"30001\",\"stepName\":\"退浆\",\"machineId\":\"40001\",\"machineCode\":\"PT-DS-01\",\"machineName\":\"连续退浆机1号\",\"recommendationScore\":282,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 282\",\"startTime\":\"2026-06-19 16:55:57\",\"endTime\":\"2026-06-19 20:04:57\",\"predictedHours\":3.15},{\"planStepId\":\"2067894172089245698\",\"stepId\":\"30002\",\"stepName\":\"煮练\",\"machineId\":\"40002\",\"machineCode\":\"PT-SC-01\",\"machineName\":\"高效煮练机1号\",\"recommendationScore\":276,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 276\",\"startTime\":\"2026-06-19 20:04:57\",\"endTime\":\"2026-06-19 23:40:57\",\"predictedHours\":3.60},{\"planStepId\":\"2067894172089245699\",\"stepId\":\"30004\",\"stepName\":\"染色\",\"machineId\":\"40004\",\"machineCode\":\"DY-OF-01\",\"machineName\":\"高温溢流染色机1号\",\"recommendationScore\":264,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 264\",\"startTime\":\"2026-06-19 23:40:57\",\"endTime\":\"2026-06-20 05:04:57\",\"predictedHours\":5.40},{\"planStepId\":\"2067894172152160257\",\"stepId\":\"30005\",\"stepName\":\"染后水洗\",\"machineId\":\"40006\",\"machineCode\":\"WS-01\",\"machineName\":\"连续水洗机1号\",\"recommendationScore\":272,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 272\",\"startTime\":\"2026-06-20 05:04:57\",\"endTime\":\"2026-06-20 07:19:57\",\"predictedHours\":2.25},{\"planStepId\":\"2067894172152160258\",\"stepId\":\"30007\",\"stepName\":\"拉幅定型\",\"machineId\":\"40009\",\"machineCode\":\"ST-02\",\"machineName\":\"拉幅定型机2号\",\"recommendationScore\":260,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 260\",\"startTime\":\"2026-06-20 07:19:57\",\"endTime\":\"2026-06-20 09:34:57\",\"predictedHours\":2.25},{\"planStepId\":\"2067894172152160259\",\"stepId\":\"30008\",\"stepName\":\"成品检验\",\"machineId\":\"40011\",\"machineCode\":\"IN-02\",\"machineName\":\"智能验布机2号\",\"recommendationScore\":302,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 302\",\"startTime\":\"2026-06-20 09:34:57\",\"endTime\":\"2026-06-20 10:55:57\",\"predictedHours\":1.35},{\"planStepId\":\"2067894172152160260\",\"stepId\":\"30009\",\"stepName\":\"卷装\",\"machineId\":\"40012\",\"machineCode\":\"PK-01\",\"machineName\":\"自动卷装机1号\",\"recommendationScore\":302,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 302\",\"startTime\":\"2026-06-20 10:55:57\",\"endTime\":\"2026-06-20 11:49:57\",\"predictedHours\":0.90}]},{\"strategy\":\"UTILIZATION\",\"strategyName\":\"设备利用率优先\",\"solverStatus\":\"OPTIMAL\",\"startTime\":\"2026-06-19 16:55:57\",\"endTime\":\"2026-06-20 13:55:57\",\"totalMinutes\":\"1260\",\"delayMinutes\":\"1555\",\"machineChanges\":6,\"estimatedUtilizationScore\":100,\"explanation\":\"好的，作为纺织生产计划分析助手，以下是对该排产结果的分析说明：\\n\\n### 方案优势\\n- **设备利用率优先**：方案为每道工序都分配了当时处于空闲（IDLE）且能力匹配的设备，确保了设备不闲置，利用率达到100%。\\n- **连续无等待**：从退浆到卷装，7道工序紧密衔接，上一道工序结束时间即为下一道工序开始时间，生产流程非常顺畅，无中间等待浪费。\\n\\n### 主要风险与延期情况\\n- **严重延期**：方案总时长1260分钟（21小时），但总延期时间高达1555分钟（约25.9小时）。这意味着该订单的交期非常紧张，当前排产结果已远超原定计划时间。\\n- **无缓冲时间**：工序间零等待虽然效率高，但抗风险能力极弱。任何一道工序出现设备故障、质量问题或物料延迟，都会导致后续所有工序顺延，进一步加剧延期。\\n\\n### 人工确认重点\\n1.  **延期接受度**：请与销售或客户确认，当前排产导致的**1555分钟延期**是否可接受？是否需要调整交期或启用加急方案？\\n2.  **设备状态确认**：请现场确认所列设备（连续退浆机1号、高效煮练机1号等）在计划开始时间（2026-06-19 16:55）确实处于**空闲且完好**状态，避免因设备问题导致计划无法执行。\\n3.  **物料与人员**：请确认所有工序所需的染料、助剂、坯布等物料已到位，且各机台的操作人员已安排妥当，确保计划能按时启动。\",\"model\":\"deepseek-v4-flash\",\"aiGenerated\":true,\"fallbackReason\":null,\"steps\":[{\"planStepId\":\"2067894172089245697\",\"stepId\":\"30001\",\"stepName\":\"退浆\",\"machineId\":\"40001\",\"machineCode\":\"PT-DS-01\",\"machineName\":\"连续退浆机1号\",\"recommendationScore\":387,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 387\",\"startTime\":\"2026-06-19 16:55:57\",\"endTime\":\"2026-06-19 20:25:57\",\"predictedHours\":3.50},{\"planStepId\":\"2067894172089245698\",\"stepId\":\"30002\",\"stepName\":\"煮练\",\"machineId\":\"40002\",\"machineCode\":\"PT-SC-01\",\"machineName\":\"高效煮练机1号\",\"recommendationScore\":384,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 384\",\"startTime\":\"2026-06-19 20:25:57\",\"endTime\":\"2026-06-20 00:25:57\",\"predictedHours\":4.00},{\"planStepId\":\"2067894172089245699\",\"stepId\":\"30004\",\"stepName\":\"染色\",\"machineId\":\"40004\",\"machineCode\":\"DY-OF-01\",\"machineName\":\"高温溢流染色机1号\",\"recommendationScore\":382,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 382\",\"startTime\":\"2026-06-20 00:25:57\",\"endTime\":\"2026-06-20 06:25:57\",\"predictedHours\":6.00},{\"planStepId\":\"2067894172152160257\",\"stepId\":\"30005\",\"stepName\":\"染后水洗\",\"machineId\":\"40006\",\"machineCode\":\"WS-01\",\"machineName\":\"连续水洗机1号\",\"recommendationScore\":382,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 382\",\"startTime\":\"2026-06-20 06:25:57\",\"endTime\":\"2026-06-20 08:55:57\",\"predictedHours\":2.50},{\"planStepId\":\"2067894172152160258\",\"stepId\":\"30007\",\"stepName\":\"拉幅定型\",\"machineId\":\"40009\",\"machineCode\":\"ST-02\",\"machineName\":\"拉幅定型机2号\",\"recommendationScore\":380,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 380\",\"startTime\":\"2026-06-20 08:55:57\",\"endTime\":\"2026-06-20 11:25:57\",\"predictedHours\":2.50},{\"planStepId\":\"2067894172152160259\",\"stepId\":\"30008\",\"stepName\":\"成品检验\",\"machineId\":\"40010\",\"machineCode\":\"IN-01\",\"machineName\":\"成品验布机1号\",\"recommendationScore\":400,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 400\",\"startTime\":\"2026-06-20 11:25:57\",\"endTime\":\"2026-06-20 12:55:57\",\"predictedHours\":1.50},{\"planStepId\":\"2067894172152160260\",\"stepId\":\"30009\",\"stepName\":\"卷装\",\"machineId\":\"40012\",\"machineCode\":\"PK-01\",\"machineName\":\"自动卷装机1号\",\"recommendationScore\":397,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 397\",\"startTime\":\"2026-06-20 12:55:57\",\"endTime\":\"2026-06-20 13:55:57\",\"predictedHours\":1.00}]},{\"strategy\":\"COST\",\"strategyName\":\"成本优先\",\"solverStatus\":\"OPTIMAL\",\"startTime\":\"2026-06-19 16:55:57\",\"endTime\":\"2026-06-20 15:00:57\",\"totalMinutes\":\"1325\",\"delayMinutes\":\"1620\",\"machineChanges\":6,\"estimatedUtilizationScore\":100,\"explanation\":\"好的，这是对您提供的 OR-Tools 排产结果的分析。\\n\\n### 方案优势\\n\\n*   **成本最优**：求解器状态为“OPTIMAL”，策略为“成本优先”，说明此方案在满足所有约束的前提下，实现了生产成本的最小化。\\n*   **设备利用率高**：预估设备利用率评分达到满分100分，且所有工序均分配在“空闲（IDLE）”机台上，表明方案有效利用了现有设备资源，减少了闲置。\\n*   **流程连续**：从退浆到卷装，各工序时间紧密衔接，无明显的等待间隙，生产流程顺畅。\\n\\n### 主要风险与延期情况\\n\\n*   **存在延期**：方案总耗时1325分钟（约22小时），但总延期时间高达1620分钟（27小时）。这意味着该订单的交付日期可能比预期晚了近一天，存在客户交付风险。\\n*   **换线成本**：方案涉及6次机台切换，虽然次数不多，但每次切换都可能带来短暂的准备时间或质量波动风险。\\n\\n### 人工确认重点\\n\\n1.  **延期原因**：请确认延期1620分钟是否可接受？是否需要与销售或客户沟通调整交期？延期是由于订单本身紧急，还是因为“成本优先”策略选择了更慢但更便宜的设备？\\n2.  **机台状态**：方案中所有机台在计划开始时均为“IDLE”状态。请人工核实这些机台在`2026-06-19 16:55`时是否确实空闲、无故障、且已准备好生产。\\n3.  **染色工序**：染色工序（高温溢流染色机2号）耗时最长（6.3小时），是生产瓶颈。请确认该机台的工艺参数、染料、助剂等是否已准备就绪，避免因准备不足导致整体延误。\\n4.  **质量风险**：虽然方案连续，但连续生产对前道工序（退浆、煮练）的质量稳定性要求高。建议在关键工序（如染色前）设置质量检查点，防止批量返工。\",\"model\":\"deepseek-v4-flash\",\"aiGenerated\":true,\"fallbackReason\":null,\"steps\":[{\"planStepId\":\"2067894172089245697\",\"stepId\":\"30001\",\"stepName\":\"退浆\",\"machineId\":\"40001\",\"machineCode\":\"PT-DS-01\",\"machineName\":\"连续退浆机1号\",\"recommendationScore\":301,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 301\",\"startTime\":\"2026-06-19 16:55:57\",\"endTime\":\"2026-06-19 20:36:57\",\"predictedHours\":3.68},{\"planStepId\":\"2067894172089245698\",\"stepId\":\"30002\",\"stepName\":\"煮练\",\"machineId\":\"40002\",\"machineCode\":\"PT-SC-01\",\"machineName\":\"高效煮练机1号\",\"recommendationScore\":298,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 298\",\"startTime\":\"2026-06-19 20:36:57\",\"endTime\":\"2026-06-20 00:48:57\",\"predictedHours\":4.20},{\"planStepId\":\"2067894172089245699\",\"stepId\":\"30004\",\"stepName\":\"染色\",\"machineId\":\"40005\",\"machineCode\":\"DY-OF-02\",\"machineName\":\"高温溢流染色机2号\",\"recommendationScore\":290,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 290\",\"startTime\":\"2026-06-20 00:48:57\",\"endTime\":\"2026-06-20 07:06:57\",\"predictedHours\":6.30},{\"planStepId\":\"2067894172152160257\",\"stepId\":\"30005\",\"stepName\":\"染后水洗\",\"machineId\":\"40006\",\"machineCode\":\"WS-01\",\"machineName\":\"连续水洗机1号\",\"recommendationScore\":296,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 296\",\"startTime\":\"2026-06-20 07:06:57\",\"endTime\":\"2026-06-20 09:44:57\",\"predictedHours\":2.63},{\"planStepId\":\"2067894172152160258\",\"stepId\":\"30007\",\"stepName\":\"拉幅定型\",\"machineId\":\"40008\",\"machineCode\":\"ST-01\",\"machineName\":\"拉幅定型机1号\",\"recommendationScore\":286,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 286\",\"startTime\":\"2026-06-20 09:44:57\",\"endTime\":\"2026-06-20 12:22:57\",\"predictedHours\":2.63},{\"planStepId\":\"2067894172152160259\",\"stepId\":\"30008\",\"stepName\":\"成品检验\",\"machineId\":\"40011\",\"machineCode\":\"IN-02\",\"machineName\":\"智能验布机2号\",\"recommendationScore\":311,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 311\",\"startTime\":\"2026-06-20 12:22:57\",\"endTime\":\"2026-06-20 13:57:57\",\"predictedHours\":1.58},{\"planStepId\":\"2067894172152160260\",\"stepId\":\"30009\",\"stepName\":\"卷装\",\"machineId\":\"40012\",\"machineCode\":\"PK-01\",\"machineName\":\"自动卷装机1号\",\"recommendationScore\":311,\"recommendationReason\":\"能力约束匹配；IDLE；策略评分 311\",\"startTime\":\"2026-06-20 13:57:57\",\"endTime\":\"2026-06-20 15:00:57\",\"predictedHours\":1.05}]}]}', 100, 1, '2026-06-20 19:49:05', NULL);

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
  `deptId` bigint NOT NULL,
  `createdBy` bigint NOT NULL,
  `note` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`batchId`) USING BTREE,
  UNIQUE INDEX `batchNo`(`batchNo` ASC) USING BTREE,
  INDEX `idx_batch_scope`(`deptId` ASC, `createdBy` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of batchinfo
-- ----------------------------
INSERT INTO `batchinfo` VALUES (30001, 'YL-JN-20260618-001', '江南原料仓', '2026-06-18 08:10:00', 800.00, 168.00, '涤棉 65/35', 100, 1, '24/m2，黑色胚布，优先用于江南皮革厂黑色涤棉订单');
INSERT INTO `batchinfo` VALUES (30002, 'YL-JN-20260618-002', '苏州联盛纺织', '2026-06-18 08:35:00', 620.00, 165.00, '涤棉 65/35', 100, 1, '24/m2，深灰色胚布，可作为黑色订单备选批次');
INSERT INTO `batchinfo` VALUES (30003, 'YL-JN-20260618-003', '常熟恒丰织造', '2026-06-18 09:05:00', 960.00, 160.00, '涤棉 65/35', 100, 1, '24/m2，本白胚布，满足160厘米门幅要求');
INSERT INTO `batchinfo` VALUES (30004, 'YL-JN-20260618-004', '无锡华锦纺织', '2026-06-18 09:30:00', 1250.00, 180.00, '涤棉 65/35', 100, 1, '24/m2，深色专用大批量胚布，适合连续生产');
INSERT INTO `batchinfo` VALUES (30005, 'YL-JN-20260618-005', '南通新源布业', '2026-06-18 10:00:00', 540.00, 158.00, '涤棉 80/20', 100, 1, '24/m2，黑色胚布，门幅不足160厘米，用于验证候选过滤');
INSERT INTO `batchinfo` VALUES (30006, 'YL-JN-20260618-006', '盐城棉纺供应中心', '2026-06-18 10:20:00', 880.00, 172.00, '纯棉 100%', 100, 1, '本白纯棉胚布，用于纯棉产品订单');
INSERT INTO `batchinfo` VALUES (30007, 'YL-JN-20260618-007', '嘉兴涤纶原料厂', '2026-06-18 10:45:00', 730.00, 175.00, '涤纶 100%', 100, 1, '深色涤纶胚布，用于纯涤产品订单');
INSERT INTO `batchinfo` VALUES (30008, 'YL-JN-20260618-008', '吴江锦棉织造', '2026-06-18 11:10:00', 680.00, 170.00, '锦棉 60/40', 100, 1, '中性色锦棉胚布，用于锦棉类产品订单');

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
  `deptId` bigint NOT NULL,
  `createdBy` bigint NOT NULL,
  PRIMARY KEY (`exceptionId`) USING BTREE,
  INDEX `planStepId`(`planStepId` ASC) USING BTREE,
  INDEX `idx_exception_scope`(`deptId` ASC, `createdBy` ASC) USING BTREE,
  CONSTRAINT `exceptionrecord_ibfk_1` FOREIGN KEY (`planStepId`) REFERENCES `planstep` (`planStepId`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exceptionrecord
-- ----------------------------

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
-- Records of inspectiondata
-- ----------------------------

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
-- Records of machine
-- ----------------------------
INSERT INTO `machine` VALUES (40001, 'PT-DS-01', '连续退浆机1号', 'PRETREAT', '适用于150至185厘米门幅胚布退浆，前处理首道设备', 1, '2026-06-18 11:05:57');
INSERT INTO `machine` VALUES (40002, 'PT-SC-01', '高效煮练机1号', 'PRETREAT', '棉及涤棉织物煮练，去除油剂与天然杂质', 1, '2026-06-18 11:05:57');
INSERT INTO `machine` VALUES (40003, 'PT-BL-01', '连续漂白机1号', 'PRETREAT', '用于浅色订单及白度要求较高产品的漂白处理', 1, '2026-06-18 11:05:57');
INSERT INTO `machine` VALUES (40004, 'DY-OF-01', '高温溢流染色机1号', 'DYEING', '主力染色设备，适合涤棉及深色订单', 1, '2026-06-18 11:05:57');
INSERT INTO `machine` VALUES (40005, 'DY-OF-02', '高温溢流染色机2号', 'DYEING', '并行染色设备，可用于插单或颜色切换', 1, '2026-06-18 11:05:57');
INSERT INTO `machine` VALUES (40006, 'WS-01', '连续水洗机1号', 'POST_DYE', '染后皂洗与多槽串联水洗设备', 1, '2026-06-18 11:05:57');
INSERT INTO `machine` VALUES (40007, 'FN-FN-01', '功能整理机1号', 'FINISHING', '支持柔软、吸湿排汗及耐磨等后整理工艺', 1, '2026-06-18 11:05:57');
INSERT INTO `machine` VALUES (40008, 'ST-01', '拉幅定型机1号', 'FINISHING', '控制成品门幅、克重和布面平整度的主力设备', 1, '2026-06-18 11:05:57');
INSERT INTO `machine` VALUES (40009, 'ST-02', '拉幅定型机2号', 'FINISHING', '备用及并行定型设备，支持紧急订单插排', 1, '2026-06-18 11:05:57');
INSERT INTO `machine` VALUES (40010, 'IN-01', '成品验布机1号', 'INSPECTION', '用于成品外观检验、缺陷确认和匹号', 1, '2026-06-18 11:05:57');
INSERT INTO `machine` VALUES (40011, 'IN-02', '智能验布机2号', 'INSPECTION', '用于并行质检和视觉检测流程验证', 1, '2026-06-18 11:05:57');
INSERT INTO `machine` VALUES (40012, 'PK-01', '自动卷装机1号', 'PACKING', '支持自动卷装、长度统计与标签粘贴', 1, '2026-06-18 11:05:57');

-- ----------------------------
-- Table structure for orderbatchlink
-- ----------------------------
DROP TABLE IF EXISTS `orderbatchlink`;
CREATE TABLE `orderbatchlink`  (
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of orderbatchlink
-- ----------------------------
INSERT INTO `orderbatchlink` VALUES (2067894081014128641, 2067457510645342210, 2067457511756832769, 30004, 200.00, NULL, NULL);

-- ----------------------------
-- Table structure for orderinfo
-- ----------------------------
DROP TABLE IF EXISTS `orderinfo`;
CREATE TABLE `orderinfo`  (
  `orderId` bigint NOT NULL,
  `orderNo` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `customerName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `orderDate` datetime NOT NULL,
  `deliveryDate` datetime NOT NULL,
  `priority` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `deptId` bigint NOT NULL,
  `createdBy` bigint NOT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`orderId`) USING BTREE,
  UNIQUE INDEX `uk_orderinfo_orderNo`(`orderNo` ASC) USING BTREE,
  INDEX `idx_order_scope`(`deptId` ASC, `createdBy` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of orderinfo
-- ----------------------------
INSERT INTO `orderinfo` VALUES (2067457510645342210, '1', '江南皮革厂', '2026-06-16 12:00:17', '2026-06-19 12:00:14', 'NORMAL', 'NEW', 100, 1, NULL, '2026-06-18 12:00:57');

-- ----------------------------
-- Table structure for orderitem
-- ----------------------------
DROP TABLE IF EXISTS `orderitem`;
CREATE TABLE `orderitem`  (
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of orderitem
-- ----------------------------
INSERT INTO `orderitem` VALUES (2067457511756832769, 2067457510645342210, 'zy', '胚布', '24/m2', '黑', 1000.00, 'm', 168.00, 200.00, NULL);

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
-- Records of planstep
-- ----------------------------
INSERT INTO `planstep` VALUES (2067894172089245697, 2067894171762089985, 30001, 40001, '2026-06-19 16:55:57', '2026-06-19 20:25:57', 3.50, 1, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2067894172089245698, 2067894171762089985, 30002, 40002, '2026-06-19 20:25:57', '2026-06-20 00:25:57', 4.00, 2, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2067894172089245699, 2067894171762089985, 30004, 40005, '2026-06-20 00:25:57', '2026-06-20 06:25:57', 6.00, 3, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2067894172152160257, 2067894171762089985, 30005, 40006, '2026-06-20 06:25:57', '2026-06-20 08:55:57', 2.50, 4, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2067894172152160258, 2067894171762089985, 30007, 40008, '2026-06-20 08:55:57', '2026-06-20 11:25:57', 2.50, 5, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2067894172152160259, 2067894171762089985, 30008, 40011, '2026-06-20 11:25:57', '2026-06-20 12:55:57', 1.50, 6, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2067894172152160260, 2067894171762089985, 30009, 40012, '2026-06-20 12:55:57', '2026-06-20 13:55:57', 1.00, 7, 'PENDING', NULL);

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
-- Records of processparameter
-- ----------------------------

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
-- Records of processroute
-- ----------------------------
INSERT INTO `processroute` VALUES (20001, '涤棉常规染整路线', '适用于现有涤棉65/35及80/20来料：退浆、煮练、染色、水洗、定型、成检、卷装', 1, '2026-06-18 15:09:02');
INSERT INTO `processroute` VALUES (20002, '纯棉漂白染整路线', '适用于纯棉100%及浅色产品：退浆、煮练、漂白、染色、水洗、定型、成检、卷装', 1, '2026-06-18 15:09:02');
INSERT INTO `processroute` VALUES (20003, '纯涤快速染整路线', '适用于涤纶100%来料：染色、水洗、定型、成检、卷装，减少不必要前处理', 1, '2026-06-18 15:09:02');
INSERT INTO `processroute` VALUES (20004, '锦棉功能整理路线', '适用于锦棉混纺来料：煮练、染色、水洗、功能整理、定型、成检、卷装', 1, '2026-06-18 15:09:02');

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
-- Records of processstep
-- ----------------------------
INSERT INTO `processstep` VALUES (30001, 'DESIZE', '退浆', 'PRETREAT', 10, 3.50, '去除浆料和油剂，为后续煮练染色做准备', 1);
INSERT INTO `processstep` VALUES (30002, 'SCOUR', '煮练', 'PRETREAT', 20, 4.00, '去除纤维杂质、蜡质和残余油污', 1);
INSERT INTO `processstep` VALUES (30003, 'BLEACH', '漂白', 'PRETREAT', 30, 3.00, '提升白度，适用于纯棉和浅色产品', 1);
INSERT INTO `processstep` VALUES (30004, 'DYE', '染色', 'DYEING', 40, 6.00, '溢流染色，覆盖涤棉、纯棉、纯涤和锦棉产品', 1);
INSERT INTO `processstep` VALUES (30005, 'WASH', '染后水洗', 'POST_DYE', 50, 2.50, '染后皂洗、还原清洗及浮色去除', 1);
INSERT INTO `processstep` VALUES (30006, 'FINISH', '功能整理', 'FINISHING', 60, 3.00, '进行吸湿排汗、柔软或耐磨功能整理', 1);
INSERT INTO `processstep` VALUES (30007, 'STENTER', '拉幅定型', 'FINISHING', 70, 2.50, '控制门幅、克重、手感和尺寸稳定性', 1);
INSERT INTO `processstep` VALUES (30008, 'FINAL_INSPECT', '成品检验', 'INSPECTION', 80, 1.50, '进行成品外观、门幅和质量判定', 1);
INSERT INTO `processstep` VALUES (30009, 'ROLLING', '卷装', 'PACKING', 90, 1.00, '完成卷装、匹号和标签粘贴', 1);

-- ----------------------------
-- Table structure for productionplan
-- ----------------------------
DROP TABLE IF EXISTS `productionplan`;
CREATE TABLE `productionplan`  (
  `planId` bigint NOT NULL,
  `orderId` bigint NULL DEFAULT NULL,
  `orderItemId` bigint NULL DEFAULT NULL,
  `batchId` bigint NULL DEFAULT NULL,
  `routeId` bigint NULL DEFAULT NULL,
  `planStartTime` datetime NULL DEFAULT NULL,
  `planEndTime` datetime NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `deptId` bigint NOT NULL,
  `createdBy` bigint NOT NULL,
  `createTime` datetime NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`planId`) USING BTREE,
  INDEX `batchId`(`batchId` ASC) USING BTREE,
  INDEX `routeId`(`routeId` ASC) USING BTREE,
  INDEX `idx_productionplan_orderId`(`orderId` ASC) USING BTREE,
  INDEX `idx_productionplan_orderItemId`(`orderItemId` ASC) USING BTREE,
  INDEX `idx_plan_scope`(`deptId` ASC, `createdBy` ASC) USING BTREE,
  CONSTRAINT `productionplan_ibfk_1` FOREIGN KEY (`batchId`) REFERENCES `batchinfo` (`batchId`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `productionplan_ibfk_2` FOREIGN KEY (`routeId`) REFERENCES `processroute` (`routeId`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of productionplan
-- ----------------------------
INSERT INTO `productionplan` VALUES (2067894171762089985, 2067457510645342210, 2067457511756832769, 30004, 20001, '2026-06-19 16:55:57', '2026-06-20 13:55:57', 'DRAFT', 100, 1, '2026-06-19 16:56:05', NULL);

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
-- Records of qccamera
-- ----------------------------
INSERT INTO `qccamera` VALUES (2067457245565329409, 'LOCAL_CAM_001', '电脑摄像头', 'local_webcam', '127.0.0.1', '质检工作站本机', 1, '系统自动初始化的本机摄像头资源');

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
-- Records of qcitem
-- ----------------------------
INSERT INTO `qcitem` VALUES (10001, 'AI_FABRIC_DEFECT', 'AI 布面瑕疵检测', 'VISUAL', '处', 0.0000, 0.0000, 1, '浏览器 ONNX 对布面破洞、污渍、断经等缺陷进行检测；检测到任一缺陷即判为不通过');

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
  `deptId` bigint NOT NULL,
  `createdBy` bigint NOT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`inspectionId`) USING BTREE,
  INDEX `planStepId`(`planStepId` ASC) USING BTREE,
  INDEX `qcItemId`(`qcItemId` ASC) USING BTREE,
  INDEX `idx_qc_scope`(`deptId` ASC, `createdBy` ASC) USING BTREE,
  CONSTRAINT `qcrecord_ibfk_1` FOREIGN KEY (`planStepId`) REFERENCES `planstep` (`planStepId`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `qcrecord_ibfk_2` FOREIGN KEY (`qcItemId`) REFERENCES `qcitem` (`qcItemId`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qcrecord
-- ----------------------------

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
-- Records of routestep
-- ----------------------------
INSERT INTO `routestep` VALUES (21001, 20001, 30001, 1, 1);
INSERT INTO `routestep` VALUES (21002, 20001, 30002, 2, 1);
INSERT INTO `routestep` VALUES (21003, 20001, 30004, 3, 1);
INSERT INTO `routestep` VALUES (21004, 20001, 30005, 4, 1);
INSERT INTO `routestep` VALUES (21005, 20001, 30007, 5, 1);
INSERT INTO `routestep` VALUES (21006, 20001, 30008, 6, 1);
INSERT INTO `routestep` VALUES (21007, 20001, 30009, 7, 1);
INSERT INTO `routestep` VALUES (21008, 20002, 30001, 1, 1);
INSERT INTO `routestep` VALUES (21009, 20002, 30002, 2, 1);
INSERT INTO `routestep` VALUES (21010, 20002, 30003, 3, 1);
INSERT INTO `routestep` VALUES (21011, 20002, 30004, 4, 1);
INSERT INTO `routestep` VALUES (21012, 20002, 30005, 5, 1);
INSERT INTO `routestep` VALUES (21013, 20002, 30007, 6, 1);
INSERT INTO `routestep` VALUES (21014, 20002, 30008, 7, 1);
INSERT INTO `routestep` VALUES (21015, 20002, 30009, 8, 1);
INSERT INTO `routestep` VALUES (21016, 20003, 30004, 1, 1);
INSERT INTO `routestep` VALUES (21017, 20003, 30005, 2, 1);
INSERT INTO `routestep` VALUES (21018, 20003, 30007, 3, 1);
INSERT INTO `routestep` VALUES (21019, 20003, 30008, 4, 1);
INSERT INTO `routestep` VALUES (21020, 20003, 30009, 5, 1);
INSERT INTO `routestep` VALUES (21021, 20004, 30002, 1, 1);
INSERT INTO `routestep` VALUES (21022, 20004, 30004, 2, 1);
INSERT INTO `routestep` VALUES (21023, 20004, 30005, 3, 1);
INSERT INTO `routestep` VALUES (21024, 20004, 30006, 4, 1);
INSERT INTO `routestep` VALUES (21025, 20004, 30007, 5, 1);
INSERT INTO `routestep` VALUES (21026, 20004, 30008, 6, 1);
INSERT INTO `routestep` VALUES (21027, 20004, 30009, 7, 1);

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

-- ----------------------------
-- Records of stepmachinecapability
-- ----------------------------
INSERT INTO `stepmachinecapability` VALUES (50001, 30001, 40001, 150.00, 185.00, 45.00, 3500.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50002, 30002, 40002, 150.00, 185.00, 42.00, 3500.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50003, 30003, 40003, 150.00, 185.00, 38.00, 3200.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50004, 30004, 40004, 150.00, 185.00, 32.00, 3300.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50005, 30004, 40005, 150.00, 185.00, 34.00, 3200.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50006, 30005, 40006, 150.00, 185.00, 40.00, 3500.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50007, 30006, 40007, 150.00, 182.00, 28.00, 3000.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50008, 30007, 40008, 150.00, 185.00, 30.00, 3400.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50009, 30007, 40009, 150.00, 185.00, 30.00, 3400.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50010, 30008, 40010, 150.00, 185.00, 50.00, 3600.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50011, 30008, 40011, 150.00, 185.00, 55.00, 3600.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50012, 30009, 40012, 150.00, 185.00, 55.00, 3600.00, 1);

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `deptId` bigint NOT NULL,
  `parentId` bigint NOT NULL DEFAULT 0,
  `ancestors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `deptName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `orderNum` int NOT NULL DEFAULT 0,
  `leader` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `createdBy` bigint NULL DEFAULT NULL,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedBy` bigint NULL DEFAULT NULL,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`deptId`) USING BTREE,
  INDEX `idx_sys_dept_parent`(`parentId` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (100, 0, '0', '织慧通公司', 1, NULL, NULL, NULL, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45');
INSERT INTO `sys_dept` VALUES (110, 100, '0,100', '第一工厂', 1, NULL, NULL, NULL, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45');
INSERT INTO `sys_dept` VALUES (111, 110, '0,100,110', '订单管理部', 1, NULL, NULL, NULL, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45');
INSERT INTO `sys_dept` VALUES (112, 110, '0,100,110', '来料仓', 2, NULL, NULL, NULL, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45');
INSERT INTO `sys_dept` VALUES (113, 110, '0,100,110', '工艺部', 3, NULL, NULL, NULL, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45');
INSERT INTO `sys_dept` VALUES (114, 110, '0,100,110', '生产车间', 4, NULL, NULL, NULL, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45');
INSERT INTO `sys_dept` VALUES (115, 110, '0,100,110', '质量部', 5, NULL, NULL, NULL, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45');
INSERT INTO `sys_dept` VALUES (116, 110, '0,100,110', '设备管理组', 6, NULL, NULL, NULL, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45');

-- ----------------------------
-- Table structure for sys_login_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log`  (
  `infoId` bigint NOT NULL,
  `userId` bigint NULL DEFAULT NULL,
  `userName` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `sessionId` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ipaddr` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `browser` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `os` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `loginTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`infoId`) USING BTREE,
  INDEX `idx_login_log_user`(`userId` ASC) USING BTREE,
  INDEX `idx_login_log_time`(`loginTime` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_login_log
-- ----------------------------
INSERT INTO `sys_login_log` VALUES (2067965660532203521, 1, 'admin', 'fe62564b41b24fe2bfd63b62d936e660', '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '0', '登录成功', '2026-06-19 21:40:09');
INSERT INTO `sys_login_log` VALUES (2067965722377216002, 1, 'admin', 'b63b3842459c4daf9180cfb28b5dfb31', '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '0', '登录成功', '2026-06-19 21:40:24');
INSERT INTO `sys_login_log` VALUES (2067965787443453953, 1, 'admin', '256378a10e7e48dcb521d147d8dbdbcc', '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '0', '登录成功', '2026-06-19 21:40:39');
INSERT INTO `sys_login_log` VALUES (2067967029645303810, 1, 'admin', 'ce2036e65e1743ce914bb5603bd2b7c3', '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '0', '登录成功', '2026-06-19 21:45:35');
INSERT INTO `sys_login_log` VALUES (2067974110817431553, 1, 'admin', '73d408bb09ca4b88bbdbf7597790f581', '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '0', '登录成功', '2026-06-19 22:13:44');
INSERT INTO `sys_login_log` VALUES (2067977283657449473, 1, 'admin', 'f272ef2e06bb47d2ad8c040837279a7a', '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '0', '登录成功', '2026-06-19 22:26:20');
INSERT INTO `sys_login_log` VALUES (2067990554502377474, NULL, 'admin', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) HeadlessChrome/149.', NULL, '1', '验证码已过期', '2026-06-19 23:19:04');
INSERT INTO `sys_login_log` VALUES (2067991206154612738, 1, 'admin', 'f0ee762220404545bd2551299a44f14d', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) HeadlessChrome/149.', NULL, '0', '登录成功', '2026-06-19 23:21:40');
INSERT INTO `sys_login_log` VALUES (2067992816217894914, NULL, 'admin', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '1', '验证码错误', '2026-06-19 23:28:04');
INSERT INTO `sys_login_log` VALUES (2067992816473747458, NULL, 'admin', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '1', '请输入验证码', '2026-06-19 23:28:04');
INSERT INTO `sys_login_log` VALUES (2067992979334377474, NULL, 'admin', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '1', '验证码错误', '2026-06-19 23:28:42');
INSERT INTO `sys_login_log` VALUES (2067992979925774337, NULL, 'admin', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '1', '用户名或密码错误', '2026-06-19 23:28:43');
INSERT INTO `sys_login_log` VALUES (2067992980521365506, 1, 'admin', 'e0e9e3ae526648a4ab46e9563ff9d7b8', '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '0', '登录成功', '2026-06-19 23:28:43');
INSERT INTO `sys_login_log` VALUES (2067993726511886338, NULL, 'admin', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '1', '验证码错误', '2026-06-19 23:31:41');
INSERT INTO `sys_login_log` VALUES (2067993727174586369, NULL, 'admin', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '1', '用户名或密码错误', '2026-06-19 23:31:41');
INSERT INTO `sys_login_log` VALUES (2067993727896006658, 1, 'admin', 'a8cef9e70501484793f7f73546dbeaa9', '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '0', '登录成功', '2026-06-19 23:31:41');
INSERT INTO `sys_login_log` VALUES (2067994277714735106, NULL, 'admin', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) HeadlessChrome/149.', NULL, '1', '用户名或密码错误', '2026-06-19 23:33:52');
INSERT INTO `sys_login_log` VALUES (2067994390650564610, 1, 'admin', '64795b07caf84253b18b2ed5188df3f9', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) HeadlessChrome/149.', NULL, '0', '登录成功', '2026-06-19 23:34:19');
INSERT INTO `sys_login_log` VALUES (2067997164670976002, 1, 'admin', '2cb1865f81f844ca8d705e4baf0f0fe1', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Sa', NULL, '0', '登录成功', '2026-06-19 23:45:20');
INSERT INTO `sys_login_log` VALUES (2067999246413103105, 2067999177769123842, '17870120718', 'ef226c48343346329c7a09ca81265d0b', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Sa', NULL, '0', '登录成功', '2026-06-19 23:53:37');
INSERT INTO `sys_login_log` VALUES (2067999400671215617, 1, 'admin', '664a04eb7ac4446bbcb6247f40633f7a', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Sa', NULL, '0', '登录成功', '2026-06-19 23:54:13');
INSERT INTO `sys_login_log` VALUES (2067999536046571522, 1, 'admin', '9103207a08d845dd9fbb1261679b3173', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Sa', NULL, '0', '登录成功', '2026-06-19 23:54:46');
INSERT INTO `sys_login_log` VALUES (2068219245547401218, 2067999177769123842, '17870120718', 'f35ebe636a12435286ed1d66edcbdd39', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Sa', NULL, '0', '登录成功', '2026-06-20 14:27:48');
INSERT INTO `sys_login_log` VALUES (2068219480633946113, 1, 'admin', 'f57279a2b6f540bfa504f91229d7c8f0', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Sa', NULL, '0', '登录成功', '2026-06-20 14:28:45');
INSERT INTO `sys_login_log` VALUES (2068223120513826817, NULL, 'admin', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '1', '用户名或密码错误', '2026-06-20 14:43:12');
INSERT INTO `sys_login_log` VALUES (2068223173768904706, NULL, 'admin', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '1', '用户名或密码错误', '2026-06-20 14:43:25');
INSERT INTO `sys_login_log` VALUES (2068223225077825537, NULL, 'admin', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', NULL, '1', '用户名或密码错误', '2026-06-20 14:43:37');
INSERT INTO `sys_login_log` VALUES (2068225529369722881, 2067999177769123842, '17870120718', 'db124fb897584a0e833f38f7249c52c9', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Sa', NULL, '0', '登录成功', '2026-06-20 14:52:47');
INSERT INTO `sys_login_log` VALUES (2068233146775281666, 1, 'admin', '99595ee4d1304cc7be779b6f93f52217', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Sa', NULL, '0', '登录成功', '2026-06-20 15:23:03');
INSERT INTO `sys_login_log` VALUES (2068233619599171585, 1, 'admin', '08d065076e464e4bbaf921e2f4aaa47d', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Sa', NULL, '0', '登录成功', '2026-06-20 15:24:56');
INSERT INTO `sys_login_log` VALUES (2068242785386553345, 1, 'admin', 'b7a5752fbffe4a768c23a687a0d75fca', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Sa', NULL, '0', '登录成功', '2026-06-20 16:01:21');
INSERT INTO `sys_login_log` VALUES (2068296827462672386, 1, 'admin', '871987598e40426ba3a9248c1c1ce0b2', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Sa', NULL, '0', '登录成功', '2026-06-20 19:36:05');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `menuId` bigint NOT NULL,
  `menuKey` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `menuName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `parentId` bigint NOT NULL DEFAULT 0,
  `orderNum` int NOT NULL DEFAULT 0,
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `menuType` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `perms` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  `createdBy` bigint NULL DEFAULT NULL,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedBy` bigint NULL DEFAULT NULL,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`menuId`) USING BTREE,
  UNIQUE INDEX `uk_sys_menu_key`(`menuKey` ASC) USING BTREE,
  INDEX `idx_sys_menu_parent`(`parentId` ASC) USING BTREE,
  INDEX `idx_sys_menu_perms`(`perms` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1000, 'dashboard', '看板', 0, 1, '/dashboard', NULL, 'C', '0', '0', 'dashboard:view', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (2000, 'order', '订单管理', 0, 2, NULL, NULL, 'M', '0', '0', NULL, NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (2001, 'order-list', '订单列表', 2000, 1, '/order/list', NULL, 'C', '0', '0', 'order:order:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (2011, 'order-add', '订单新增', 2001, 1, NULL, NULL, 'F', '0', '0', 'order:order:add', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (2012, 'order-edit', '订单修改', 2001, 2, NULL, NULL, 'F', '0', '0', 'order:order:edit', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (2013, 'order-remove', '订单删除', 2001, 3, NULL, NULL, 'F', '0', '0', 'order:order:remove', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (3000, 'gray', '来料资源', 0, 3, NULL, NULL, 'M', '0', '0', NULL, NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (3001, 'gray-inbound', '来料资源池', 3000, 1, '/gray/inbound', NULL, 'C', '0', '0', 'batch:resource:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (3011, 'batch-add', '来料新增', 3001, 1, NULL, NULL, 'F', '0', '0', 'batch:resource:add', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (3012, 'batch-edit', '来料修改', 3001, 2, NULL, NULL, 'F', '0', '0', 'batch:resource:edit', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (3013, 'batch-remove', '来料删除', 3001, 3, NULL, NULL, 'F', '0', '0', 'batch:resource:remove', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (4000, 'process-center', '工艺中心', 0, 4, NULL, NULL, 'M', '0', '0', NULL, NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (4001, 'process-center-process', '工艺路线', 4000, 1, '/process-center/process', NULL, 'C', '0', '0', 'process:route:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (4002, 'master-machine', '设备管理', 4000, 2, '/master/machine', NULL, 'C', '0', '0', 'process:machine:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (4011, 'process-manage', '工艺维护', 4001, 1, NULL, NULL, 'F', '0', '0', 'process:route:edit', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (4012, 'machine-manage', '设备维护', 4002, 1, NULL, NULL, 'F', '0', '0', 'process:machine:edit', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (5000, 'schedule', '排产管理', 0, 5, NULL, NULL, 'M', '0', '0', NULL, NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (5001, 'schedule-order-pool', '订单排产池', 5000, 1, '/schedule/order-pool', NULL, 'C', '0', '0', 'plan:production:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (5002, 'schedule-pool', '批次执行池', 5000, 2, '/schedule/pool', NULL, 'C', '0', '0', 'plan:production:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (5003, 'schedule-main', '生产计划', 5000, 3, '/schedule/main', NULL, 'C', '0', '0', 'plan:production:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (5004, 'schedule-board', '调度甘特图', 5000, 4, '/schedule/board', NULL, 'C', '0', '0', 'plan:production:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (5005, 'schedule-ai-advisor', 'AI智能排产', 5000, 5, '/schedule/ai-advisor', NULL, 'C', '0', '0', 'plan:production:list', NULL, 0, NULL, '2026-06-20 16:00:43', NULL, '2026-06-20 16:00:43', NULL);
INSERT INTO `sys_menu` VALUES (5011, 'plan-create', '创建计划', 5003, 1, NULL, NULL, 'F', '0', '0', 'plan:production:create', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (5012, 'plan-edit', '修改计划', 5003, 2, NULL, NULL, 'F', '0', '0', 'plan:production:edit', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (5013, 'plan-reschedule', '计划重排', 5003, 3, NULL, NULL, 'F', '0', '0', 'plan:production:reschedule', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (6000, 'quality', '质量管理', 0, 6, NULL, NULL, 'M', '0', '0', NULL, NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (6001, 'quality-realtime', '实时质检', 6000, 1, '/quality/realtime', NULL, 'C', '0', '0', 'quality:realtime:view', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (6002, 'quality-ncr', '异常闭环', 6000, 2, '/quality/ncr', NULL, 'C', '0', '0', 'exception:record:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (6003, 'quality-rework', '返工处理', 6000, 3, '/quality/rework', NULL, 'C', '0', '0', 'exception:record:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (6004, 'quality-ai-analysis', 'AI质检分析', 6000, 4, '/quality/ai-analysis', NULL, 'C', '0', '0', 'quality:realtime:view', NULL, 0, NULL, '2026-06-20 16:00:43', NULL, '2026-06-20 16:00:43', NULL);
INSERT INTO `sys_menu` VALUES (6011, 'quality-detect', '执行质检', 6001, 1, NULL, NULL, 'F', '0', '0', 'quality:realtime:detect', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (6012, 'quality-review', '质检复核', 6001, 2, NULL, NULL, 'F', '0', '0', 'quality:record:review', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (6013, 'exception-handle', '异常处理', 6002, 1, NULL, NULL, 'F', '0', '0', 'exception:record:handle', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (6014, 'exception-rework', '返工登记', 6003, 1, NULL, NULL, 'F', '0', '0', 'exception:record:rework', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (7000, 'system', '系统管理', 0, 7, NULL, NULL, 'M', '0', '0', NULL, NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (7001, 'system-user', '用户管理', 7000, 1, '/system/user', NULL, 'C', '0', '0', 'system:user:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (7002, 'system-role', '角色管理', 7000, 2, '/system/role', NULL, 'C', '0', '0', 'system:role:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (7003, 'system-menu', '菜单管理', 7000, 3, '/system/menu', NULL, 'C', '0', '0', 'system:menu:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (7004, 'system-dept', '部门管理', 7000, 4, '/system/dept', NULL, 'C', '0', '0', 'system:dept:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (7005, 'system-post', '岗位管理', 7000, 5, '/system/post', NULL, 'C', '0', '0', 'system:post:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (7006, 'system-online', '在线用户', 7000, 6, '/system/online', NULL, 'C', '0', '0', 'system:online:list', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (7011, 'system-user-manage', '用户维护', 7001, 1, NULL, NULL, 'F', '0', '0', 'system:user:manage', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (7012, 'system-role-manage', '角色维护', 7002, 1, NULL, NULL, 'F', '0', '0', 'system:role:manage', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (7013, 'system-menu-manage', '菜单维护', 7003, 1, NULL, NULL, 'F', '0', '0', 'system:menu:manage', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (7014, 'system-dept-manage', '部门维护', 7004, 1, NULL, NULL, 'F', '0', '0', 'system:dept:manage', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (7015, 'system-post-manage', '岗位维护', 7005, 1, NULL, NULL, 'F', '0', '0', 'system:post:manage', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_menu` VALUES (7016, 'system-online-force', '强制下线', 7006, 1, NULL, NULL, 'F', '0', '0', 'system:online:forceLogout', NULL, 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
  `postId` bigint NOT NULL,
  `postCode` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `postName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `postSort` int NOT NULL DEFAULT 0,
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `createdBy` bigint NULL DEFAULT NULL,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedBy` bigint NULL DEFAULT NULL,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`postId`) USING BTREE,
  UNIQUE INDEX `uk_sys_post_code`(`postCode` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_post
-- ----------------------------
INSERT INTO `sys_post` VALUES (1, 'SYSTEM_ADMIN', '系统管理员', 1, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_post` VALUES (2, 'ORDER_CLERK', '订单专员', 2, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_post` VALUES (3, 'WAREHOUSE_MANAGER', '仓库管理员', 3, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_post` VALUES (4, 'PROCESS_ENGINEER', '工艺工程师', 4, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_post` VALUES (5, 'SCHEDULER', '排产员', 5, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_post` VALUES (6, 'QC_INSPECTOR', '质检员', 6, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_post` VALUES (7, 'QC_MANAGER', '质量主管', 7, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_post` VALUES (8, 'EQUIPMENT_MANAGER', '设备管理员', 8, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);
INSERT INTO `sys_post` VALUES (9, 'VIEWER', '只读人员', 9, '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', NULL);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `roleId` bigint NOT NULL,
  `roleName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `roleKey` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `roleSort` int NOT NULL DEFAULT 0,
  `dataScope` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DEPT',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `createdBy` bigint NULL DEFAULT NULL,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedBy` bigint NULL DEFAULT NULL,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`roleId`) USING BTREE,
  UNIQUE INDEX `uk_sys_role_key`(`roleKey` ASC) USING BTREE,
  UNIQUE INDEX `uk_sys_role_name`(`roleName` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'admin', 1, 'ALL', '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', '系统内置，不可删除或停用');
INSERT INTO `sys_role` VALUES (2, '订单管理员', 'order_manager', 2, 'DEPT', '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', '订单与批次分配');
INSERT INTO `sys_role` VALUES (3, '来料管理员', 'material_manager', 3, 'DEPT', '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', '来料与资源池');
INSERT INTO `sys_role` VALUES (4, '工艺工程师', 'process_engineer', 4, 'DEPT', '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', '工艺与设备能力');
INSERT INTO `sys_role` VALUES (5, '排产员', 'scheduler', 5, 'DEPT_AND_CHILD', '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', '排产与计划');
INSERT INTO `sys_role` VALUES (6, '质检员', 'qc_inspector', 6, 'SELF', '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', '质检执行');
INSERT INTO `sys_role` VALUES (7, '质量主管', 'qc_manager', 7, 'DEPT_AND_CHILD', '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', '质量复核与异常');
INSERT INTO `sys_role` VALUES (8, '只读用户', 'viewer', 8, 'DEPT', '0', 0, NULL, '2026-06-19 21:24:45', NULL, '2026-06-19 21:24:45', '只读访问');

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`  (
  `roleId` bigint NOT NULL,
  `deptId` bigint NOT NULL,
  PRIMARY KEY (`roleId`, `deptId`) USING BTREE,
  INDEX `fk_role_dept_dept`(`deptId` ASC) USING BTREE,
  CONSTRAINT `fk_role_dept_dept` FOREIGN KEY (`deptId`) REFERENCES `sys_dept` (`deptId`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_role_dept_role` FOREIGN KEY (`roleId`) REFERENCES `sys_role` (`roleId`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------
INSERT INTO `sys_role_dept` VALUES (8, 110);
INSERT INTO `sys_role_dept` VALUES (2, 111);
INSERT INTO `sys_role_dept` VALUES (3, 112);
INSERT INTO `sys_role_dept` VALUES (4, 113);
INSERT INTO `sys_role_dept` VALUES (5, 114);
INSERT INTO `sys_role_dept` VALUES (6, 115);
INSERT INTO `sys_role_dept` VALUES (7, 115);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `roleId` bigint NOT NULL,
  `menuId` bigint NOT NULL,
  PRIMARY KEY (`roleId`, `menuId`) USING BTREE,
  INDEX `fk_role_menu_menu`(`menuId` ASC) USING BTREE,
  CONSTRAINT `fk_role_menu_menu` FOREIGN KEY (`menuId`) REFERENCES `sys_menu` (`menuId`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_role_menu_role` FOREIGN KEY (`roleId`) REFERENCES `sys_role` (`roleId`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (1, 1000);
INSERT INTO `sys_role_menu` VALUES (2, 1000);
INSERT INTO `sys_role_menu` VALUES (3, 1000);
INSERT INTO `sys_role_menu` VALUES (4, 1000);
INSERT INTO `sys_role_menu` VALUES (5, 1000);
INSERT INTO `sys_role_menu` VALUES (6, 1000);
INSERT INTO `sys_role_menu` VALUES (7, 1000);
INSERT INTO `sys_role_menu` VALUES (8, 1000);
INSERT INTO `sys_role_menu` VALUES (1, 2000);
INSERT INTO `sys_role_menu` VALUES (2, 2000);
INSERT INTO `sys_role_menu` VALUES (5, 2000);
INSERT INTO `sys_role_menu` VALUES (8, 2000);
INSERT INTO `sys_role_menu` VALUES (1, 2001);
INSERT INTO `sys_role_menu` VALUES (2, 2001);
INSERT INTO `sys_role_menu` VALUES (5, 2001);
INSERT INTO `sys_role_menu` VALUES (8, 2001);
INSERT INTO `sys_role_menu` VALUES (1, 2011);
INSERT INTO `sys_role_menu` VALUES (2, 2011);
INSERT INTO `sys_role_menu` VALUES (1, 2012);
INSERT INTO `sys_role_menu` VALUES (2, 2012);
INSERT INTO `sys_role_menu` VALUES (1, 2013);
INSERT INTO `sys_role_menu` VALUES (2, 2013);
INSERT INTO `sys_role_menu` VALUES (1, 3000);
INSERT INTO `sys_role_menu` VALUES (2, 3000);
INSERT INTO `sys_role_menu` VALUES (3, 3000);
INSERT INTO `sys_role_menu` VALUES (5, 3000);
INSERT INTO `sys_role_menu` VALUES (8, 3000);
INSERT INTO `sys_role_menu` VALUES (1, 3001);
INSERT INTO `sys_role_menu` VALUES (2, 3001);
INSERT INTO `sys_role_menu` VALUES (3, 3001);
INSERT INTO `sys_role_menu` VALUES (5, 3001);
INSERT INTO `sys_role_menu` VALUES (8, 3001);
INSERT INTO `sys_role_menu` VALUES (1, 3011);
INSERT INTO `sys_role_menu` VALUES (3, 3011);
INSERT INTO `sys_role_menu` VALUES (1, 3012);
INSERT INTO `sys_role_menu` VALUES (3, 3012);
INSERT INTO `sys_role_menu` VALUES (1, 3013);
INSERT INTO `sys_role_menu` VALUES (3, 3013);
INSERT INTO `sys_role_menu` VALUES (1, 4000);
INSERT INTO `sys_role_menu` VALUES (4, 4000);
INSERT INTO `sys_role_menu` VALUES (8, 4000);
INSERT INTO `sys_role_menu` VALUES (1, 4001);
INSERT INTO `sys_role_menu` VALUES (4, 4001);
INSERT INTO `sys_role_menu` VALUES (8, 4001);
INSERT INTO `sys_role_menu` VALUES (1, 4002);
INSERT INTO `sys_role_menu` VALUES (4, 4002);
INSERT INTO `sys_role_menu` VALUES (8, 4002);
INSERT INTO `sys_role_menu` VALUES (1, 4011);
INSERT INTO `sys_role_menu` VALUES (4, 4011);
INSERT INTO `sys_role_menu` VALUES (1, 4012);
INSERT INTO `sys_role_menu` VALUES (4, 4012);
INSERT INTO `sys_role_menu` VALUES (1, 5000);
INSERT INTO `sys_role_menu` VALUES (5, 5000);
INSERT INTO `sys_role_menu` VALUES (8, 5000);
INSERT INTO `sys_role_menu` VALUES (1, 5001);
INSERT INTO `sys_role_menu` VALUES (5, 5001);
INSERT INTO `sys_role_menu` VALUES (8, 5001);
INSERT INTO `sys_role_menu` VALUES (1, 5002);
INSERT INTO `sys_role_menu` VALUES (5, 5002);
INSERT INTO `sys_role_menu` VALUES (8, 5002);
INSERT INTO `sys_role_menu` VALUES (1, 5003);
INSERT INTO `sys_role_menu` VALUES (5, 5003);
INSERT INTO `sys_role_menu` VALUES (8, 5003);
INSERT INTO `sys_role_menu` VALUES (1, 5004);
INSERT INTO `sys_role_menu` VALUES (5, 5004);
INSERT INTO `sys_role_menu` VALUES (8, 5004);
INSERT INTO `sys_role_menu` VALUES (1, 5005);
INSERT INTO `sys_role_menu` VALUES (5, 5005);
INSERT INTO `sys_role_menu` VALUES (8, 5005);
INSERT INTO `sys_role_menu` VALUES (1, 5011);
INSERT INTO `sys_role_menu` VALUES (5, 5011);
INSERT INTO `sys_role_menu` VALUES (1, 5012);
INSERT INTO `sys_role_menu` VALUES (5, 5012);
INSERT INTO `sys_role_menu` VALUES (1, 5013);
INSERT INTO `sys_role_menu` VALUES (5, 5013);
INSERT INTO `sys_role_menu` VALUES (1, 6000);
INSERT INTO `sys_role_menu` VALUES (6, 6000);
INSERT INTO `sys_role_menu` VALUES (7, 6000);
INSERT INTO `sys_role_menu` VALUES (8, 6000);
INSERT INTO `sys_role_menu` VALUES (1, 6001);
INSERT INTO `sys_role_menu` VALUES (6, 6001);
INSERT INTO `sys_role_menu` VALUES (7, 6001);
INSERT INTO `sys_role_menu` VALUES (8, 6001);
INSERT INTO `sys_role_menu` VALUES (1, 6002);
INSERT INTO `sys_role_menu` VALUES (6, 6002);
INSERT INTO `sys_role_menu` VALUES (7, 6002);
INSERT INTO `sys_role_menu` VALUES (8, 6002);
INSERT INTO `sys_role_menu` VALUES (1, 6003);
INSERT INTO `sys_role_menu` VALUES (6, 6003);
INSERT INTO `sys_role_menu` VALUES (7, 6003);
INSERT INTO `sys_role_menu` VALUES (8, 6003);
INSERT INTO `sys_role_menu` VALUES (1, 6004);
INSERT INTO `sys_role_menu` VALUES (6, 6004);
INSERT INTO `sys_role_menu` VALUES (7, 6004);
INSERT INTO `sys_role_menu` VALUES (8, 6004);
INSERT INTO `sys_role_menu` VALUES (1, 6011);
INSERT INTO `sys_role_menu` VALUES (6, 6011);
INSERT INTO `sys_role_menu` VALUES (1, 6012);
INSERT INTO `sys_role_menu` VALUES (7, 6012);
INSERT INTO `sys_role_menu` VALUES (1, 6013);
INSERT INTO `sys_role_menu` VALUES (7, 6013);
INSERT INTO `sys_role_menu` VALUES (1, 6014);
INSERT INTO `sys_role_menu` VALUES (7, 6014);
INSERT INTO `sys_role_menu` VALUES (1, 7000);
INSERT INTO `sys_role_menu` VALUES (1, 7001);
INSERT INTO `sys_role_menu` VALUES (1, 7002);
INSERT INTO `sys_role_menu` VALUES (1, 7003);
INSERT INTO `sys_role_menu` VALUES (1, 7004);
INSERT INTO `sys_role_menu` VALUES (1, 7005);
INSERT INTO `sys_role_menu` VALUES (1, 7006);
INSERT INTO `sys_role_menu` VALUES (1, 7011);
INSERT INTO `sys_role_menu` VALUES (1, 7012);
INSERT INTO `sys_role_menu` VALUES (1, 7013);
INSERT INTO `sys_role_menu` VALUES (1, 7014);
INSERT INTO `sys_role_menu` VALUES (1, 7015);
INSERT INTO `sys_role_menu` VALUES (1, 7016);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `userId` bigint NOT NULL,
  `deptId` bigint NOT NULL,
  `postId` bigint NOT NULL,
  `roleId` bigint NOT NULL,
  `userName` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nickName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `phonenumber` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `loginIp` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `loginDate` datetime NULL DEFAULT NULL,
  `pwdUpdateTime` datetime NULL DEFAULT NULL,
  `createdBy` bigint NULL DEFAULT NULL,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedBy` bigint NULL DEFAULT NULL,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`userId`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_name`(`userName` ASC) USING BTREE,
  UNIQUE INDEX `uk_sys_user_phone`(`phonenumber` ASC) USING BTREE,
  UNIQUE INDEX `uk_sys_user_email`(`email` ASC) USING BTREE,
  INDEX `idx_sys_user_dept`(`deptId` ASC) USING BTREE,
  INDEX `idx_sys_user_role`(`roleId` ASC) USING BTREE,
  INDEX `fk_sys_user_post`(`postId` ASC) USING BTREE,
  CONSTRAINT `fk_sys_user_dept` FOREIGN KEY (`deptId`) REFERENCES `sys_dept` (`deptId`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_sys_user_post` FOREIGN KEY (`postId`) REFERENCES `sys_post` (`postId`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_sys_user_role` FOREIGN KEY (`roleId`) REFERENCES `sys_role` (`roleId`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 100, 1, 1, 'admin', '超级管理员', 'admin@zhihuitong.local', NULL, NULL, '$2a$10$.FHZCqNd7uPHJrQ37stJ6.80g4yzR0BLdSVg1XUDGQGll1/zOxhHO', '0', 0, '127.0.0.1', '2026-06-20 19:36:05', '2026-06-19 23:54:32', NULL, '2026-06-19 21:24:45', 1, '2026-06-20 19:36:05', '初始密码 Admin@123，首次部署后必须修改');
INSERT INTO `sys_user` VALUES (2067999177769123842, 115, 7, 6, '17870120718', '麋鹿超人', '3210783832@qq.com', '17870120718', NULL, '$2a$10$TnK0W.twnqHufNSrsBUyGOI.20KL43Ct3wLBBzQqJwrQ47usDTqPS', '0', 0, '127.0.0.1', '2026-06-20 14:52:47', '2026-06-19 23:53:20', 1, '2026-06-19 23:53:20', NULL, '2026-06-20 14:52:46', '');

SET FOREIGN_KEY_CHECKS = 1;
