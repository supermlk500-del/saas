package com.zhihuitong.modules.quality.service;

import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.quality.entity.QcCamera;
import com.zhihuitong.modules.quality.mapper.QcCameraMapper;
import org.springframework.stereotype.Service;

@Service
public class QcCameraService {

    private final QcCameraMapper qcCameraMapper;

    public QcCameraService(QcCameraMapper qcCameraMapper) {
        this.qcCameraMapper = qcCameraMapper;
    }

    public QcCamera requireCamera(Long cameraId) {
        QcCamera camera = qcCameraMapper.selectById(cameraId);
        if (camera == null) {
            throw new BusinessException(404, "QC camera not found");
        }
        return camera;
    }
}
