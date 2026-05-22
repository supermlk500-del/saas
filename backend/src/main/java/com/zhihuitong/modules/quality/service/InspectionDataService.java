package com.zhihuitong.modules.quality.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.quality.dto.InspectionDataQuery;
import com.zhihuitong.modules.quality.dto.InspectionDataUpsertRequest;
import com.zhihuitong.modules.quality.entity.InspectionData;
import com.zhihuitong.modules.quality.mapper.InspectionDataMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InspectionDataService {

    private final InspectionDataMapper inspectionDataMapper;
    private final QcRecordService qcRecordService;
    private final QcCameraService qcCameraService;

    public InspectionDataService(InspectionDataMapper inspectionDataMapper,
                                 QcRecordService qcRecordService,
                                 QcCameraService qcCameraService) {
        this.inspectionDataMapper = inspectionDataMapper;
        this.qcRecordService = qcRecordService;
        this.qcCameraService = qcCameraService;
    }

    public TableDataInfo<InspectionData> list(InspectionDataQuery query) {
        Page<InspectionData> page = inspectionDataMapper.selectPage(query.toPage(), Wrappers.<InspectionData>lambdaQuery()
                .eq(query.getQcRecordId() != null, InspectionData::getQcRecordId, query.getQcRecordId())
                .eq(query.getCameraId() != null, InspectionData::getCameraId, query.getCameraId())
                .eq(StringUtils.hasText(query.getFileType()), InspectionData::getFileType, query.getFileType())
                .orderByDesc(InspectionData::getCaptureTime));
        return TableDataInfoBuilder.build(page);
    }

    @Transactional
    public InspectionData create(InspectionDataUpsertRequest request) {
        qcRecordService.requireRecord(request.getQcRecordId());
        if (request.getCameraId() != null) {
            qcCameraService.requireCamera(request.getCameraId());
        }
        InspectionData entity = new InspectionData();
        copyRequest(request, entity);
        inspectionDataMapper.insert(entity);
        return entity;
    }

    @Transactional
    public void delete(Long dataId) {
        requireData(dataId);
        inspectionDataMapper.deleteById(dataId);
    }

    public InspectionData requireData(Long dataId) {
        InspectionData entity = inspectionDataMapper.selectById(dataId);
        if (entity == null) {
            throw new BusinessException(404, "Inspection data not found");
        }
        return entity;
    }

    private void copyRequest(InspectionDataUpsertRequest request, InspectionData entity) {
        entity.setQcRecordId(request.getQcRecordId());
        entity.setCameraId(request.getCameraId());
        entity.setFileType(request.getFileType().trim());
        entity.setFilePath(request.getFilePath().trim());
        entity.setFileName(request.getFileName().trim());
        entity.setCaptureTime(request.getCaptureTime());
        entity.setResultSummary(request.getResultSummary());
        entity.setRemark(request.getRemark());
    }
}
