package com.zhihuitong.modules.quality.dto;

import lombok.Data;

@Data
public class InspectionDetectionBox {

    private String label;

    private Double score;

    private Integer x1;

    private Integer y1;

    private Integer x2;

    private Integer y2;
}
