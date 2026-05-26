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

 Date: 26/05/2026 15:10:41
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
-- Records of batchinfo
-- ----------------------------
INSERT INTO `batchinfo` VALUES (10001, 'GB-20260518-001', '江苏恒裕纺织有限公司', '2026-05-18 08:15:00', 2480.50, 168.00, '棉 95%, 氨纶 5%', '32S精梳汗布胚布，出口休闲系列，先排常规单染');
INSERT INTO `batchinfo` VALUES (10002, 'GB-20260518-002', '绍兴远达针织有限公司', '2026-05-18 10:40:00', 3120.00, 180.00, '涤纶 88%, 氨纶 12%', '秋冬双面布，客户要求手感柔软、色牢度 4 级以上');
INSERT INTO `batchinfo` VALUES (10003, 'GB-20260519-001', '常熟新裕织造有限公司', '2026-05-19 09:20:00', 2865.00, 175.00, '棉 65%, 涤纶 35%', '工装面料胚布，后续需做功能整理');
INSERT INTO `batchinfo` VALUES (10004, 'GB-20260520-001', '福建联盛纺织科技有限公司', '2026-05-20 14:05:00', 2650.80, 168.00, '棉 100%', '婴童系列浅色单，客户要求白度稳定');
INSERT INTO `batchinfo` VALUES (10005, 'GB-20260521-001', '张家港华彩纺织有限公司', '2026-05-21 11:30:00', 1988.60, 160.00, '莫代尔 60%, 棉 40%', '待业务确认手感目标后再排产');
INSERT INTO `batchinfo` VALUES (10006, 'GB-20260521-002', '绍兴柯桥瑞丰面料有限公司', '2026-05-21 16:10:00', 3380.20, 182.00, '锦纶 78%, 氨纶 22%', '运动弹力面料，需确认定型门幅余量');

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
-- Records of exceptionrecord
-- ----------------------------
INSERT INTO `exceptionrecord` VALUES (63001, 61009, 'PROCESS', 'LOW', '夜班染色阶段蒸汽压力短时波动，缸温出现 2℃ 偏差', '机修已复位调压阀，班组每 30 分钟复核一次温压曲线', '2026-05-22 23:05:00', 'PROCESSING');
INSERT INTO `exceptionrecord` VALUES (2058399713991524353, 61021, 'QUALITY', 'MEDIUM', 'Auto-created from QC failure record 2058399713693728770', '', '2026-05-24 12:08:30', 'CLOSED');
INSERT INTO `exceptionrecord` VALUES (2058421270872760322, 61001, 'QUALITY', 'MEDIUM', 'Auto-created from QC failure record 2058421270809845761', '', '2026-05-24 13:34:09', 'CLOSED');
INSERT INTO `exceptionrecord` VALUES (2058785122365923329, 61001, 'QUALITY', 'MEDIUM', 'Auto-created from QC failure record 2058785122298814465', '', '2026-05-25 13:39:58', 'OPEN');

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
INSERT INTO `inspectiondata` VALUES (2058194958493089794, 2058194958425980930, NULL, 'image', 'photo/upload/20260523/source_20260523223451296_87109bd9.jpg', 'source_20260523223451296_87109bd9.jpg', '2026-05-23 22:34:52', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=offline; variant=source');
INSERT INTO `inspectiondata` VALUES (2058194958556004353, 2058194958425980930, NULL, 'image-result', 'photo/results/20260523/result_20260523223451983_05da19e5.jpg', 'result_20260523223451983_05da19e5.jpg', '2026-05-23 22:34:52', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=offline; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058197101073559554, 2058197100960313345, NULL, 'image', 'photo/upload/20260523/source_20260523224322082_527d22bb.jpg', 'source_20260523224322082_527d22bb.jpg', '2026-05-23 22:43:23', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=offline; variant=source');
INSERT INTO `inspectiondata` VALUES (2058197101073559555, 2058197100960313345, NULL, 'image-result', 'photo/results/20260523/result_20260523224322801_7cc2cd89.jpg', 'result_20260523224322801_7cc2cd89.jpg', '2026-05-23 22:43:23', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=offline; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058399714054438913, 2058399713693728770, NULL, 'image', 'photo/upload/20260524/source_20260524120828759_b781c682.jpg', 'source_20260524120828759_b781c682.jpg', '2026-05-24 12:08:30', 'judge=FAIL, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=offline; variant=source');
INSERT INTO `inspectiondata` VALUES (2058399714054438914, 2058399713693728770, NULL, 'image-result', 'photo/results/20260524/result_20260524120829381_cb2159c7.jpg', 'result_20260524120829381_cb2159c7.jpg', '2026-05-24 12:08:30', 'judge=FAIL, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=offline; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058416184532598785, 2058416184465489921, NULL, 'image', 'photo/upload/20260524/source_20260524131355750_2e932c7c.jpg', 'source_20260524131355750_2e932c7c.jpg', '2026-05-24 13:13:57', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=offline; variant=source');
INSERT INTO `inspectiondata` VALUES (2058416184595513345, 2058416184465489921, NULL, 'image-result', 'photo/results/20260524/result_20260524131356396_09c765ac.jpg', 'result_20260524131356396_09c765ac.jpg', '2026-05-24 13:13:57', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=offline; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058416186554253314, 2058416186487144449, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524131356717_db211145.jpg', 'source_20260524131356717_db211145.jpg', '2026-05-24 12:00:00', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058416186617167873, 2058416186487144449, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524131356904_2c10a46e.jpg', 'result_20260524131356904_2c10a46e.jpg', '2026-05-24 12:00:00', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058420976969490433, 2058420976902381569, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524133258550_95b8c06a.jpg', 'source_20260524133258550_95b8c06a.jpg', '2026-05-24 13:32:58', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058420976969490434, 2058420976902381569, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524133259088_43a0cc09.jpg', 'result_20260524133259088_43a0cc09.jpg', '2026-05-24 13:32:58', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058421270872760323, 2058421270809845761, NULL, 'image', 'photo/upload/20260524/source_20260524133408934_78b3623d.jpg', 'source_20260524133408934_78b3623d.jpg', '2026-05-24 13:34:09', 'judge=FAIL, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=offline; variant=source');
INSERT INTO `inspectiondata` VALUES (2058421270935674881, 2058421270809845761, NULL, 'image-result', 'photo/results/20260524/result_20260524133409099_003ec0a6.jpg', 'result_20260524133409099_003ec0a6.jpg', '2026-05-24 13:34:09', 'judge=FAIL, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=offline; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058421867118239745, 2058421867051130882, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524133631281_6eddfd90.jpg', 'source_20260524133631281_6eddfd90.jpg', '2026-05-24 13:36:30', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058421867181154306, 2058421867051130882, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524133631379_3845fcfe.jpg', 'result_20260524133631379_3845fcfe.jpg', '2026-05-24 13:36:30', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426357015842817, 2058426356919373825, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135421253_7dc9cec3.jpg', 'source_20260524135421253_7dc9cec3.jpg', '2026-05-24 13:54:21', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426357015842818, 2058426356919373825, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135421779_2490ccb6.jpg', 'result_20260524135421779_2490ccb6.jpg', '2026-05-24 13:54:21', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426362699124737, 2058426362632015873, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135423090_e5d305cc.jpg', 'source_20260524135423090_e5d305cc.jpg', '2026-05-24 13:54:23', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426362766233602, 2058426362632015873, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135423204_6e0c9af0.jpg', 'result_20260524135423204_6e0c9af0.jpg', '2026-05-24 13:54:23', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426368369823745, 2058426368306909186, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135424439_d1daa690.jpg', 'source_20260524135424439_d1daa690.jpg', '2026-05-24 13:54:24', 'judge=RECHECK, defectType=other_defect, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426368436932610, 2058426368306909186, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135424556_6ea9f14f.jpg', 'result_20260524135424556_6ea9f14f.jpg', '2026-05-24 13:54:24', 'judge=RECHECK, defectType=other_defect, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426374132797441, 2058426374065688578, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135425828_efeafb3f.jpg', 'source_20260524135425828_efeafb3f.jpg', '2026-05-24 13:54:25', 'judge=FAIL, defectType=star_jump, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426374195712001, 2058426374065688578, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135425930_e85305a7.jpg', 'result_20260524135425930_e85305a7.jpg', '2026-05-24 13:54:25', 'judge=FAIL, defectType=star_jump, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426379853828098, 2058426379786719233, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135427192_c8787fc2.jpg', 'source_20260524135427192_c8787fc2.jpg', '2026-05-24 13:54:27', 'judge=FAIL, defectType=stain, boxCount=3', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426379920936961, 2058426379786719233, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135427299_966e5b10.jpg', 'result_20260524135427299_966e5b10.jpg', '2026-05-24 13:54:27', 'judge=FAIL, defectType=stain, boxCount=3', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426385528721409, 2058426385461612546, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135428542_82eaf5e9.jpg', 'source_20260524135428542_82eaf5e9.jpg', '2026-05-24 13:54:28', 'judge=RECHECK, defectType=knot, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426385528721410, 2058426385461612546, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135428645_92dd3920.jpg', 'result_20260524135428645_92dd3920.jpg', '2026-05-24 13:54:28', 'judge=RECHECK, defectType=knot, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426391224586241, 2058426391157477377, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135429886_c15f7697.jpg', 'source_20260524135429886_c15f7697.jpg', '2026-05-24 13:54:29', 'judge=FAIL, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426391224586242, 2058426391157477377, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135429999_351ee5d8.jpg', 'result_20260524135429999_351ee5d8.jpg', '2026-05-24 13:54:29', 'judge=FAIL, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426396903673857, 2058426396836564993, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135431259_4212d06b.jpg', 'source_20260524135431259_4212d06b.jpg', '2026-05-24 13:54:31', 'judge=FAIL, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426396970782721, 2058426396836564993, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135431360_e7b8f0fc.jpg', 'result_20260524135431360_e7b8f0fc.jpg', '2026-05-24 13:54:31', 'judge=FAIL, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426402461126658, 2058426402394017794, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135432589_3775cef5.jpg', 'source_20260524135432589_3775cef5.jpg', '2026-05-24 13:54:32', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426402528235521, 2058426402394017794, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135432692_36735526.jpg', 'result_20260524135432692_36735526.jpg', '2026-05-24 13:54:32', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426408018579457, 2058426407947276290, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135433915_2c3af542.jpg', 'source_20260524135433915_2c3af542.jpg', '2026-05-24 13:54:33', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426408018579458, 2058426407947276290, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135434018_518a85ac.jpg', 'result_20260524135434018_518a85ac.jpg', '2026-05-24 13:54:33', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426413517312002, 2058426413450203137, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135435224_04a01fb9.jpg', 'source_20260524135435224_04a01fb9.jpg', '2026-05-24 13:54:35', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426413517312003, 2058426413450203137, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135435323_babcb519.jpg', 'result_20260524135435323_babcb519.jpg', '2026-05-24 13:54:35', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426418969907202, 2058426418906992641, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135436522_53b16a76.jpg', 'source_20260524135436522_53b16a76.jpg', '2026-05-24 13:54:36', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426418969907203, 2058426418906992641, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135436622_dd38964b.jpg', 'result_20260524135436622_dd38964b.jpg', '2026-05-24 13:54:36', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426424435085315, 2058426424435085314, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135437829_03a375a4.jpg', 'source_20260524135437829_03a375a4.jpg', '2026-05-24 13:54:37', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426424502194178, 2058426424435085314, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135437935_f1f1f4da.jpg', 'result_20260524135437935_f1f1f4da.jpg', '2026-05-24 13:54:37', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426430047064065, 2058426429975760897, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135439147_5ca00da1.jpg', 'source_20260524135439147_5ca00da1.jpg', '2026-05-24 13:54:39', 'judge=RECHECK, defectType=thick_weft, boxCount=3', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426430047064066, 2058426429975760897, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135439261_9f3c8e19.jpg', 'result_20260524135439261_9f3c8e19.jpg', '2026-05-24 13:54:39', 'judge=RECHECK, defectType=thick_weft, boxCount=3', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426435533213698, 2058426435533213697, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135440481_e9b50fe7.jpg', 'source_20260524135440481_e9b50fe7.jpg', '2026-05-24 13:54:40', 'judge=FAIL, defectType=star_jump, boxCount=3', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426435596128257, 2058426435533213697, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135440581_4573affb.jpg', 'result_20260524135440581_4573affb.jpg', '2026-05-24 13:54:40', 'judge=FAIL, defectType=star_jump, boxCount=3', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426441052917761, 2058426440990003202, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135441789_7426b9da.jpg', 'source_20260524135441789_7426b9da.jpg', '2026-05-24 13:54:41', 'judge=RECHECK, defectType=star_jump, boxCount=3', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426441052917762, 2058426440990003202, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135441892_d024531d.jpg', 'result_20260524135441892_d024531d.jpg', '2026-05-24 13:54:41', 'judge=RECHECK, defectType=star_jump, boxCount=3', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426446610370563, 2058426446610370562, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135443116_ef7176ee.jpg', 'source_20260524135443116_ef7176ee.jpg', '2026-05-24 13:54:43', 'judge=RECHECK, defectType=star_jump, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426446677479425, 2058426446610370562, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135443218_f4f52285.jpg', 'result_20260524135443218_f4f52285.jpg', '2026-05-24 13:54:43', 'judge=RECHECK, defectType=star_jump, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426452092325890, 2058426452092325889, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135444430_56dabd23.jpg', 'source_20260524135444430_56dabd23.jpg', '2026-05-24 13:54:44', 'judge=RECHECK, defectType=other_defect, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426452155240449, 2058426452092325889, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135444525_0b41d615.jpg', 'result_20260524135444525_0b41d615.jpg', '2026-05-24 13:54:44', 'judge=RECHECK, defectType=other_defect, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426457628807169, 2058426457561698305, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135445753_e89ad020.jpg', 'source_20260524135445753_e89ad020.jpg', '2026-05-24 13:54:45', 'judge=RECHECK, defectType=star_jump, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426457628807170, 2058426457561698305, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135445845_dc359e8b.jpg', 'result_20260524135445845_dc359e8b.jpg', '2026-05-24 13:54:45', 'judge=RECHECK, defectType=star_jump, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426463177871362, 2058426463047847937, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135447058_77ea0d16.jpg', 'source_20260524135447058_77ea0d16.jpg', '2026-05-24 13:54:47', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426463177871363, 2058426463047847937, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135447153_6ea8a0df.jpg', 'result_20260524135447153_6ea8a0df.jpg', '2026-05-24 13:54:47', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426468693381123, 2058426468693381122, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135448384_4e79ccc7.jpg', 'source_20260524135448384_4e79ccc7.jpg', '2026-05-24 13:54:48', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426468760489986, 2058426468693381122, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135448491_99c64544.jpg', 'result_20260524135448491_99c64544.jpg', '2026-05-24 13:54:48', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426474175336451, 2058426474175336450, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135449702_6c3a5ca4.jpg', 'source_20260524135449702_6c3a5ca4.jpg', '2026-05-24 13:54:49', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426474238251009, 2058426474175336450, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135449800_ce006df8.jpg', 'result_20260524135449800_ce006df8.jpg', '2026-05-24 13:54:49', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426479724400641, 2058426479661486082, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135451011_5ff545b8.jpg', 'source_20260524135451011_5ff545b8.jpg', '2026-05-24 13:54:50', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426479724400642, 2058426479661486082, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135451109_21eebec3.jpg', 'result_20260524135451109_21eebec3.jpg', '2026-05-24 13:54:50', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426485143441411, 2058426485143441410, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135452308_a2033569.jpg', 'source_20260524135452308_a2033569.jpg', '2026-05-24 13:54:52', 'judge=RECHECK, defectType=knot, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426485206355970, 2058426485143441410, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135452407_a81d11b9.jpg', 'result_20260524135452407_a81d11b9.jpg', '2026-05-24 13:54:52', 'judge=RECHECK, defectType=knot, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426490591842306, 2058426490524733442, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135453605_a529b6f3.jpg', 'source_20260524135453605_a529b6f3.jpg', '2026-05-24 13:54:53', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426490591842307, 2058426490524733442, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135453705_3af12ed1.jpg', 'result_20260524135453705_3af12ed1.jpg', '2026-05-24 13:54:53', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426495989911554, 2058426495922802689, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135454893_a71adcbf.jpg', 'source_20260524135454893_a71adcbf.jpg', '2026-05-24 13:54:54', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426495989911555, 2058426495922802689, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135454992_41269001.jpg', 'result_20260524135454992_41269001.jpg', '2026-05-24 13:54:54', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426501408952322, 2058426501346037762, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135456188_428d18f2.jpg', 'source_20260524135456188_428d18f2.jpg', '2026-05-24 13:54:56', 'judge=RECHECK, defectType=other_defect, boxCount=4', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426501408952323, 2058426501346037762, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135456285_e909f991.jpg', 'result_20260524135456285_e909f991.jpg', '2026-05-24 13:54:56', 'judge=RECHECK, defectType=other_defect, boxCount=4', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426506874130434, 2058426506874130433, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135457500_cad49f91.jpg', 'source_20260524135457500_cad49f91.jpg', '2026-05-24 13:54:57', 'judge=RECHECK, defectType=star_jump, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426506937044993, 2058426506874130433, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135457597_cc22a977.jpg', 'result_20260524135457597_cc22a977.jpg', '2026-05-24 13:54:57', 'judge=RECHECK, defectType=star_jump, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426512381251586, 2058426512314142722, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135458794_6be86a8a.jpg', 'source_20260524135458794_6be86a8a.jpg', '2026-05-24 13:54:58', 'judge=RECHECK, defectType=knot, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426512381251587, 2058426512314142722, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135458898_b1067fa4.jpg', 'result_20260524135458898_b1067fa4.jpg', '2026-05-24 13:54:58', 'judge=RECHECK, defectType=knot, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426517871595522, 2058426517808680962, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135500110_2c2a50a2.jpg', 'source_20260524135500110_2c2a50a2.jpg', '2026-05-24 13:55:00', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426517871595523, 2058426517808680962, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135500209_aac02907.jpg', 'result_20260524135500209_aac02907.jpg', '2026-05-24 13:55:00', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426523307413506, 2058426523240304641, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135501405_7ae3a535.jpg', 'source_20260524135501405_7ae3a535.jpg', '2026-05-24 13:55:01', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426523307413507, 2058426523240304641, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135501503_987f68a6.jpg', 'result_20260524135501503_987f68a6.jpg', '2026-05-24 13:55:01', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426528780980226, 2058426528713871362, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135502708_bffa01f4.jpg', 'source_20260524135502708_bffa01f4.jpg', '2026-05-24 13:55:02', 'judge=RECHECK, defectType=stain, boxCount=2', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426528780980227, 2058426528713871362, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135502809_03f7e863.jpg', 'result_20260524135502809_03f7e863.jpg', '2026-05-24 13:55:02', 'judge=RECHECK, defectType=stain, boxCount=2', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426534258741249, 2058426534191632386, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135504014_8b677ca9.jpg', 'source_20260524135504014_8b677ca9.jpg', '2026-05-24 13:55:03', 'judge=RECHECK, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426534258741250, 2058426534191632386, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135504114_0f3808b2.jpg', 'result_20260524135504114_0f3808b2.jpg', '2026-05-24 13:55:03', 'judge=RECHECK, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426539702947842, 2058426539702947841, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135505321_f10b2bbd.jpg', 'source_20260524135505321_f10b2bbd.jpg', '2026-05-24 13:55:05', 'judge=RECHECK, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426539770056706, 2058426539702947841, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135505419_b11bb147.jpg', 'result_20260524135505419_b11bb147.jpg', '2026-05-24 13:55:05', 'judge=RECHECK, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426545252012033, 2058426545180708865, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135506632_08f2019b.jpg', 'source_20260524135506632_08f2019b.jpg', '2026-05-24 13:55:06', 'judge=RECHECK, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426545252012034, 2058426545180708865, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135506735_bc08929b.jpg', 'result_20260524135506735_bc08929b.jpg', '2026-05-24 13:55:06', 'judge=RECHECK, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426550742355970, 2058426550742355969, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135507956_d90fab44.jpg', 'source_20260524135507956_d90fab44.jpg', '2026-05-24 13:55:07', 'judge=RECHECK, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426550809464834, 2058426550742355969, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135508050_d814c403.jpg', 'result_20260524135508050_d814c403.jpg', '2026-05-24 13:55:07', 'judge=RECHECK, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426556362723330, 2058426556295614465, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135509274_faa09b37.jpg', 'source_20260524135509274_faa09b37.jpg', '2026-05-24 13:55:09', 'judge=RECHECK, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426556362723331, 2058426556295614465, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135509377_f3e18c88.jpg', 'result_20260524135509377_f3e18c88.jpg', '2026-05-24 13:55:09', 'judge=RECHECK, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426561710460930, 2058426561710460929, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135510570_af4ca7ee.jpg', 'source_20260524135510570_af4ca7ee.jpg', '2026-05-24 13:55:10', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426561710460931, 2058426561710460929, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135510663_c2a27498.jpg', 'result_20260524135510663_c2a27498.jpg', '2026-05-24 13:55:10', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426567150473218, 2058426567083364353, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135511851_7c9a1a46.jpg', 'source_20260524135511851_7c9a1a46.jpg', '2026-05-24 13:55:11', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426567150473219, 2058426567083364353, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135511950_493cf69d.jpg', 'result_20260524135511950_493cf69d.jpg', '2026-05-24 13:55:11', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426572540153858, 2058426572473044994, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135513144_7698c21a.jpg', 'source_20260524135513144_7698c21a.jpg', '2026-05-24 13:55:13', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426572540153859, 2058426572473044994, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135513242_52d50522.jpg', 'result_20260524135513242_52d50522.jpg', '2026-05-24 13:55:13', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058426577887891458, 2058426577824976897, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524135514427_f746f5e0.jpg', 'source_20260524135514427_f746f5e0.jpg', '2026-05-24 13:55:14', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058426577955000322, 2058426577824976897, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524135514520_5116b839.jpg', 'result_20260524135514520_5116b839.jpg', '2026-05-24 13:55:14', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058429290516254721, 2058429290449145858, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524140601162_b4f836ab.jpg', 'source_20260524140601162_b4f836ab.jpg', '2026-05-24 14:06:00', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058429290516254722, 2058429290449145858, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524140601259_4566a4b8.jpg', 'result_20260524140601259_4566a4b8.jpg', '2026-05-24 14:06:00', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058429302000259074, 2058429301933150210, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524140603901_4618404b.jpg', 'source_20260524140603901_4618404b.jpg', '2026-05-24 14:06:03', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058429302067367937, 2058429301933150210, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524140603998_5049169a.jpg', 'result_20260524140603998_5049169a.jpg', '2026-05-24 14:06:03', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058429349609803778, 2058429349609803777, NULL, 'image', 'photo/upload/20260524/source_20260524140615098_f8d31e7c.jpg', 'source_20260524140615098_f8d31e7c.jpg', '2026-05-24 14:06:15', 'judge=FAIL, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=offline; variant=source');
INSERT INTO `inspectiondata` VALUES (2058429349676912642, 2058429349609803777, NULL, 'image-result', 'photo/results/20260524/result_20260524140615258_b7901d64.jpg', 'result_20260524140615258_b7901d64.jpg', '2026-05-24 14:06:15', 'judge=FAIL, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=offline; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058429408673992705, 2058429408611078146, NULL, 'image', 'photo/upload/20260524/source_20260524140629168_c7ab0594.jpg', 'source_20260524140629168_c7ab0594.jpg', '2026-05-24 14:06:29', 'judge=FAIL, defectType=knot, boxCount=1', 'Stored by Java ONNX detection; inspectType=offline; variant=source');
INSERT INTO `inspectiondata` VALUES (2058429408673992706, 2058429408611078146, NULL, 'image-result', 'photo/results/20260524/result_20260524140629347_23857c1c.jpg', 'result_20260524140629347_23857c1c.jpg', '2026-05-24 14:06:29', 'judge=FAIL, defectType=knot, boxCount=1', 'Stored by Java ONNX detection; inspectType=offline; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058429522733895682, 2058429522733895681, NULL, 'image', 'photo/upload/20260524/source_20260524140656375_276b63d1.jpg', 'source_20260524140656375_276b63d1.jpg', '2026-05-24 14:06:57', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=offline; variant=source');
INSERT INTO `inspectiondata` VALUES (2058429522733895683, 2058429522733895681, NULL, 'image-result', 'photo/results/20260524/result_20260524140656515_164b0c5e.jpg', 'result_20260524140656515_164b0c5e.jpg', '2026-05-24 14:06:57', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=offline; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058429593227563009, 2058429593160454146, NULL, 'image', 'photo/upload/20260524/source_20260524140713150_9d96175f.jpg', 'source_20260524140713150_9d96175f.jpg', '2026-05-24 14:07:13', 'judge=FAIL, defectType=star_jump, boxCount=1', 'Stored by Java ONNX detection; inspectType=offline; variant=source');
INSERT INTO `inspectiondata` VALUES (2058429593227563010, 2058429593160454146, NULL, 'image-result', 'photo/results/20260524/result_20260524140713304_d4236be4.jpg', 'result_20260524140713304_d4236be4.jpg', '2026-05-24 14:07:13', 'judge=FAIL, defectType=star_jump, boxCount=1', 'Stored by Java ONNX detection; inspectType=offline; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058479676425928705, 2058479676107161601, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524172613936_56c052c6.jpg', 'source_20260524172613936_56c052c6.jpg', '2026-05-24 17:26:12', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058479676555952129, 2058479676107161601, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524172614045_ed080cda.jpg', 'result_20260524172614045_ed080cda.jpg', '2026-05-24 17:26:12', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058479709057613825, 2058479708927590401, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524172621827_5adbadc9.jpg', 'source_20260524172621827_5adbadc9.jpg', '2026-05-24 17:26:21', 'judge=FAIL, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058479709120528386, 2058479708927590401, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524172621967_bcb6ff26.jpg', 'result_20260524172621967_bcb6ff26.jpg', '2026-05-24 17:26:21', 'judge=FAIL, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058479737222365185, 2058479737159450626, 2058416127540396034, 'frame', 'photo/upload/20260524/source_20260524172628583_9a686c40.jpg', 'source_20260524172628583_9a686c40.jpg', '2026-05-24 17:26:28', 'judge=FAIL, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058479737289474049, 2058479737159450626, 2058416127540396034, 'frame-result', 'photo/results/20260524/result_20260524172628696_74c29a4e.jpg', 'result_20260524172628696_74c29a4e.jpg', '2026-05-24 17:26:28', 'judge=FAIL, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058785023367766017, 2058785023279685633, NULL, 'image', 'photo/upload/20260525/source_20260525133933789_fd19eb65.jpg', 'source_20260525133933789_fd19eb65.jpg', '2026-05-25 13:39:35', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=offline; variant=source');
INSERT INTO `inspectiondata` VALUES (2058785023367766018, 2058785023279685633, NULL, 'image-result', 'photo/results/20260525/result_20260525133934394_d49a0423.jpg', 'result_20260525133934394_d49a0423.jpg', '2026-05-25 13:39:35', 'judge=PASS, defectType=none, boxCount=0', 'Stored by Java ONNX detection; inspectType=offline; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058785122365923330, 2058785122298814465, NULL, 'image', 'photo/upload/20260525/source_20260525133957884_6bab1911.jpg', 'source_20260525133957884_6bab1911.jpg', '2026-05-25 13:39:58', 'judge=FAIL, defectType=size_stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=offline; variant=source');
INSERT INTO `inspectiondata` VALUES (2058785122433032193, 2058785122298814465, NULL, 'image-result', 'photo/results/20260525/result_20260525133958042_da8a2fa3.jpg', 'result_20260525133958042_da8a2fa3.jpg', '2026-05-25 13:39:58', 'judge=FAIL, defectType=size_stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=offline; variant=annotated');
INSERT INTO `inspectiondata` VALUES (2058883651616837633, 2058883651444871169, 2058416127540396034, 'frame', 'photo/upload/20260525/source_20260525201128942_8fb72a56.jpg', 'source_20260525201128942_8fb72a56.jpg', '2026-05-25 20:11:27', 'judge=RECHECK, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=source');
INSERT INTO `inspectiondata` VALUES (2058883651725889537, 2058883651444871169, 2058416127540396034, 'frame-result', 'photo/results/20260525/result_20260525201129167_18458d1a.jpg', 'result_20260525201129167_18458d1a.jpg', '2026-05-25 20:11:27', 'judge=RECHECK, defectType=stain, boxCount=1', 'Stored by Java ONNX detection; inspectType=video; variant=annotated');

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
INSERT INTO `machine` VALUES (40001, 'PT-DS-01', '连续退浆机 1 号', 'PRETREAT', '适合 150~185cm 门幅的针织胚布退浆', 1, '2026-05-15 08:00:00');
INSERT INTO `machine` VALUES (40002, 'PT-SC-01', '高效煮练机 1 号', 'PRETREAT', '棉类前处理主力设备，蒸汽稳定性较好', 1, '2026-05-15 08:10:00');
INSERT INTO `machine` VALUES (40003, 'PT-BL-01', '连续漂白机 1 号', 'PRETREAT', '用于浅色及婴童单白度提升', 1, '2026-05-15 08:20:00');
INSERT INTO `machine` VALUES (40004, 'DY-OF-01', '高温溢流染色机 1 号', 'DYEING', '当前承担涤氨双面布深色单生产', 2, '2026-05-15 08:30:00');
INSERT INTO `machine` VALUES (40005, 'DY-OF-02', '高温溢流染色机 2 号', 'DYEING', '适合棉弹汗布与涤棉布染色切换', 1, '2026-05-15 08:35:00');
INSERT INTO `machine` VALUES (40006, 'FN-FN-01', '功能整理机 1 号', 'FINISHING', '支持吸湿排汗及耐磨后整理', 1, '2026-05-15 08:45:00');
INSERT INTO `machine` VALUES (40007, 'ST-01', '拉幅定型机 1 号', 'FINISHING', '主要控制门幅、克重和布面平整度', 2, '2026-05-15 09:00:00');
INSERT INTO `machine` VALUES (40008, 'IN-01', '成品验布机 1 号', 'INSPECTION', '用于成品检验与匹号确认', 1, '2026-05-15 09:10:00');
INSERT INTO `machine` VALUES (40009, 'PK-01', '自动卷装机 1 号', 'PACKING', '支持自动贴标与卷装长度统计', 1, '2026-05-15 09:20:00');
INSERT INTO `machine` VALUES (40010, 'WS-01', '连续水洗机 1 号', 'POST_DYE', '适合染后皂洗与多槽串联水洗', 2, '2026-05-15 09:30:00');

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
INSERT INTO `orderbatchlink` VALUES (70001, 90001, 91001, 10001, 2400.00, 15000.00, '首批主料已全部分配到汗布订单');
INSERT INTO `orderbatchlink` VALUES (70002, 90002, 91002, 10002, 3000.00, 12600.00, '深灰双面布已锁定夜班染色窗口');
INSERT INTO `orderbatchlink` VALUES (70003, 90003, 91003, 10003, 2800.00, 13800.00, '工装功能整理单全量分配');
INSERT INTO `orderbatchlink` VALUES (70004, 90004, 91004, 10004, 2500.00, 16000.00, '婴童浅色布已进入待排状态');
INSERT INTO `orderbatchlink` VALUES (70005, 90005, 91005, 10005, 1900.00, 11800.00, '家居服面料待补整理方案后排产');
INSERT INTO `orderbatchlink` VALUES (70006, 90006, 91006, 10006, 3200.00, 13500.00, '高弹运动面料待确认最终工艺路线');

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
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`orderId`) USING BTREE,
  UNIQUE INDEX `uk_orderinfo_orderNo`(`orderNo` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of orderinfo
-- ----------------------------
INSERT INTO `orderinfo` VALUES (90001, 'ORD-20260520-001', '海澜之家针织事业部', '2026-05-20 09:10:00', '2026-05-30 18:00:00', 'HIGH', 'IN_PROGRESS', '出口休闲系列汗布订单，优先保交期', '2026-05-20 09:15:00');
INSERT INTO `orderinfo` VALUES (90002, 'ORD-20260520-002', '迪卡侬华东供应链', '2026-05-20 11:30:00', '2026-05-29 18:00:00', 'HIGH', 'IN_PROGRESS', '秋冬运动双面布订单，夜班染色窗口已锁定', '2026-05-20 11:35:00');
INSERT INTO `orderinfo` VALUES (90003, 'ORD-20260519-003', '际华工装面料部', '2026-05-19 15:20:00', '2026-05-28 12:00:00', 'MEDIUM', 'DONE', '工装功能整理订单，本批已完工待发运', '2026-05-19 15:25:00');
INSERT INTO `orderinfo` VALUES (90004, 'ORD-20260521-004', '巴拉巴拉婴童面料中心', '2026-05-21 10:10:00', '2026-06-01 18:00:00', 'HIGH', 'PLANNING', '婴童浅色布订单，待客户确认克重窗口后下发', '2026-05-21 10:15:00');
INSERT INTO `orderinfo` VALUES (90005, 'ORD-20260522-005', '都市丽人家居服事业部', '2026-05-22 14:40:00', '2026-06-03 18:00:00', 'MEDIUM', 'READY', '莫代尔棉家居服面料，待排柔软整理方案', '2026-05-22 14:45:00');
INSERT INTO `orderinfo` VALUES (90006, 'ORD-20260522-006', '安踏运动针织开发部', '2026-05-22 16:10:00', '2026-06-05 18:00:00', 'MEDIUM', 'READY', '锦氨高弹运动面料，待确认定型和后整理路线', '2026-05-22 16:15:00');

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
INSERT INTO `orderitem` VALUES (91001, 90001, 'HLJ-JERSEY-32S', '32S精梳棉弹汗布', '180g/m2', '军绿', 15000.00, 'm', 168.00, 2400.00, '需控制色差，适配外贸休闲男装');
INSERT INTO `orderitem` VALUES (91002, 90002, 'DK-DBL-SPORT', '涤氨双面运动布', '260g/m2', '深灰', 12600.00, 'm', 180.00, 3000.00, '重点控制回弹手感与深色匀染');
INSERT INTO `orderitem` VALUES (91003, 90003, 'JH-WORK-FUNC', '涤棉工装功能面料', '220g/m2', '藏青', 13800.00, 'm', 175.00, 2800.00, '需做吸湿排汗功能整理');
INSERT INTO `orderitem` VALUES (91004, 90004, 'BLB-KIDS-WHITE', '全棉婴童白坯布', '165g/m2', '米白', 16000.00, 'm', 168.00, 2500.00, '对白度与手感要求较高');
INSERT INTO `orderitem` VALUES (91005, 90005, 'DSLR-MODAL-HOME', '莫代尔棉家居服面料', '190g/m2', '豆沙粉', 11800.00, 'm', 160.00, 1900.00, '建议后续补柔软整理路线');
INSERT INTO `orderitem` VALUES (91006, 90006, 'ANTA-NYLON-ELASTIC', '锦氨高弹运动面料', '240g/m2', '黑色', 13500.00, 'm', 182.00, 3200.00, '弹力与门幅稳定性是重点');

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
INSERT INTO `planstep` VALUES (61001, 60001, 30001, 40001, '2026-05-23 08:00:00', '2026-05-23 11:30:00', 3.50, 1, 'READY', '已完成缸前配布与开机确认');
INSERT INTO `planstep` VALUES (61002, 60001, 30002, 40002, '2026-05-23 11:30:00', '2026-05-23 15:30:00', 4.00, 2, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61003, 60001, 30003, 40003, '2026-05-23 15:30:00', '2026-05-23 18:30:00', 3.00, 3, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61004, 60001, 30004, 40005, '2026-05-23 18:30:00', '2026-05-24 00:30:00', 6.00, 4, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61005, 60001, 30005, 40010, '2026-05-24 00:30:00', '2026-05-24 03:00:00', 2.50, 5, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61006, 60001, 30007, 40007, '2026-05-24 03:00:00', '2026-05-24 05:30:00', 2.50, 6, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61007, 60001, 30008, 40008, '2026-05-24 05:30:00', '2026-05-24 07:00:00', 1.50, 7, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61008, 60001, 30009, 40009, '2026-05-24 07:00:00', '2026-05-24 08:00:00', 1.00, 8, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61009, 60002, 30004, 40004, '2026-05-22 20:00:00', '2026-05-23 02:00:00', 6.00, 1, 'RUNNING', '夜班染深灰，当前重点跟踪缸温与回修率');
INSERT INTO `planstep` VALUES (61010, 60002, 30005, 40010, '2026-05-23 02:00:00', '2026-05-23 04:30:00', 2.50, 2, 'READY', '已预留水洗机窗口');
INSERT INTO `planstep` VALUES (61011, 60002, 30007, 40007, '2026-05-23 04:30:00', '2026-05-23 07:00:00', 2.50, 3, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61012, 60002, 30008, 40008, '2026-05-23 07:00:00', '2026-05-23 08:30:00', 1.50, 4, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61013, 60002, 30009, 40009, '2026-05-23 08:30:00', '2026-05-23 09:30:00', 1.00, 5, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61014, 60003, 30001, 40001, '2026-05-20 08:00:00', '2026-05-20 11:30:00', 3.50, 1, 'FINISHED', '前处理一次通过');
INSERT INTO `planstep` VALUES (61015, 60003, 30002, 40002, '2026-05-20 11:30:00', '2026-05-20 15:30:00', 4.00, 2, 'FINISHED', NULL);
INSERT INTO `planstep` VALUES (61016, 60003, 30004, 40005, '2026-05-20 15:30:00', '2026-05-20 21:30:00', 6.00, 3, 'FINISHED', '色差控制在客户样卡 4-5 级');
INSERT INTO `planstep` VALUES (61017, 60003, 30006, 40006, '2026-05-20 21:30:00', '2026-05-21 00:30:00', 3.00, 4, 'FINISHED', '已完成吸湿排汗整理');
INSERT INTO `planstep` VALUES (61018, 60003, 30007, 40007, '2026-05-21 00:30:00', '2026-05-21 03:00:00', 2.50, 5, 'FINISHED', NULL);
INSERT INTO `planstep` VALUES (61019, 60003, 30008, 40008, '2026-05-21 03:00:00', '2026-05-21 04:30:00', 1.50, 6, 'FINISHED', '成检抽检通过');
INSERT INTO `planstep` VALUES (61020, 60003, 30009, 40009, '2026-05-21 04:30:00', '2026-05-21 05:30:00', 1.00, 7, 'FINISHED', '已完成打卷和标签打印');
INSERT INTO `planstep` VALUES (61021, 60004, 30001, 40001, '2026-05-24 08:00:00', '2026-05-24 11:30:00', 3.50, 1, 'PENDING', '待正式下发后排入机台');
INSERT INTO `planstep` VALUES (61022, 60004, 30002, 40002, '2026-05-24 11:30:00', '2026-05-24 15:30:00', 4.00, 2, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61023, 60004, 30003, 40003, '2026-05-24 15:30:00', '2026-05-24 18:30:00', 3.00, 3, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61024, 60004, 30004, 40005, '2026-05-24 18:30:00', '2026-05-25 00:30:00', 6.00, 4, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61025, 60004, 30005, 40010, '2026-05-25 00:30:00', '2026-05-25 03:00:00', 2.50, 5, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61026, 60004, 30007, 40007, '2026-05-25 03:00:00', '2026-05-25 05:30:00', 2.50, 6, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61027, 60004, 30008, 40008, '2026-05-25 05:30:00', '2026-05-25 07:00:00', 1.50, 7, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (61028, 60004, 30009, 40009, '2026-05-25 07:00:00', '2026-05-25 08:00:00', 1.00, 8, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2057734625475338242, 2057734625013964801, 30001, 40001, '2026-05-22 16:05:27', '2026-05-22 19:35:27', 3.50, 1, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2057734625538252802, 2057734625013964801, 30002, 40002, '2026-05-22 19:35:27', '2026-05-22 23:35:27', 4.00, 2, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2057734625538252803, 2057734625013964801, 30004, NULL, '2026-05-22 23:35:27', '2026-05-23 05:35:27', 6.00, 3, 'PENDING', 'No matching active machine capability was found during plan generation');
INSERT INTO `planstep` VALUES (2057734625538252804, 2057734625013964801, 30006, NULL, '2026-05-23 05:35:27', '2026-05-23 08:35:27', 3.00, 4, 'PENDING', 'No matching active machine capability was found during plan generation');
INSERT INTO `planstep` VALUES (2057734625601167362, 2057734625013964801, 30007, 40007, '2026-05-23 08:35:27', '2026-05-23 11:05:27', 2.50, 5, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2057734625601167363, 2057734625013964801, 30008, 40008, '2026-05-23 11:05:27', '2026-05-23 12:35:27', 1.50, 6, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2057734625601167364, 2057734625013964801, 30009, 40009, '2026-05-23 12:35:27', '2026-05-23 13:35:27', 1.00, 7, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2058567358653079554, 2058567358376255489, 30001, 40001, '2026-05-16 23:17:24', '2026-05-17 02:47:24', 3.50, 1, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2058567358653079555, 2058567358376255489, 30002, 40002, '2026-05-17 02:47:24', '2026-05-17 06:47:24', 4.00, 2, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2058567358653079556, 2058567358376255489, 30003, 40003, '2026-05-17 06:47:24', '2026-05-17 09:47:24', 3.00, 3, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2058567358653079557, 2058567358376255489, 30004, 40005, '2026-05-17 09:47:24', '2026-05-17 15:47:24', 6.00, 4, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2058567358653079558, 2058567358376255489, 30005, 40010, '2026-05-17 15:47:24', '2026-05-17 18:17:24', 2.50, 5, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2058567358653079559, 2058567358376255489, 30007, 40007, '2026-05-17 18:17:24', '2026-05-17 20:47:24', 2.50, 6, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2058567358720188417, 2058567358376255489, 30008, 40008, '2026-05-17 20:47:24', '2026-05-17 22:17:24', 1.50, 7, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2058567358720188418, 2058567358376255489, 30009, 40009, '2026-05-17 22:17:24', '2026-05-17 23:17:24', 1.00, 8, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2058721876988538882, 2058721876820766721, 30001, 40001, '2026-05-28 09:28:30', '2026-05-28 12:58:30', 3.50, 1, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2058721876988538883, 2058721876820766721, 30002, 40002, '2026-05-28 12:58:30', '2026-05-28 16:58:30', 4.00, 2, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2058721876988538884, 2058721876820766721, 30004, NULL, '2026-05-28 16:58:30', '2026-05-28 22:58:30', 6.00, 3, 'PENDING', 'No matching active machine capability was found during plan generation');
INSERT INTO `planstep` VALUES (2058721876988538885, 2058721876820766721, 30006, NULL, '2026-05-28 22:58:30', '2026-05-29 01:58:30', 3.00, 4, 'PENDING', 'No matching active machine capability was found during plan generation');
INSERT INTO `planstep` VALUES (2058721876988538886, 2058721876820766721, 30007, 40007, '2026-05-29 01:58:30', '2026-05-29 04:28:30', 2.50, 5, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2058721876988538887, 2058721876820766721, 30008, 40008, '2026-05-29 04:28:30', '2026-05-29 05:58:30', 1.50, 6, 'PENDING', NULL);
INSERT INTO `planstep` VALUES (2058721877055647745, 2058721876820766721, 30009, 40009, '2026-05-29 05:58:30', '2026-05-29 06:58:30', 1.00, 7, 'PENDING', NULL);

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
INSERT INTO `processparameter` VALUES (62001, 61009, '染色温度', '128', '℃', 'process', '2026-05-22 22:30:00', '深灰色主染段实时记录');
INSERT INTO `processparameter` VALUES (62002, 61009, '缸内压力', '0.28', 'MPa', 'process', '2026-05-22 22:45:00', '蒸汽压力恢复正常');
INSERT INTO `processparameter` VALUES (62003, 61009, '浴比', '1:8', '', 'recipe', '2026-05-22 21:50:00', '按标准配方投料');
INSERT INTO `processparameter` VALUES (62004, 61010, '水洗温度', '78', '℃', 'process', '2026-05-23 02:10:00', '待夜班转序后执行');
INSERT INTO `processparameter` VALUES (62005, 61016, '染色温度', '130', '℃', 'process', '2026-05-20 17:20:00', '涤棉布染色主段稳定');
INSERT INTO `processparameter` VALUES (62006, 61017, '轧余率', '68', '%', 'process', '2026-05-20 22:10:00', '功能整理液吸附均匀');
INSERT INTO `processparameter` VALUES (62007, 61018, '定型温度', '175', '℃', 'process', '2026-05-21 01:20:00', '按工艺卡控制门幅回缩');
INSERT INTO `processparameter` VALUES (62008, 61018, '车速', '32', 'm/min', 'process', '2026-05-21 01:25:00', '兼顾克重与门幅稳定性');

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
INSERT INTO `processroute` VALUES (20001, '常规棉弹汗布染整路线-A', '适用于棉弹汗布的常规前处理、染色、定型与成检', 1, '2026-05-18 13:30:00');
INSERT INTO `processroute` VALUES (20002, '涤氨双面布染整路线-B', '适用于涤氨双面布，重点控制染色均匀性与回弹手感', 1, '2026-05-18 13:45:00');
INSERT INTO `processroute` VALUES (20003, '涤棉功能整理路线-C', '适用于涤棉工装布，包含后整理功能加工', 1, '2026-05-19 08:50:00');

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
INSERT INTO `processstep` VALUES (30001, 'DESIZE', '退浆', 'PRETREAT', 10, 3.50, '适用于常规针织胚布的酶退浆与上油去除', 1);
INSERT INTO `processstep` VALUES (30002, 'SCOUR', '煮练', 'PRETREAT', 20, 4.00, '去除棉类胚布中的杂质、蜡质和浆料残留', 1);
INSERT INTO `processstep` VALUES (30003, 'BLEACH', '漂白', 'PRETREAT', 30, 3.00, '提升浅色布种白度，为染色和婴童单做准备', 1);
INSERT INTO `processstep` VALUES (30004, 'DYE', '染色', 'DYEING', 40, 6.00, '常规溢流染色工序，适合棉弹与涤氨布种', 1);
INSERT INTO `processstep` VALUES (30005, 'WASH', '水洗', 'POST_DYE', 50, 2.50, '染后皂洗、水洗及还原清洗', 1);
INSERT INTO `processstep` VALUES (30006, 'FINISH', '功能整理', 'FINISHING', 60, 3.00, '进行吸湿排汗或耐磨功能整理', 1);
INSERT INTO `processstep` VALUES (30007, 'STENTER', '定型', 'FINISHING', 70, 2.50, '控制门幅、克重和手感，稳定成品尺寸', 1);
INSERT INTO `processstep` VALUES (30008, 'FINAL_INSPECT', '成检', 'QUALITY_GATE', 80, 1.50, '落布前进行成品外观与工艺符合性确认', 1);
INSERT INTO `processstep` VALUES (30009, 'ROLLING', '卷装', 'PACKING', 90, 1.00, '按批次和匹号完成卷装与标签粘贴', 1);

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
  `createTime` datetime NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`planId`) USING BTREE,
  INDEX `batchId`(`batchId` ASC) USING BTREE,
  INDEX `routeId`(`routeId` ASC) USING BTREE,
  INDEX `idx_productionplan_orderId`(`orderId` ASC) USING BTREE,
  INDEX `idx_productionplan_orderItemId`(`orderItemId` ASC) USING BTREE,
  CONSTRAINT `productionplan_ibfk_1` FOREIGN KEY (`batchId`) REFERENCES `batchinfo` (`batchId`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `productionplan_ibfk_2` FOREIGN KEY (`routeId`) REFERENCES `processroute` (`routeId`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of productionplan
-- ----------------------------
INSERT INTO `productionplan` VALUES (60001, 90001, 91001, 10001, 20001, '2026-05-23 08:00:00', '2026-05-24 08:00:00', 'RELEASED', '2026-05-22 16:20:00', '外贸休闲汗布首批排产，优先保证色差稳定');
INSERT INTO `productionplan` VALUES (60002, 90002, 91002, 10002, 20002, '2026-05-22 20:00:00', '2026-05-23 09:30:00', 'RUNNING', '2026-05-22 15:40:00', '秋冬双面布深灰色单，夜班优先生产');
INSERT INTO `productionplan` VALUES (60003, 90003, 91003, 10003, 20003, '2026-05-20 08:00:00', '2026-05-21 05:30:00', 'COMPLETED', '2026-05-19 17:10:00', '工装布功能整理单已完工，待业务安排发运');
INSERT INTO `productionplan` VALUES (60004, 90004, 91004, 10004, 20001, '2026-05-24 08:00:00', '2026-05-25 08:00:00', 'DRAFT', '2026-05-22 17:00:00', '婴童浅色单待客户确认克重窗口后正式下发');
INSERT INTO `productionplan` VALUES (2057734625013964801, NULL, NULL, 10006, 20003, '2026-05-22 16:05:27', '2026-05-23 13:35:27', 'DRAFT', '2026-05-22 16:05:40', '不急');
INSERT INTO `productionplan` VALUES (2058567358376255489, 90004, 91004, 10004, 20001, '2026-05-16 23:17:24', '2026-05-17 23:17:24', 'DRAFT', '2026-05-24 23:14:39', '婴童浅色布订单，待客户确认克重窗口后下发');
INSERT INTO `productionplan` VALUES (2058721876820766721, 90006, 91006, 10006, 20003, '2026-05-28 09:28:30', '2026-05-29 06:58:30', 'DRAFT', '2026-05-25 09:28:39', '锦氨高弹运动面料，待确认定型和后整理路线');

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
INSERT INTO `qccamera` VALUES (2058416127540396034, 'LOCAL_CAM_001', '电脑摄像头', 'local_webcam', '127.0.0.1', '质检工作站本机', 1, '系统自动初始化的本机摄像头资源');

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
INSERT INTO `qcitem` VALUES (2058194754972876802, 'FABRIC_SURFACE_DEFECT', '胚布外观缺陷检测', 'image', '项', NULL, NULL, 1, '用于 Java ONNX 本地推理的默认图片检测标准');

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
-- Records of qcrecord
-- ----------------------------
INSERT INTO `qcrecord` VALUES (2058194958425980930, 61001, 2058194754972876802, '2026-05-23 22:34:52', 'offline', NULL, NULL, 'photo/results/20260523/result_20260523223451983_05da19e5.jpg', NULL, 'PASS', 'PASS', 'codex', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058197100960313345, 61001, 2058194754972876802, '2026-05-23 22:43:23', 'offline', NULL, NULL, 'photo/results/20260523/result_20260523224322801_7cc2cd89.jpg', NULL, 'PASS', 'PASS', 'codex', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058399713693728770, 61021, 2058194754972876802, '2026-05-24 12:08:30', 'offline', NULL, NULL, 'photo/results/20260524/result_20260524120829381_cb2159c7.jpg', 0.79, 'stain:0.79', 'FAIL', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=stain; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058416184465489921, 61001, 2058194754972876802, '2026-05-24 13:13:57', 'offline', NULL, NULL, 'photo/results/20260524/result_20260524131356396_09c765ac.jpg', NULL, 'PASS', 'PASS', 'codex', 'Triggered via Java ONNX Runtime detection; boxCount=0; remark=detect-image smoke test');
INSERT INTO `qcrecord` VALUES (2058416186487144449, 61001, 2058194754972876802, '2026-05-24 12:00:00', 'video', 2058416127540396034, '2026-05-24 12:00:00', 'photo/results/20260524/result_20260524131356904_2c10a46e.jpg', NULL, 'PASS', 'PASS', 'codex', 'Triggered via Java ONNX Runtime detection; boxCount=0; remark=detect-frame smoke test');
INSERT INTO `qcrecord` VALUES (2058420976902381569, 61001, 2058194754972876802, '2026-05-24 13:32:58', 'video', 2058416127540396034, '2026-05-24 13:32:58', 'photo/results/20260524/result_20260524133259088_43a0cc09.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058421270809845761, 61001, 2058194754972876802, '2026-05-24 13:34:09', 'offline', NULL, NULL, 'photo/results/20260524/result_20260524133409099_003ec0a6.jpg', 0.79, 'stain:0.79', 'FAIL', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=stain; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058421867051130882, 61001, 2058194754972876802, '2026-05-24 13:36:30', 'video', 2058416127540396034, '2026-05-24 13:36:30', 'photo/results/20260524/result_20260524133631379_3845fcfe.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426356919373825, 61001, 2058194754972876802, '2026-05-24 13:54:21', 'video', 2058416127540396034, '2026-05-24 13:54:21', 'photo/results/20260524/result_20260524135421779_2490ccb6.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426362632015873, 61001, 2058194754972876802, '2026-05-24 13:54:23', 'video', 2058416127540396034, '2026-05-24 13:54:23', 'photo/results/20260524/result_20260524135423204_6e0c9af0.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426368306909186, 61001, 2058194754972876802, '2026-05-24 13:54:24', 'video', 2058416127540396034, '2026-05-24 13:54:24', 'photo/results/20260524/result_20260524135424556_6ea9f14f.jpg', 0.19, 'other_defect:0.19', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=other_defect; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426374065688578, 61001, 2058194754972876802, '2026-05-24 13:54:25', 'video', 2058416127540396034, '2026-05-24 13:54:25', 'photo/results/20260524/result_20260524135425930_e85305a7.jpg', 0.42, 'star_jump:0.42', 'FAIL', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=star_jump; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426379786719233, 61001, 2058194754972876802, '2026-05-24 13:54:27', 'video', 2058416127540396034, '2026-05-24 13:54:27', 'photo/results/20260524/result_20260524135427299_966e5b10.jpg', 0.59, 'stain:0.59,star_jump:0.51,star_jump:0.29', 'FAIL', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=stain; boxCount=3');
INSERT INTO `qcrecord` VALUES (2058426385461612546, 61001, 2058194754972876802, '2026-05-24 13:54:28', 'video', 2058416127540396034, '2026-05-24 13:54:28', 'photo/results/20260524/result_20260524135428645_92dd3920.jpg', 0.26, 'knot:0.26', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=knot; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426391157477377, 61001, 2058194754972876802, '2026-05-24 13:54:29', 'video', 2058416127540396034, '2026-05-24 13:54:29', 'photo/results/20260524/result_20260524135429999_351ee5d8.jpg', 0.54, 'stain:0.54', 'FAIL', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=stain; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426396836564993, 61001, 2058194754972876802, '2026-05-24 13:54:31', 'video', 2058416127540396034, '2026-05-24 13:54:31', 'photo/results/20260524/result_20260524135431360_e7b8f0fc.jpg', 0.70, 'stain:0.70', 'FAIL', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=stain; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426402394017794, 61001, 2058194754972876802, '2026-05-24 13:54:32', 'video', 2058416127540396034, '2026-05-24 13:54:32', 'photo/results/20260524/result_20260524135432692_36735526.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426407947276290, 61001, 2058194754972876802, '2026-05-24 13:54:33', 'video', 2058416127540396034, '2026-05-24 13:54:33', 'photo/results/20260524/result_20260524135434018_518a85ac.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426413450203137, 61001, 2058194754972876802, '2026-05-24 13:54:35', 'video', 2058416127540396034, '2026-05-24 13:54:35', 'photo/results/20260524/result_20260524135435323_babcb519.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426418906992641, 61001, 2058194754972876802, '2026-05-24 13:54:36', 'video', 2058416127540396034, '2026-05-24 13:54:36', 'photo/results/20260524/result_20260524135436622_dd38964b.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426424435085314, 61001, 2058194754972876802, '2026-05-24 13:54:37', 'video', 2058416127540396034, '2026-05-24 13:54:37', 'photo/results/20260524/result_20260524135437935_f1f1f4da.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426429975760897, 61001, 2058194754972876802, '2026-05-24 13:54:39', 'video', 2058416127540396034, '2026-05-24 13:54:39', 'photo/results/20260524/result_20260524135439261_9f3c8e19.jpg', 0.24, 'thick_weft:0.24,star_jump:0.20,star_jump:0.17', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=thick_weft; boxCount=3');
INSERT INTO `qcrecord` VALUES (2058426435533213697, 61001, 2058194754972876802, '2026-05-24 13:54:40', 'video', 2058416127540396034, '2026-05-24 13:54:40', 'photo/results/20260524/result_20260524135440581_4573affb.jpg', 0.55, 'star_jump:0.55,star_jump:0.30,stain:0.28', 'FAIL', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=star_jump; boxCount=3');
INSERT INTO `qcrecord` VALUES (2058426440990003202, 61001, 2058194754972876802, '2026-05-24 13:54:41', 'video', 2058416127540396034, '2026-05-24 13:54:41', 'photo/results/20260524/result_20260524135441892_d024531d.jpg', 0.22, 'star_jump:0.22,stain:0.13,star_jump:0.12', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=star_jump; boxCount=3');
INSERT INTO `qcrecord` VALUES (2058426446610370562, 61001, 2058194754972876802, '2026-05-24 13:54:43', 'video', 2058416127540396034, '2026-05-24 13:54:43', 'photo/results/20260524/result_20260524135443218_f4f52285.jpg', 0.36, 'star_jump:0.36', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=star_jump; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426452092325889, 61001, 2058194754972876802, '2026-05-24 13:54:44', 'video', 2058416127540396034, '2026-05-24 13:54:44', 'photo/results/20260524/result_20260524135444525_0b41d615.jpg', 0.18, 'other_defect:0.18', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=other_defect; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426457561698305, 61001, 2058194754972876802, '2026-05-24 13:54:45', 'video', 2058416127540396034, '2026-05-24 13:54:45', 'photo/results/20260524/result_20260524135445845_dc359e8b.jpg', 0.31, 'star_jump:0.31', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=star_jump; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426463047847937, 61001, 2058194754972876802, '2026-05-24 13:54:47', 'video', 2058416127540396034, '2026-05-24 13:54:47', 'photo/results/20260524/result_20260524135447153_6ea8a0df.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426468693381122, 61001, 2058194754972876802, '2026-05-24 13:54:48', 'video', 2058416127540396034, '2026-05-24 13:54:48', 'photo/results/20260524/result_20260524135448491_99c64544.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426474175336450, 61001, 2058194754972876802, '2026-05-24 13:54:49', 'video', 2058416127540396034, '2026-05-24 13:54:49', 'photo/results/20260524/result_20260524135449800_ce006df8.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426479661486082, 61001, 2058194754972876802, '2026-05-24 13:54:50', 'video', 2058416127540396034, '2026-05-24 13:54:50', 'photo/results/20260524/result_20260524135451109_21eebec3.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426485143441410, 61001, 2058194754972876802, '2026-05-24 13:54:52', 'video', 2058416127540396034, '2026-05-24 13:54:52', 'photo/results/20260524/result_20260524135452407_a81d11b9.jpg', 0.26, 'knot:0.26', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=knot; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426490524733442, 61001, 2058194754972876802, '2026-05-24 13:54:53', 'video', 2058416127540396034, '2026-05-24 13:54:53', 'photo/results/20260524/result_20260524135453705_3af12ed1.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426495922802689, 61001, 2058194754972876802, '2026-05-24 13:54:54', 'video', 2058416127540396034, '2026-05-24 13:54:54', 'photo/results/20260524/result_20260524135454992_41269001.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426501346037762, 61001, 2058194754972876802, '2026-05-24 13:54:56', 'video', 2058416127540396034, '2026-05-24 13:54:56', 'photo/results/20260524/result_20260524135456285_e909f991.jpg', 0.23, 'other_defect:0.23,knot:0.15,hair_grain:0.13', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=other_defect; boxCount=4');
INSERT INTO `qcrecord` VALUES (2058426506874130433, 61001, 2058194754972876802, '2026-05-24 13:54:57', 'video', 2058416127540396034, '2026-05-24 13:54:57', 'photo/results/20260524/result_20260524135457597_cc22a977.jpg', 0.27, 'star_jump:0.27', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=star_jump; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426512314142722, 61001, 2058194754972876802, '2026-05-24 13:54:58', 'video', 2058416127540396034, '2026-05-24 13:54:58', 'photo/results/20260524/result_20260524135458898_b1067fa4.jpg', 0.36, 'knot:0.36', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=knot; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426517808680962, 61001, 2058194754972876802, '2026-05-24 13:55:00', 'video', 2058416127540396034, '2026-05-24 13:55:00', 'photo/results/20260524/result_20260524135500209_aac02907.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426523240304641, 61001, 2058194754972876802, '2026-05-24 13:55:01', 'video', 2058416127540396034, '2026-05-24 13:55:01', 'photo/results/20260524/result_20260524135501503_987f68a6.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426528713871362, 61001, 2058194754972876802, '2026-05-24 13:55:02', 'video', 2058416127540396034, '2026-05-24 13:55:02', 'photo/results/20260524/result_20260524135502809_03f7e863.jpg', 0.19, 'stain:0.19,stain:0.15', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=stain; boxCount=2');
INSERT INTO `qcrecord` VALUES (2058426534191632386, 61001, 2058194754972876802, '2026-05-24 13:55:03', 'video', 2058416127540396034, '2026-05-24 13:55:03', 'photo/results/20260524/result_20260524135504114_0f3808b2.jpg', 0.22, 'stain:0.22', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=stain; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426539702947841, 61001, 2058194754972876802, '2026-05-24 13:55:05', 'video', 2058416127540396034, '2026-05-24 13:55:05', 'photo/results/20260524/result_20260524135505419_b11bb147.jpg', 0.13, 'stain:0.13', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=stain; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426545180708865, 61001, 2058194754972876802, '2026-05-24 13:55:06', 'video', 2058416127540396034, '2026-05-24 13:55:06', 'photo/results/20260524/result_20260524135506735_bc08929b.jpg', 0.15, 'stain:0.15', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=stain; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426550742355969, 61001, 2058194754972876802, '2026-05-24 13:55:07', 'video', 2058416127540396034, '2026-05-24 13:55:07', 'photo/results/20260524/result_20260524135508050_d814c403.jpg', 0.12, 'stain:0.12', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=stain; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426556295614465, 61001, 2058194754972876802, '2026-05-24 13:55:09', 'video', 2058416127540396034, '2026-05-24 13:55:09', 'photo/results/20260524/result_20260524135509377_f3e18c88.jpg', 0.30, 'stain:0.30', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=stain; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058426561710460929, 61001, 2058194754972876802, '2026-05-24 13:55:10', 'video', 2058416127540396034, '2026-05-24 13:55:10', 'photo/results/20260524/result_20260524135510663_c2a27498.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426567083364353, 61001, 2058194754972876802, '2026-05-24 13:55:11', 'video', 2058416127540396034, '2026-05-24 13:55:11', 'photo/results/20260524/result_20260524135511950_493cf69d.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426572473044994, 61001, 2058194754972876802, '2026-05-24 13:55:13', 'video', 2058416127540396034, '2026-05-24 13:55:13', 'photo/results/20260524/result_20260524135513242_52d50522.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058426577824976897, 61001, 2058194754972876802, '2026-05-24 13:55:14', 'video', 2058416127540396034, '2026-05-24 13:55:14', 'photo/results/20260524/result_20260524135514520_5116b839.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058429290449145858, 61001, 2058194754972876802, '2026-05-24 14:06:00', 'video', 2058416127540396034, '2026-05-24 14:06:00', 'photo/results/20260524/result_20260524140601259_4566a4b8.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058429301933150210, 61001, 2058194754972876802, '2026-05-24 14:06:03', 'video', 2058416127540396034, '2026-05-24 14:06:03', 'photo/results/20260524/result_20260524140603998_5049169a.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058429349609803777, 61001, 2058194754972876802, '2026-05-24 14:06:15', 'offline', NULL, NULL, 'photo/results/20260524/result_20260524140615258_b7901d64.jpg', 0.79, 'stain:0.79', 'FAIL', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=stain; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058429408611078146, 61001, 2058194754972876802, '2026-05-24 14:06:29', 'offline', NULL, NULL, 'photo/results/20260524/result_20260524140629347_23857c1c.jpg', 0.57, 'knot:0.57', 'FAIL', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=knot; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058429522733895681, 61001, 2058194754972876802, '2026-05-24 14:06:57', 'offline', NULL, NULL, 'photo/results/20260524/result_20260524140656515_164b0c5e.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058429593160454146, 61001, 2058194754972876802, '2026-05-24 14:07:13', 'offline', NULL, NULL, 'photo/results/20260524/result_20260524140713304_d4236be4.jpg', 0.55, 'star_jump:0.55', 'FAIL', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=star_jump; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058479676107161601, 61001, 2058194754972876802, '2026-05-24 17:26:12', 'video', 2058416127540396034, '2026-05-24 17:26:12', 'photo/results/20260524/result_20260524172614045_ed080cda.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0; remark=实时视频流关键帧留档');
INSERT INTO `qcrecord` VALUES (2058479708927590401, 61001, 2058194754972876802, '2026-05-24 17:26:21', 'video', 2058416127540396034, '2026-05-24 17:26:21', 'photo/results/20260524/result_20260524172621967_bcb6ff26.jpg', 0.45, 'stain:0.45', 'FAIL', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=stain; boxCount=1; remark=实时视频流关键帧留档');
INSERT INTO `qcrecord` VALUES (2058479737159450626, 61001, 2058194754972876802, '2026-05-24 17:26:28', 'video', 2058416127540396034, '2026-05-24 17:26:28', 'photo/results/20260524/result_20260524172628696_74c29a4e.jpg', 0.62, 'stain:0.62', 'FAIL', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=stain; boxCount=1; remark=实时视频流关键帧留档');
INSERT INTO `qcrecord` VALUES (2058785023279685633, 61001, 2058194754972876802, '2026-05-25 13:39:35', 'offline', NULL, NULL, 'photo/results/20260525/result_20260525133934394_d49a0423.jpg', NULL, 'PASS', 'PASS', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; boxCount=0');
INSERT INTO `qcrecord` VALUES (2058785122298814465, 61001, 2058194754972876802, '2026-05-25 13:39:58', 'offline', NULL, NULL, 'photo/results/20260525/result_20260525133958042_da8a2fa3.jpg', 0.77, 'size_stain:0.77', 'FAIL', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=size_stain; boxCount=1');
INSERT INTO `qcrecord` VALUES (2058883651444871169, 61001, 2058194754972876802, '2026-05-25 20:11:27', 'video', 2058416127540396034, '2026-05-25 20:11:27', 'photo/results/20260525/result_20260525201129167_18458d1a.jpg', 0.16, 'stain:0.16', 'RECHECK', 'onnx-runtime', 'Triggered via Java ONNX Runtime detection; defectType=stain; boxCount=1; remark=实时视频流关键帧留档');

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
INSERT INTO `routestep` VALUES (21003, 20001, 30003, 3, 1);
INSERT INTO `routestep` VALUES (21004, 20001, 30004, 4, 1);
INSERT INTO `routestep` VALUES (21005, 20001, 30005, 5, 1);
INSERT INTO `routestep` VALUES (21006, 20001, 30007, 6, 1);
INSERT INTO `routestep` VALUES (21007, 20001, 30008, 7, 1);
INSERT INTO `routestep` VALUES (21008, 20001, 30009, 8, 1);
INSERT INTO `routestep` VALUES (21009, 20002, 30004, 1, 1);
INSERT INTO `routestep` VALUES (21010, 20002, 30005, 2, 1);
INSERT INTO `routestep` VALUES (21011, 20002, 30007, 3, 1);
INSERT INTO `routestep` VALUES (21012, 20002, 30008, 4, 1);
INSERT INTO `routestep` VALUES (21013, 20002, 30009, 5, 1);
INSERT INTO `routestep` VALUES (21014, 20003, 30001, 1, 1);
INSERT INTO `routestep` VALUES (21015, 20003, 30002, 2, 1);
INSERT INTO `routestep` VALUES (21016, 20003, 30004, 3, 1);
INSERT INTO `routestep` VALUES (21017, 20003, 30006, 4, 1);
INSERT INTO `routestep` VALUES (21018, 20003, 30007, 5, 1);
INSERT INTO `routestep` VALUES (21019, 20003, 30008, 6, 1);
INSERT INTO `routestep` VALUES (21020, 20003, 30009, 7, 1);

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
INSERT INTO `stepmachinecapability` VALUES (50004, 30004, 40004, 160.00, 185.00, 32.00, 3300.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50005, 30004, 40005, 150.00, 185.00, 34.00, 3200.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50006, 30005, 40010, 150.00, 185.00, 40.00, 3500.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50007, 30006, 40006, 150.00, 182.00, 28.00, 3000.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50008, 30007, 40007, 150.00, 185.00, 30.00, 3400.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50009, 30008, 40008, 150.00, 185.00, 50.00, 3600.00, 1);
INSERT INTO `stepmachinecapability` VALUES (50010, 30009, 40009, 150.00, 185.00, 55.00, 3600.00, 1);

SET FOREIGN_KEY_CHECKS = 1;
