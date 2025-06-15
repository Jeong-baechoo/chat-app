package com.example.chatapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 설정 클래스
 * SpringDoc OpenAPI를 사용하여 API 문서 자동 생성
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // JWT 보안 스키마 정의
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("JWT_TOKEN")
                .description("JWT 인증 토큰 (쿠키로 전송)");

        // 보안 요구사항
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("JWT 쿠키 인증");

        return new OpenAPI()
                .info(getApiInfo())
                .servers(getServers())
                .components(new Components().addSecuritySchemes("JWT 쿠키 인증", securityScheme))
                .addSecurityItem(securityRequirement);
    }

    private Info getApiInfo() {
        return new Info()
                .title("채팅 애플리케이션 API")
                .description("실시간 채팅 애플리케이션의 REST API 및 WebSocket 명세")
                .version("1.0.0")
                .contact(new Contact()
                        .name("API 지원")
                        .email("support@example.com"));
    }

    private List<Server> getServers() {
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("로컬 개발 서버");

        Server testServer = new Server()
                .url("http://localhost:8080")
                .description("테스트 서버");

        return List.of(localServer, testServer);
    }
}