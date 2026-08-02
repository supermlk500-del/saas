package com.zhihuitong.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrowserInferenceClassItem {

    private int index;

    private String code;

    private String name;

    private String color;
}
