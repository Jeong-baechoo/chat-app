# Chat Application Documentation

## 📚 주요 문서

### 핵심 문서 (Core Documentation)
- **[README.md](../readme.md)** - 프로젝트 개요 및 시작 가이드
- **[CLAUDE.md](../CLAUDE.md)** - Claude Code 개발 가이드 (AI 개발 환경 설정)
- **[API_DOCUMENTATION.md](../API_DOCUMENTATION.md)** - REST API 및 WebSocket 명세
- **[FRONTEND_DEVELOPER_GUIDE.md](../FRONTEND_DEVELOPER_GUIDE.md)** - 프론트엔드 개발자 가이드

### 기술 문서 (Technical Documentation)
- **[docs/tasks.md](./tasks.md)** - 개발 작업 목록
- **[docs/websocket-stomp-setup.md](./websocket-stomp-setup.md)** - WebSocket STOMP 설정 가이드
- **[k6/README.md](../k6/README.md)** - 성능 테스트 가이드
- **[frontend-example/README.md](../frontend-example/README.md)** - 프론트엔드 통합 예제 코드

### 포트폴리오 문서 (Portfolio Documentation)
- **[portfolio-docs/](../portfolio-docs/)** - 프로젝트 개선 사항 및 기술적 결정 사항
  - 환경 설정 분리, DTO 일관성, 필터 테스트, DB 최적화, DDD 적용 등

## 🗑 삭제된 문서
다음 문서들은 중복되거나 더 이상 필요하지 않아 삭제되었습니다:
- HELP.md - Spring Boot 기본 도움말 (불필요)
- IMPROVEMENT_CHECKLIST.md - portfolio-docs로 이동
- PERFORMANCE_OPTIMIZATION.md - portfolio-docs/04로 통합
- SWAGGER_ANNOTATIONS_SUMMARY.md - API_DOCUMENTATION.md로 통합
- SWAGGER_GUIDE.md - FRONTEND_DEVELOPER_GUIDE.md로 통합
- TECH_STACK_ANALYSIS.md - CLAUDE.md로 통합
- USE_CASES.md - README.md로 통합

## 📁 문서 구조
```
chat-app/
├── README.md                      # 프로젝트 개요
├── CLAUDE.md                      # AI 개발 가이드
├── API_DOCUMENTATION.md           # API 명세
├── FRONTEND_DEVELOPER_GUIDE.md    # 프론트엔드 가이드
├── docs/                          # 기술 문서
│   ├── README.md (이 파일)
│   ├── tasks.md
│   └── websocket-stomp-setup.md
├── frontend-example/              # 예제 코드
│   └── README.md
├── k6/                           # 성능 테스트
│   └── README.md
└── portfolio-docs/               # 포트폴리오 문서
    ├── README.md
    └── *.md (개선 사항들)
```