package com.example.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.internship.entity.Job;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface JobMapper extends BaseMapper<Job> {

    /**
     * 统计岗位各状态数量
     */
    @Select("SELECT status, COUNT(*) AS count FROM job GROUP BY status")
    List<Map<String, Object>> countByStatus();

    /**
     * 按日期统计岗位发布数量（近30天）
     */
    @Select("SELECT DATE(publish_time) AS date, COUNT(*) AS count FROM job WHERE publish_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) GROUP BY DATE(publish_time) ORDER BY date")
    List<Map<String, Object>> countByPublishDate();

    /**
     * 智能推荐：匹配学生专业、期望岗位、期望地区
     */
    @Select("<script>" +
            "SELECT j.* FROM job j WHERE j.status = 'published' " +
            "<if test='preferredJobs != null and preferredJobs != \"\"'>" +
            "AND (j.title LIKE CONCAT('%', #{major}, '%') OR j.requirements LIKE CONCAT('%', #{major}, '%') " +
            "OR j.title REGEXP REPLACE(#{preferredJobs}, ',', '|') OR j.description REGEXP REPLACE(#{preferredJobs}, ',', '|')) " +
            "</if>" +
            "<if test='preferredLocations != null and preferredLocations != \"\"'>" +
            "AND FIND_IN_SET(j.location, REPLACE(#{preferredLocations}, ',', ',')) " +
            "</if>" +
            "ORDER BY j.publish_time DESC LIMIT 10" +
            "</script>")
    List<Job> findRecommended(@Param("major") String major,
                              @Param("preferredJobs") String preferredJobs,
                              @Param("preferredLocations") String preferredLocations);
}
