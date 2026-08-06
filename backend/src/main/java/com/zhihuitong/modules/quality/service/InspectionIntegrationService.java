package com.zhihuitong.modules.quality.service;

import com.zhihuitong.modules.ai.dto.BrowserInferenceClassItem;
import com.zhihuitong.modules.ai.model.YoloBox;
import com.zhihuitong.modules.ai.model.YoloDetectResult;
import com.zhihuitong.modules.plan.service.PlanStepService;
import com.zhihuitong.modules.quality.dto.ClientDetectionBox;
import com.zhihuitong.modules.quality.dto.ClientImageDetectionRequest;
import com.zhihuitong.modules.quality.dto.ClientRealtimeEventRequest;
import com.zhihuitong.modules.quality.dto.InspectionDataUpsertRequest;
import com.zhihuitong.modules.quality.dto.QcRecordUpsertRequest;
import com.zhihuitong.modules.quality.entity.InspectionData;
import com.zhihuitong.modules.quality.entity.QcRecord;
import com.zhihuitong.modules.quality.model.QcStreamSessionContext;
import com.zhihuitong.modules.quality.vo.InspectionIntegrationResultVo;
import com.zhihuitong.modules.quality.vo.StoredInspectionFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InspectionIntegrationService {

    private final QcItemService qcItemService;
    private final QcRecordService qcRecordService;
    private final InspectionDataService inspectionDataService;
    private final QcCameraService qcCameraService;
    private final PlanStepService planStepService;
    private final InspectionFileStorageService inspectionFileStorageService;
    private final ClientInferenceResultValidator clientInferenceResultValidator;

    public InspectionIntegrationService(QcItemService qcItemService,
                                         QcRecordService qcRecordService,
                                         InspectionDataService inspectionDataService,
                                         QcCameraService qcCameraService,
                                         PlanStepService planStepService,
                                         InspectionFileStorageService inspectionFileStorageService,
                                         ClientInferenceResultValidator clientInferenceResultValidator) {
        this.qcItemService = qcItemService;
        this.qcRecordService = qcRecordService;
        this.inspectionDataService = inspectionDataService;
        this.qcCameraService = qcCameraService;
        this.planStepService = planStepService;
        this.inspectionFileStorageService = inspectionFileStorageService;
        this.clientInferenceResultValidator = clientInferenceResultValidator;
    }

    /**
     * Persists one browser-inferred realtime evidence event. The server validates
     * the client result and stores the evidence, but never runs model inference.
     */
    @Transactional
    public InspectionIntegrationResultVo persistClientStreamEvent(QcStreamSessionContext sessionContext,
                                                                   ClientRealtimeEventRequest eventRequest,
                                                                   MultipartFile sourceFile,
                                                                   MultipartFile resultFile) {
        ClientImageDetectionRequest validationRequest = new ClientImageDetectionRequest();
        validationRequest.setPlanStepId(sessionContext.getPlanStepId());
        validationRequest.setQcItemId(sessionContext.getQcItemId());
        validationRequest.setCameraId(sessionContext.getCameraId());
        validationRequest.setInspector(sessionContext.getInspector());
        validationRequest.setRemark(sessionContext.getRemark());
        validationRequest.setModelSha256(eventRequest.getModelSha256());
        validationRequest.setImageWidth(eventRequest.getImageWidth());
        validationRequest.setImageHeight(eventRequest.getImageHeight());
        validationRequest.setPreprocessTimeMs(eventRequest.getPreprocessTimeMs());
        validationRequest.setInferenceTimeMs(eventRequest.getInferenceTimeMs());
        validationRequest.setPostprocessTimeMs(eventRequest.getPostprocessTimeMs());
        validationRequest.setProviderStrategy(eventRequest.getProviderStrategy());
        validationRequest.setDetections(eventRequest.getDetections());

        planStepService.requirePlanStep(sessionContext.getPlanStepId());
        qcItemService.requireQcItem(sessionContext.getQcItemId());
        if (sessionContext.getCameraId() != null) {
            qcCameraService.requireCamera(sessionContext.getCameraId());
        }
        Map<Integer, BrowserInferenceClassItem> classMap = clientInferenceResultValidator.validateImageResult(
                validationRequest,
                sourceFile,
                resultFile
        );

        StoredInspectionFile storedSourceFile = inspectionFileStorageService.storeSourceImage(sourceFile);
        StoredInspectionFile storedResultFile = inspectionFileStorageService.storeResultImage(resultFile);
        YoloDetectResult algorithmResult = buildClientAlgorithmResult(validationRequest, classMap, "video");
        algorithmResult.setSourceImageUrl(storedSourceFile.getRelativePath());
        algorithmResult.setImageUrl(storedResultFile.getRelativePath());

        LocalDateTime frameTime = eventRequest.getFrameTime() != null
                ? eventRequest.getFrameTime()
                : LocalDateTime.now();
        QcRecord qcRecord = qcRecordService.create(buildQcRecordRequest(
                sessionContext.getPlanStepId(),
                sessionContext.getQcItemId(),
                sessionContext.getCameraId(),
                "video",
                algorithmResult,
                StringUtils.hasText(sessionContext.getInspector()) ? sessionContext.getInspector() : "browser-onnx",
                mergeRemark(sessionContext.getRemark(), "client-event=" + eventRequest.getEventId()),
                frameTime
        ));
        List<InspectionData> inspectionDataList = persistInspectionData(
                qcRecord,
                sessionContext.getCameraId(),
                algorithmResult,
                storedSourceFile,
                "video",
                frameTime
        );
        return buildResult(qcRecord, inspectionDataList, algorithmResult);
    }

    @Transactional
    public InspectionIntegrationResultVo persistClientImageDetection(ClientImageDetectionRequest request,
                                                                      MultipartFile sourceFile,
                                                                      MultipartFile resultFile) {
        planStepService.requirePlanStep(request.getPlanStepId());
        if (request.getCameraId() != null) {
            qcCameraService.requireCamera(request.getCameraId());
        }
        qcItemService.requireQcItem(request.getQcItemId());
        Map<Integer, BrowserInferenceClassItem> classMap = clientInferenceResultValidator.validateImageResult(
                request,
                sourceFile,
                resultFile
        );

        StoredInspectionFile storedSourceFile = inspectionFileStorageService.storeSourceImage(sourceFile);
        StoredInspectionFile storedResultFile = inspectionFileStorageService.storeResultImage(resultFile);
        String inspectType = "offline";
        YoloDetectResult algorithmResult = buildClientAlgorithmResult(request, classMap, inspectType);
        algorithmResult.setSourceImageUrl(storedSourceFile.getRelativePath());
        algorithmResult.setImageUrl(storedResultFile.getRelativePath());

        QcRecord qcRecord = qcRecordService.create(buildQcRecordRequest(
                request.getPlanStepId(),
                request.getQcItemId(),
                request.getCameraId(),
                inspectType,
                algorithmResult,
                StringUtils.hasText(request.getInspector()) ? request.getInspector() : "browser-onnx",
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
        request.setInspector(StringUtils.hasText(inspector) ? inspector : "browser-onnx");
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
        LocalDateTime effectiveCaptureTime = captureTime != null ? captureTime : qcRecord.getInspectTime();
        records.add(createInspectionDataRecord(
                qcRecord,
                cameraId,
                inspectTypeToFileType(inspectType),
                storedSourceFile.getRelativePath(),
                storedSourceFile.getFileName(),
                effectiveCaptureTime,
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
                    effectiveCaptureTime,
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
        StringBuilder builder = new StringBuilder("Triggered via browser ONNX result");
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
        return StringUtils.hasText(algorithmResult.getImageUrl())
                ? algorithmResult.getImageUrl()
                : algorithmResult.getSourceImageUrl();
    }

    private String buildResultSummary(YoloDetectResult algorithmResult) {
        String resultJudge = resolveResultJudge(algorithmResult);
        int boxCount = algorithmResult.getBoxes() == null ? 0 : algorithmResult.getBoxes().size();
        String defectType = StringUtils.hasText(algorithmResult.getDefectType()) ? algorithmResult.getDefectType() : "none";
        return "judge=" + resultJudge + ", defectType=" + defectType + ", boxCount=" + boxCount;
    }

    private String buildInspectionRemark(String inspectType, boolean resultImage) {
        return "Stored by browser ONNX result; inspectType=" + inspectType
                + "; variant=" + (resultImage ? "annotated" : "source");
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

    private String mergeRemark(String sessionRemark, String eventRemark) {
        if (StringUtils.hasText(sessionRemark) && StringUtils.hasText(eventRemark)) {
            return sessionRemark.trim() + "; " + eventRemark.trim();
        }
        if (StringUtils.hasText(eventRemark)) {
            return eventRemark.trim();
        }
        return sessionRemark;
    }

    private YoloDetectResult buildClientAlgorithmResult(ClientImageDetectionRequest request,
                                                        Map<Integer, BrowserInferenceClassItem> classMap,
                                                        String inspectType) {
        List<YoloBox> boxes = new ArrayList<>();
        for (ClientDetectionBox item : request.getDetections()) {
            BrowserInferenceClassItem classItem = classMap.get(item.getClassIndex());
            YoloBox box = new YoloBox();
            box.setLabel(classItem.getName());
            box.setScore(item.getScore());
            box.setX1((int) Math.round(item.getX1()));
            box.setY1((int) Math.round(item.getY1()));
            box.setX2((int) Math.round(item.getX2()));
            box.setY2((int) Math.round(item.getY2()));
            boxes.add(box);
        }
        boxes.sort((left, right) -> Double.compare(right.getScore(), left.getScore()));

        YoloDetectResult result = new YoloDetectResult();
        result.setInspectType(inspectType);
        result.setBoxes(boxes);
        result.setResultJudge(boxes.isEmpty() ? "PASS" : "FAIL");
        result.setConfidenceScore(boxes.isEmpty() ? null : BigDecimal.valueOf(boxes.get(0).getScore()));
        result.setDefectType(boxes.isEmpty() ? null : boxes.get(0).getLabel());
        result.setResultValue(boxes.isEmpty()
                ? "PASS"
                : boxes.stream()
                .limit(3)
                .map(item -> item.getLabel() + ":" + String.format(Locale.US, "%.2f", item.getScore()))
                .collect(Collectors.joining(",")));
        return result;
    }
}
