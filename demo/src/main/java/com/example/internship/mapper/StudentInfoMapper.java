package com.example.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.internship.entity.StudentInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentInfoMapper extends BaseMapper<StudentInfo> {
}
