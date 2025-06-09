package com.example.chatapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 시크릿 보안 검증 컴포넌트
 * 애플리케이션 시작 시 JWT 시크릿의 보안 강도를 검증합니다.
 */
@Component
@Slf4j
public class JwtSecurityValidator {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // 금지된 약한 시크릿 패턴들
    private static final List<String> FORBIDDEN_SECRETS = Arrays.asList(
            "dev-jwt-secret-key-for-local-development-minimum-32-chars",
            "DEVELOPMENT_ONLY_SECRET_DO_NOT_USE_IN_PRODUCTION_abcdef123456789",
            "TEST_ONLY_JWT_SECRET_FOR_UNIT_TESTS_32_CHARS_MIN",
            "test-secret",
            "secret",
            "jwt-secret",
            "password",
            "123456",
            "default"
    );

    private static final int MINIMUM_SECRET_LENGTH = 32;

    @PostConstruct
    public void validateJwtSecret() {
        log.info("JWT 시크릿 보안 검증을 시작합니다...");

        // 1. null 또는 빈 문자열 검증
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "JWT_SECRET 환경변수가 설정되지 않았습니다. " +
                    "보안을 위해 강력한 시크릿을 설정해주세요."
            );
        }

        // 2. 최소 길이 검증
        if (jwtSecret.length() < MINIMUM_SECRET_LENGTH) {
            throw new IllegalStateException(
                    String.format("JWT 시크릿이 너무 짧습니다. 최소 %d자 이상이어야 합니다. (현재: %d자)",
                            MINIMUM_SECRET_LENGTH, jwtSecret.length())
            );
        }

        // 3. 운영환경에서 금지된 약한 시크릿 검증
        String activeProfile = System.getProperty("spring.profiles.active", "");
        if ("prod".equals(activeProfile)) {
            for (String forbiddenSecret : FORBIDDEN_SECRETS) {
                if (jwtSecret.equals(forbiddenSecret)) {
                    throw new IllegalStateException(
                            "운영환경에서 개발/테스트용 시크릿을 사용할 수 없습니다. " +
                            "반드시 강력한 임의의 시크릿을 JWT_SECRET 환경변수로 설정해주세요."
                    );
                }
            }
        } else {
            // 개발/테스트 환경에서는 경고만 출력
            for (String forbiddenSecret : FORBIDDEN_SECRETS) {
                if (jwtSecret.equals(forbiddenSecret)) {
                    log.warn("개발/테스트용 기본 시크릿을 사용 중입니다. 운영환경에서는 반드시 강력한 시크릿으로 변경하세요.");
                    break;
                }
            }
        }

        // 4. 복잡성 검증 (대소문자, 숫자, 특수문자 포함 권장)
        if (!isComplexSecret(jwtSecret)) {
            log.warn("JWT 시크릿의 복잡성이 부족합니다. 보안 강화를 위해 대소문자, 숫자, 특수문자를 포함하는 것을 권장합니다.");
        }

        log.info("JWT 시크릿 보안 검증이 완료되었습니다. (길이: {}자)", jwtSecret.length());
    }

    /**
     * 시크릿의 복잡성을 검증합니다.
     * 대소문자, 숫자, 특수문자 중 최소 3가지 이상 포함 여부를 확인합니다.
     */
    private boolean isComplexSecret(String secret) {
        boolean hasLowercase = secret.matches(".*[a-z].*");
        boolean hasUppercase = secret.matches(".*[A-Z].*");
        boolean hasDigit = secret.matches(".*\\d.*");
        boolean hasSpecialChar = secret.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        int complexityCount = 0;
        if (hasLowercase) complexityCount++;
        if (hasUppercase) complexityCount++;
        if (hasDigit) complexityCount++;
        if (hasSpecialChar) complexityCount++;

        return complexityCount >= 3;
    }
}