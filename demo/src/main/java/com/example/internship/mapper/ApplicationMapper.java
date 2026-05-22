package com.example.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.internship.entity.Application;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {

    /**
     * 统计各状态的投递数量（管理员维度）
     */
    @Select("SELECT status, COUNT(*) AS count FROM application GROUP BY status")
    List<Map<String, Object>> countByStatus();

    /**
     * 按日期统计投递量（近30天）
     */
    @Select("SELECT DATE(apply_time) AS date, COUNT(*) AS count FROM application WHERE apply_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) GROUP BY DATE(apply_time) ORDER BY date")
    List<Map<String, Object>> countByApplyDate();

    /**
     * 按岗位统计投递量
     */
    @Select("SELECT j.title, COUNT(a.id) AS count FROM application a JOIN job j ON a.job_id = j.id GROUP BY a.job_id, j.title ORDER BY count DESC")
    List<Map<String, Object>> countByJob();

    /**
     * 企业统计各状态投递数量
     */
    @Select("SELECT a.status, COUNT(*) AS count FROM application a JOIN job j ON a.job_id = j.id WHERE j.company_id = #{companyId} GROUP BY a.status")
    List<Map<String, Object>> countByStatusForCompany(@Param("companyId") Long companyId);

    /**
     * 企业统计按日期投递量（近7天）
     */
    @Select("SELECT DATE(a.apply_time) AS date, COUNT(*) AS count FROM application a JOIN job j ON a.job_id = j.id WHERE j.company_id = #{companyId} AND a.apply_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY DATE(a.apply_time) ORDER BY date")
    List<Map<String, Object>> countByDateForCompany(@Param("companyId") Long companyId);
}
