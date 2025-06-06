-- MySQL 초기화 스크립트
-- 데이터베이스와 사용자가 이미 환경변수로 생성되므로, 추가 설정만 진행

-- 한국어 지원을 위한 문자셋 설정
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 타임존 설정
SET time_zone = '+09:00';

-- 데이터베이스 사용
USE chatapp;

-- 권한 설정 확인 (필요시)
-- 이미 환경변수로 사용자가 생성되었으므로, 추가 권한 부여
GRANT ALL PRIVILEGES ON chatapp.* TO 'chatapp'@'%';
FLUSH PRIVILEGES;

-- 초기 데이터 (선택사항)
-- 운영 환경에서는 애플리케이션이 자동으로 테이블을 생성합니다.