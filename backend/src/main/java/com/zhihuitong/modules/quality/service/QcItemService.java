package com.zhihuitong.modules.quality.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.quality.dto.QcItemQuery;
import com.zhihuitong.modules.quality.dto.QcItemStatusPatchRequest;
import com.zhihuitong.modules.quality.dto.QcItemUpsertRequest;
import com.zhihuitong.modules.quality.entity.QcItem;
import com.zhihuitong.modules.quality.mapper.QcItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class QcItemService {

    private final QcItemMapper qcItemMapper;

    public QcItemService(QcItemMapper qcItemMapper) {
        this.qcItemMapper = qcItemMapper;
    }

    public TableDataInfo<QcItem> list(QcItemQuery query) {
        Page<QcItem> page = qcItemMapper.selectPage(query.toPage(), Wrappers.<QcItem>lambdaQuery()
                .like(StringUtils.hasText(query.getQcItemCode()), QcItem::getQcItemCode, query.getQcItemCode())
                .like(StringUtils.hasText(query.getQcItemName()), QcItem::getQcItemName, query.getQcItemName())
                .eq(StringUtils.hasText(query.getQcType()), QcItem::getQcType, query.getQcType())
                .eq(query.getIsActive() != null, QcItem::getIsActive, query.getIsActive())
                .orderByDesc(QcItem::getQcItemId));
        return TableDataInfoBuilder.build(page);
    }

    public QcItem getDetail(Long qcItemId) {
        return requireQcItem(qcItemId);
    }

    @Transactional
    public QcItem create(QcItemUpsertRequest request) {
        validateRange(request.getStandardMin(), request.getStandardMax());
        checkCodeUnique(request.getQcItemCode(), null);
        QcItem entity = new QcItem();
        copyRequest(request, entity);
        qcItemMapper.insert(entity);
        return entity;
    }

    @Transactional
    public QcItem update(Long qcItemId, QcItemUpsertRequest request) {
        validateRange(request.getStandardMin(), request.getStandardMax());
        checkCodeUnique(request.getQcItemCode(), qcItemId);
        QcItem entity = requireQcItem(qcItemId);
        copyRequest(request, entity);
        qcItemMapper.updateById(entity);
        return entity;
    }

    @Transactional
    public QcItem patchStatus(Long qcItemId, QcItemStatusPatchRequest request) {
        QcItem entity = requireQcItem(qcItemId);
        entity.setIsActive(request.getIsActive());
        qcItemMapper.updateById(entity);
        return entity;
    }

    public QcItem requireQcItem(Long qcItemId) {
        QcItem entity = qcItemMapper.selectById(qcItemId);
        if (entity == null) {
            throw new BusinessException(404, "QC item not found");
        }
        return entity;
    }

    private void copyRequest(QcItemUpsertRequest request, QcItem entity) {
        entity.setQcItemCode(request.getQcItemCode().trim());
        entity.setQcItemName(request.getQcItemName().trim());
        entity.setQcType(request.getQcType());
        entity.setUnit(request.getUnit());
        entity.setStandardMin(request.getStandardMin());
        entity.setStandardMax(request.getStandardMax());
        entity.setIsActive(request.getIsActive());
        entity.setDescription(request.getDescription());
    }

    private void validateRange(java.math.BigDecimal min, java.math.BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessException(422, "standardMin must be less than or equal to standardMax");
        }
    }

    private void checkCodeUnique(String code, Long excludeId) {
        Long count = qcItemMapper.selectCount(Wrappers.<QcItem>lambdaQuery()
                .eq(QcItem::getQcItemCode, code)
                .ne(excludeId != null, QcItem::getQcItemId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(409, "QC item code already exists");
        }
    }
}
