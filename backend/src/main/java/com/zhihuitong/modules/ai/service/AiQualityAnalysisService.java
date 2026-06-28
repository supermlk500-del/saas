package com.zhihuitong.modules.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhihuitong.modules.ai.vo.AiQualityAnalysisVo;
import com.zhihuitong.modules.plan.entity.ProcessParameter;
import com.zhihuitong.modules.plan.mapper.ProcessParameterMapper;
import com.zhihuitong.modules.quality.entity.QcRecord;
import com.zhihuitong.modules.quality.mapper.QcRecordMapper;
import com.zhihuitong.modules.quality.service.QcRecordService;
import com.zhihuitong.security.service.DataScopeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AiQualityAnalysisService {

    private final QcRecordService qcRecordService;
    private final QcRecordMapper qcRecordMapper;
    private final ProcessParameterMapper processParameterMapper;
    private final DeepSeekExplanationService explanationService;
    private final DataScopeService dataScopeService;

    public AiQualityAnalysisService(QcRecordService qcRecordService,
                                    QcRecordMapper qcRecordMapper,
                                    ProcessParameterMapper processParameterMapper,
                                    DeepSeekExplanationService explanationService,
                                    DataScopeService dataScopeService) {
        this.qcRecordService = qcRecordService;
        this.qcRecordMapper = qcRecordMapper;
        this.processParameterMapper = processParameterMapper;
        this.explanationService = explanationService;
        this.dataScopeService = dataScopeService;
    }

    public AiQualityAnalysisVo analyze(Long inspectionId) {
        QcRecord record = qcRecordService.requireRecord(inspectionId);
        dataScopeService.assertAccessible(record.getDeptId(), record.getCreatedBy());
        List<QcRecord> recentRecords = qcRecordMapper.selectList(dataScopeService.apply(Wrappers.<QcRecord>lambdaQuery(),
                        QcRecord::getDeptId, QcRecord::getCreatedBy)
                .eq(QcRecord::getPlanStepId, record.getPlanStepId())
                .orderByDesc(QcRecord::getInspectTime)
                .last("LIMIT 50"));
        List<ProcessParameter> parameters = processParameterMapper.selectList(Wrappers.<ProcessParameter>lambdaQuery()
                .eq(ProcessParameter::getPlanStepId, record.getPlanStepId())
                .orderByDesc(ProcessParameter::getRecordTime)
                .last("LIMIT 20"));

        AiQualityAnalysisVo result = new AiQualityAnalysisVo();
        result.setInspectionId(inspectionId);
        result.setResultJudge(record.getResultJudge());
        result.setDefectType(resolveDefectType(record));
        result.setSeverity(resolveSeverity(record));
        result.setRecentSampleCount(recentRecords.size());
        result.setRecentFailureCount((int) recentRecords.stream().filter(item -> "FAIL".equals(item.getResultJudge())).count());
        result.setRecentRecheckCount((int) recentRecords.stream().filter(item -> "RECHECK".equals(item.getResultJudge())).count());
        int abnormal = result.getRecentFailureCount() + result.getRecentRecheckCount();
        result.setRecentDefectRate(recentRecords.isEmpty() ? 0
                : BigDecimal.valueOf(abnormal * 100.0 / recentRecords.size())
                .setScale(1, RoundingMode.HALF_UP).doubleValue());
        result.setRiskLevel(resolveRiskLevel(result));
        result.setPossibleCauses(resolveCauses(result.getDefectType(), parameters));
        result.setRecommendedActions(resolveActions(result));

        String fallback = buildFallbackReport(result);
        DeepSeekExplanationService.ExplanationResult explanation = explanationService.explain(
                "你是纺织质量分析助手。根据给定的 ONNX 检测结果、近期统计和工艺参数生成中文质检报告。"
                        + "不得把推测写成确定事实；必须提醒人工复核，并分别写风险、可能原因、建议动作。",
                result,
                fallback
        );
        result.setReport(explanation.content());
        result.setModel(explanation.model());
        result.setAiGenerated(explanation.aiGenerated());
        result.setFallbackReason(explanation.fallbackReason());
        return result;
    }

    private String resolveDefectType(QcRecord record) {
        if (!StringUtils.hasText(record.getResultValue()) || "PASS".equalsIgnoreCase(record.getResultValue())) {
            return "none";
        }
        String first = record.getResultValue().split(",")[0];
        return first.contains(":") ? first.substring(0, first.indexOf(':')) : first;
    }

    private String resolveSeverity(QcRecord record) {
        BigDecimal score = record.getConfidenceScore();
        if ("PASS".equals(record.getResultJudge())) {
            return "NONE";
        }
        if ("FAIL".equals(record.getResultJudge()) && score != null && score.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
            return "HIGH";
        }
        if ("FAIL".equals(record.getResultJudge()) || (score != null && score.compareTo(BigDecimal.valueOf(0.4)) >= 0)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolveRiskLevel(AiQualityAnalysisVo result) {
        if ("HIGH".equals(result.getSeverity()) || result.getRecentDefectRate() >= 40) {
            return "HIGH";
        }
        if (!"PASS".equals(result.getResultJudge()) || result.getRecentDefectRate() >= 15) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private List<String> resolveCauses(String defectType, List<ProcessParameter> parameters) {
        List<String> causes = new ArrayList<>();
        String normalized = defectType == null ? "" : defectType.toLowerCase(Locale.ROOT);
        if (normalized.contains("stain")) {
            causes.add("来料油污、染液残留或设备清洁不足");
        }
        if (normalized.contains("knot") || normalized.contains("jump") || normalized.contains("weft")) {
            causes.add("张力、车速或织造稳定性出现波动");
        }
        if (normalized.contains("hair") || normalized.contains("size")) {
            causes.add("前处理、退浆或表面整理参数可能偏离工艺窗口");
        }
        boolean hasTemperature = parameters.stream().anyMatch(item -> item.getParamName() != null
                && item.getParamName().contains("温度"));
        boolean hasSpeed = parameters.stream().anyMatch(item -> item.getParamName() != null
                && item.getParamName().contains("车速"));
        if (hasTemperature) {
            causes.add("需复核缺陷时段的温度曲线是否存在异常波动");
        }
        if (hasSpeed) {
            causes.add("需比对车速变化与缺陷连续出现时段");
        }
        if (causes.isEmpty()) {
            causes.add("现有数据不足以锁定根因，需结合原图、机台和来料记录复核");
        }
        return causes.stream().distinct().toList();
    }

    private List<String> resolveActions(AiQualityAnalysisVo result) {
        List<String> actions = new ArrayList<>();
        actions.add("由质检员复核原图、检测框与缺陷类别");
        if (!"LOW".equals(result.getRiskLevel())) {
            actions.add("暂停该批次继续流转，抽取相邻布段扩大复检");
        }
        if (result.getRecentDefectRate() >= 15) {
            actions.add("检查同机台最近 50 条记录并核对工艺参数趋势");
        }
        actions.add("将误报或漏报样本加入模型持续学习数据集");
        return actions;
    }

    private String buildFallbackReport(AiQualityAnalysisVo result) {
        return "本次判定为 " + result.getResultJudge() + "，缺陷类型 " + result.getDefectType()
                + "，风险等级 " + result.getRiskLevel() + "。同工序最近 " + result.getRecentSampleCount()
                + " 条记录异常率为 " + result.getRecentDefectRate() + "%。可能原因包括："
                + String.join("；", result.getPossibleCauses()) + "。建议：" + String.join("；", result.getRecommendedActions()) + "。";
    }
}
