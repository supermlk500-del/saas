package com.zhihuitong.modules.quality.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.EqualsAndHashCode;
import lombok.Data;

@Data
@EqualsAndHashCode(callSuper = true)
public class QcItemQuery extends PageQuery {

    private String qcItemCode;

    private String qcItemName;

    private String qcType;

    private Integer isActive;
}
