# FlowCheck

> 부트캠프 수강생 학습 진척도 관리 플랫폼

**서비스:** -

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Backend | Java 17, Spring Boot 3.x, Spring Data JPA, Hibernate 6 |
| Frontend | Thymeleaf, Bootstrap 5, Chart.js |
| Database | MySQL 8.0 (AWS RDS) |
| Auth | Google OAuth 2.0, JWT (HttpOnly 쿠키) |
| Infra | AWS EC2 (t2.micro), Docker, GitHub Actions CI/CD |
| 보안 | HTTPS (Let's Encrypt + Nginx) |
| 외부 API | Google Sheets API v4, Google Drive API |

---

## 아키텍처

```
GitHub
  ↓ push
GitHub Actions (Gradle 빌드 → Docker 이미지)
  ↓ SCP
AWS EC2 (Ubuntu, t2.micro + Swap 1GB)
  ↓ JDBC
AWS RDS MySQL 8.0
```

---

## ERD

```
PM ──< PM_TRACK >── TRACK ──< STUDENT
                      │
                   COURSE ──< COURSE_WEEK
                      │
              LEARNING_PROGRESS

TRACK ──< GOOGLE_FORM
TRACK ──< NPS_RESULT
TRACK ──< SATISFACTION_RESULT
```

---

## 프로젝트 구조

```
src/main/java/com/bootcamp/flowcheck/
├── domain/
│   ├── auth/           # PM 엔티티, 레포지토리
│   ├── course/         # 과정, 주차 관리
│   ├── googleform/     # 구글폼 관리
│   ├── nps/            # NPS 다면평가
│   ├── progress/       # 학습 진척도
│   ├── satisfaction/   # 만족도 분석
│   ├── student/        # 수강생 관리
│   └── track/          # 트랙, PM-트랙 매핑
├── global/
│   ├── config/         # Security, CORS 설정
│   ├── exception/      # 전역 예외 처리
│   ├── jwt/            # JWT 인증 필터
│   ├── oauth/          # Google OAuth 성공 핸들러
│   └── sheets/         # Google Sheets/Drive 서비스
└── web/
    ├── WebController.java     # Thymeleaf 페이지 컨트롤러
    └── AdminController.java   # 관리자 API
```

---

## 주요 기능

### 인증
- Google OAuth 2.0 로그인
- JWT 발급 → HttpOnly 쿠키 저장
- JwtAuthenticationFilter로 Stateless 인증

### 학습 진척도
- 수강생 설문 제출 (인증 불필요, URL 접속)
- 위험도 자동 분류: 즉시면담 / 주의 / 관찰 / 정상
- PM 대시보드: 즉시면담 알림, 오늘 제출 현황, 난이도 분포

### Google Sheets 연동
- Service Account 방식 인증 (credentials.json)
- 스프레드시트 ID로 응답 수 실시간 조회
- Google Drive API로 스프레드시트 자동 공유

### 데이터 분석
- 만족도: 엑셀 업로드 → 키워드 기반 헤더 자동 매핑 → 커리큘럼/강의/운영 3섹션 분석
- NPS: 엑셀 업로드 → NPS 지수 계산, 분포 바, 항목별 평균, 수강생별 상세

### 소프트 딜리트
- 모든 엔티티 `deleted_at` 컬럼으로 논리 삭제
- 관리자 페이지에서 삭제 데이터 복구 가능

---

## CI/CD

`main` 브랜치 push 시 자동 실행:

```yaml
1. Gradle bootJar 빌드
2. Docker 이미지 빌드 (commit SHA 태그)
3. SCP로 EC2에 이미지 전송
4. SSH로 EC2 접속
5. 기존 컨테이너 중지/제거
6. 새 컨테이너 실행 (환경변수 주입)
```

민감 정보는 GitHub Secrets 관리 → 런타임 환경변수 주입

---

## 로컬 실행

### 사전 요구사항
- Java 17
- MySQL 8.0
- Google OAuth 클라이언트 ID/Secret

### 환경 설정

```yaml
# src/main/resources/application-local.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/flowcheck
    username: root
    password: your_password
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_CLIENT_ID
            client-secret: YOUR_CLIENT_SECRET
jwt:
  secret: YOUR_JWT_SECRET
```

### 실행

```bash
./gradlew bootRun
```

---

## 트러블슈팅

| 문제 | 원인 | 해결 |
|------|------|------|
| EC2→RDS 연결 실패 | RDS 보안 그룹에 EC2 미등록 | AWS 콘솔 EC2-RDS 연결 설정 |
| 컨테이너 자동 종료 | t2.micro OOM (RAM 908MB) | EC2 Swap 1GB 추가 |
| 테이블명 대소문자 불일치 | Linux MySQL 대소문자 구분, Hibernate 6 소문자 변환 | PhysicalNamingStrategyStandardImpl 적용 |
| JWT 검증 실패 | GitHub Secret 값 끝 줄바꿈(`\n`) 포함 | Secret 재저장 |
| Thymeleaf 500 에러 | 3.1부터 `#request` 사용 금지 | Controller에서 `baseUrl` model 주입 |
| 모든 페이지 JSON 반환 | `@RestControllerAdvice`가 `@Controller`까지 처리 | `GlobalMvcExceptionHandler` 분리, `@Order` 설정 |
| Google Sheets 인증 실패 | credentials.json Docker 볼륨 미마운트 | deploy.yml에 `-v` 마운트 추가, Base64 Secret 관리 |
