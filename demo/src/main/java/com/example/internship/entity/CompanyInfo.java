package com.example.internship.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 企业信息表
 */
@Data
@TableName("company_info")
public class CompanyInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String companyName;
    private String contactPerson;
    private String companyIntro;
    private String companyScale;
}
