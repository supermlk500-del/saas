-- 浏览器 ONNX 质检所需的最低基础数据。
-- 可重复执行；已有同编码数据时不会覆盖管理员修改。
INSERT INTO `qcitem`
    (`qcItemId`, `qcItemCode`, `qcItemName`, `qcType`, `unit`,
     `standardMin`, `standardMax`, `isActive`, `description`)
SELECT
    10001,
    'AI_FABRIC_DEFECT',
    'AI 布面瑕疵检测',
    'VISUAL',
    '处',
    0.0000,
    0.0000,
    1,
    '浏览器 ONNX 对布面破洞、污渍、断经等缺陷进行检测；检测到任一缺陷即判为不通过'
WHERE NOT EXISTS (
    SELECT 1
    FROM `qcitem`
    WHERE `qcItemCode` = 'AI_FABRIC_DEFECT'
);
