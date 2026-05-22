package com.zhihuitong.common.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class PageQuery {

    @Min(value = 1, message = "pageNum must be greater than or equal to 1")
    private long pageNum = 1;

    @Min(value = 1, message = "pageSize must be greater than or equal to 1")
    @Max(value = 200, message = "pageSize must be less than or equal to 200")
    private long pageSize = 10;

    public <T> Page<T> toPage() {
        return Page.of(pageNum, pageSize);
    }

    public long getPageNum() {
        return pageNum;
    }

    public void setPageNum(long pageNum) {
        this.pageNum = pageNum;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }
}
