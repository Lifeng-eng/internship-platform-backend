package com.example.internship;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 大学生实训实习岗位投递与校企对接系统
 */
@SpringBootApplication
@MapperScan("com.example.internship.mapper")
public class InternshipApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternshipApplication.class, args);
    }
}
