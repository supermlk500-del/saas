package com.zhihuitong.common.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class TableDataInfo<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;
    private List<T> rows;
    private long total;

    public static <T> TableDataInfo<T> empty() {
        TableDataInfo<T> result = new TableDataInfo<>();
        result.setCode(200);
        result.setMsg("success");
        result.setRows(Collections.emptyList());
        result.setTotal(0L);
        return result;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}

