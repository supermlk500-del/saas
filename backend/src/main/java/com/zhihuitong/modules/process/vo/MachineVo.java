package com.zhihuitong.modules.process.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MachineVo {

    private Long machineId;

    private String machineCode;

    private String machineName;

    private String machineType;

    private String description;

    private String status;

    private LocalDateTime createTime;
}
