package com.zhihuitong.common.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhihuitong.common.domain.TableDataInfo;

import java.util.List;

public final class TableDataInfoBuilder {

    private TableDataInfoBuilder() {
    }

    public static <T> TableDataInfo<T> build(List<T> rows, long total) {
        TableDataInfo<T> result = new TableDataInfo<>();
        result.setCode(200);
        result.setMsg("success");
        result.setRows(rows);
        result.setTotal(total);
        return result;
    }

    public static <T> TableDataInfo<T> build(IPage<T> page) {
        return build(page.getRecords(), page.getTotal());
    }
}
