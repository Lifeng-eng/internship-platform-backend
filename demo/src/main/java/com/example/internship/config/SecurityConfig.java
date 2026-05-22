package com.example.internship.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults()) // 启用 CORS，使用 WebConfig 中的配置
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 认证接口 - 公开
                .requestMatchers("/api/auth/**").permitAll()
                // 岗位浏览 - 公开
                .requestMatchers(HttpMethod.GET, "/api/jobs", "/api/jobs/*").permitAll()
                // 学生接口
                .requestMatchers("/api/applications/**").hasAnyRole("STUDENT", "COMPANY", "ADMIN")
                // 简历上传、删除、信息查询 - 仅学生
                .requestMatchers(HttpMethod.POST, "/api/profile/resume").hasRole("STUDENT")
                .requestMatchers(HttpMethod.DELETE, "/api/profile/resume").hasRole("STUDENT")
                .requestMatchers("/api/profile/resume/info").hasRole("STUDENT")
                // 简历下载 - 所有已登录用户均可下载
                .requestMatchers(HttpMethod.GET, "/api/profile/resume").authenticated()
                // 个人中心 - 需认证
                .requestMatchers("/api/profile/**").authenticated()
                .requestMatchers("/api/notifications/**").authenticated()
                // 企业接口
                .requestMatchers(HttpMethod.POST, "/api/jobs").hasRole("COMPANY")
                .requestMatchers(HttpMethod.PUT, "/api/jobs/*").hasRole("COMPANY")
                .requestMatchers(HttpMethod.DELETE, "/api/jobs/*").hasRole("COMPANY")
                .requestMatchers("/api/jobs/*/close").hasRole("COMPANY")
                .requestMatchers("/api/company/**").hasRole("COMPANY")
                // 管理员接口
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 其他需要认证
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
