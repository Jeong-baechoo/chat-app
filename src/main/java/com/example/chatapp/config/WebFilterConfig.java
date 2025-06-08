package com.example.chatapp.config;

import com.example.chatapp.infrastructure.filter.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 웹 필터 설정 클래스
 */
@Configuration
public class WebFilterConfig {

    /**
     * JWT 인증 필터 등록
     * 이미 @Component로 등록된 JwtAuthenticationFilter 빈을 사용하여 필터 등록
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);  // 이미 등록된 빈 주입
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1); // 필터 적용 순서 (낮은 숫자가 먼저 실행)
        return registrationBean;
    }
}
