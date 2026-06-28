package com.zhihuitong.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_post")
public class SysPost {
    @TableId(value = "postId", type = IdType.ASSIGN_ID)
    private Long postId;
    private String postCode;
    private String postName;
    private Integer postSort;
    private String status;
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
    private Long createdBy;
    private LocalDateTime createTime;
    private Long updatedBy;
    private LocalDateTime updateTime;
    private String remark;
}
