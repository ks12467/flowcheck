-- ── 운영 DB 마이그레이션 SQL (ddl-auto: none 환경에서 수동 실행) ──────────────────
-- 최초 배포 또는 신규 테이블 추가 시 아래 스크립트를 운영 DB에 수동 적용하세요.

-- NPS 다면평가 결과 테이블 (ed54331 커밋에서 추가)
CREATE TABLE IF NOT EXISTS NPS_RESULT (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    track_id               BIGINT       NOT NULL,
    name                   VARCHAR(255) NOT NULL,
    `class`                VARCHAR(255) NULL,
    specialty              VARCHAR(255) NULL,
    difficulty             INT          NULL,
    skill                  INT          NULL,
    growth                 INT          NULL,
    communication          INT          NULL,
    immersion              INT          NULL,
    ops_satisfaction       INT          NULL,
    curriculum_satisfaction INT         NULL,
    nps                    INT          NULL,
    nps_comment            VARCHAR(2000) NULL,
    ops_comment            VARCHAR(2000) NULL,
    uploaded_at            DATETIME     NULL,
    upload_name            VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_nps_result_track FOREIGN KEY (track_id) REFERENCES TRACK (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 주차별 만족도 결과 테이블 (ed54331 커밋에서 추가)
CREATE TABLE IF NOT EXISTS SATISFACTION_RESULT (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    track_id                BIGINT       NOT NULL,
    upload_name             VARCHAR(255) NOT NULL,
    respondent_name         VARCHAR(255) NULL,
    curriculum_satisfaction INT          NULL,
    curriculum_difficulty   INT          NULL,
    lecture_delivery        INT          NULL,
    lecture_material        INT          NULL,
    lecture_quality         INT          NULL,
    lecture_difficulty      INT          NULL,
    lecture_nps             INT          NULL,
    assignment_satisfaction INT          NULL,
    scrum_score             INT          NULL,
    tutor_help_score        INT          NULL,
    tutor_meet_score        INT          NULL,
    manager_score           INT          NULL,
    feedback_speed_score    INT          NULL,
    brief_score             INT          NULL,
    free_comment            VARCHAR(2000) NULL,
    operation_comment       VARCHAR(2000) NULL,
    uploaded_at             DATETIME     NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_satisfaction_result_track FOREIGN KEY (track_id) REFERENCES TRACK (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 트랙별 설문 문항 테이블 (a8d3877 커밋에서 추가 — 설문 공유 링크에 필요)
CREATE TABLE IF NOT EXISTS SURVEY_QUESTION (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    track_id       BIGINT       NOT NULL,
    question_order INT          NOT NULL,
    question_text  VARCHAR(500) NOT NULL,
    question_type  VARCHAR(10)  NOT NULL,
    min_score      INT          NULL,
    max_score      INT          NULL,
    created_at     DATETIME     NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_survey_question_track FOREIGN KEY (track_id) REFERENCES TRACK (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
