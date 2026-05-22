package com.example.internship.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 岗位表
 */
@Data
@TableName("job")
public class Job {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Long companyId;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String location;
    private String description;
    private String requirements;
    private String status; // pending / published / rejected / closed
    private LocalDateTime publishTime;
    private LocalDateTime reviewTime;
    private Long reviewerId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
