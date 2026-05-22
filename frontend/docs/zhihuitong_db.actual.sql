-- zhihuitong 数据库建表脚本（按真实数据库元数据修正）
-- 修正依据：C:\Program Files\MySQL\MySQL Server 8.0\data\zhihuitong\*.ibd
-- 提取方式：ibd2sdi
-- 注意：当前 MySQL 实例启用了 lower_case_table_names=1，
-- 因此真实表名为全小写且不带下划线；字段名保留 camelCase。

CREATE DATABASE IF NOT EXISTS zhihuitong
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE zhihuitong;

CREATE TABLE batchinfo (
  batchId bigint NOT NULL,
  batchNo varchar(50) DEFAULT NULL,
  supplier varchar(100) DEFAULT NULL,
  inDate datetime DEFAULT NULL,
  weight decimal(12,2) DEFAULT NULL,
  width decimal(8,2) DEFAULT NULL,
  composition varchar(100) DEFAULT NULL,
  note varchar(255) DEFAULT NULL,
  PRIMARY KEY (batchId),
  UNIQUE KEY batchNo (batchNo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE processroute (
  routeId bigint NOT NULL,
  routeName varchar(100) DEFAULT NULL,
  description varchar(255) DEFAULT NULL,
  isActive tinyint DEFAULT NULL,
  createTime datetime DEFAULT NULL,
  PRIMARY KEY (routeId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE processstep (
  stepId bigint NOT NULL,
  stepCode varchar(50) DEFAULT NULL,
  stepName varchar(100) DEFAULT NULL,
  stepType varchar(50) DEFAULT NULL,
  sortOrder int DEFAULT NULL,
  defaultHours decimal(8,2) DEFAULT NULL,
  description varchar(255) DEFAULT NULL,
  isActive tinyint DEFAULT NULL,
  PRIMARY KEY (stepId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE machine (
  machineId bigint NOT NULL,
  machineCode varchar(50) DEFAULT NULL,
  machineName varchar(100) DEFAULT NULL,
  machineType varchar(50) DEFAULT NULL,
  description varchar(255) DEFAULT NULL,
  status tinyint DEFAULT NULL,
  createTime datetime DEFAULT NULL,
  PRIMARY KEY (machineId),
  UNIQUE KEY machineCode (machineCode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE qcitem (
  qcItemId bigint NOT NULL,
  qcItemCode varchar(50) DEFAULT NULL,
  qcItemName varchar(100) DEFAULT NULL,
  qcType varchar(20) DEFAULT NULL,
  unit varchar(20) DEFAULT NULL,
  standardMin decimal(12,4) DEFAULT NULL,
  standardMax decimal(12,4) DEFAULT NULL,
  isActive tinyint DEFAULT NULL,
  description varchar(255) DEFAULT NULL,
  PRIMARY KEY (qcItemId),
  UNIQUE KEY qcItemCode (qcItemCode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE qccamera (
  cameraId bigint NOT NULL,
  cameraCode varchar(50) DEFAULT NULL,
  cameraName varchar(100) DEFAULT NULL,
  cameraType varchar(50) DEFAULT NULL,
  ipAddress varchar(50) DEFAULT NULL,
  location varchar(100) DEFAULT NULL,
  status tinyint DEFAULT NULL,
  remark varchar(255) DEFAULT NULL,
  PRIMARY KEY (cameraId),
  UNIQUE KEY cameraCode (cameraCode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE routestep (
  routeStepId bigint NOT NULL,
  routeId bigint DEFAULT NULL,
  stepId bigint DEFAULT NULL,
  sortOrder int DEFAULT NULL,
  isMandatory tinyint DEFAULT NULL,
  PRIMARY KEY (routeStepId),
  KEY routeId (routeId),
  KEY stepId (stepId),
  CONSTRAINT routestep_ibfk_1 FOREIGN KEY (routeId) REFERENCES processroute (routeId),
  CONSTRAINT routestep_ibfk_2 FOREIGN KEY (stepId) REFERENCES processstep (stepId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stepmachinecapability (
  capId bigint NOT NULL,
  stepId bigint DEFAULT NULL,
  machineId bigint DEFAULT NULL,
  minWidth decimal(8,2) DEFAULT NULL,
  maxWidth decimal(8,2) DEFAULT NULL,
  maxSpeed decimal(8,2) DEFAULT NULL,
  maxBatchWeight decimal(12,2) DEFAULT NULL,
  isActive tinyint DEFAULT NULL,
  PRIMARY KEY (capId),
  KEY stepId (stepId),
  KEY machineId (machineId),
  CONSTRAINT stepmachinecapability_ibfk_1 FOREIGN KEY (stepId) REFERENCES processstep (stepId),
  CONSTRAINT stepmachinecapability_ibfk_2 FOREIGN KEY (machineId) REFERENCES machine (machineId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE productionplan (
  planId bigint NOT NULL,
  batchId bigint DEFAULT NULL,
  routeId bigint DEFAULT NULL,
  planStartTime datetime DEFAULT NULL,
  planEndTime datetime DEFAULT NULL,
  status varchar(20) DEFAULT NULL,
  createTime datetime DEFAULT NULL,
  remark varchar(255) DEFAULT NULL,
  PRIMARY KEY (planId),
  KEY batchId (batchId),
  KEY routeId (routeId),
  CONSTRAINT productionplan_ibfk_1 FOREIGN KEY (batchId) REFERENCES batchinfo (batchId),
  CONSTRAINT productionplan_ibfk_2 FOREIGN KEY (routeId) REFERENCES processroute (routeId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE planstep (
  planStepId bigint NOT NULL,
  planId bigint DEFAULT NULL,
  stepId bigint DEFAULT NULL,
  machineId bigint DEFAULT NULL,
  planStartTime datetime DEFAULT NULL,
  planEndTime datetime DEFAULT NULL,
  planHours decimal(12,2) DEFAULT NULL,
  sequenceNo tinyint DEFAULT NULL,
  status varchar(20) DEFAULT NULL,
  remark varchar(255) DEFAULT NULL,
  PRIMARY KEY (planStepId),
  KEY planId (planId),
  KEY stepId (stepId),
  KEY machineId (machineId),
  CONSTRAINT planstep_ibfk_1 FOREIGN KEY (planId) REFERENCES productionplan (planId),
  CONSTRAINT planstep_ibfk_2 FOREIGN KEY (stepId) REFERENCES processstep (stepId),
  CONSTRAINT planstep_ibfk_3 FOREIGN KEY (machineId) REFERENCES machine (machineId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE processparameter (
  paramId bigint NOT NULL,
  planStepId bigint DEFAULT NULL,
  paramName varchar(100) DEFAULT NULL,
  paramValue varchar(100) DEFAULT NULL,
  unit varchar(20) DEFAULT NULL,
  paramType varchar(20) DEFAULT NULL,
  recordTime datetime DEFAULT NULL,
  remark varchar(255) DEFAULT NULL,
  PRIMARY KEY (paramId),
  KEY planStepId (planStepId),
  CONSTRAINT processparameter_ibfk_1 FOREIGN KEY (planStepId) REFERENCES planstep (planStepId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE qcrecord (
  inspectionId bigint NOT NULL,
  planStepId bigint DEFAULT NULL,
  qcItemId bigint DEFAULT NULL,
  inspectTime datetime DEFAULT NULL,
  inspectType enum('offline','video') DEFAULT NULL,
  cameraId bigint DEFAULT NULL,
  frameTime datetime DEFAULT NULL,
  imageUrl varchar(255) DEFAULT NULL,
  confidenceScore decimal(5,2) DEFAULT NULL,
  resultValue varchar(100) DEFAULT NULL,
  resultJudge varchar(20) DEFAULT NULL,
  inspector varchar(100) DEFAULT NULL,
  remark varchar(255) DEFAULT NULL,
  PRIMARY KEY (inspectionId),
  KEY planStepId (planStepId),
  KEY qcItemId (qcItemId),
  CONSTRAINT qcrecord_ibfk_1 FOREIGN KEY (planStepId) REFERENCES planstep (planStepId),
  CONSTRAINT qcrecord_ibfk_2 FOREIGN KEY (qcItemId) REFERENCES qcitem (qcItemId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inspectiondata (
  dataId bigint NOT NULL,
  qcRecordId bigint DEFAULT NULL,
  cameraId bigint DEFAULT NULL,
  fileType varchar(20) DEFAULT NULL,
  filePath varchar(255) DEFAULT NULL,
  fileName varchar(255) DEFAULT NULL,
  captureTime datetime DEFAULT NULL,
  resultSummary varchar(255) DEFAULT NULL,
  remark varchar(255) DEFAULT NULL,
  PRIMARY KEY (dataId),
  KEY qcRecordId (qcRecordId),
  KEY cameraId (cameraId),
  CONSTRAINT inspectiondata_ibfk_1 FOREIGN KEY (qcRecordId) REFERENCES qcrecord (inspectionId),
  CONSTRAINT inspectiondata_ibfk_2 FOREIGN KEY (cameraId) REFERENCES qccamera (cameraId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exceptionrecord (
  exceptionId bigint NOT NULL,
  planStepId bigint DEFAULT NULL,
  exceptionType varchar(50) DEFAULT NULL,
  exceptionLevel varchar(20) DEFAULT NULL,
  description varchar(255) DEFAULT NULL,
  handleResult varchar(255) DEFAULT NULL,
  createTime datetime DEFAULT NULL,
  status varchar(20) DEFAULT NULL,
  PRIMARY KEY (exceptionId),
  KEY planStepId (planStepId),
  CONSTRAINT exceptionrecord_ibfk_1 FOREIGN KEY (planStepId) REFERENCES planstep (planStepId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
