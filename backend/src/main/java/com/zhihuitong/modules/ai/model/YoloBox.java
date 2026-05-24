package com.zhihuitong.modules.ai.model;

import lombok.Data;

@Data
public class YoloBox {

    private String label;

    private double score;

    private int x1;

    private int y1;

    private int x2;

    private int y2;
}
