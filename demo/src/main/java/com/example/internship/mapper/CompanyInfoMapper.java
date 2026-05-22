package com.example.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.internship.entity.CompanyInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CompanyInfoMapper extends BaseMapper<CompanyInfo> {
}
