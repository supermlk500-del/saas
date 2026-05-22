package com.zhihuitong.modules.exception.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhihuitong.modules.exception.entity.ExceptionRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExceptionRecordMapper extends BaseMapper<ExceptionRecord> {
}
