package com.zhihuitong.modules.quality.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("qcitem")
public class QcItem {

    @TableId(value = "qcItemId", type = IdType.ASSIGN_ID)
    private Long qcItemId;

    private String qcItemCode;

    private String qcItemName;

    private String qcType;

    private String unit;

    private BigDecimal standardMin;

    private BigDecimal standardMax;

    private Integer isActive;

    private String description;
}
