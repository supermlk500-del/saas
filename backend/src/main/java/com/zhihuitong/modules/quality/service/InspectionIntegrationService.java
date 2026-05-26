package com.zhihuitong.modules.quality.service;

import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.ai.model.YoloDetectResult;
import com.zhihuitong.modules.ai.service.OnnxYoloService;
import com.zhihuitong.modules.plan.service.PlanStepService;
import com.zhihuitong.modules.quality.dto.InspectionDataUpsertRequest;
import com.zhihuitong.modules.quality.dto.QcDetectFrameRequest;
import com.zhihuitong.modules.quality.dto.QcDetectImageRequest;
import com.zhihuitong.modules.quality.dto.QcRecordUpsertRequest;
import com.zhihuitong.modules.quality.dto.QcStreamSnapshotRequest;
import com.zhihuitong.modules.quality.entity.InspectionData;
import com.zhihuitong.modules.quality.entity.QcRecord;
import com.zhihuitong.modules.quality.model.QcStreamSessionContext;
import com.zhihuitong.modules.quality.vo.InspectionIntegrationResultVo;
import com.zhihuitong.modules.quality.vo.StoredInspectionFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class InspectionIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(InspectionIntegrationService.class);

    private final OnnxYoloService onnxYoloService;
    private final QcItemService qcItemService;
    private final QcRecordService qcRecordService;
    private final InspectionDataService inspectionDataService;
    private final QcCameraService qcCameraService;
    private final PlanStepService planStepService;
    private final InspectionFileStorageService inspectionFileStorageService;

    public InspectionIntegrationService(OnnxYoloService onnxYoloService,
                                        QcItemService qcItemService,
                                        QcRecordService qcRecordService,
                                        InspectionDataService inspectionDataService,
                                        QcCameraService qcCameraService,
                                        PlanStepService planStepService,
                                        InspectionFileStorageService inspectionFileStorageService) {
        this.onnxYoloService = onnxYoloService;
        this.qcItemService = qcItemService;
        this.qcRecordService = qcRecordService;
        this.inspectionDataService = inspectionDataService;
        this.qcCameraService = qcCameraService;
        this.planStepService = planStepService;
        this.inspectionFileStorageService = inspectionFileStorageService;
    }

    @Transactional
    public InspectionIntegrationResultVo detectImage(QcDetectImageRequest request) {
        MultipartFile file = requireFile(request.getFile());
        planStepService.requirePlanStep(request.getPlanStepId());
        if (request.getCameraId() != null) {
            qcCameraService.requireCamera(request.getCameraId());
        }
        qcItemService.requireQcItem(request.getQcItemId());
        StoredInspectionFile storedSourceFile = inspectionFileStorageService.storeSourceImage(file);
        log.info("detect-image request: planStepId={}, qcItemId={}, cameraId={}, originalFileName={}, sizeBytes={}, storedSourcePath={}",
                request.getPlanStepId(),
                request.getQcItemId(),
                request.getCameraId(),
                file.getOriginalFilename(),
                file.getSize(),
                storedSourceFile.getRelativePath());

        String inspectType = "offline";
        YoloDetectResult algorithmResult = onnxYoloService.detect(
                storedSourceFile.getAbsolutePath(),
                storedSourceFile.getRelativePath(),
                inspectType
        );

        QcRecord qcRecord = qcRecordService.create(buildQcRecordRequest(
                request.getPlanStepId(),
                request.getQcItemId(),
                request.getCameraId(),
                inspectType,
                algorithmResult,
                request.getInspector(),
                request.getRemark(),
                null
        ));
        List<InspectionData> inspectionDataList = persistInspectionData(
                qcRecord,
                request.getCameraId(),
                algorithmResult,
                storedSourceFile,
                inspectType,
                null
        );
        return buildResult(qcRecord, inspectionDataList, algorithmResult);
    }

    @Transactional
    public InspectionIntegrationResultVo detectFrame(QcDetectFrameRequest request) {
        MultipartFile file = requireFile(request.getFile());
        planStepService.requirePlanStep(request.getPlanStepId());
        qcCameraService.requireCamera(request.getCameraId());
        qcItemService.requireQcItem(request.getQcItemId());
        StoredInspectionFile storedSourceFile = inspectionFileStorageService.storeSourceImage(file);
        log.info("detect-frame request: planStepId={}, qcItemId={}, cameraId={}, frameTime={}, originalFileName={}, sizeBytes={}, storedSourcePath={}",
                request.getPlanStepId(),
                request.getQcItemId(),
                request.getCameraId(),
                request.getFrameTime(),
                file.getOriginalFilename(),
                file.getSize(),
                storedSourceFile.getRelativePath());

        String inspectType = "video";
        YoloDetectResult algorithmResult = onnxYoloService.detect(
                storedSourceFile.getAbsolutePath(),
                storedSourceFile.getRelativePath(),
                inspectType
        );

        QcRecord qcRecord = qcRecordService.create(buildQcRecordRequest(
                request.getPlanStepId(),
                request.getQcItemId(),
                request.getCameraId(),
                inspectType,
                algorithmResult,
                request.getInspector(),
                request.getRemark(),
                request.getFrameTime()
        ));
        List<InspectionData> inspectionDataList = persistInspectionData(
                qcRecord,
                request.getCameraId(),
                algorithmResult,
                storedSourceFile,
                inspectType,
                request.getFrameTime()
        );
        return buildResult(qcRecord, inspectionDataList, algorithmResult);
    }

    @Transactional
    public InspectionIntegrationResultVo snapshotFromStreamSession(QcStreamSessionContext sessionContext,
                                                                   QcStreamSnapshotRequest request) {
        MultipartFile file = requireFile(request.getFile());
        planStepService.requirePlanStep(sessionContext.getPlanStepId());
        qcItemService.requireQcItem(sessionContext.getQcItemId());
        qcCameraService.requireCamera(sessionContext.getCameraId());
        StoredInspectionFile storedSourceFile = inspectionFileStorageService.storeSourceImage(file);
        log.info("qc-stream snapshot request: sessionId={}, planStepId={}, qcItemId={}, cameraId={}, frameTime={}, originalFileName={}, sizeBytes={}, storedSourcePath={}",
                sessionContext.getSessionId(),
                sessionContext.getPlanStepId(),
                sessionContext.getQcItemId(),
                sessionContext.getCameraId(),
                request.getFrameTime(),
                file.getOriginalFilename(),
                file.getSize(),
                storedSourceFile.getRelativePath());

        String inspectType = "video";
        YoloDetectResult algorithmResult = onnxYoloService.detect(
                storedSourceFile.getAbsolutePath(),
                storedSourceFile.getRelativePath(),
                inspectType
        );

        QcRecord qcRecord = qcRecordService.create(buildQcRecordRequest(
                sessionContext.getPlanStepId(),
                sessionContext.getQcItemId(),
                sessionContext.getCameraId(),
                inspectType,
                algorithmResult,
                sessionContext.getInspector(),
                mergeRemark(sessionContext.getRemark(), request.getRemark()),
                request.getFrameTime()
        ));
        List<InspectionData> inspectionDataList = persistInspectionData(
                qcRecord,
                sessionContext.getCameraId(),
                algorithmResult,
                storedSourceFile,
                inspectType,
                request.getFrameTime()
        );
        return buildResult(qcRecord, inspectionDataList, algorithmResult);
    }

    private QcRecordUpsertRequest buildQcRecordRequest(Long planStepId,
                                                       Long qcItemId,
                                                       Long cameraId,
                                                       String inspectType,
                                                       YoloDetectResult algorithmResult,
                                                       String inspector,
                                                       String remark,
                                                       LocalDateTime frameTime) {
        QcRecordUpsertRequest request = new QcRecordUpsertRequest();
        request.setPlanStepId(planStepId);
        request.setQcItemId(qcItemId);
        request.setInspectTime(frameTime != null ? frameTime : LocalDateTime.now());
        request.setInspectType(inspectType);
        request.setCameraId(cameraId);
        request.setFrameTime(frameTime);
        request.setImageUrl(preferredResultImage(algorithmResult));
        request.setConfidenceScore(algorithmResult.getConfidenceScore());
        request.setResultValue(resolveResultValue(algorithmResult));
        request.setResultJudge(resolveResultJudge(algorithmResult));
        request.setInspector(StringUtils.hasText(inspector) ? inspector : "onnx-runtime");
        request.setRemark(resolveQcRemark(algorithmResult, remark));
        return request;
    }

    private List<InspectionData> persistInspectionData(QcRecord qcRecord,
                                                       Long cameraId,
                                                       YoloDetectResult algorithmResult,
                                                       StoredInspectionFile storedSourceFile,
                                                       String inspectType,
                                                       LocalDateTime captureTime) {
        List<InspectionData> records = new ArrayList<>();
        records.add(createInspectionDataRecord(
                qcRecord,
                cameraId,
                inspectTypeToFileType(inspectType),
                storedSourceFile.getRelativePath(),
                storedSourceFile.getFileName(),
                captureTime != null ? captureTime : qcRecord.getInspectTime(),
                buildResultSummary(algorithmResult),
                buildInspectionRemark(inspectType, false)
        ));
        if (StringUtils.hasText(algorithmResult.getImageUrl())) {
            String resultPath = algorithmResult.getImageUrl();
            records.add(createInspectionDataRecord(
                    qcRecord,
                    cameraId,
                    inspectTypeToFileType(inspectType) + "-result",
                    resultPath,
                    extractFileName(resultPath),
                    captureTime != null ? captureTime : qcRecord.getInspectTime(),
                    buildResultSummary(algorithmResult),
                    buildInspectionRemark(inspectType, true)
            ));
        }
        return records;
    }

    private InspectionIntegrationResultVo buildResult(QcRecord qcRecord,
                                                      List<InspectionData> inspectionDataList,
                                                      YoloDetectResult algorithmResult) {
        InspectionIntegrationResultVo result = new InspectionIntegrationResultVo();
        result.setInspectionId(qcRecord.getInspectionId());
        result.setResultJudge(algorithmResult.getResultJudge());
        result.setConfidenceScore(algorithmResult.getConfidenceScore());
        result.setResultValue(algorithmResult.getResultValue());
        result.setImageUrl(algorithmResult.getImageUrl());
        result.setSourceImageUrl(algorithmResult.getSourceImageUrl());
        result.setBoxes(algorithmResult.getBoxes());
        result.setQcRecord(qcRecord);
        result.setInspectionDataList(inspectionDataList);
        result.setAlgorithmResult(algorithmResult);
        return result;
    }

    private MultipartFile requireFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "file must not be empty");
        }
        return file;
    }

    private String resolveResultJudge(YoloDetectResult algorithmResult) {
        return algorithmResult.getResultJudge();
    }

    private String resolveResultValue(YoloDetectResult algorithmResult) {
        if (StringUtils.hasText(algorithmResult.getResultValue())) {
            return algorithmResult.getResultValue();
        }
        if (StringUtils.hasText(algorithmResult.getDefectType()) && algorithmResult.getConfidenceScore() != null) {
            return algorithmResult.getDefectType() + ":" + algorithmResult.getConfidenceScore().stripTrailingZeros().toPlainString();
        }
        return algorithmResult.getDefectType();
    }

    private String resolveQcRemark(YoloDetectResult algorithmResult, String userRemark) {
        StringBuilder builder = new StringBuilder("Triggered via Java ONNX Runtime detection");
        if (StringUtils.hasText(algorithmResult.getDefectType())) {
            builder.append("; defectType=").append(algorithmResult.getDefectType());
        }
        if (algorithmResult.getBoxes() != null) {
            builder.append("; boxCount=").append(algorithmResult.getBoxes().size());
        }
        if (StringUtils.hasText(userRemark)) {
            builder.append("; remark=").append(userRemark.trim());
        }
        return builder.toString();
    }

    private String preferredResultImage(YoloDetectResult algorithmResult) {
        if (StringUtils.hasText(algorithmResult.getImageUrl())) {
            return algorithmResult.getImageUrl();
        }
        return algorithmResult.getSourceImageUrl();
    }

    private String buildResultSummary(YoloDetectResult algorithmResult) {
        String resultJudge = resolveResultJudge(algorithmResult);
        int boxCount = algorithmResult.getBoxes() == null ? 0 : algorithmResult.getBoxes().size();
        String defectType = StringUtils.hasText(algorithmResult.getDefectType()) ? algorithmResult.getDefectType() : "none";
        return "judge=" + resultJudge + ", defectType=" + defectType + ", boxCount=" + boxCount;
    }

    private String buildInspectionRemark(String inspectType, boolean resultImage) {
        StringBuilder builder = new StringBuilder("Stored by Java ONNX detection");
        builder.append("; inspectType=").append(inspectType);
        if (resultImage) {
            builder.append("; variant=annotated");
        } else {
            builder.append("; variant=source");
        }
        return builder.toString();
    }

    private String extractFileName(String path) {
        if (!StringUtils.hasText(path)) {
            return "unknown";
        }
        String normalized = path.replace("\\", "/");
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }

    private InspectionData createInspectionDataRecord(QcRecord qcRecord,
                                                      Long cameraId,
                                                      String fileType,
                                                      String relativePath,
                                                      String fileName,
                                                      LocalDateTime captureTime,
                                                      String resultSummary,
                                                      String remark) {
        InspectionDataUpsertRequest request = new InspectionDataUpsertRequest();
        request.setQcRecordId(qcRecord.getInspectionId());
        request.setCameraId(cameraId);
        request.setFileType(fileType);
        request.setFilePath(relativePath);
        request.setFileName(fileName);
        request.setCaptureTime(captureTime);
        request.setResultSummary(resultSummary);
        request.setRemark(remark);
        return inspectionDataService.create(request);
    }

    private String inspectTypeToFileType(String inspectType) {
        return "video".equalsIgnoreCase(inspectType) ? "frame" : "image";
    }

    private String mergeRemark(String sessionRemark, String snapshotRemark) {
        if (StringUtils.hasText(sessionRemark) && StringUtils.hasText(snapshotRemark)) {
            return sessionRemark.trim() + "; snapshot=" + snapshotRemark.trim();
        }
        if (StringUtils.hasText(snapshotRemark)) {
            return snapshotRemark.trim();
        }
        return sessionRemark;
    }
}
