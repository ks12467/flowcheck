package com.bootcamp.flowcheck.domain.nps.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NpsAiReportResponse {

    // ── 1. 제출현황 ──────────────────────────────────────────────────────────
    private int totalStudents;
    private int submittedCount;
    private List<String> notSubmitted;

    // ── 2. NPS 분류 원본 데이터 ───────────────────────────────────────────────
    private List<NpsStudentEntry> detractors;   // 0~6
    private List<NpsStudentEntry> passives;     // 7~8
    private List<NpsStudentEntry> promoters;    // 9~10

    // ── 3. NPS 변동 인원 ──────────────────────────────────────────────────────
    private boolean hasPreviousUpload;
    private String previousUploadName;
    private List<NpsChange> declined;
    private List<NpsChange> improved;

    // ── Gemini 분석 텍스트 (마크다운) ─────────────────────────────────────────
    private String detractorAnalysis;
    private String passiveAnalysis;
    private String promoterAnalysis;
    private String npsChangeAnalysis;
    private String difficultyAnalysis;
    private String sscStart;
    private String sscStop;
    private String sscContinue;

    // ── 내부 클래스 ───────────────────────────────────────────────────────────

    @Getter
    @AllArgsConstructor
    public static class NpsStudentEntry {
        private String name;
        private Integer nps;
        private Integer opsSatisfaction;
        private String npsComment;
        private String opsComment;
    }

    @Getter
    @AllArgsConstructor
    public static class NpsChange {
        private String name;
        private Integer previousNps;
        private Integer currentNps;
    }
}
