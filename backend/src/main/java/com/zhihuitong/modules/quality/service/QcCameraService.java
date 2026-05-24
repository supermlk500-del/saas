package com.zhihuitong.modules.quality.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.quality.dto.QcCameraQuery;
import com.zhihuitong.modules.quality.entity.QcCamera;
import com.zhihuitong.modules.quality.mapper.QcCameraMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class QcCameraService {

    private static final String LOCAL_CAMERA_CODE = "LOCAL_CAM_001";

    private final QcCameraMapper qcCameraMapper;

    public QcCameraService(QcCameraMapper qcCameraMapper) {
        this.qcCameraMapper = qcCameraMapper;
    }

    public TableDataInfo<QcCamera> list(QcCameraQuery query) {
        ensureDefaultLocalCamera();
        Page<QcCamera> page = qcCameraMapper.selectPage(query.toPage(), Wrappers.<QcCamera>lambdaQuery()
                .like(StringUtils.hasText(query.getCameraCode()), QcCamera::getCameraCode, query.getCameraCode())
                .like(StringUtils.hasText(query.getCameraName()), QcCamera::getCameraName, query.getCameraName())
                .eq(StringUtils.hasText(query.getCameraType()), QcCamera::getCameraType, query.getCameraType())
                .eq(query.getStatus() != null, QcCamera::getStatus, query.getStatus())
                .orderByAsc(QcCamera::getCameraCode));
        return TableDataInfoBuilder.build(page);
    }

    public QcCamera requireCamera(Long cameraId) {
        ensureDefaultLocalCamera();
        QcCamera camera = qcCameraMapper.selectById(cameraId);
        if (camera == null) {
            throw new BusinessException(404, "QC camera not found");
        }
        return camera;
    }

    @Transactional
    public QcCamera ensureDefaultLocalCamera() {
        QcCamera existing = qcCameraMapper.selectOne(Wrappers.<QcCamera>lambdaQuery()
                .eq(QcCamera::getCameraCode, LOCAL_CAMERA_CODE)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        QcCamera camera = new QcCamera();
        camera.setCameraCode(LOCAL_CAMERA_CODE);
        camera.setCameraName("电脑摄像头");
        camera.setCameraType("local_webcam");
        camera.setIpAddress("127.0.0.1");
        camera.setLocation("质检工作站本机");
        camera.setStatus(1);
        camera.setRemark("系统自动初始化的本机摄像头资源");
        qcCameraMapper.insert(camera);
        return camera;
    }
}
